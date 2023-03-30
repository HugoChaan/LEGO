package com.faceunity.app_ptag.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.faceunity.app_ptag.FuDevInitializeWrapper
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.FuDemoHomeFragmentBinding
import com.faceunity.app_ptag.ui.home.dev.IDevBuilderInstance
import com.faceunity.app_ptag.ui.home.entity.state.ExceptionEvent
import com.faceunity.app_ptag.ui.home.entity.state.LoadingState
import com.faceunity.app_ptag.ui.home.widget.FuDemoCopySuccessDialog
import com.faceunity.app_ptag.ui.home.widget.FuDemoShareDialog
import com.faceunity.app_ptag.use_case.android.SaveBitmapUseCase
import com.faceunity.app_ptag.use_case.render_kit.SwitchCameraUseCase
import com.faceunity.app_ptag.use_case.renderer.BindRendererListenerUseCase
import com.faceunity.app_ptag.use_case.renderer.CreateRendererUseCase
import com.faceunity.app_ptag.use_case.renderer.PhotoRecordUseCase
import com.faceunity.app_ptag.use_case.renderer.RendererBindLifecycleUseCase
import com.faceunity.app_ptag.util.DialogDisplayHelper
import com.faceunity.app_ptag.util.ToastUtils
import com.faceunity.app_ptag.util.expand.format
import com.faceunity.app_ptag.view_model.FuAvatarManagerViewModel
import com.faceunity.app_ptag.view_model.FuPreviewViewModel
import com.faceunity.app_ptag.weight.DownloadingDialog
import com.faceunity.app_ptag.weight.FuDemoRetryDialog
import com.faceunity.app_ptag.weight.avatar_manager.AvatarManagerDialog
import com.faceunity.app_ptag.weight.avatar_manager.entity.FuAvatarContainer
import com.faceunity.app_ptag.weight.avatar_manager.entity.FuAvatarWrapper
import com.faceunity.app_ptag.weight.avatar_manager.parser.FuAvatarContainerParser
import com.faceunity.app_ptag.weight.dev_setting.DevSettingDialog
import com.faceunity.app_ptag.weight.dev_setting.entity.DevSetting
import com.faceunity.core.avatar.model.Scene
import com.faceunity.core.entity.FURenderOutputData
import com.faceunity.core.renderer.entity.FUDrawFrameMatrix
import com.faceunity.core.renderer.infe.OnGLRendererListener
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.editor_ptag.util.GL
import com.faceunity.editor_ptag.util.isVisible
import com.faceunity.editor_ptag.util.visibleOrGone
import com.faceunity.pta.pta_core.widget.TouchView
import com.faceunity.toolbox.media.FUMediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File


/**
 * 主页
 */
class FuDemoHomeFragment : Fragment() {

    private var _binding: FuDemoHomeFragmentBinding? = null
    private val binding get() = _binding!!

    private val fuAvatarManagerViewModel by viewModels<FuAvatarManagerViewModel>()
    private val fuPreviewViewModel by viewModels<FuPreviewViewModel>()
    private val viewModel by viewModels<FuDemoHomeViewModel>()

    private val avatarWrapper = FuAvatarContainer(mutableListOf(), mutableListOf())
    private lateinit var avatarManagerDialog: AvatarManagerDialog
    private val downloadingDialog: DownloadingDialog by lazy {
        DownloadingDialog(requireContext())
    }


    private val ptaRenderer = CreateRendererUseCase.buildLessRam().apply {
        RendererBindLifecycleUseCase(this).execute(lifecycle)
    }

    private var loadAvatarId: String? = null

    private var isAvatarShow: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FuDemoHomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initRenderer()
        initAvatarManagerDialog()
        subscribeUi()
        setAvatarLoadListener()

