package com.faceunity.app_ptag.ui.drive

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.agora.gpt.ConvertClient
import com.agora.gpt.ConvertListener
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.FuDemoDriveFragmentBinding
import com.faceunity.app_ptag.databinding.FuDriveSettingBottomsheetBinding
import com.faceunity.app_ptag.ui.drive.entity.BodyFollowMode
import com.faceunity.app_ptag.ui.drive.entity.BodyTrackMode
import com.faceunity.app_ptag.ui.drive.entity.DrivePage
import com.faceunity.app_ptag.ui.home.dev.IDevBuilderInstance
import com.faceunity.app_ptag.ui.interaction.util.KeyboardHeightObserver
import com.faceunity.app_ptag.ui.interaction.util.KeyboardHeightProvider
import com.faceunity.app_ptag.ui.interaction.util.KeyboardUtils
import com.faceunity.app_ptag.use_case.render_kit.SwitchCameraUseCase
import com.faceunity.app_ptag.use_case.renderer.BindRendererListenerUseCase
import com.faceunity.app_ptag.use_case.renderer.CreateRendererUseCase
import com.faceunity.app_ptag.use_case.renderer.RendererBindLifecycleUseCase
import com.faceunity.app_ptag.util.ToastUtils
import com.faceunity.app_ptag.util.expand.format
import com.faceunity.app_ptag.view_model.FuDriveViewModel
import com.faceunity.app_ptag.view_model.FuStaViewModel
import com.faceunity.core.entity.FUPostProcessMirrorParamData
import com.faceunity.core.entity.FURenderInputData
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.visible
import com.faceunity.editor_ptag.util.visibleOrGone
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * 驱动页面
 * 此处为了 UI 状态，所有开关经过了一层 [FuDemoDriveViewModel] 中转。也可根据实际需求直接调用 [FuDriveViewModel] 中的方法
 */
class FuDemoDriveFragment : Fragment(), KeyboardHeightObserver {

    companion object {
        fun newInstance() = FuDemoDriveFragment()
    }

    private lateinit var binding: FuDemoDriveFragmentBinding
    private lateinit var settingBinding: FuDriveSettingBottomsheetBinding

    private lateinit var viewModel: FuDemoDriveViewModel
    protected lateinit var fuDriveViewModel: FuDriveViewModel
    private val fuStaViewModel by viewModels<FuStaViewModel>()

    private lateinit var settingDialog: BottomSheetDialog

    private lateinit var mKeyboardHeightProvider: KeyboardHeightProvider

    private var mKeyboardHeight: Int = 0


    private val ptaRenderer = CreateRendererUseCase.buildCamera().apply {
        //RendererBindLifecycleUseCase(this).execute(lifecycle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FuDemoDriveFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(FuDemoDriveViewModel::class.java)
        fuDriveViewModel = ViewModelProvider(this).get(FuDriveViewModel::class.java)

        initView()
        initSettingDialog()
        initRenderer()
        inject()

        updateFPSInfo()

        ConvertClient.getInstance().init(activity)
        ConvertClient.getInstance()
            .setChatGPTListener(object : ConvertListener {
                override fun onVoice2Text(index: Int, text: String?) {
                    Log.i("LEGO", "onVoice2Text: text=$text")
                }

                override fun onText2Voice(index: Int, pcmFilePath: String?) {
                    Log.i("LEGO", "onText2Voice: pcmFilePath=$pcmFilePath")
                }

                override fun onQues2AnsSuccess(index: Int, question: String?, answer: String?) {
                    //问chatGPT回调结果
                    Log.i("LEGO", "question=$question,answer=$answer")
                    val ans = answer ?: return
                    if (FuDevDataCenter.staControl == null) {
                        ToastUtils.showFailureToast(requireContext(), "未配置语音驱动服务")
                        return
                    }
                    fuStaViewModel.auditionVoice(ans)
                }

                override fun onFailure(errorCode: Int, message: String?) {
                    //各种错误回调
                    Log.i("hugo", "onFailure ,message=$message")
                }
            })
        ConvertClient.getInstance().startVoice2Text()
    }

    private fun updateFPSInfo() {
        val isShowFPS = IDevBuilderInstance.isShowFps() ?: false
        binding.testInfo.visibleOrGone(isShowFPS)
    }

    private fun requestRender() {
        viewModel.drawAvatar()
        fuStaViewModel.initSTARender()
        fuStaViewModel.initSTAServer()
    }

