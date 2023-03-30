package com.faceunity.app_ptag.ui.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.FuDevInitializeWrapper
import com.faceunity.app_ptag.ui.edit.entity.GenderProFilter
import com.faceunity.app_ptag.ui.edit.entity.control.ControlModel
import com.faceunity.app_ptag.ui.edit.entity.facepup.BodyShapeUiState
import com.faceunity.app_ptag.ui.edit.entity.facepup.CustomFacepupIcon
import com.faceunity.app_ptag.ui.edit.entity.facepup.CustomFacepupModel
import com.faceunity.app_ptag.ui.edit.entity.facepup.TranSlider
import com.faceunity.app_ptag.ui.edit.entity.state.EditHistoryUiState
import com.faceunity.app_ptag.ui.edit.entity.state.EditItemDownloadState
import com.faceunity.app_ptag.ui.edit.entity.state.EditPageUiState
import com.faceunity.app_ptag.ui.edit.entity.state.SelectedUiState
import com.faceunity.app_ptag.ui.edit.expand.download.DownloadDispatcher
import com.faceunity.app_ptag.ui.edit.expand.download.entity.DownloadTask
import com.faceunity.app_ptag.ui.edit.filter.GenderFilter
import com.faceunity.app_ptag.ui.edit.parser.ControlModelOldParser
import com.faceunity.app_ptag.ui.edit.translator.CustomFacepupModelTranslator
import com.faceunity.app_ptag.use_case.FuUseCaseFactory
import com.faceunity.app_ptag.use_case.download.SmartPrepareAvatarUseCase
import com.faceunity.app_ptag.use_case.render_kit.SaveAvatarIconUseCase
import com.faceunity.app_ptag.use_case.render_kit.ShotSceneUseCase
import com.faceunity.app_ptag.use_case.render_kit.SwitchCameraUseCase
import com.faceunity.app_ptag.use_case.renderer.PhotoRecordUseCase
import com.faceunity.app_ptag.util.CanNotFindRenderAvatarThrowable
import com.faceunity.app_ptag.util.SingleLiveEvent
import com.faceunity.app_ptag.view_model.helper.FuDownloadUIHelper
import com.faceunity.app_ptag.view_model.helper.FuDownloadUiImpl
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.core.entity.FUAnimationBundleData
import com.faceunity.core.entity.FUColorRGBData
import com.faceunity.core.enumeration.FULogicNodeEnum
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.data_center.FuEditHistoryManager
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.store.DevEditRepository
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadBundleUseCase
import com.faceunity.editor_ptag.use_case.cloud_platform.UploadAvatarConfigUseCase
import com.faceunity.editor_ptag.use_case.edit.ComponentModifyCheckUseCase
import com.faceunity.editor_ptag.use_case.edit.SaveAvatarJsonUseCase
import com.faceunity.editor_ptag.use_case.edit.WearBundleUseCase
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.editor_ptag.util.FuResourceCheck
import com.faceunity.editor_ptag.util.safeCollect
import com.faceunity.fupta.cloud_download.parser.IFuAPIParser
import com.faceunity.fupta.facepup.FacePupManager
import com.faceunity.fupta.facepup.FacePupParser
import com.faceunity.fupta.facepup.entity.bodyshape.BodyShapeItem
import com.faceunity.fupta.facepup.entity.bodyshape.BodyShapeModel
import com.faceunity.fupta.facepup.entity.config.FacePupConfig
import com.faceunity.fupta.facepup.entity.origin.FacepupMeshTranslation
import com.faceunity.fupta.facepup.entity.tier.FacepupGeneralTier
import com.faceunity.fupta.facepup.entity.tier.FacepupSlider
import com.faceunity.fupta.facepup.translator.FacepupGeneralTierTranslator
import com.faceunity.pta.pta_core.model.AvatarInfo
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 编辑相关
 */
class FuDemoEditViewModel : ViewModel(), FuDownloadUiImpl by FuDownloadUIHelper() {

    //region 绘制、形象加载


    private val _selectedUiState = MutableStateFlow(SelectedUiState.empty())
    val selectedUiState: StateFlow<SelectedUiState> = _selectedUiState.asStateFlow()
    private fun notifySelectedUiState(avatar: Avatar) {
        viewModelScope.launch {
            val state = SelectedUiState.build(avatar)
            _selectedUiState.emit(state)
        }
    }

