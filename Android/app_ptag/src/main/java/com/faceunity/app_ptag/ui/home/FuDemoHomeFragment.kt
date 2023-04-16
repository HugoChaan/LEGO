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
import com.faceunity.app_ptag.view_model.FuStaViewModel
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
import kotlin.math.absoluteValue


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

        fuStaViewModel.initSTARender()
        fuStaViewModel.initSTAServer()

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
        binding.buildAvatarBtn.setOnClickListener {
           findNavController().navigate(R.id.buildFragment)
        }
        binding.glTextureView.apply {
            isOpaque = false
        }
        binding.touchView.apply {
            setTouchListener(object : TouchView.OnTouchListener {
                private var lastDeltaX = 0f

                override fun onScale(scale: Float) {
                    fuPreviewViewModel.scaleAvatar(scale)
                }

                override fun onMove(deltaX: Float, deltaY: Float) {
                    lastDeltaX = deltaX
                    fuPreviewViewModel.rotateAvatar(deltaX * (binding.glTextureView.width))
                    fuPreviewViewModel.cancelRollAvatar()
                    fuPreviewViewModel.moveVerticalAvatar(-deltaY) //Android 与 OpenGL 坐标系上下相反，故为负
                }

                override fun onClick() {
                }

                override fun onUp() {
                    if (lastDeltaX.absoluteValue > 0.001) { //如果手势离开屏幕时高于一定的速度,则触发惯性滚动
                        fuPreviewViewModel.rollAvatar(lastDeltaX)
                    }
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
        binding.interactionBtn.setOnClickListener {
            findNavController().navigate(R.id.interactionFragment)
        }
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

                override fun fpsPrint(fps: Double, renderTime: Double) {}

                override fun onRenderAfter(
                    outputData: FURenderOutputData,
                    drawMatrix: FUDrawFrameMatrix
                ) {
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

    private val fuStaViewModel by viewModels<FuStaViewModel>()

    private fun onAvatarShow() {
        FuLog.info("Avatar show on screen.")
        isAvatarShow = true
        fuStaViewModel.auditionVoice("大家好， 欢迎来到乐高app", "Ailun")
        binding.glTextureView.postDelayed({
            findNavController().navigate(R.id.interactionFragment)
        }, 1500)
    }

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