    private fun initView() {
        binding.glTextureView.apply {
            isOpaque = false
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    view.performClick()
                }
                ptaRenderer.onTouchEvent(
                    event.x.toInt(),
                    event.y.toInt(),
                    event.action
                ) //为了可以拖拽预览相机窗口
                mKeyboardHeight == 0
            }

            setOnClickListener {
                if (mKeyboardHeight > 0) {
                    KeyboardUtils.hideSoftInput(activity)
                    playAnim(binding.frameInput, 0)
                }
            }

        }
        binding.backBtn.apply {
            setOnClickListener {
                Navigation.findNavController(it).popBackStack() //返回上一个页面
            }
        }
        binding.settingBtn.apply {
            setOnClickListener {
                settingDialog.show()
            }

        }
//        binding.switchCameraToward.apply {
//            setOnClickListener {
//                ptaRenderer.switchCamera() //切换相机
//            }
//        }
//        binding.arMode.apply {
//            setOnClickListener {
//                viewModel.checkoutArPage() //切换AR模式，完成后会通知 [drivePageLiveData]
//            }
//        }
//        binding.arModeText.apply {
//            setOnClickListener {
//                viewModel.checkoutArPage() //切换AR模式，完成后会通知 [drivePageLiveData]
//            }
//        }
//        binding.trackMode.apply {
//            setOnClickListener {
//                viewModel.checkoutTrackPage() //切换跟踪模式，完成后会通知 [drivePageLiveData]
//            }
//        }
//        binding.trackModeText.apply {
//            setOnClickListener {
//                viewModel.checkoutTrackPage() //切换跟踪模式，完成后会通知 [drivePageLiveData]
//            }
//        }
        binding.textMode.apply {
            setOnClickListener {
                viewModel.checkoutTextPage() //切换文本驱动模式，完成后会通知 [drivePageLiveData]
            }
        }
        binding.textModeText.apply {
            setOnClickListener {
                viewModel.checkoutTextPage() //切换文本驱动模式，完成后会通知 [drivePageLiveData]
            }
        }
        binding.btnSend.apply {
            setOnClickListener {
                if (FuDevDataCenter.staControl == null) {
                    ToastUtils.showFailureToast(requireContext(), "未配置语音驱动服务")
                    return@setOnClickListener
                }
                val auditionText = binding.etInput.text.toString().trim()
                fuStaViewModel.auditionVoice(auditionText)
                binding.etInput.text.clear()
                KeyboardUtils.hideSoftInput(activity)
            }
        }
        mKeyboardHeightProvider = KeyboardHeightProvider(activity)
        mKeyboardHeightProvider.setKeyboardHeightObserver(this)
        binding.etInput.apply {
            setOnClickListener {
                KeyboardUtils.showSoftInput(activity, binding.etInput)
            }
            post {
                mKeyboardHeightProvider.start()
            }
            addTextChangedListener {
                doOnTextChanged { text, start, before, count -> if (TextUtils.isEmpty(text)) binding.btnSend.gone() else binding.btnSend.visible() }
            }
        }

    }

    private fun initSettingDialog() {
        settingBinding =
            FuDriveSettingBottomsheetBinding.inflate(LayoutInflater.from(requireContext())).apply {

            }
        settingDialog = BottomSheetDialog(requireContext()).apply {
            setContentView(settingBinding.root)
            dismissWithAnimation = true
        }

        fun enableFollowMode(enable: Boolean) {
            settingBinding.bodyFollowModeLayout.isEnabled = enable
            settingBinding.bodyFollowModeTitle.setTextColor(if (enable) Color.parseColor("#27272B") else Color.parseColor("#664A5076"))
            settingBinding.bodyFollowModeText.text = if (enable) viewModel.bodyFollowMode.showText() else ""
        }

        settingBinding.switchFaceTrackBtn.apply {
            setOnClickListener {
                viewModel.isFaceTrack = !viewModel.isFaceTrack
                viewModel.isFaceTrack.let {
                    setFaceTrack(it)
                    if (it) {
                        settingBinding.switchFaceTrackBtn.setImageResource(R.drawable.btn_switch_on)
                    } else {
                        settingBinding.switchFaceTrackBtn.setImageResource(R.drawable.btn_switch_off)
                    }
                }
            }
        }

        settingBinding.bodyTrackLayout.apply {
            settingBinding.bodyTrackText.text = viewModel.bodyTrackMode.showText()
            val popupMenu = PopupMenu(requireContext(), settingBinding.bodyTrackText, Gravity.END)
            val list = BodyTrackMode.values().map { it.showText() }
            list.forEach {
                popupMenu.menu.add(it)
            }
            popupMenu.setOnMenuItemClickListener {
                val bodyTrackMode = when (list.indexOf(it.title)) {
                    0 -> {
                        BodyTrackMode.Full
                    }
                    1 -> {
                        BodyTrackMode.Half
                    }
                    2 -> {
                        BodyTrackMode.Close
                    }
                    else -> throw Throwable()
                }
                enableFollowMode(bodyTrackMode == BodyTrackMode.Full)
                viewModel.bodyTrackMode = bodyTrackMode
                viewModel.bodyTrackMode.let {
                    setBodyTrack(it)
                    settingBinding.bodyTrackText.text = it.showText()
                }
                true
            }

            setOnClickListener {
                popupMenu.show()
            }
        }

        settingBinding.bodyFollowModeLayout.apply {
            settingBinding.bodyFollowModeText.text =
                viewModel.bodyFollowMode.showText()
            val popupMenu =
                PopupMenu(requireContext(), settingBinding.bodyFollowModeText, Gravity.END)
            val list = BodyFollowMode.values().map { it.showText() }
            list.forEach {
                popupMenu.menu.add(it)
            }
            popupMenu.setOnMenuItemClickListener {
                val bodyFollowMode = when (list.indexOf(it.title)) {
                    0 -> {
                        BodyFollowMode.Fix
                    }
                    1 -> {
                        BodyFollowMode.Stage
                    }
                    else -> throw Throwable()
                }
                setBodyFollowMode(bodyFollowMode)
                settingBinding.bodyFollowModeText.text = bodyFollowMode.showText()
                true
            }

            setOnClickListener {
                popupMenu.show()
            }
        }
    }

    override fun onKeyboardHeightChanged(height: Int, orientation: Int) {
        if (height > 0) {
            playAnim(binding.frameInput, -height)
            binding.etInput.maxLines = 3
        } else {
            playAnim(binding.frameInput, 0)
            binding.etInput.maxLines = 1
        }
        mKeyboardHeight = height
    }

    private fun playAnim(view: View, translation: Int) {
        view.animate().translationY(translation.toFloat())
            .setDuration(100)
            .start()
    }

    private fun initRenderer() {
        ptaRenderer.apply {

            bindGLTextureView(binding.glTextureView, 3)
            val params = BindRendererListenerUseCase.Params(true)
            BindRendererListenerUseCase(
                ptaRenderer,
                params,
                listener = object : BindRendererListenerUseCase.Listener {
                    override fun surfaceState(isAlive: Boolean) {
                        if (isAlive) requestRender()
                    }

                    override fun fpsPrint(fps: Double, renderTime: Double) {
                        lifecycleScope.launchWhenResumed {
                            binding.testInfo.text =
                                "fps:${fps.format(1)}，renderTime:${renderTime.format(1)}"
                        }
                    }

                    override fun onRenderDataPrepared(inputData: FURenderInputData) {
                        fuStaViewModel.syncSpeechStatus()
                    }
                })
        }
    }

    private fun inject() {
        viewModel.drivePageLiveData.observe(viewLifecycleOwner) {
            FuLog.debug("switch page: $it")
            if (it == null) return@observe
            when (it) { //根据不同的页面，对 Renderer 采用不同的渲染状态
//                DrivePage.Ar -> {
//                    onCloseTrack()
//                    onCloseText()
//                    onOpenArMode()
//                }
//                DrivePage.Track -> {
//                    onCloseArMode()
//                    onCloseText()
//                    onOpenTrack()
//                }
                DrivePage.Text -> {
                    onCloseArMode()
                    onCloseTrack()
                    onOpenText()
                }
                else -> {}
            }
            onPageUpdate(it)
        }
    }

    /**
     * UI 更新
     */
    private fun onPageUpdate(drivePage: DrivePage) {
        when (drivePage) {
//            DrivePage.Ar -> {
//                binding.settingBtn.gone()
//                binding.switchCameraToward.visible()
//                binding.frameInput.gone()
//                binding.arMode.setImageResource(R.drawable.icon_drive_face_sel)
//                binding.trackMode.setImageResource(R.drawable.icon_drive_body_nor)
//                binding.textMode.setImageResource(R.drawable.icon_drive_text_nor)
//            }
//            DrivePage.Track -> {
//                binding.settingBtn.visible()
//                binding.switchCameraToward.visible()
//                binding.frameInput.gone()
//                binding.arMode.setImageResource(R.drawable.icon_drive_face_nor)
//                binding.trackMode.setImageResource(R.drawable.icon_drive_body_sel)
//                binding.textMode.setImageResource(R.drawable.icon_drive_text_nor)
//            }
            DrivePage.Text -> {
                binding.settingBtn.gone()
                //binding.switchCameraToward.gone()
                binding.frameInput.visible()
                //binding.arMode.setImageResource(R.drawable.icon_drive_face_nor)
                //binding.trackMode.setImageResource(R.drawable.icon_drive_body_nor)
                binding.textMode.setImageResource(R.drawable.icon_drive_text_sel)
            }
        }
    }


    private fun onOpenArMode() {
        DevDrawRepository.getSceneOrNull()?.rendererConfig?.setPostProcessMirrorParam(null)
        fuDriveViewModel.openArModeWithMask() //进入到 AR 页面时直接开启 AR 模式
    }

    private fun onCloseArMode() {
        fuDriveViewModel.closeArModeWithMask() //进入其他页面时关闭 AR 模式
        DevDrawRepository.getSceneOrNull()?.rendererConfig?.setPostProcessMirrorParam(
            FUPostProcessMirrorParamData( // 开启地面反射
                maxTransparency = 0.7f,
                maxDistance = 30f
            )
        )
    }

    private fun onOpenTrack() {
        viewModel.isFaceTrack.let {
            setFaceTrack(it)
        }
        viewModel.bodyTrackMode.let {
            setBodyTrack(it)
        }
        viewModel.bodyFollowMode.let {
            setBodyFollowMode(it)
        }
        ptaRenderer.setSmallViewportSwitch(true)
    }

    private fun onCloseTrack() {
        fuDriveViewModel.closeBodyTrack()
        fuDriveViewModel.closeFaceTrack()
        SwitchCameraUseCase().execute("quanshen", 0f)
        ptaRenderer.setSmallViewportSwitch(false)
    }

    private fun onOpenText() {
        val avatarId = DevAvatarManagerRepository.getCurrentAvatarId()
        if (avatarId == null) {
            ToastUtils.showFailureToast(requireContext(), "找不到可以驱动的 Avatar")
            return
        }
        fuStaViewModel.setSTAControlAvatar(avatarId)
    }

    private fun onCloseText() {
        KeyboardUtils.hideSoftInput(activity)
        fuStaViewModel.stopSpeech()
    }

    private fun setFaceTrack(isFaceTrack: Boolean) {
        if (isFaceTrack) {
            fuDriveViewModel.openFaceTrack() //开启人脸跟踪
        } else {
            fuDriveViewModel.closeFaceTrack() //关闭人脸跟踪
        }
    }

    private fun setBodyTrack(mode: BodyTrackMode) {
        var isHalfCamera = false
        when (mode) {
            BodyTrackMode.Full -> {
                fuDriveViewModel.openBodyFullTrack() //开启全身跟踪
            }
            BodyTrackMode.Half -> {
                fuDriveViewModel.openBodyHalfTrack() //开启半身跟踪
                setBodyFollowMode(BodyFollowMode.Fix)
                isHalfCamera = true
            }
            BodyTrackMode.Close -> {
                fuDriveViewModel.closeBodyTrack() //关闭跟踪
            }
        }
        if (isHalfCamera) {
            SwitchCameraUseCase().execute("halfbody_drive", 0f)
        } else {
            SwitchCameraUseCase().execute("quanshen", 0f)
        }
    }

    private fun setBodyFollowMode(mode: BodyFollowMode) {
        when (mode) {
            BodyFollowMode.Fix -> {
                fuDriveViewModel.setBodyFollowModeFix() //设置人体跟踪模式为 Fix
            }
            BodyFollowMode.Stage -> {
                fuDriveViewModel.setBodyFollowModeStage() //设置人体跟踪模式为 Stage
            }
        }
        viewModel.bodyFollowMode = mode
    }

    override fun onPause() {
        super.onPause()
        fuStaViewModel.stopSpeech()
    }

    override fun onDestroy() {
        super.onDestroy()
        mKeyboardHeightProvider.close()
        mKeyboardHeightProvider.setKeyboardHeightObserver(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.release()
        ptaRenderer.release()
        fuStaViewModel.release()
    }

}