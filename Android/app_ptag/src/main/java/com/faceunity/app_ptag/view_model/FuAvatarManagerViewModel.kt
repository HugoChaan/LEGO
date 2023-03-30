package com.faceunity.app_ptag.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceunity.app_ptag.ui.home.entity.state.ExceptionEvent
import com.faceunity.app_ptag.ui.home.entity.state.LoadingState
import com.faceunity.app_ptag.use_case.FuUseCaseFactory
import com.faceunity.app_ptag.use_case.download.SmartPrepareAvatarUseCase
import com.faceunity.app_ptag.use_case.render_kit.SmartCheckAvatarStateUseCase
import com.faceunity.app_ptag.util.SingleLiveEvent
import com.faceunity.app_ptag.view_model.helper.FuDownloadUIHelper
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadAvatarConfigUseCase
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.pta.pta_core.model.AvatarInfo
import kotlinx.coroutines.launch

/**
 * Avatar 管理的辅助 ViewModel。
 * - Avatar 列表 的添加、删除
 * - 根据 Avatar ID，下载相关配置
 * - 切换 Avatar，并视情况下载相关资源。
 * tip：旧版 FuAvatarManagerViewModel 职责过重，新版仅负责 形象管理。更多业务通过相关 UseCase 实现。
 */
class FuAvatarManagerViewModel : ViewModel() {

    private val _avatarCollectionLiveData = SingleLiveEvent<Collection<AvatarInfo>>()
    val avatarCollectionLiveData: LiveData<Collection<AvatarInfo>> get() = _avatarCollectionLiveData

    private val _avatarSelectLiveData = SingleLiveEvent<String>()
    val avatarSelectLiveData: LiveData<String> get() = _avatarSelectLiveData


    //region 重构后的接口

    private val downloadUiHelper = FuDownloadUIHelper()

    val loadingState = downloadUiHelper.getLoadingState()
    private fun notifyLoadingState(loadingState: LoadingState) {
        downloadUiHelper.notifyLoadingState(loadingState)
    }

    val exceptionEvent = downloadUiHelper.getExceptionEvent()
    private fun notifyExceptionEvent(exceptionEvent: ExceptionEvent) {
        downloadUiHelper.notifyExceptionEvent(exceptionEvent)
    }
    fun finishExceptionEvent() {
        downloadUiHelper.finishExceptionEvent()
    }


    fun requestAvatarContainer() {
        syncAvatarContainer()
    }

    /** 是否初始化形象管理。正常来说一次运行只用初始化一次。 */
//    private var isInitAvatarManager
//        get() = FuDI.getMemoryProperty("isInitAvatarManager", true)
//        set(value) = FuDI.setMemoryProperty("isInitAvatarManager", value)

//    private fun initAvatarManager() {
//        DevAvatarManagerRepository.loadAvatar()
//        DevAvatarManagerRepository.mapAvatar { //将每个形象与其动画绑定。
//            DevSceneManagerRepository.filterGenderDefaultSceneFirst(it.gender())?.run {
//                DevSceneManagerRepository.setSceneConfig(it.avatar, sceneResource)
//            }
//        }
//        syncAvatarContainer() //同步 Avatar 列表的 UI
//    }

    private val downloadAvatarConfigUseCase by lazy {
        FuUseCaseFactory.downloadAvatarConfigUseCase
    }

    /**
     * 尝试加载该 [avatarId]。它会在第一次被调用时初始化一些信息，并判断相关状态，加载该 Avatar。
     */
    fun smartLoadAvatar(avatarId: String) {
//        if (isInitAvatarManager) {
//            initAvatarManager()
//            isInitAvatarManager = false
//        }
        val avatarState = SmartCheckAvatarStateUseCase(avatarId)
        when(avatarState) {
            SmartCheckAvatarStateUseCase.AvatarState.LackDir -> { //找不到 avatarId 对应的文件夹
                //根据 Avatar Id 下载对应文件，并添加到 DevAvatarManagerRepository 中，然后再切换该 Avatar。
                viewModelScope.launch {
                    notifyLoadingState(LoadingState.ShowWithTip("正在下载形象配置"))
                    downloadAvatarConfigUseCase(DownloadAvatarConfigUseCase.Params(avatarId)).onSuccess {
                        notifyLoadingState(LoadingState.Hidden)
                        val newAvatarInfo =
                            DevAvatarManagerRepository.createAvatarInfoUseConfig(avatarId)
                        if (newAvatarInfo == null) {
                            notifyExceptionEvent(ExceptionEvent.FailedToast("Avatar 配置读取异常：$it"))
                            return@onSuccess
                        }
                        DevAvatarManagerRepository.addAvatarAndSwitch(newAvatarInfo)
                        smartSwitchAvatar(avatarId)
                    }.onFailure {
                        notifyExceptionEvent(ExceptionEvent.FailedToast("Avatar 配置下载异常：$it"))
                    }
                }
            }
            SmartCheckAvatarStateUseCase.AvatarState.LackJson, SmartCheckAvatarStateUseCase.AvatarState.LackIcon -> { //缺少 avatar.json 或者 avatar.png
                //根据 Avatar Id 下载对应文件，然后再切换该 Avatar。
                viewModelScope.launch {
                    notifyLoadingState(LoadingState.ShowWithTip("正在下载形象配置"))
                    downloadAvatarConfigUseCase(DownloadAvatarConfigUseCase.Params(avatarId)).onSuccess {
                        notifyLoadingState(LoadingState.Hidden)
                        val avatarInfo = DevAvatarManagerRepository.getAvatarInfo(avatarId)!!
                        DevAvatarManagerRepository.flushAvatarInfoUseConfig(avatarInfo)
                        smartSwitchAvatar(avatarId)
                    }.onFailure {
                        notifyExceptionEvent(ExceptionEvent.FailedToast("Avatar 配置下载异常：$it"))
                    }
                }
            }
            SmartCheckAvatarStateUseCase.AvatarState.Normal -> { //形象状态正常
                smartSwitchAvatar(avatarId)
            }
        }
    }

