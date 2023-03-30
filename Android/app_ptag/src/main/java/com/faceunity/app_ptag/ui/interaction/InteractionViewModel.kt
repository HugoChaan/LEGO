package com.faceunity.app_ptag.ui.interaction

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.FuDevInitializeWrapper
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.StyleRecordInterface
import com.faceunity.app_ptag.ui.interaction.entity.AnimationConfig
import com.faceunity.app_ptag.ui.interaction.entity.EmotionConfig
import com.faceunity.app_ptag.ui.interaction.network.InteractionCloudSyncRepository
import com.faceunity.app_ptag.ui.interaction.tag.FuTagManager
import com.faceunity.app_ptag.ui.interaction.tag.parser.FuAnimationTagParser
import com.faceunity.app_ptag.ui.interaction.weight.animation.AnimationUiState
import com.faceunity.app_ptag.ui.interaction.weight.emotion.EmotionUiState
import com.faceunity.app_ptag.use_case.FuUseCaseFactory
import com.faceunity.app_ptag.util.SingleLiveEvent
import com.faceunity.core.avatar.entity.FUSpriteData
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.core.avatar.model.Scene
import com.faceunity.core.avatar.model.Sprite2D
import com.faceunity.core.entity.FUImageData
import com.faceunity.core.entity.FUSpriteDescData
import com.faceunity.core.enumeration.FUImageFormatEnum
import com.faceunity.core.enumeration.FUSprite2DTypeEnum
import com.faceunity.core.enumeration.FUTextureScaleTypeEnum
import com.faceunity.editor_ptag.business.sta.SpeechToAnimationWrapper
import com.faceunity.editor_ptag.business.sta.callback.SpeechCallback
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.data_center.FuTokenObserver
import com.faceunity.editor_ptag.parser.IFuJsonParser
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadBundleUseCase
import com.faceunity.editor_ptag.util.FuElapsedTimeChecker
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.editor_ptag.util.FuResourceCheck
import com.faceunity.editor_ptag.util.checkBundleStatus
import com.faceunity.fupta.cloud_download.CloudResourceManager
import com.faceunity.fupta.cloud_download.entity.CloudResource
import com.faceunity.fupta.cloud_download.parser.IFuAPIParser
import com.faceunity.pta.pta_core.model.AvatarInfo
import com.faceunity.toolbox.media.FUMediaUtils
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class InteractionViewModel : ViewModel() {

    private val staControl: SpeechToAnimationWrapper
        get() = FuDevDataCenter.staControl!!

    //region 绘制接口

    private val _drawFinishLiveData = SingleLiveEvent<Boolean>()
    val drawFinishLiveData: LiveData<Boolean> get() = _drawFinishLiveData

    private var scene: Scene? = null
    private var isFirstInit = true

    fun drawAvatar() {
        viewModelScope.launch {
            //创建一个用于互动页面的 Scene，此处可自定义一些额外配置
            if (scene == null) {
                scene = FuUseCaseFactory.smartCreateSceneUseCase()
                if (scene == null) { //如果创建后仍为 null 表示所需数据不全。
                    _drawFinishLiveData.postValue(true)
                    return@launch
                }
            }
            if (isFirstInit) {
                scene!!.apply {
                    FuDevInitializeWrapper.sceneCustom(this)
                    DevDrawRepository.currentScene = this
                }
                //进入互动页面对所有 Avatar 进行一个重建
                FuElapsedTimeChecker.start("RebuildAllAvatar")
                DevAvatarManagerRepository.mapAvatar {
                    if (it.state == AvatarInfo.State.PrepareAvatar) {
                        FuUseCaseFactory.rebuildAvatarUseCase(it)
                        DevSceneManagerRepository.filterGenderDefaultSceneFirst(it.gender())?.run {
                            DevSceneManagerRepository.setAvatarDefaultAnimation(it.avatar, sceneResource) //根据互动页需求，先绑定一个默认呼吸。
                        }
                    }

                }
                FuElapsedTimeChecker.end("RebuildAllAvatar")
//                val currentAvatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo()
//                if (currentAvatarInfo != null) {
//                    DevDrawRepository.replaceCurrentAvatar(currentAvatarInfo.avatar)
//                }
            }

//            DevDrawRepository.setUseScene(scene!!)
            isFirstInit = false
            _drawFinishLiveData.postValue(true)
        }
    }

    fun setCustomSceneBackground(bitmap: Bitmap) {
        FUMediaUtils.readRgbaByteFromBitmap(bitmap)?.let {
            val descData = FUSpriteDescData(FUImageData(
                    FUImageFormatEnum.RGBA,
                    bitmap.width,
                    bitmap.height,
                    it,
                    bitmap.width * 4
                ))
            DevDrawRepository.getSceneOrNull()?.sprite?.addSprite(Sprite2D().apply {
                setSpriteDesc(FUSprite2DTypeEnum.BACKGROUND, descData)
            })
        }
    }

    //endregion 绘制接口


    //region 网络接口


    fun sendAnimationTts(content: String) {
        val controlAvatar = DevAvatarManagerRepository.getCurrentAvatarInfo() ?: return
        val animationTagParser = FuAnimationTagParser(controlAvatar.avatar, controlAvatar.gender())
        FuTagManager.addParser(animationTagParser)
        staControl.startSpeech(content, "Kenny", includeTag = true, speechCallback = object : SpeechCallback {
            override fun onStart(avatar: Avatar?) {

            }

            override fun onFinish(avatar: Avatar?) {
                FuTagManager.removeParser(animationTagParser)
            }

            override fun onPhonemeAction(content: String) {
                FuLog.debug("onPhonemeAction:$content")
                FuTagManager.action(content)
            }
        })
    }

    private val interactionCloudRepo: InteractionCloudSyncRepository by lazy {
        FuDI.getCustom<InteractionCloudSyncRepository>().apply {
            FuDevDataCenter.tokenObserver.addTokenObserver(object : FuTokenObserver.TokenObserver("sta") {
                override fun onTokenUpdate(token: String) {
                    this@apply.token = token
                }
            })
        }
    }

    suspend fun init(coroutineContext: CoroutineContext) {
        withContext(coroutineContext) {
            val job1 = async {
                requestAnimationConfig()
            }
            val job2 = async {
                requestTagConfig()
            }
            job1.await()
            job2.await()
            buildInitAnimationUiState()
        }
    }

    private val jsonParser: IFuAPIParser by lazy {
        FuDI.getCustom()
    }


    private suspend fun requestAnimationConfig() {
        interactionCloudRepo.animation().onSuccess {
            jsonParser.toJson(it.data.AnimationConfig).onSuccess {
                FuDevDataCenter.fastWriteString(it, {appAnimationConfig()})
            }
            jsonParser.toJson(it.data.EmoticonConfig).onSuccess {
                FuDevDataCenter.fastWriteString(it, {appEmotionConfig()})
            }
        }.onFailure {
            FuLog.error(it.toString())
        }
    }

    private suspend fun requestTagConfig() {
        interactionCloudRepo.actionTag().onSuccess {
            jsonParser.toJson(it.data.TagConfig).onSuccess {
                FuDevDataCenter.fastWriteString(it, {appTagConfig()})
            }
        }.onFailure {
            FuLog.error(it.toString())
        }
    }


    //endregion 网络接口


    /**
     * 根据当前的形象状态和资源状态，构造一个 [AnimationUiState]。
     */
    private fun buildInitAnimationUiState() {
        val avatarId = DevAvatarManagerRepository.getCurrentAvatarId() ?: return
        val avatarInfo = DevAvatarManagerRepository.getAvatarInfo(avatarId) ?: return
        val gender = avatarInfo.gender()
        val animList = animationConfig.map[gender] ?: emptyList()
        val expList = emotionConfig.map[gender] ?: emptyList()
        viewModelScope.launch {
            _animationUiState.update {
                it.copy(
                    list = animList,
                    downloadStateMap = buildDownloadStyle(animList.map { it.path }),
                    selectItem = null
                )
            }
            _emotionUiState.update { it.copy(
                list = expList,
                downloadStateMap = buildDownloadStyle(expList.map { it.path }),
                selectItem = null
            ) }
        }
    }

    fun downloadTtsAnimation(): Flow<DownloadBundleUseCase.State> {
        val allTagFileIdList = FuTagManager.tagAnimMap.values.map { it.pathMap.values }.flatten().toSet()
        return downloadBundleUseCase(allTagFileIdList)
    }

    private fun buildDownloadStyle(fileIdList: Collection<String>): MutableMap<String, StyleRecordInterface.DownloadStyle> {
        val downloadStyleMap = mutableMapOf<String, StyleRecordInterface.DownloadStyle>()
        fileIdList.forEach { path ->
            downloadStyleMap[path] = when (cloudResourceManager.getCloudResourceStatus(path)) {
                CloudResource.Status.Normal -> StyleRecordInterface.DownloadStyle.Normal
                CloudResource.Status.NeedDownload, CloudResource.Status.NeedUpdate -> StyleRecordInterface.DownloadStyle.NeedDownload
                CloudResource.Status.Lose -> StyleRecordInterface.DownloadStyle.Error
                else -> StyleRecordInterface.DownloadStyle.Error
            }
        }
        return downloadStyleMap
    }

    fun onSwitchAvatar(avatarId: String) {
        val avatarInfo = DevAvatarManagerRepository.getAvatarInfo(avatarId) ?: return
        val gender = avatarInfo.gender()
        val animList = animationConfig.map[gender] ?: emptyList()
        val expList = emotionConfig.map[gender] ?: emptyList()
        viewModelScope.launch {
            _animationUiState.update {
                it.copy(
                    list = animList,
                    downloadStateMap = buildDownloadStyle(animList.map { it.path })
                )
            }
            _emotionUiState.update { it.copy(
                list = expList,
                downloadStateMap = buildDownloadStyle(expList.map { it.path })
            ) }
        }
    }

    fun flushAnimationPageDownloadState() {
        viewModelScope.launch {
            _animationUiState.update {
                it.copy(
                    downloadStateMap = buildDownloadStyle(it.list.map { it.path })
                )
            }
        }
    }

    private val downloadBundleUseCase by lazy {
        FuUseCaseFactory.downloadBundleUseCase
    }

    private val cloudResourceManager by lazy {
        FuDI.getCustom<CloudResourceManager>()
    }

    suspend fun playAnimation(animationItem: AnimationConfig.Item) {
        val avatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo() ?: return
        val gender = avatarInfo.gender()
        val avatar = avatarInfo.avatar
        val fileId = animationItem.path
        _animationUiState.update {
            it.copy(
                selectItem = animationItem
            )
        }
        val bundleStatus = FuResourceCheck.checkBundleStatus(fileId, cloudResourceManager)
        if (bundleStatus.isNotPrepare) {
            downloadBundleUseCase(listOf(fileId)).onStart {
                updateAnimDownloadUiState(fileId, StyleRecordInterface.DownloadStyle.Downloading)
            }.onCompletion {
                updateAnimDownloadUiState(fileId, StyleRecordInterface.DownloadStyle.Normal)
            }.catch {
                updateAnimDownloadUiState(fileId, StyleRecordInterface.DownloadStyle.Error)
            }.collect()
        }
        val fullPath = animationItem.path.let {
            FuDevDataCenter.resourceManager.path.ossBundle(it)
        }
        FuAnimationTagParser.playAnim(fullPath, avatar) {
            DevSceneManagerRepository.setAvatarDefaultAnimation(avatar, gender)
        }
    }

    private fun updateAnimDownloadUiState(fileId: String, style: StyleRecordInterface.DownloadStyle) {
        _animationUiState.update { it.copy(
            downloadStateMap = it.downloadStateMap.toMutableMap().apply {
                this[fileId] = style
            }
        ) }
    }

    suspend fun playEmotionAnimation(expItem: EmotionConfig.Item) {
        val avatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo() ?: return
        val gender = avatarInfo.gender()
        val avatar = avatarInfo.avatar
        val fileId = expItem.path
        _emotionUiState.update {
            it.copy(
                selectItem = expItem
            )
        }
        val bundleStatus = FuResourceCheck.checkBundleStatus(fileId, cloudResourceManager)
        if (bundleStatus.isNotPrepare) {
            downloadBundleUseCase(listOf(fileId)).onStart {
                updateEmotionDownloadUiState(fileId, StyleRecordInterface.DownloadStyle.Downloading)
            }.onCompletion {
                updateEmotionDownloadUiState(fileId, StyleRecordInterface.DownloadStyle.Normal)
            }.catch {
                updateEmotionDownloadUiState(fileId, StyleRecordInterface.DownloadStyle.Error)
            }.collect()
        }
        val fullPath = expItem.path.let {
            FuDevDataCenter.resourceManager.path.ossBundle(it)
        }
        FuAnimationTagParser.playEmotionAnim(fullPath, avatar) {
//            DevSceneManagerRepository.setAvatarDefaultAnimation(avatar, gender)
        }
    }

    private fun updateEmotionDownloadUiState(fileId: String, style: StyleRecordInterface.DownloadStyle) {
        _emotionUiState.update { it.copy(
            downloadStateMap = it.downloadStateMap.toMutableMap().apply {
                this[fileId] = style
            }
        ) }
    }

    /**
     * 因为当前版本在 Avatar 释放后再执行其方法会 Crash。临时的解决办法是在释放前取消查询任务。
     * 后续版本会优化该机制。
     */
    fun tempCancelAnimTask() {
        FuAnimationTagParser.cancelAllTask()
    }

    private val _animationUiState = MutableStateFlow(AnimationUiState.default)
    val animationUiState: StateFlow<AnimationUiState> = _animationUiState.asStateFlow()

    private val animationConfig: AnimationConfig by lazy {
        FuDevDataCenter.fastLoadString { appAnimationConfig() }.let {
            FuDI.getCustom<IFuJsonParser>().parse<Map<String, List<AnimationConfig.Item>>>(it, object : TypeToken<Map<String, List<AnimationConfig.Item>>>() {}.type)
        }.let {
            AnimationConfig(it)
        }
    }

    private val _emotionUiState = MutableStateFlow(EmotionUiState.default)
    val emotionUiState: StateFlow<EmotionUiState> = _emotionUiState.asStateFlow()

    private val emotionConfig: EmotionConfig by lazy {
        FuDevDataCenter.fastLoadString { appEmotionConfig() }.let {
            FuDI.getCustom<IFuJsonParser>().parse<Map<String, List<EmotionConfig.Item>>>(it, object : TypeToken<Map<String, List<EmotionConfig.Item>>>() {}.type)
        }.let {
            EmotionConfig(it)
        }
    }

}