        loadAvatarId = arguments?.getString("avatarId")?.ifBlank { null }
        viewModel.initCloud()
        loadAvatarId?.run {
            viewModel.specifyDefaultAvatar(this)
        }
        viewModel.tryFastShow()
        isAvatarShow = false
    }

    /**
     * 请求渲染虚拟形象。通常在 [OnGLRendererListener.onSurfaceCreated] 中调用。
     */
    private fun requestRender() {
        viewModel.drawDefaultAvatarOnReady()
    }

    private fun initView() {
        binding.glTextureView.apply {
            isOpaque = false
        }
        binding.touchView.apply {
            setTouchListener(object : TouchView.OnTouchListener {
                private var lastDeltaX = 0f

                override fun onScale(scale: Float) {
//                    fuPreviewViewModel.scaleAvatar(scale)
                }

                override fun onMove(deltaX: Float, deltaY: Float) {
                    lastDeltaX = deltaX
                    fuPreviewViewModel.rotateAvatar(deltaX * (binding.glTextureView.width))
                    fuPreviewViewModel.cancelRollAvatar()
//                    fuPreviewViewModel.moveVerticalAvatar(-deltaY) //Android 与 OpenGL 坐标系上下相反，故为负
                }

                override fun onClick() {
                }

                override fun onUp() {
//                    if (lastDeltaX.absoluteValue > 0.001) { //如果手势离开屏幕时高于一定的速度,则触发惯性滚动
//                        fuPreviewViewModel.rollAvatar(lastDeltaX)
//                    }
                }
            })
        }
        binding.managerAvatarBtn.setOnClickListener {
            fuAvatarManagerViewModel.requestAvatarContainer()
            avatarManagerDialog.show()
        }
        binding.editAvatarBtn.setOnClickListener {
            findNavController().navigate(R.id.editFragment)
        }
        binding.buildAvatarBtn.setOnClickListener {
            findNavController().navigate(R.id.buildFragment)
        }
        binding.driveBtn.setOnClickListener {
            findNavController().navigate(R.id.driveFragment)
        }
        binding.interactionBtn.setOnClickListener {
            findNavController().navigate(R.id.interactionFragment)
        }
        binding.savePhotoBtn.setOnClickListener {
            if (!isAvatarShow) {
                ToastUtils.showFailureToast(requireContext(), "请等待 Avatar 加载完成")
                return@setOnClickListener
            }
            saveSnapShotToFile()
        }
        binding.scanBtn.setOnClickListener {
            findNavController().navigate(R.id.scanCodeFragment)
        }
        if (IDevBuilderInstance.enableDevConfig()) {
            binding.savePhotoBtn.setOnLongClickListener {
                binding.devSettingBtn.visibleOrGone(!binding.devSettingBtn.isVisible())
                true
            }
            val randomAvatarDevMenu = DevSetting.buildClick("随机 Avatar")
            {
                IDevBuilderInstance.buildRandomAvatar(lifecycleScope, requireContext(), downloadingDialog, fuAvatarManagerViewModel)
            }

            binding.devSettingBtn.apply {
                val devSettingDialog = DevSettingDialog(requireContext())
                devSettingDialog.setSetting(IDevBuilderInstance.buildHomeDevMenu(requireContext()) + IDevBuilderInstance.buildTempTest() + randomAvatarDevMenu)
                setOnClickListener {
                    devSettingDialog.show()
                }
            }
        }

        val isShowFPS =  IDevBuilderInstance.isShowFps() ?: false
        binding.testInfo.visibleOrGone(isShowFPS)

    }

    private fun initRenderer() {
        ptaRenderer.apply {
            bindGLTextureView(binding.glTextureView)
            val params = BindRendererListenerUseCase.Params(true)
            BindRendererListenerUseCase(ptaRenderer, params, listener = object : BindRendererListenerUseCase.Listener {
                override fun surfaceState(isAlive: Boolean) {
                    viewModel.syncOpenGLState(isAlive)
                    if (isAlive) {
                        requestRender()
                    } else {
                        isAvatarShow = false
                    }
                    if (isAlive) {
                        Dispatchers.GL.init(binding.glTextureView)
                    } else {
                        Dispatchers.GL.release()
                    }
                }

                override fun fpsPrint(fps: Double, renderTime: Double) {
                    lifecycleScope.launchWhenResumed {
                        binding.testInfo.text = "fps:${fps.format(1)}，renderTime:${renderTime.format(1)}"
                    }
                }

                override fun onRenderAfter(
                    outputData: FURenderOutputData,
                    drawMatrix: FUDrawFrameMatrix
                ) {
                    if (isNeedTakePic) { //截图功能，不需要可去掉
                        lifecycleScope.launch {
                            photoRecordUseCase.runCatching {
                                execute(outputData, PhotoRecordUseCase.Config(drawMatrix.texMatrix, true))
                            }.onSuccess {
                                onSaveSnapShotSuccess(it)
                            }.onFailure {
                                onSaveSnapShotError(it.toString())
                            }
                        }
                        isNeedTakePic = false
                    }
                    if (isAvatarExecuteCompleted) {
                        onAvatarShow()
                        isAvatarExecuteCompleted = false
                    }
                }

            })
        }
    }


    private fun initAvatarManagerDialog() {
        avatarManagerDialog = AvatarManagerDialog(
            requireContext(),
            onItemClick = { item: FuAvatarWrapper ->
                fuAvatarManagerViewModel.smartSwitchAvatar(item.id) //加载形象。如果已准备好则直接加载，否则从云端下载。
            },
            onItemDelete = { item: FuAvatarWrapper ->
                fuAvatarManagerViewModel.removeAvatar(item.id) //删除形象
            }
        )
        avatarManagerDialog.create()

        fuAvatarManagerViewModel.avatarCollectionLiveData.observe(viewLifecycleOwner) {
            val fuAvatarContainerParser = FuAvatarContainerParser()
            val wrapperList = DevAvatarManagerRepository.mapAvatar {
                fuAvatarContainerParser.parserAvatarInfoToFuAvatarWrapper(it)
            }
            avatarWrapper.avatarList.apply {
                clear()
                addAll(wrapperList)
            }
            avatarManagerDialog.syncAvatarContainer(avatarWrapper) //同步最新的形象容器至 UI
        }
        fuAvatarManagerViewModel.avatarSelectLiveData.observe(viewLifecycleOwner) {
            avatarWrapper.selectId.apply {
                clear()
                it?.let { add(it) }
            }
            avatarManagerDialog.syncAvatarContainer(avatarWrapper) //同步最新的形象容器至 UI
        }
    }

    private fun subscribeUi() {
        fun onLoadingState(it: LoadingState) {
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
                else -> {}
            }
        }
        fun onExceptionEvent(it: ExceptionEvent) {
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
        }
        viewModel.loadingState.observe(viewLifecycleOwner) {
            onLoadingState(it)
        }
        viewModel.exceptionEvent.observe(viewLifecycleOwner) {
            onExceptionEvent(it)
            viewModel.finishExceptionEvent()
        }
        var isInitAvatarContainer = false
        viewModel.notifySwitchAvatar.observe(viewLifecycleOwner) {
            //服务器和基础接口初始化完毕
            if (!isInitAvatarContainer) {
                fuAvatarManagerViewModel.requestAvatarContainer()
                isInitAvatarContainer = true
            }
            fuAvatarManagerViewModel.smartLoadAvatar(it)
        }
        fuAvatarManagerViewModel.loadingState.observe(viewLifecycleOwner) {
            onLoadingState(it)
        }
        fuAvatarManagerViewModel.exceptionEvent.observe(viewLifecycleOwner) {
            onExceptionEvent(it)
            fuAvatarManagerViewModel.finishExceptionEvent()
        }
    }

    private var isAvatarExecuteCompleted = false

    private fun setAvatarLoadListener() {
        DevDrawRepository.setSceneEvent(object : DevDrawRepository.SceneEvent {
            override fun onSceneCreated(scene: Scene) {
                if (scene.getAvatars().isNotEmpty()) {
                    isAvatarExecuteCompleted = true
                }
            }
        })
    }

    private fun onAvatarShow() {
        FuLog.info("Avatar show on screen.")
        isAvatarShow = true
    }

    //region 截图

    private val photoRecordUseCase by lazy {
        PhotoRecordUseCase()
    }


    @Volatile
    private var isNeedTakePic = false

    /**
     * 截图功能。成功则会调用 [onSaveSnapShotSuccess]，失败则会调用 [onSaveSnapShotError]
     */
    private fun saveSnapShotToFile() {
        isNeedTakePic = true
    }


    private fun onSaveSnapShotSuccess(bitmap: Bitmap) {
        val cacheImageFile = File(requireContext().externalCacheDir, "cacheImage${System.currentTimeMillis()}.png")
        cacheImageFile.apply {
            FUMediaUtils.addBitmapToExternal(path, bitmap, false)
        }


        requireActivity().runOnUiThread {
            FuDemoShareDialog(requireContext(), bitmap, object : FuDemoShareDialog.OnClickListener {
                override fun onSaveClick() {
                    saveBitmapToFile(bitmap)
                }

                override fun onCopyClick() {
                    lifecycleScope.launchWhenResumed {
                        viewModel.uploadCurrentAvatar().collect { avatarIdResult ->
                            avatarIdResult.onSuccess { avatarId ->
                                FuDemoCopySuccessDialog(requireContext(), avatarId).show()
                                fun copy(text: String) {
                                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val textCd = ClipData.newPlainText("text", text)
                                    clipboard.setPrimaryClip(textCd)
                                }
                                copy(avatarId)
                            }.onFailure {
                                ToastUtils.showFailureToast(requireContext(), it.toString())
                            }
                        }
                    }
                }

                override fun onSaveQrCodeClick() {
                    lifecycleScope.launchWhenResumed {
                        viewModel.buildQrCode(cacheImageFile).onStart {
                            downloadingDialog.updateText("生成二维码中")
                            DialogDisplayHelper.show(downloadingDialog)
                        }.onCompletion {
                            DialogDisplayHelper.dismiss(downloadingDialog)
                        }.onEach {
                            saveQRCodeBitmapToFile(it.bitmap, it.avatarId)
                        }.catch {
                            ToastUtils.showFailureToast(requireContext(), "生成二维码异常：$it")
                        }.collect()
                    }
                }
            }).show()
        }
    }

    private fun onSaveSnapShotError(errorMsg: String) {
        ToastUtils.showFailureToast(requireContext(), errorMsg)
    }

    /**
     * 将录制的照片发送到手机 "Pictures/PTAG/" 目录下
     */
    private fun saveBitmapToFile(bitmap: Bitmap) {
        SaveBitmapUseCase(bitmap, "avatar").run {
            if (this) {
                ToastUtils.showSuccessToast(requireContext(), "形象已保存至相册！")
            } else {
                ToastUtils.showFailureToast(requireContext(), "形象保存失败")
            }
        }
    }

    private fun saveQRCodeBitmapToFile(bitmap: Bitmap, avatarId: String = "") {
        SaveBitmapUseCase(bitmap, "qrcode-$avatarId").run {
            if (this) {
                ToastUtils.showSuccessToast(requireContext(), "二维码已保存至相册！")
            } else {
                ToastUtils.showFailureToast(requireContext(), "二维码保存失败")
            }
        }
    }

    //endregion 截图

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        DialogDisplayHelper.dismiss(downloadingDialog) //防止快速切换页面之类的
        if (this::avatarManagerDialog.isInitialized) {
            avatarManagerDialog.dismiss() //进入其他页面，如果点击过 AvatarManager，则需要关闭
        }
        DevDrawRepository.clearAvatarEvent()
        DevDrawRepository.clearSceneEvent()
        ptaRenderer.release()
        FuDevInitializeWrapper.releaseRenderKit()
    }

}