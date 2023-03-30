package com.faceunity.app_ptag.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.use_case.FuUseCaseFactory
import com.faceunity.app_ptag.use_case.download.BuildAvatarByJsonUseCase
import com.faceunity.core.avatar.model.Scene
import com.faceunity.editor_ptag.business.pta.IPhotoToAvatarSyncControl
import com.faceunity.editor_ptag.business.pta.config.RequestPhotoToAvatarParams
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadAvatarConfigUseCase
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.editor_ptag.util.safeCollect
import com.faceunity.pta.pta_core.data_build.FuBundleDataBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 照片生成形象的 ViewModel
 * - 根据图片生成对应的 avatar.json
 */
class FuBuildAvatarViewModel : ViewModel() {

    private val ptaControl: IPhotoToAvatarSyncControl
        get() = FuDevDataCenter.ptaControl!!

    private var cachePhotoPath: String? = null
    private val _photoPathLiveData = MutableLiveData<String>()
    val photoPathLiveData: LiveData<String> get() = _photoPathLiveData

    private val _isInitPTASuccessLiveData = MutableLiveData<Boolean>()
    val isInitPTASuccessLiveData: LiveData<Boolean> get() = _isInitPTASuccessLiveData


    /**
     * 初始化 PTA 需要的资源。资源路径由 [FuDevDataCenter.resourcePath] 配置，需要其中的 [ossBuildAvatarInfoList()] 与 [binBuildAvatarFileList()]
     * 初始化信息会通知 [isInitPTASuccessLiveData]。
     */
    fun initPTAResource() {
        viewModelScope.launch {
            ptaControl.initPTAResource().onSuccess {
                _isInitPTASuccessLiveData.postValue(true)
                FuLog.info("初始化 Photo To Avatar 成功 ")
            }.onFailure {
                _isInitPTASuccessLiveData.postValue(false)
                FuLog.error("初始化 Photo To Avatar 异常")
            }
        }
    }

    /**
     * 配置形象生成需要使用的图片文件
     * 成功后会通知 [photoPathLiveData]
     */
    fun cachePhoto(photoFile: File) {
        cachePhotoPath = photoFile.path
        _photoPathLiveData.postValue(cachePhotoPath!!)
    }

    /**
     * 清除在 [cachePhoto] 中配置的图片文件
     */
    fun clearCachePhoto() {
        cachePhotoPath = null
    }

    private val saveAvatarIconUseCase by lazy {
        FuUseCaseFactory.saveAvatarIconUseCase
    }

    private val downloadAvatarConfigUseCase by lazy {
        FuUseCaseFactory.downloadAvatarConfigUseCase
    }

    /**
     * 根据传入配置生成形象并切换。
     */
    suspend fun buildAndLoadAvatarThenSwitch(isMale: Boolean): Result<String> {
        var ret: Result<String>? = null
        buildAndLoadAvatar(isMale, File(cachePhotoPath!!)).safeCollect {
            when(it) {
                is BuildAvatarState.Success -> {
                    downloadAvatarConfigUseCase(DownloadAvatarConfigUseCase.Params(it.avatarId)).getOrThrow()
                    val avatarInfo = DevAvatarManagerRepository.createAvatarInfoUseConfig(it.avatarId)
                    DevAvatarManagerRepository.addAvatarAndSwitch(avatarInfo!!)
                    ret = Result.success(it.avatarId)
                }
                is BuildAvatarState.Failed -> {
                    ret = Result.failure(it.throwable)
                }
            }
        }
        return ret ?: Result.failure(Throwable("buildAndLoadAvatar collect error"))
    }

    /**
     * 根据传入配置，生成一个 Avatar 并在本地构造一个缩略图（因此需要下载 Bundle），然后将其上传至云服务获取 Avatar ID 并返回。
     */
    suspend fun buildAndLoadAvatar(isMale: Boolean, photoFile: File): Flow<BuildAvatarState> = flow {
        //生成与照片对应的 avatar.json
        val json = ptaControl.requestPhotoToAvatar(RequestPhotoToAvatarParams(
            gender = if (isMale) RequestPhotoToAvatarParams.Gender.Male else RequestPhotoToAvatarParams.Gender.Female,
            photoFile = photoFile
        )).getOrElse {
            emit(BuildAvatarState.Failed(it))
            return@flow
        }
        //构建出 avatar.json 对应的 Avatar。不包含动画与配置文件，可能存在部分 Bundle 未下载。
        val avatar = FuUseCaseFactory.buildAvatarByJsonUseCase(
            BuildAvatarByJsonUseCase.Params(
                avatarJson = json
            )
        ).getOrElse {
            emit(BuildAvatarState.Failed(it))
            return@flow
        }
        //下载 Avatar 身上的 Bundle
        FuUseCaseFactory.downloadAvatarBundleUseCase(avatar).safeCollect {  }
        //因为生成头像需要一个特殊的相机，故也需要下载
        DevSceneManagerRepository.filterGenderDefaultSceneFirst(if (isMale) "male" else "female")?.sceneResource?.cameraList?.let {
            FuUseCaseFactory.downloadBundleUseCase(it.map { it.path }).safeCollect {  }
        }
        //生成该 Avatar 的头像
        val iconFile = File(FuDI.getContext().externalCacheDir, "tempImage${System.currentTimeMillis()}.png")
        val gender = if (isMale) "male" else "female"
        val customScene: (Scene.() -> Unit) = {
            DevSceneManagerRepository.filterGenderDefaultSceneFirst(gender)?.let {
                it.sceneResource.light?.let {
                    this.setLightingBundle(FuBundleDataBuilder.buildFuBundleData(FuDevDataCenter.resourcePath.ossBundle(it), it))
                }
            }
        }
        saveAvatarIconUseCase.execute(
            avatar,
            gender,
            iconFile.path,
            customScene
        )
        //上传 avatar.json 与对应头像，获得一个 Avatar ID 并返回。
        val avatarId = FuUseCaseFactory.uploadAvatarConfigUseCase.uploadCloud(
            json,
            iconFile
        ).getOrElse {
            emit(BuildAvatarState.Failed(it))
            return@flow
        }

        emit(BuildAvatarState.Success(avatarId))
    }

    sealed class BuildAvatarState {

        data class Success(val avatarId: String): BuildAvatarState()

        data class Failed(val throwable: Throwable): BuildAvatarState()
    }

    /**
     * 释放形象生成库的资源
     */
    fun releasePTAResource() {
        ptaControl.releasePTAResource()
    }
}