    private val _editPageUiState: MutableStateFlow<EditPageUiState> = MutableStateFlow(EditPageUiState.default())
    val editPageUiState = _editPageUiState.asStateFlow()


    fun drawAvatar() {
        //创建一个用于编辑页面的 Scene，此处可自定义一些额外配置
        val scene = FuUseCaseFactory.smartCreateSceneUseCase()
        if (scene == null) return
        scene.apply {
            FuDevInitializeWrapper.sceneCustom(this)
        }
        val currentAvatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo()
        if (currentAvatarInfo == null) return

        //因为编辑页面可能有外界导入的 Avatar，故需要校验并下载。
        viewModelScope.launch {
            SmartPrepareAvatarUseCase(currentAvatarInfo.id) {
                parseDownloadFlow(this) { drawAvatar() }
            }
            //根据当前 AvatarInfo 的 avatar.json，重建一个用于编辑页面的 Avatar。
            FuUseCaseFactory.rebuildAvatarUseCase(currentAvatarInfo)
            DevSceneManagerRepository.filterGenderDefaultSceneFirst(currentAvatarInfo.gender())?.let {
                //根据编辑页面的需求，给该 Avatar 绑定一个呼吸动画
                DevSceneManagerRepository.setAvatarDefaultAnimation(currentAvatarInfo.avatar, it.sceneResource)
            }
            //给 avatar 绑定默认配置，防止裸体。
            FuDevDataCenter.defaultAvatarConfigurator!!.bindAvatar(currentAvatarInfo.avatar)

            DevDrawRepository.replaceCurrentAvatar(currentAvatarInfo.avatar, scene)
            DevDrawRepository.setUseScene(scene)
            notifySelectedUiState(currentAvatarInfo.avatar)
            _editPageUiState.update { it.copy(
                gender = currentAvatarInfo.gender(),
                isAvatarPrepare = true
            ) }
        }

    }

    /**
     * 当 Avatar 加载完毕之后，读取它的一些配置，进行一些初始化。
     */
    fun syncAvatarEditStatus() {
        val avatarInfo = loadControlAvatarInfo()
        if (avatarInfo == null) {
            return
        }
        //历史功能初始化
        defaultAvatar = avatarInfo.avatar.clone()
        historyInit()
        //捏脸模块初始化
        DevEditRepository.syncFacepupInfo(avatarInfo, DevEditRepository.useFacepupContainer())
        DevEditRepository.setControlAvatar(avatarInfo.id)
        //编辑菜单性别更新
        updateFilterGroup(avatarInfo.gender())

        _editPageUiState.update { it.copy(
            gender = avatarInfo.gender()
        ) }
    }

    /**
     * 根据传入的 [avatarId] 刷新编辑页对应的菜单。
     * 如男性角色显示男性菜单。
     */
    private fun updateFilterGroup(gender: String) {
        val filterGroup = _editPageUiState.value.filterGroup
        if (gender == "male") {
            filterGroup.replaceSameTagRule(GenderProFilter(GenderFilter.GenderFilterKey.JustMale, GenderFilter.GenderFilterKey.All))
        } else if (gender == "female") {
            filterGroup.replaceSameTagRule(GenderProFilter(GenderFilter.GenderFilterKey.JustFemale, GenderFilter.GenderFilterKey.All))
        }
        _editPageUiState.update { it.copy(
            filterGroup = filterGroup
        ) }
    }

    /**
     * 获取要控制的 AvatarInfo
     */
    private fun loadControlAvatarInfo(): AvatarInfo? {
        return DevAvatarManagerRepository.getCurrentAvatarInfo()
    }

    /**
     * 获取要控制的 Avatar
     */
    private fun loadControlAvatar(): Avatar? {
        return DevAvatarManagerRepository.getCurrentAvatarInfo()?.avatar
    }

    //endregion 绘制、形象加载

    //region 编辑模型创建

