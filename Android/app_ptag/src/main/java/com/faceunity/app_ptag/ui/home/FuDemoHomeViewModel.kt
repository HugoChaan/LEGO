package com.faceunity.app_ptag.ui.home

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.FuDependencyInjection
import com.faceunity.app_ptag.FuDevInitializeWrapper
import com.faceunity.app_ptag.edit_cloud.IEditCloudControl
import com.faceunity.app_ptag.edit_cloud.entity.FuUpdateEditDataState
import com.faceunity.app_ptag.ui.home.entity.FuQrCodeResult
import com.faceunity.app_ptag.ui.home.entity.state.ExceptionEvent
import com.faceunity.app_ptag.ui.home.entity.state.LoadingState
import com.faceunity.app_ptag.use_case.FuUseCaseFactory
import com.faceunity.app_ptag.util.SingleLiveEvent
import com.faceunity.app_ptag.view_model.helper.FuDownloadUIHelper
import com.faceunity.core.avatar.model.Scene
import com.faceunity.editor_ptag.cache.FuCacheResource
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadAvatarConfigUseCase
import com.faceunity.editor_ptag.use_case.cloud_platform.InitCloudPlatformUseCase
import com.faceunity.editor_ptag.use_case.cloud_platform.UploadAvatarConfigUseCase
import com.faceunity.editor_ptag.use_case.render_kit.*
import com.faceunity.editor_ptag.util.FuElapsedTimeChecker
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.editor_ptag.util.FuResourceCheck
import com.faceunity.editor_ptag.util.ifTrue
import com.faceunity.fupta.avatar_data.entity.resource.merge
import com.faceunity.fupta.cloud_download.ICloudPlatformSyncAPI
import com.faceunity.fupta.cloud_download.getCacheTokenOrRequest
import com.faceunity.pta.pta_core.model.AvatarInfo
import com.faceunity.pta.pta_core.util.FuDefaultAvatarConfigurator
import com.faceunity.pta.pta_core.util.tryFill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 *
 */
class FuDemoHomeViewModel : ViewModel() {

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

    //region 云服务

    /**
     * 在 HomeViewModel 中，云服务是否就绪。
     */
    @Volatile private var isCloudPrepare = false

    /**
     * 本次运行中是否完成网络初始化。成功后整个 Application 中不会再进行网络初始化
     */
    private var isInitCloudFinish: Boolean
        set(value) = FuDependencyInjection.setMemoryProperty("isInitCloudFinish", value)
        get() = FuDependencyInjection.getMemoryProperty("isInitCloudFinish", false)

    /**
     * 根据不同的环境，有不同的 ItemListVersion 和初始化情况。
     */
    private val CloudConfigKey = "${FuDevInitializeWrapper.defaultCloudConfig.mode.text}Config"
    /**
     * itemList 的版本号，用于请求客户端需要资源时的参数
     */
    private var itemListVersion: Int
        get() = FuDI.getStorageField(CloudConfigKey).getAsInt("ItemListVersion")
        set(value) = FuDI.getStorageField(CloudConfigKey).set("ItemListVersion", value)

    /**
     * 是否显示 project.json 里的默认形象。它仅在第一次安装时执行。
     */
    private var isInitDefaultAvatar: Boolean
        get() = FuDI.getStorageField(CloudConfigKey).getAsBoolean("isInitDefaultAvatar")
        set(value) = FuDI.getStorageField(CloudConfigKey).set("isInitDefaultAvatar", value)

    /**
     * 初始化云端服务。
     * 完成后会设置 [isInitCloudFinish]，它会在内存中记录云端请求缓存成功，整个 Application 中不会再请求。
     * 完成后会设置 [isCloudPrepare]，表示对 Home 页面来说云端资源准备就绪，待 [isGLPrepare] 后显示形象。
     */
    fun initCloud() {
        if (isInitCloudFinish) {
            FuLog.info("已初始化过云服务，使用缓存。")
            isCloudPrepare = true
            return
        }
        viewModelScope.launch {
            FuElapsedTimeChecker.start("initCloud")
            val isInitCloudSuccess = requestCloudInterface()
            if (!isInitCloudSuccess) {
                return@launch
            }
            val isInitClientSuccess = requestClientCloudInterface()
            tryInitEnvironment()
            if (isInitClientSuccess) {
                prepareDownloadBundle()
                tryInitEnvironment()
            }
            if (isInitCloudSuccess && isInitClientSuccess) {
                isInitCloudFinish = true
                isCloudPrepare = true
            }
            FuElapsedTimeChecker.end("initCloud")
        }
    }