    /**
     * 切换一个 [avatarId] 所代表的 Avatar。
     * Avatar 应该是一个构造就绪的状态（拥有完整的道具和动画）。该方法仅检查是否需要下载相关 Bundle。
     */
    fun smartSwitchAvatar(avatarId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val avatarInfo = prepareAvatar(avatarId).getOrElse {
                notifyExceptionEvent(ExceptionEvent.FailedToast("Avatar 准备失败：$it"))
                return@launch
            }
            switchLocalAvatar(avatarId).getOrElse {
                notifyExceptionEvent(ExceptionEvent.FailedToast("Avatar 切换失败：$it"))
            }
            syncAvatarContainer()
            onSuccess?.invoke()
            FuLog.debug("切换Avatar $avatarId 成功")
        }
    }


    /**
     * 解析该 AvatarInfo 的资源状态，未就绪则会构建并下载对应资源。最后返回一个可直接显示的 AvatarInfo。
     */
    private suspend fun prepareAvatar(avatarId: String): Result<AvatarInfo> {
        return SmartPrepareAvatarUseCase(avatarId) {
            downloadUiHelper.parseDownloadFlow(
                this,
                { smartSwitchAvatar(avatarId) })
        }
    }

    /**
     * 尝试渲染一个资源已经就绪的 Avatar，成功则切换，失败则不处理。
     */
    private fun switchLocalAvatar(avatarId: String) : Result<Avatar> {
        if (!DevDrawRepository.isScenePrepare()) return Result.failure(Throwable("Scene isn't prepare, please call setUseScene()."))
        val avatarInfo = DevAvatarManagerRepository.switchAvatarAndGet(avatarId)
        if (avatarInfo == null) return Result.failure(Throwable("AvatarManager not find avatar id $avatarId."))
        return avatarInfo.runCatching {
            DevDrawRepository.replaceCurrentAvatarByCompare(avatar)
            DevDrawRepository.setUseScene(DevDrawRepository.currentScene!!) //临时每次都显示设置一次渲染 Scene
            avatar
        }
    }


    /**
     * 删除 Avatar
     */
    fun removeAvatar(avatarId: String) {
        val avatarInfo = DevAvatarManagerRepository.getAvatarInfo(avatarId) ?: return
        val allAvatarId = DevAvatarManagerRepository.mapAvatar { it.id }.toMutableList()
        val currentAvatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo()
        DevAvatarManagerRepository.deleteAvatar(avatarInfo)
        if (currentAvatarInfo == avatarInfo) { //如果选中当前被删除的 Avatar，则选中上一个
            val selectIndex = allAvatarId.indexOfFirst { avatarInfo.id == it }
            val switchAvatarId = allAvatarId.getOrNull(if (selectIndex > 0) selectIndex - 1 else 1) ?: DevAvatarManagerRepository.getFirstAvatarId()
            if (switchAvatarId != null) {
                smartSwitchAvatar(switchAvatarId)
            } else {
                notifyExceptionEvent(ExceptionEvent.FailedToast("找不到可以切换的 Avatar"))
            }

        }
        syncAvatarContainer()

    }

    //endregion 重构后的接口



    //region LiveData 通知

    /**
     * 构建 AvatarList 的容器，用于页面渲染
     */
    private fun syncAvatarContainer() {
        _avatarCollectionLiveData.postValue(DevAvatarManagerRepository.mapAvatar {it})
        DevAvatarManagerRepository.getCurrentAvatarId()?.let {
            _avatarSelectLiveData.postValue(it)
        }
    }

    //endregion LiveData 通知


}