    /**
     * 申请加载 [ControlModel] （编辑组件的业务数据结构）信息。
     */
    suspend fun requestEditorData(): Result<ControlModel> = withContext(Dispatchers.IO) {
        DevEditRepository.initEditNeedModel().getOrElse {
            return@withContext Result.failure(it)
        }
        val controlModel = DevEditRepository.buildEditModel(ControlModelOldParser(FuLog)).getOrNull() as? ControlModel
        if (controlModel == null) return@withContext Result.failure(Throwable("构造编辑模型失败"))
        Result.success(controlModel)
    }


    //endregion 编辑模型创建


    //region 形象编辑

    private val componentModifyCheckUseCase by lazy {
        ComponentModifyCheckUseCase(FuDevDataCenter.resourceManager, FuDevDataCenter.bundleMetaManager)
    }

    private val wearBundleUseCase by lazy {
        WearBundleUseCase(FuDevDataCenter.resourceManager)
    }

    private val downloadUseCase by lazy {
        FuUseCaseFactory.downloadBundleUseCase
    }

    private val cloudResourceManager by lazy {
        FuDevDataCenter.getCloudResourceManager()
    }

    private val downloadDispatcher = DownloadDispatcher()

    private var minorKey: String? = null

    fun notifySwitchMinorMenu(key: String) {
        minorKey = key
    }

    /**
     * 点击某个道具。它会下载所需道具并应用到 Avatar 上。
     */
    suspend fun clickItem(fileId: String): Result<String> {
        try {
            val avatar = loadControlAvatar() ?: return Result.failure(CanNotFindRenderAvatarThrowable())
            val modifyResult = prepareItem(avatar, fileId).getOrThrow()
            wearItem(avatar, modifyResult).getOrThrow()
            pushHistory(avatar)
            return Result.success(fileId)
        } catch (ex: CancellationException) {
            return Result.failure(ex)
        } catch (ex: Throwable) {
            return Result.failure(ex)
        }
    }

    /**
     * 点击某个道具并通过调度器执行它。该调度器的逻辑是每一栏道具点击多个时只对最后一个执行。
     */
    suspend fun clickItemByDispatcher(fileId: String): Result<String> {
        try {
            val avatar = loadControlAvatar() ?: return Result.failure(CanNotFindRenderAvatarThrowable())
            val currentKey = minorKey ?: ""
            //为调度器里添加该任务
            downloadDispatcher.addTask(currentKey, fileId, DownloadTask.empty())
            //进行某些耗时任务
            val modifyResult = prepareItem(avatar, fileId).getOrThrow()
            //如果调度器里该任务还在，则执行内容
            downloadDispatcher.useTask(currentKey, fileId)?.let {
                wearItem(avatar, modifyResult).getOrThrow()
            }
            pushHistory(avatar)
            return Result.success(fileId)
        } catch (ex: CancellationException) {
            return Result.failure(ex)
        } catch (ex: Throwable) {
            return Result.failure(ex)
        }
    }

    /**
     * 点击某个颜色。它会应用到 Avatar 上。
     */
    fun clickColor(key: String, color: FUColorRGBData): Result<Unit> {
        val avatar = loadControlAvatar() ?: return Result.failure(CanNotFindRenderAvatarThrowable())
        avatar.color.setColor(key, color)
        pushHistory(avatar)
        return Result.success(Unit)
    }


    /**
     * 检查 [fileId] 所需的资源是否下载完成，如果需要则下载。完成后返回一个就绪的该结果。
     */
    private suspend fun prepareItem(avatar: Avatar, fileId: String): Result<ComponentModifyCheckUseCase.ModifyResult> {
        val bundleStatus =
            FuResourceCheck.checkBundleStatus(listOf(fileId), cloudResourceManager)
        bundleStatus.allDownload.takeIf{ it.isNotEmpty() }?.run {
            downloadEditItem(this).getOrElse {
                return Result.failure(it)
            }
        }
        val checkResult = componentModifyCheckUseCase(ComponentModifyCheckUseCase.Params(avatar, fileId)).getOrElse {
            return Result.failure(it)
        }
        checkResult.needDownloadBundle(FuDevDataCenter.resourceManager).takeIf{ it.isNotEmpty() }?.run {
            downloadEditItem(this).getOrElse {
                return Result.failure(it)
            }
        }
        return Result.success(checkResult)
    }