    private val initCloudPlatformUseCase by lazy {
        FuUseCaseFactory.initCloudPlatformUseCase
    }
    private val requestItemListUseCase by lazy {
        FuUseCaseFactory.requestItemListUseCase
    }
    private val downloadAvatarConfigUseCase by lazy {
        FuUseCaseFactory.downloadAvatarConfigUseCase
    }
    private val downloadBundleUseCase by lazy {
        FuUseCaseFactory.downloadBundleUseCase
    }

    /**
     * 请求云端平台的数据并做相关缓存。
     */
    suspend fun requestCloudInterface(): Boolean {
        return withContext(Dispatchers.IO) {
            var isSuccess = true
            initCloudPlatformUseCase().onStart {
                notifyLoadingState(LoadingState.ShowWithTip("正在请求服务器"))
            }.onCompletion {
                notifyLoadingState(LoadingState.Hidden)
            }.onEach {
                FuLog.info("initCloudPlatformUseCase:$it")
                when(it) {
                    is InitCloudPlatformUseCase.State.GotToken -> {
                        FuDevDataCenter.tokenObserver.updateToken(it.token.value)
                    }
                    is InitCloudPlatformUseCase.State.GotProject -> {
                        val item_list_version = it.project.item_list_version
                        val defaultAvatarList = it.project.avatar_list.map { it.avatar_id }

                        if (itemListVersion != item_list_version) {
                            FuLog.info("current itemListVersion is $itemListVersion, remote itemListVersion is $item_list_version, update.")
                            val itemListValue = requestItemListUseCase.execute(item_list_version)
                            parseItemList(itemListValue)
                            itemListVersion = item_list_version
                        } else {
                            FuLog.info("current itemListVersion already is $item_list_version, break update.")
                        }
                        if (!isInitDefaultAvatar) {
                            FuLog.info("init default avatar")
                            parseAvatarList(defaultAvatarList)
                            isInitDefaultAvatar = true
                        }
                    }
                }
            }.catch {
                FuLog.error(it.stackTraceToString())
                val tipContent = downloadUiHelper.parseCatchTip(it)
                notifyExceptionEvent(ExceptionEvent.RetryDialog(tipContent, {initCloud()}))
                isSuccess = false
            }.collect()
            isSuccess
        }
    }

    /**
     * 当 itemList 发生更新时，将新旧 itemList 进行比较，更新 资源管理器（CloudResourceManager）里相关 Bundle 的状态位。（比如某个 Bundle 是否需要更新）
     */
    private fun parseItemList(itemListContentMap: Map<String, String>) {
        FuElapsedTimeChecker.start("同步ItemList信息")
        val itemListMap = itemListContentMap.map { it.key to FuDevDataCenter.resourceParser.parseItemList(it.value) }.toMap()
        val mergeItemList = itemListMap.map { it.value }.merge()
        val oldItemListResource = FuDevDataCenter.fastLoadItemListMerge()
        val resManager = FuDevDataCenter.getCloudResourceManager()
        if (oldItemListResource.list.isNotEmpty()) {
            resManager.syncContainer(mergeItemList, oldItemListResource)
        } else {
            resManager.initContainer(mergeItemList)
        }

        itemListContentMap.forEach { (tag, content) ->
            FuDevDataCenter.fastWriteString(content) { ossItemList(tag) }
        }
        FuElapsedTimeChecker.end("同步ItemList信息")
    }

    /**
     * 下载指定的 Avatar ID 的数据。
     */
    private suspend fun parseAvatarList(defaultAvatarList: List<String>) {
        defaultAvatarList.forEach { avatarId ->
            downloadAvatarConfigUseCase(DownloadAvatarConfigUseCase.Params(avatarId))
        }
        showAvatarId = defaultAvatarList.firstOrNull()
    }

    private var defaultAvatarConfigurator: FuDefaultAvatarConfigurator = FuDefaultAvatarConfigurator(FuDevDataCenter.fastLoadString { avatarBundleConfig() })

