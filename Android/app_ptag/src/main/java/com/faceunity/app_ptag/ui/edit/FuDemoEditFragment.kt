package com.faceunity.app_ptag.ui.edit

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.faceunity.app_ptag.databinding.FuEditFragmentBinding
import com.faceunity.app_ptag.ui.edit.entity.control.*
import com.faceunity.app_ptag.ui.edit.entity.state.EditItemDownloadState
import com.faceunity.app_ptag.ui.edit.expand.facepup.FuBodyShapePreviewHelper
import com.faceunity.app_ptag.ui.edit.weight.FuDemoSaveAvatarDialog
import com.faceunity.app_ptag.ui.edit.weight.control.AvatarControlListener
import com.faceunity.app_ptag.ui.edit.weight.facepup.BodyShapeControlView
import com.faceunity.app_ptag.ui.edit.weight.facepup.FacepupControlView
import com.faceunity.app_ptag.ui.home.entity.state.ExceptionEvent
import com.faceunity.app_ptag.ui.home.entity.state.LoadingState
import com.faceunity.app_ptag.use_case.render_kit.SwitchCameraUseCase
import com.faceunity.app_ptag.use_case.renderer.BindRendererListenerUseCase
import com.faceunity.app_ptag.use_case.renderer.CreateRendererUseCase
import com.faceunity.app_ptag.use_case.renderer.RendererBindLifecycleUseCase
import com.faceunity.app_ptag.util.DialogDisplayHelper
import com.faceunity.app_ptag.util.ToastUtils
import com.faceunity.app_ptag.view_model.FuAvatarManagerViewModel
import com.faceunity.app_ptag.view_model.FuEditViewModel
import com.faceunity.app_ptag.view_model.FuPreviewViewModel
import com.faceunity.app_ptag.weight.DownloadingDialog
import com.faceunity.app_ptag.weight.FuDemoRetryDialog
import com.faceunity.core.entity.FUColorRGBData
import com.faceunity.core.faceunity.FUSceneKit
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.util.*
import com.faceunity.fupta.facepup.entity.bodyshape.BodyShapeItem
import com.faceunity.fupta.facepup.entity.tier.FacepupSlider
import com.faceunity.pta.pta_core.widget.TouchGLTextureView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FuDemoEditFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = FuDemoEditFragment()
    }

    private var _binding: FuEditFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<FuDemoEditViewModel>()
    private lateinit var fuPreviewViewModel: FuPreviewViewModel
    private lateinit var fuAvatarManagerViewModel: FuAvatarManagerViewModel
    private lateinit var fuEditViewModel: FuEditViewModel

    private val downloadingDialog: DownloadingDialog by lazy {
        DownloadingDialog(requireContext())
    }

    private val ptaRenderer = CreateRendererUseCase.build().apply {
        RendererBindLifecycleUseCase(this).execute(lifecycle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FuEditFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showSaveDialog()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fuPreviewViewModel = ViewModelProvider(this).get(FuPreviewViewModel::class.java)
        fuAvatarManagerViewModel = ViewModelProvider(this).get(FuAvatarManagerViewModel::class.java)
        fuEditViewModel = ViewModelProvider(this).get(FuEditViewModel::class.java)

        initView()
        initRenderer()
        initFacepup()
        initBodyShape()
        subscribeUi()
        subscribeEditUi()

    }

    private fun requestRender() {
        lifecycleScope.launch {
            viewModel.requestEditorData().onSuccess {
                binding.avatarControlView.holder.bindData(it) //将编辑组件的控制模型绑定到 Avatar 控制组件
                binding.avatarControlView.holder.bindControlListener(controlListener)
            }.onFailure {
                ToastUtils.showFailureToast(requireContext(), it.toString())
            }
            viewModel.initFacepup()
            viewModel.drawAvatar()
        }

    }

    private fun initView() {
        binding.glTextureView.apply {
            isOpaque = false
            if (this is TouchGLTextureView) {
                setTouchListener(object : TouchGLTextureView.OnTouchListener {

                    override fun onScale(scale: Float) {

                    }

                    override fun onMove(deltaX: Float, deltaY: Float) {
                        fuPreviewViewModel.rotateAvatar(deltaX * (binding.glTextureView.width))
                    }

                    override fun onClick() {

                    }

                    override fun onUp() {

                    }
                })
            }
        }
        binding.backBtn.setOnClickListener {
            showSaveDialog()
        }
        binding.saveBtn.setOnClickListener {
            if (it.isEnabled) {
                saveCurrentAvatar {
                    viewModel.syncAvatarEditStatus()
                    ToastUtils.showSuccessToast(requireContext(), "形象已保存")
                }
            } else {
                ToastUtils.showFailureToast(requireContext(), "形象未修改")
            }
        }
        binding.avatarControlView.visibleControl()
    }


    private fun initRenderer() {
        ptaRenderer.apply {
            bindGLTextureView(binding.glTextureView)
            BindRendererListenerUseCase(ptaRenderer, listener = object : BindRendererListenerUseCase.Listener {
                override fun surfaceState(isAlive: Boolean) {
                    if (isAlive) requestRender()
                    if (isAlive) {
                        Dispatchers.GL.init(binding.glTextureView)
                    } else {
                        Dispatchers.GL.release()
                    }
                }
            })
        }
    }

    private fun initFacepup() {
        binding.facepupControlView.apply {

            setControlListener(object : FacepupControlView.ControlListener {

                private var bodyShapeParams: Map<String, Float> = emptyMap()

                override fun onSeekBarScroll(slider: FacepupSlider, scale: Float) {
                    viewModel.setFacepupTierSeekBarItem(getFileId(), getGroupKey(), slider, scale)
                }

                override fun onResetClick(groupKey: String) {
                    viewModel.resetFacepupByGroupKey(groupKey)
                }

                override fun onStart() {
                    kotlin.runCatching {
                        val avatar = DevAvatarManagerRepository.getCurrentAvatarInfo()!!.avatar
                        bodyShapeParams = FuBodyShapePreviewHelper.getCurrentInfo(FuBodyShapePreviewHelper.Type.HalfBody,
                            avatar
                        )
                        FuBodyShapePreviewHelper.setInfoByDefault(FuBodyShapePreviewHelper.Type.HalfBody, avatar)
                    }
                    switchCamera("banshen") {
                        binding.facepupControlView.visible()
                        binding.avatarControlView.gone()
                        binding.backBtn.gone()
                        binding.saveBtn.gone()
                    }
                }

                override fun onFinish() {
                    kotlin.runCatching {
                        val avatar = DevAvatarManagerRepository.getCurrentAvatarInfo()!!.avatar
                        FuBodyShapePreviewHelper.setInfoByInput(avatar, bodyShapeParams)
                    }
                    switchCamera("quanshen") {
                        binding.facepupControlView.gone()
                        binding.avatarControlView.visible()
                        binding.backBtn.visible()
                        binding.saveBtn.visible()
                        viewModel.pushHistory()
                    }
                }

                override fun onModeSwitch(mode: Int) {
                    showFacepupDialog(getGroupKey(), getFileId(), mode)
                }


                private fun switchCamera(bundleName: String, onStart: (() -> Unit)? = null) {
                    FUSceneKit.getInstance().executeGLAction({
                        SwitchCameraUseCase().execute(bundleName)
                        lifecycleScope.launch(Dispatchers.Main) {
                            onStart?.invoke()
                        }
                    })
                }
            })
        }
    }

    private fun initBodyShape() {
        binding.bodyShapeControlView.apply {
            setControlListener(object : BodyShapeControlView.ControlListener {
                override fun onSeekBarScroll(bodyShapeItem: BodyShapeItem, value: Float) {
                    viewModel.setBodyShape(bodyShapeItem, value)
                }

                override fun onResetClick() {
                    viewModel.resetBodyShape()
                }

                override fun onStart() {
                    binding.bodyShapeControlView.visible()
                    binding.avatarControlView.gone()
                    binding.backBtn.gone()
                    binding.saveBtn.gone()
                }

                override fun onFinish() {
                    binding.bodyShapeControlView.gone()
                    binding.avatarControlView.visible()
                    binding.backBtn.visible()
                    binding.saveBtn.visible()
                    viewModel.pushHistory()
                }
            })
        }
        //申请并收集 捏形 UI 更新
        lifecycleScope.launchWhenResumed {
            viewModel.bodyShapeUiState.safeCollect {
                FuLog.debug("collect bodyShapeUiState：$it")
                binding.bodyShapeControlView.collect(it)
            }
        }
    }

    /**
     * 基础 UI 相关的事件订阅
     */
    private fun subscribeUi() {
        //加载状态 UI 更新
        viewModel.getLoadingState().observe(viewLifecycleOwner) {
            when (it) {
                is LoadingState.Hidden -> {
                    DialogDisplayHelper.dismiss(downloadingDialog)
                }
                is LoadingState.Show -> {
                    DialogDisplayHelper.show(downloadingDialog)
                }
                is LoadingState.ShowWithTip -> {
                    downloadingDialog.updateText(it.content, it.processText)
                    DialogDisplayHelper.show(downloadingDialog)
                }
            }
        }
        //异常提示 UI 更新
        viewModel.getExceptionEvent().observe(viewLifecycleOwner) {
            when (it) {
                is ExceptionEvent.Nothing -> {

                }
                is ExceptionEvent.RetryDialog -> {
                    val retryDialog = FuDemoRetryDialog(requireContext(), it.content).apply {
                        callback = object : FuDemoRetryDialog.Callback {
                            override fun onCancel() {
                                dismiss()
                            }

                            override fun onFinish() { //网络错误下的重试按钮要执行的操作
                                it.retryBlock()
                                dismiss()
                            }
                        }
                        create()
                    }
                    DialogDisplayHelper.show(retryDialog)
                }
                is ExceptionEvent.FailedToast -> {
                    ToastUtils.showFailureToast(requireContext(), it.content)
                }
            }
            viewModel.finishExceptionEvent()
        }
        //编辑页其他 UI 状态更新
        lifecycleScope.launch {
            viewModel.editPageUiState.collect {
                if (it.isAvatarPrepare) { //在形象加载出来后，同步它的相关信息
                    viewModel.syncAvatarEditStatus()
                    viewModel.requestBodyShape()
                }
                binding.facepupControlView.syncGender(it.gender) //将性别信息传给捏脸页面
                binding.avatarControlView.holder.notifyFilterGroup(it.filterGroup) //同步刷新 UI 的筛选状态
            }
        }
    }

    /**
     * 编辑模型相关的事件订阅
     */
    private fun subscribeEditUi() {
        //下载状态 UI 更新
        viewModel.editItemDownloadState.observe(viewLifecycleOwner) { state ->
            binding.avatarControlView.controlStyleRecord {
                when(state) {
                    is EditItemDownloadState.Start -> notifyDownloadStart(state.fileIdList)
                    is EditItemDownloadState.Success -> notifyDownloadSuccess(state.fileIdList)
                    is EditItemDownloadState.Error -> notifyDownloadError(state.fileIdList)
                }
            }
        }
        //选中状态 UI 更新
        lifecycleScope.launchWhenResumed {
            viewModel.selectedUiState.safeCollect {
                FuLog.debug("collect selectedUiState：$it")
                binding.avatarControlView.holder.notifySelectUiState(it)
            }
        }
        //历史状态 UI 更新
        lifecycleScope.launchWhenResumed {
            viewModel.editHistoryUiState.safeCollect {
                FuLog.debug("collect editHistoryUiState：$it")
                binding.avatarControlView.setHistoryBackEnable(it.canBack)
                binding.avatarControlView.setHistoryForwardEnable(it.canForward)
                binding.avatarControlView.setHistoryResetEnable(it.canReset)
                binding.saveBtn.isEnabled = it.canReset
            }
        }

    }


    private val controlListener = object : AvatarControlListener {
        private var currentFacepupGroupKey: String? = null

        override fun onMinorSelect(item: MinorCategoryModel) {
            viewModel.notifySwitchMinorMenu(item.key)
            if (viewModel.isHasFacepup(item.key)) {
                binding.avatarControlView.visibleFacepupBtn()
                currentFacepupGroupKey = item.key
            } else {
                binding.avatarControlView.goneFacepupBtn()
                currentFacepupGroupKey = null
            }
            if (binding.avatarControlView.holder.getCurrentMinorType() == SubCategoryModel.Type.Config) {
                binding.avatarControlView.visibleFacepupBtn()
            }
        }

        override fun onNormalItemClick(item: SubCategoryBundleModel) {
            val fileId = item.fileId
            if (fileId in viewModel.selectedUiState.value.bundle) {
                FuLog.debug("当前条目已选择，跳过执行。")
                return
            }
            if (currentFacepupGroupKey != null && !viewModel.isHasFacepupPackInfo(fileId)) { //当点击道具时重置该组的捏脸信息。除非是当前道具。
                viewModel.resetFacepupByGroupKey(currentFacepupGroupKey!!)
            }
            lifecycleScope.launchWhenResumed {
                viewModel.clickItemByDispatcher(fileId).onSuccess {
                }.onFailure {
                    ToastUtils.showFailureToast(requireContext(), "穿戴失败：${it.message ?: it}")
                }
            }
        }

        override fun onColorItemClick(item: SubCategoryColorModel) {
            if (viewModel.selectedUiState.value.color.get(item.key) == item.color) {
                FuLog.debug("当前条目已选择，跳过执行。")
                return
            }
            viewModel.clickColor(item.key, item.color.let { FUColorRGBData(it.red, it.green, it.blue) })
        }

        override fun onConfigItemClick(item: SubCategoryConfigModel) {
            val facePupConfig = item.facePupConfig
            if (facePupConfig == null) {
                ToastUtils.showFailureToast(requireContext(), "配置文件异常")
                return
            }
            viewModel.clickBodyShapeConfig(facePupConfig)
        }

        override fun onFacepupClick(groupKey: String, fileId: String?) {
            showFacepupDialog(groupKey, fileId, null)
        }

        override fun onBodyShapeClick() {
            binding.bodyShapeControlView.start()
        }

        override fun onHistoryBackClick() {
            viewModel.historyBack()
        }

        override fun onHistoryForwardClick() {
            viewModel.historyForward()
        }

        override fun onHistoryResetClick() {
            viewModel.historyReset()
        }
    }

    private fun showFacepupDialog(groupKey: String, fileId: String?, mode: Int?) {
        if (!viewModel.isHasFacepup(groupKey)) {
            ToastUtils.showFailureToast(requireContext(), "没有找到对应的捏脸数据")
            return
        }
        if (fileId == null) {
            ToastUtils.showFailureToast(requireContext(), "没有找到选中的道具")
            return
        }
        val customGroup = viewModel.buildCustomGroup(groupKey, mode ?: viewModel.facepupMode)
        binding.facepupControlView.apply {
            syncGroupInfo(customGroup, fileId)
            syncFacepupMode(viewModel.facepupMode)
            show()
        }
    }

    private val saveAvatarDialog by lazy {
        FuDemoSaveAvatarDialog(requireContext()).apply {
            onCancel = {
                //当不保存形象时，需要根据 AvatarInfo 的 avatar.json 重建一个 Avatar 绑定到该 AvatarInfo 上。
                viewModel.rebuildAvatar()
                findNavController().popBackStack()
                dismiss()
            }
            onFinish = {
                saveCurrentAvatar {
                    viewModel.syncAvatarEditStatus()
                    findNavController().popBackStack()
                    dismiss()
                }
            }
            create()
        }
    }

    private fun saveCurrentAvatar(onSuccess: () -> Unit) {
        lifecycleScope.launchWhenResumed {
            viewModel.saveCurrentAvatar().onStart {
                downloadingDialog.updateText("形象保存中，请稍等")
                DialogDisplayHelper.show(downloadingDialog)
            }.onCompletion {
                DialogDisplayHelper.dismiss(downloadingDialog)
            }.onEach {
                onSuccess()
            }.catch {
                ToastUtils.showFailureToast(requireContext(), "形象保存失败：$it")
            }.safeCollect{}
        }
    }

    private fun showSaveDialog() {
        if (binding.saveBtn.isEnabled) {
            saveAvatarDialog.show()
        } else {
            findNavController().popBackStack()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        ptaRenderer.release()
        viewModel.release()
    }

}