    private suspend fun wearItem(avatar: Avatar, modifyResult: ComponentModifyCheckUseCase.ModifyResult): Result<Unit> {
        val wearResult = wearBundleUseCase(WearBundleUseCase.Params(avatar, modifyResult))
        if (wearResult.isFailure) {
            return Result.failure(wearResult.exceptionOrNull()!!)
        }
        return Result.success(Unit)
    }

    private val _editItemDownloadState = MutableStateFlow<EditItemDownloadState>(EditItemDownloadState.Default())
    private fun notifyEditItemDownloadState(editState: EditItemDownloadState) {
        _editItemDownloadState.value = editState
    }
    /** 正处于下载队列中的道具的下载状态。*/
    val editItemDownloadState = _editItemDownloadState.asLiveData()

    private suspend fun downloadEditItem(fileIdList: List<String>) : Result<Unit> {
        var throwable: Throwable? = null
        try {
            downloadUseCase(fileIdList).onStart {
                //在开始时给所有下载任务标注为开始。因为下载引擎是有任务调度的，如果依赖 TaskState 可能存在点了后它进入下载队列而 UI 状态显示未开始。
                notifyEditItemDownloadState(EditItemDownloadState.Start(fileIdList))
            }.onCompletion {

            }.onEach {
                when(it) {
                    is DownloadBundleUseCase.State.Finish, DownloadBundleUseCase.State.Skip -> {
                    }
                    is DownloadBundleUseCase.State.TaskDownloadProgress -> {
                        when (it.taskState) {
//                        DownloadBundleUseCase.TaskState.Start -> notifyEditItemDownloadState(EditItemDownloadState.Start(fileIdList))
                            DownloadBundleUseCase.TaskState.Success -> notifyEditItemDownloadState(EditItemDownloadState.Success(fileIdList))
                            DownloadBundleUseCase.TaskState.Error -> notifyEditItemDownloadState(EditItemDownloadState.Error(fileIdList))
                        }
                    }
                    is DownloadBundleUseCase.State.StartFailed -> {
                        notifyEditItemDownloadState(EditItemDownloadState.Error(it.lostFileIdList.toList()))
                        throwable = Throwable("素材已失效，请选择其他素材。")
                    }
                }
            }.catch {
                throwable = it
            }.safeCollect {}
        } catch (ex: CancellationException) {
            FuLog.error("catch downloadEditItem CancellationException:$ex")
            return Result.failure(ex)
        } catch (ex: Throwable) {
            return Result.failure(ex)
        }


        return if (throwable == null) Result.success(Unit) else Result.failure(throwable!!)
    }

    //endregion 形象编辑

    //region 历史功能

    private val _editHistoryUiState = MutableStateFlow(EditHistoryUiState.default())
    val editHistoryUiState: StateFlow<EditHistoryUiState> = _editHistoryUiState.asStateFlow()

    private var defaultAvatar: Avatar? = null

    private fun historyInit() {
        val avatar = defaultAvatar?.clone() ?: return
        FuEditHistoryManager.reset(avatar)
        onHistoryStep()
    }

    fun historyReset() {
        val avatar = defaultAvatar?.clone() ?: return
        playAvatarAnimation(avatar)
        FuEditHistoryManager.reset(avatar)
        DevDrawRepository.replaceCurrentAvatarByCompare(avatar)
        syncAvatarToCurrentAvatarInfo(avatar)
        onHistoryStep()
    }

    fun pushHistory(avatar: Avatar? = null) {
        val controlAvatar = avatar ?: loadControlAvatar() ?: return
        FuEditHistoryManager.pushHistory(controlAvatar)
        notifySelectedUiState(controlAvatar)
        onHistoryStep()
    }

    fun historyBack() {
        val avatar = FuEditHistoryManager.pullBackHistory() ?: return
        playAvatarAnimation(avatar)
        DevDrawRepository.replaceCurrentAvatarByCompare(avatar)
        syncAvatarToCurrentAvatarInfo(avatar)
        onHistoryStep()
    }

    fun historyForward() {
        val avatar = FuEditHistoryManager.pullForwardHistory() ?: return
        playAvatarAnimation(avatar)
        DevDrawRepository.replaceCurrentAvatarByCompare(avatar)
        syncAvatarToCurrentAvatarInfo(avatar)
        onHistoryStep()
    }