    private val editCloudControl = FuDependencyInjection.getCustom<IEditCloudControl>()

    /**
     * 请求客户端配置的云端资源。对应 AppAssets 里的文件。
     */
    suspend fun requestClientCloudInterface() : Boolean {
        var isSuccess = true
        if (itemListVersion <= 0) {
            FuLog.error("getItemListVersion error")
            isSuccess = false
            return isSuccess
        }
        val flow = editCloudControl.requestEditData(itemListVersion.toString())
        flow.onStart {
            notifyLoadingState(LoadingState.ShowWithTip("正在同步客户端配置"))
        }.onCompletion {
            notifyLoadingState(LoadingState.Hidden)
        }.onEach {
            FuLog.info("requestEditData:" + it.toString())
            when(it) {
                is FuUpdateEditDataState.Start -> {

                }
                is FuUpdateEditDataState.Finish, FuUpdateEditDataState.NotNeedUpdate -> {
                    isSuccess = true
                }
                is FuUpdateEditDataState.CloudFailure -> {
                    isSuccess = false
                    notifyExceptionEvent(ExceptionEvent.RetryDialog("云端接口异常：${it.throwable.message}", {initCloud()}))
                }
                is FuUpdateEditDataState.Failure -> {
                    isSuccess = false
                    notifyExceptionEvent(ExceptionEvent.RetryDialog("操作异常：${it.throwable.message}", {initCloud()}))
                }
                is FuUpdateEditDataState.NotifyUpdateFailure -> {
                    isSuccess = false
                    notifyExceptionEvent(ExceptionEvent.RetryDialog("更新数据异常：${it.throwable.message}", {initCloud()}))
                }
            }
        }.catch {
            val tipContent = downloadUiHelper.parseCatchTip(it)
            notifyExceptionEvent(ExceptionEvent.RetryDialog(tipContent, {initCloud()}))
            isSuccess = false
        }.collect()
        return isSuccess
    }

    /**
     * 预下载一些 control_config.bundle 和 default_list 里的 Bundle。
     */
    private suspend fun prepareDownloadBundle() {
        val downloadList = mutableListOf<String>()
        DevSceneManagerRepository.getDefaultOrNull()?.let {
            downloadList.add(it.sceneResource.controllerConfig)
        }
        defaultAvatarConfigurator.getDefaultBundleList().let {
            downloadList.addAll(it)
        }

        val isDownloadSuccess = downloadBundleUseCase(downloadList).let {
            downloadUiHelper.parseDownloadFlow(it, FuDownloadUIHelper.StyleConfig(
                tipContent = "正在下载预置资源",
                isDismissOnCompletion = true
            ), { initCloud() }).isSuccess
        }
        if (isDownloadSuccess) {
            //如果下载就绪，对 defaultAvatarConfigurator 进行配置，将其变为一个生效的对象，并设置给 FuDevDataCenter 供外界使用。
            defaultAvatarConfigurator.tryFill(FuDevDataCenter.bundleMetaManager).ifTrue {
                FuDevDataCenter.defaultAvatarConfigurator = defaultAvatarConfigurator
            }
        }
    }

    //endregion 云服务

    //region 形象渲染

    /**
     * 在 HomeViewModel 中，OpenGL 是否就绪。
     */
    @Volatile private var isGLPrepare = false

    /**
     * 当前 Home 页面所在使用的 Scene
     */
    private var scene: Scene? = null

    fun syncOpenGLState(isGLPrepare: Boolean) {
        this.isGLPrepare = isGLPrepare
    }

    /**
     * 要指定显示的 Avatar。
     * 用于扫码等指定形象的设置，消费过一次后会置空。
     */
    private var showAvatarId : String? = null

    /**
     * 指定要显示的 Avatar ID。
     */
    fun specifyDefaultAvatar(avatarId: String) {
        showAvatarId = avatarId
    }