    private fun onHistoryStep() {
        _editHistoryUiState.tryEmit(EditHistoryUiState(
            backCount = FuEditHistoryManager.getBackHistoryCount(),
            forwardCount = FuEditHistoryManager.getForwardHistoryCount(),
            historyCount = FuEditHistoryManager.getHistoryCount()
        ))
    }

    /**
     * 当切换 Avatar 后，需要手动播放需要的动画
     */
    private fun playAvatarAnimation(avatar: Avatar) {
        avatar.animation.apply {
            val huxiAnim = getAnimations().firstOrNull { it.path.contains("huxi") } ?: return
            val defaultAnim = FUAnimationBundleData(
                path = huxiAnim.path,
                name = "huxi",
                nodeName = FULogicNodeEnum.DEFAULT.nodeName,
            )
            playAnimation(defaultAnim)
        }
    }

    /**
     * 将修改后的 Avatar 同步至对应 AvatarInfo
     */
    private fun syncAvatarToCurrentAvatarInfo(avatar: Avatar) {
        val avatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo() ?: return
        avatarInfo.updateAvatar(avatar)
        notifySelectedUiState(avatar)
        //在形象更新后，同步捏形数据
        syncBodyShape(BodyShapeUiState.EventType.Init)
    }


    //endregion 历史功能

    //region 业务功能

    private val saveAvatarJsonUseCase by lazy {
        SaveAvatarJsonUseCase(FuDI.getCustom())
    }
    private val saveAvatarIconUseCase by lazy {
        SaveAvatarIconUseCase(
            ShotSceneUseCase(PhotoRecordUseCase()),
            SwitchCameraUseCase()
        )
    }
    private val uploadAvatarConfigUseCase by lazy {
        UploadAvatarConfigUseCase(FuDI.getCustom())
    }

    suspend fun saveCurrentAvatar(): Flow<Unit> = flow {
        val avatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo()!!
        //根据该 AvatarInfo 里的 Avatar，得到一个新的 avatar.json 并更新至 AvatarInfo。
        saveAvatarJsonUseCase(SaveAvatarJsonUseCase.Params(avatarInfo)).getOrThrow()
        saveAvatarIconUseCase.execute(avatarInfo)
        val avatarId = uploadAvatarConfigUseCase(avatarInfo).getOrThrow()
        //在上传并自动修改 AvatarId 后，需要通知 DevAvatarManagerRepository 当前选中的 AvatarId 更新。
        DevAvatarManagerRepository.switchAvatar(avatarId)
        emit(Unit)
    }

    fun rebuildAvatar() {
        val currentAvatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo()!!
        FuUseCaseFactory.rebuildAvatarUseCase(currentAvatarInfo)
    }

    //endregion 业务功能

    //region 捏脸模型创建

    var facepupMode = 0

    private lateinit var meshLanguage: FacepupMeshTranslation
    private lateinit var generalTier: FacepupGeneralTier
    private lateinit var generalSimpleTier: FacepupGeneralTier
    private lateinit var customIcon: CustomFacepupIcon

    fun initFacepup() {
        //加载原始 JSON 数据
        val meshPointsString = FuDevDataCenter.fastLoadString { appFacepupPoints() }
        val meshConfigString = FuDevDataCenter.fastLoadString { appFacepupConfig() }
        val meshConfigSimpleString = FuDevDataCenter.fastLoadString { appCustom("facepup/MeshSimpleConfig.json") }
        val meshLanguageString = FuDevDataCenter.fastLoadString { appFacepupLanguage() }
        //解析为原始数据结构
        val meshPoints = FacePupParser.parserMeshPoints(meshPointsString)
        val meshConfig = FacePupParser.parserMeshConfig(meshConfigString)
        val meshSimpleConfig = FacePupParser.parserMeshConfig(meshConfigSimpleString)
        meshLanguage = FacePupParser.parserMeshTranslation(meshLanguageString)
        //构造带层级的数据模型。根据不同的 MeshConfig 会有不同的体现，对应简单专家两个模式。
        generalTier = FacepupGeneralTierTranslator().buildFacePupGeneralTier(
            meshPoints,
            meshConfig
        )
        generalSimpleTier = FacepupGeneralTierTranslator().buildFacePupGeneralTier(
            meshPoints,
            meshSimpleConfig
        )
        //解析图标数据
        val iconString = FuDevDataCenter.fastLoadString { appCustom("facepup/facepup_icon.json") }
        customIcon = CustomFacepupIcon.buildFromJson(iconString)
    }

    /**
     * 根据数据构造一个符合业务的捏脸模型。
     * 该代码未做缓存，仅作参考。
     * @param mode 0 为简易模式、1 为专家模式
     */
    fun buildCustomGroupNotCache(groupKey: String, mode: Int) : CustomFacepupModel {
        //构建原始数据模型
        val meshPointsString = FuDevDataCenter.fastLoadString { appFacepupPoints() }
        val meshConfigString = when (mode) {
            0 -> FuDevDataCenter.fastLoadString { appCustom("facepup/MeshSimpleConfig.json") }
            else -> FuDevDataCenter.fastLoadString { appFacepupConfig() }
        }
        val meshLanguageString = FuDevDataCenter.fastLoadString { appFacepupLanguage() }
        val facePupContainer = FacePupManager.loadFacePupContainer(meshPointsString, meshConfigString, meshLanguageString)
        //构建层级数据模型
        val generalTier = FacePupManager.buildGeneralTier(facePupContainer)

        val iconString = FuDevDataCenter.fastLoadString { appCustom("facepup/facepup_icon.json") }
        val generalTierGroup = generalTier.groupList.firstOrNull { it.groupKey == groupKey }
        val translation = FacePupParser.parserMeshTranslation(meshLanguageString)
        val isTranCustomSlider = if (mode == 1) false else true
        val customGroup = CustomFacepupModelTranslator().buildCustom(
            generalTierGroup = generalTierGroup!!,
            translation = translation,
            facepupMap = DevAvatarManagerRepository.getCurrentAvatarInfo()?.avatar?.deformation?.getDeformationCache() ?: emptyMap(),
            customIcon = CustomFacepupIcon.buildFromJson(iconString),
            isTranCustom = isTranCustomSlider
        )

        return customGroup
    }


    /**
     * 根据数据构造一个符合业务的捏脸模型。
     * @param mode 0 为简易模式、1 为专家模式
     */
    @JvmOverloads
    fun buildCustomGroup(groupKey: String, mode: Int = facepupMode) : CustomFacepupModel {
        facepupMode = mode
        val generalTierGroup = when(mode) {
            0 -> generalSimpleTier
            else -> generalTier
        }.groupList.firstOrNull { it.groupKey == groupKey }
        val facepupMap =
            DevAvatarManagerRepository.getCurrentAvatarInfo()?.avatar?.deformation?.getDeformationCache() ?: emptyMap()
        val isTranCustomSlider = if (mode == 1) false else true
        val customGroup = CustomFacepupModelTranslator().buildCustom(
            generalTierGroup = generalTierGroup!!,
            translation = meshLanguage,
            facepupMap = facepupMap,
            customIcon = customIcon,
            isTranCustom = isTranCustomSlider
        )
        return customGroup
    }

    //endregion 捏脸模型创建


    //region 捏脸功能

    /**
     * 该菜单栏是否支持捏脸
     */
    fun isHasFacepup(minorKey: String): Boolean {
        return minorKey in DevEditRepository.useFacepupContainer().getAllGroupKey()
    }


    /**
     * 将传入的捏脸值解析并设置到 Avatar 中
     */
    fun setFacepupTierSeekBarItem(fileId: String, groupKey: String, slider: FacepupSlider, seekBarValue: Float) {
        val avatar = DevAvatarManagerRepository.getCurrentAvatarInfo()?.avatar ?: return
        val facepupMap = DevEditRepository.parseFacepupTierSeekBarItem(slider, seekBarValue)
        DevEditRepository.putFacepupInfo(fileId, groupKey, facepupMap)
        facepupMap.forEach {
            avatar.deformation.setDeformation(it.key, it.value)
        }
    }

    /**
     * 重置该模型的捏脸信息
     */
    fun resetFacepupByFileId(fileId: String) {
        val avatar = DevAvatarManagerRepository.getCurrentAvatarInfo()?.avatar ?: return
        val facepupPackInfo = DevEditRepository.getFacepupPackInfo(fileId)
        facepupPackInfo?.map?.forEach {
            avatar.deformation.setDeformation(it.key, 0f)
        }
        DevEditRepository.resetFacepupInfo(fileId)
    }