    /**
     * 得到要显示的 AvatarId。
     */
    private fun getFinalAvatarId(isRelease: Boolean = true): String? {
        if (showAvatarId != null) {
            if (!isRelease) return showAvatarId
            val finalAvatarId = showAvatarId //消费掉了该事件
            showAvatarId = null
            FuLog.info("FinalAvatarId specify:$finalAvatarId")
            return finalAvatarId
        }
        val currentAvatarId = DevAvatarManagerRepository.getCurrentAvatarId()
        if (currentAvatarId != null) {
            FuLog.info("FinalAvatarId manager:$currentAvatarId")
            return currentAvatarId
        }
        return DevAvatarManagerRepository.getFirstAvatarId()
    }

    fun tryFastShow() {
        viewModelScope.launch {
            if (tryInitEnvironment()) {
                tryDrawLocalAvatar()
            }
        }
    }

    /**
     * 尝试对资源和配置做一些初始化和缓存。
     */
    private fun tryInitEnvironment(): Boolean {
        val isCacheSuccess = FuCacheResource.init(
            FuDevDataCenter.resourceParser,
            FuDevDataCenter.resourceManager
        )
        DevSceneManagerRepository.initSceneConfigNotRepeat()
        if (DevAvatarManagerRepository.getAvatarListSize() == 0) {
            DevAvatarManagerRepository.loadAvatar()
        }
        if (scene == null) { //创建一个符合条件的完整 Scene 配置，但是可能文件还未下载。
            val default = DevSceneManagerRepository.getDefaultOrNull()
            if(default == null) return false //如果读取不到配置则不创建
            scene = CreateSceneUseCase()(CreateSceneUseCase.Params()).getOrNull()
            scene?.apply {
                DevSceneManagerRepository.setSceneConfig(this, default.sceneResource)
                FuDevInitializeWrapper.sceneCustom(this)
            }
        }
        return isCacheSuccess
    }


    /**
     * 尝试用本地已有的数据显示一个 Avatar。
     */
    fun tryDrawLocalAvatar() {
        FuLog.info("tryDrawLocalAvatar")
        //公共资源检查是否就绪
        if (scene == null) {
            return
        }
        if (FuCacheResource.controllerConfigBundle == null) {
            return
        }
        if (FuDevDataCenter.defaultAvatarConfigurator == null) {
            val isFillSuccess = defaultAvatarConfigurator.tryFill(FuDevDataCenter.bundleMetaManager)
            if (isFillSuccess) {
                FuDevDataCenter.defaultAvatarConfigurator = defaultAvatarConfigurator
            } else {
                return
            }

        }
        val finalAvatarId = getFinalAvatarId(false)
        //得到指定的 AvatarInfo，并且它应该起码有 avatar.json
        val loadAvatar = DevAvatarManagerRepository.getAvatarList().firstOrNull { it.id == finalAvatarId && it.state != AvatarInfo.State.PrepareId }
        if (loadAvatar == null) {
            return
        }

        //根据 avatar.json，解析其内容
        val decodeAvatarInfo = FuUseCaseFactory.decodeAvatarJsonUseCase(DecodeAvatarJsonUseCase.Params(loadAvatar.avatarJson!!)).getOrElse {
            return
        }
        //根据解析出来的内容，构造一个 Avatar。如果有资源未就绪，则会构造失败。
        val avatar = FuUseCaseFactory.createAvatarUseCase(CreateAvatarUseCase.Params(decodeAvatarInfo)).getOrElse {
            return
        }
        //找到与 Avatar 符合的动画配置
        val sceneInfo = DevSceneManagerRepository.filterGenderDefaultSceneFirst(loadAvatar.gender())
        if (sceneInfo == null) {
            return
        }
        //给 Avatar 绑定对应的动画
        DevSceneManagerRepository.setSceneConfig(avatar, sceneInfo.sceneResource)
        //构造一个 Avatar 完成

        //临时在此处对 Avatar 进行一次重建。用于页面切换后回到首页。之后该部分逻辑会在别处实现。
        rebuildAvatar()

        loadAvatar.updateAvatar(avatar)

        //检查 Scene 的文件是否都存在
        FuUseCaseFactory.verifySceneFileExistUseCase(VerifySceneFileExistUseCase.Params(scene!!)).getOrElse {
            return
        }.run {
            if (!canShow) return
        }

        //检查 Avatar 的文件是否都存在
        FuUseCaseFactory.verifyAvatarFileExistUseCase(VerifyAvatarFileExistUseCase.Params(avatar)).getOrElse {
            return
        }.run {
            if (!canShow) return
        }
        //进一步地检查资源的状态是否都正常。应对某些资源下架的情况。
        FuResourceCheck.checkAvatarBundleStatus(avatar, FuDevDataCenter.getCloudResourceManager()).run {
            if (isNotPrepare) return
        }
        //Avatar 验证没问题
        DevDrawRepository.replaceCurrentAvatarByCompare(avatar, scene!!)
        DevDrawRepository.setUseScene(scene!!)
        FuLog.info("tryDrawLocalAvatar success")
    }

    /**
     * 当资源就绪后自动显示一个 Avatar
     */
    fun drawDefaultAvatarOnReady() {
        viewModelScope.launch {
            while (!isCloudPrepare || !isGLPrepare) { //当网络和 OpenGL 环境没有准备就绪时，阻塞。
                delay(100)
            }
            drawDefaultAvatar()
        }
    }

    private val _notifySwitchAvatar = SingleLiveEvent<String>()
    val notifySwitchAvatar: LiveData<String> get() =  _notifySwitchAvatar

    /**
     * 显示一个 Avatar，目前暂时通过 LiveData 通知 AvatarManagerViewModel 实现。
     */
    fun drawDefaultAvatar() {
        scene?.let {
            DevDrawRepository.currentScene = it
        }
        val switchAvatarId = getFinalAvatarId()
        if (switchAvatarId == null) {
            FuLog.error("Could not find load Avatar")
            return
        }
        _notifySwitchAvatar.postValue(switchAvatarId!!)
        FuLog.info("drawDefaultAvatar $switchAvatarId")
    }

    private fun rebuildAvatar() {
        DevAvatarManagerRepository.mapAvatar {
            if (it.state == AvatarInfo.State.PrepareAvatar) {
                FuUseCaseFactory.rebuildAvatarUseCase(it)
                DevSceneManagerRepository.filterGenderDefaultSceneFirst(it.gender())?.run {
                    DevSceneManagerRepository.setSceneConfig(it.avatar, sceneResource) //绑定首页的默认动画
                }
            }

        }
    }

    //region 业务接口

    /**
     * 将当前 AvatarInfo 上传到服务器上。
     * 默认数据不存在不一致的情况（没有经过编辑后未保存的现象）。
     */
    suspend fun uploadCurrentAvatar(): Flow<Result<String>> = flow {
        val avatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo()
        if (avatarInfo == null) {
            emit(Result.failure(Throwable("当前没有可以上传的 Avatar")))
            return@flow
        }
        val result = UploadAvatarConfigUseCase(FuDI.getCustom())(avatarInfo)
        result.onSuccess {
            //在上传并自动修改 AvatarId 后，需要通知 DevAvatarManagerRepository 当前选中的 AvatarId 更新。
            DevAvatarManagerRepository.switchAvatar(it)
        }
        emit(result)
    }

    /**
     * 生成一个当前形象的二维码，对应一个云端可分享的 url。
     * @param previewFile 用于展示的图片
     * @param isFlushAvatarId 是否将本地 avatar.json 传到云端获取一个新的 AvatarId。如果确保每次修改都更新，则生成二维码的时候不用更新。
     */
    suspend fun buildQrCode(previewFile: File, isFlushAvatarId: Boolean = false): Flow<FuQrCodeResult> = flow {

        fun decodeQrString(input: String): ByteArray {
            val content = input.replace("data:image/png;base64,", "")
            return Base64.decode(content, Base64.DEFAULT)
        }

        suspend fun requestQrCode(avatarId: String) {
            val cloudAPI = FuDI.getCustom<ICloudPlatformSyncAPI>()
            val token = cloudAPI.getCacheTokenOrRequest()
            val qrCode = cloudAPI.buildQrCode(token, avatarId, previewFile).getOrThrow()
            val bitmap = decodeQrString(qrCode.data.qr_code_url).let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
            emit(FuQrCodeResult(avatarId, bitmap))
        }

        if (isFlushAvatarId) {
            uploadCurrentAvatar().collect {
                val avatarId = it.getOrThrow()
                requestQrCode(avatarId)
            }
        } else {
            val avatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo()
            requestQrCode(avatarInfo!!.id)
        }


    }

    //endregion 业务接口

}