    /**
     * 重置该类别的捏脸信息
     */
    fun resetFacepupByGroupKey(groupKey: String) {
        val avatar = DevAvatarManagerRepository.getCurrentAvatarInfo()?.avatar ?: return
        val resetKey = DevEditRepository.useFacepupContainer().getAllFacePupKey(groupKey)
        resetKey.forEach {
            avatar.deformation.setDeformation(it, 0f)
        }
        DevEditRepository.resetGroupFacepupInfo(groupKey)
    }

    /**
     * 该道具是否含有捏脸信息
     */
    fun isHasFacepupPackInfo(fileId: String): Boolean {
        val facepupPackInfo = DevEditRepository.getFacepupPackInfo(fileId)
        if (facepupPackInfo == null) {
            return false
        }
        if (facepupPackInfo.map.size == 0){
            return false
        }
        return true
    }

    //endregion 捏脸功能

    //region 捏形功能

    private val _bodyShapeUiState = MutableStateFlow<BodyShapeUiState>(BodyShapeUiState.empty())
    val bodyShapeUiState = _bodyShapeUiState.asStateFlow()

    private val tranSlider = TranSlider()

    fun requestBodyShape() {
        val avatar = loadControlAvatar() ?:return
        val bodyShapeItem = FuDevDataCenter.fastLoadString { appBodyShapePitch() }.let {
            FuDI.getCustom<IFuAPIParser>().parse<BodyShapeModel>(it, object : TypeToken<BodyShapeModel>() {}.type)
        }.getOrThrow()
        _bodyShapeUiState.tryEmit(BodyShapeUiState.build(bodyShapeItem, {
            tranSlider.modelToUi(it, DevEditRepository.getFacepup(avatar))
        }))
    }

    private fun syncBodyShape(eventType: BodyShapeUiState.EventType) {
        val avatar = loadControlAvatar() ?:return
        val deformationMap = DevEditRepository.getFacepup(avatar)
        _bodyShapeUiState.update { it.copy(
            groupList = it.groupList.map {
                it.copy(
                    sliderList = it.sliderList.map {
                        val newValue = tranSlider.modelToUi(it.bodyShapeItem, deformationMap)
                        if (newValue != it.value) {
                            it.copy(
                                value = newValue
                            )
                        } else {
                            it
                        }
                    }
                )
            },
            eventType = eventType
        )}
    }

    fun clickBodyShapeConfig(facePupConfig: FacePupConfig) {
        val avatar = loadControlAvatar() ?:return
        DevEditRepository.setFacepup(avatar, facePupConfig.bone_controllers.map { it.name to it.value })
        syncBodyShape(BodyShapeUiState.EventType.Init)
        pushHistory()
    }

    /**
     * 将传入的捏形数据设置进 Avatar，并更新 [bodyShapeUiState]
     */
    fun setBodyShape(bodyShapeItem: BodyShapeItem, sliderValue: Float) {
        val avatar = loadControlAvatar() ?:return
        val deformationMap = tranSlider.uiToModel(bodyShapeItem, sliderValue)
        DevEditRepository.setFacepup(avatar, deformationMap)
        syncBodyShape(BodyShapeUiState.EventType.Slider)

    }

    fun resetBodyShape() {
        val avatar = loadControlAvatar() ?:return
        val deformationMap = mutableMapOf<String, Float>()
        _bodyShapeUiState.update { it.copy(
            groupList = it.groupList.map {
                it.copy(
                    sliderList = it.sliderList.map {
                        it.bodyShapeItem.keyMore.forEach {
                            deformationMap[it] = 0f
                        }
                        it.bodyShapeItem.keyLess.forEach {
                            deformationMap[it] = 0f
                        }
                        it.copy(
                            value = 0f
                        )
                    }
                )
            },
            eventType = BodyShapeUiState.EventType.Reset
        )}
        DevEditRepository.setFacepup(avatar, deformationMap)
    }

    //endregion 捏形功能

    fun release() {
        downloadDispatcher.cancelAllTask()
        defaultAvatar = null
        FuEditHistoryManager.clear()
    }
}