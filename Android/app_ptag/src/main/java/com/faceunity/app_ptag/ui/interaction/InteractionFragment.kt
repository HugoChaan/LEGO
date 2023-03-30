package com.faceunity.app_ptag.ui.interaction

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.compat.SPStorageFieldImpl
import com.faceunity.app_ptag.databinding.InteractionFragmentBinding
import com.faceunity.app_ptag.databinding.LayoutInteractionControlBinding
import com.faceunity.app_ptag.ui.home.entity.state.ExceptionEvent
import com.faceunity.app_ptag.ui.home.entity.state.LoadingState
import com.faceunity.app_ptag.ui.interaction.entity.InteractionControlPage
import com.faceunity.app_ptag.ui.interaction.entity.InteractionPageStatus
import com.faceunity.app_ptag.ui.interaction.network.entity.InteractionVoiceResult
import com.faceunity.app_ptag.ui.interaction.util.AudioRecorder
import com.faceunity.app_ptag.ui.interaction.util.KeyboardHeightProvider
import com.faceunity.app_ptag.ui.interaction.util.KeyboardUtils
import com.faceunity.app_ptag.ui.interaction.util.SpaceItemDecoration
import com.faceunity.app_ptag.ui.interaction.weight.PopupWindowProvider
import com.faceunity.app_ptag.ui.interaction.weight.animation.AnimationView
import com.faceunity.app_ptag.ui.interaction.weight.background.BackgroundView
import com.faceunity.app_ptag.ui.interaction.weight.background.entity.BackgroundSource
import com.faceunity.app_ptag.ui.interaction.weight.emotion.EmotionView
import com.faceunity.app_ptag.ui.interaction.weight.history.ChatAdapter
import com.faceunity.app_ptag.ui.interaction.weight.history.entity.ChatMessage
import com.faceunity.app_ptag.ui.interaction.weight.skill.SkillView
import com.faceunity.app_ptag.ui.interaction.weight.tone.ToneView
import com.faceunity.app_ptag.use_case.renderer.BindRendererListenerUseCase
import com.faceunity.app_ptag.use_case.renderer.CreateRendererUseCase
import com.faceunity.app_ptag.use_case.renderer.RendererBindLifecycleUseCase
import com.faceunity.app_ptag.util.DialogDisplayHelper
import com.faceunity.app_ptag.util.ToastUtils
import com.faceunity.app_ptag.util.expand.marginParams
import com.faceunity.app_ptag.util.expand.px
import com.faceunity.app_ptag.view_model.FuAvatarManagerViewModel
import com.faceunity.app_ptag.view_model.FuStaViewModel
import com.faceunity.app_ptag.weight.DownloadingDialog
import com.faceunity.app_ptag.weight.FuDemoRetryDialog
import com.faceunity.app_ptag.weight.avatar_manager.AvatarManagerView
import com.faceunity.app_ptag.weight.avatar_manager.entity.FuAvatarContainer
import com.faceunity.app_ptag.weight.avatar_manager.parser.FuAvatarContainerParser
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.core.entity.FURenderInputData
import com.faceunity.core.entity.FURenderOutputData
import com.faceunity.core.renderer.entity.FUDrawFrameMatrix
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.parser.IFuJsonParser
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.safeCollect
import com.faceunity.editor_ptag.util.visible
import com.faceunity.toolbox.file.FUFileUtils
import com.faceunity.toolbox.media.FUMediaUtils
import com.faceunity.toolbox.utils.FUDensityUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.roundToInt
import kotlin.properties.Delegates


class InteractionFragment : Fragment() {

    private var _binding: InteractionFragmentBinding? = null
    private val binding get() = _binding!!

    private val fuAvatarManagerViewModel by viewModels<FuAvatarManagerViewModel>()
    private val fuStaViewModel by viewModels<FuStaViewModel>()
    private val viewModel: InteractionViewModel by viewModels()

    private val ptaRenderer = CreateRendererUseCase.build().apply {
        RendererBindLifecycleUseCase(this).execute(lifecycle)
    }

    private val downloadingDialog: DownloadingDialog by lazy {
        DownloadingDialog(requireContext())
    }
    private var ignoreSTAModel = false

    private var pageStatus: InteractionPageStatus by Delegates.observable(InteractionPageStatus.Default) { _, old, new ->
        updatePageStatusStyle(new)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InteractionFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initBottomInputView()
        initRecommendView()
        initControlView()
        initHistoryView()
        initRenderer()
        subscribeUi()
        inject()
        injectDownload()
        setAvatarLoadListener()


        if (FuDevDataCenter.staControl == null) {
            ToastUtils.showFailureToast(requireContext(), "STA 没有初始化，无法加载互动页面。")
            ignoreSTAModel = true
            Navigation.findNavController(binding.backBtn).popBackStack()
            return
        }


        fuStaViewModel.initSTARender()
        fuStaViewModel.initSTAServer()

        lifecycleScope.launchWhenResumed {
            viewModel.init(this.coroutineContext)
            withTimeout(15_000) {
                viewModel.downloadTtsAnimation().onStart {
                    downloadingDialog.updateText("正在下载动画资源")
                    DialogDisplayHelper.show(downloadingDialog)
                }.onCompletion {
                    DialogDisplayHelper.dismiss(downloadingDialog)
                }.safeCollect {
                    FuLog.debug("collect:$it")
                }
            }
            FuLog.debug("timeout,close dialog.")
            DialogDisplayHelper.dismiss(downloadingDialog)

        }
    }

    private fun requestRender() {
        viewModel.drawAvatar()
        DevAvatarManagerRepository.getCurrentAvatarId()?.let {
            fuStaViewModel.setSTAControlAvatar(it)
        }
    }

    private fun initView() {
        binding.glTextureView.apply {
            isOpaque = false
        }
        binding.backBtn.apply {
            setOnClickListener {
                Navigation.findNavController(it).popBackStack() //返回上一个页面
            }
        }
        binding.touchView.apply {
            setOnClickListener {
                if (pageStatus != InteractionPageStatus.Default) {
                    binding.expandLayout.removeAllViews()
                    pageStatus = InteractionPageStatus.Default
                } else {
                    KeyboardUtils.hideSoftInput(requireContext(), binding.input.inputEditText)
                }
            }
        }

    }

    //region 文字输入、语音输入功能。

    private val keyboardHeightProvider by lazy {
        KeyboardHeightProvider(requireActivity())
    }

    private fun initBottomInputView() {
        binding.input.switchAudioBtn.setOnClickListener {
            if (isAudioRecording) return@setOnClickListener
            binding.input.inputAudioLayout.visible()
            binding.input.inputTextLayout.gone()
            KeyboardUtils.hideSoftInput(requireContext(), binding.input.inputEditText)
        }
        binding.input.switchTextBtn.setOnClickListener {
            if (isAudioRecording) return@setOnClickListener
            binding.input.inputTextLayout.visible()
            binding.input.inputAudioLayout.gone()
            KeyboardUtils.showSoftInput(requireContext(), binding.input.inputEditText)
        }
        binding.input.inputTextSendBtn.setOnClickListener {
            val text = binding.input.inputEditText.text.toString()
            if (text.isEmpty()) {
                ToastUtils.showFailureToast(requireContext(), "请输入内容")
                return@setOnClickListener
            }
            binding.input.inputEditText.setText("")
            fuStaViewModel.sendTextMessage(text)
            KeyboardUtils.hideSoftInput(requireContext(), binding.input.inputEditText)
        }
        binding.input.inputAudioRenderBtn.apply {
            val pressToTalkListener = PressToTalkListener()

            val touchRenderEvent = object : TouchRenderEvent {
                override fun onStart() {
                    setInputAudioRenderBtnStyle(true)
                    popupWindowProvider.showRecordPopupWindow(null)

                    startAudioRecord(
                        onSuccess = {
                            fuStaViewModel.sendAudioMessage(it)
                        },
                        onTimeOut = {
                            pressToTalkListener.isForceFinishAudioRecord = true
                        }
                    )
                }

                override fun onCancelTip(isCancel: Boolean) {
                    if (isCancel) {
                        popupWindowProvider.showCancelPopupWindow()
                    } else {
                        popupWindowProvider.dismissCancelPopupWindow()
                        popupWindowProvider.showRecordPopupWindow(null)
                    }
                }


                override fun onFinish(status: TouchRenderEvent.FinishStatus) {
                    setInputAudioRenderBtnStyle(false)
                    when (status) {
                        TouchRenderEvent.FinishStatus.Success -> {
                            popupWindowProvider.dismissAllPopupWindow()
                            finishAudioRecord()
                        }
                        TouchRenderEvent.FinishStatus.TooShort -> {
                            popupWindowProvider.showWarnPopupWindow("说话时间太短")
                            postDelayed(object : Runnable { //临时处理方式
                                override fun run() {
                                    popupWindowProvider.dismissAllPopupWindow()
                                }
                            }, 1500)
                            cancelAudioRecord()
                        }
                        TouchRenderEvent.FinishStatus.TooLong -> {
                            popupWindowProvider.showWarnPopupWindow("说话时间太长")
                            postDelayed(object : Runnable { //临时处理方式
                                override fun run() {
                                    popupWindowProvider.dismissAllPopupWindow()
                                }
                            }, 1500)
                            finishAudioRecord()
                        }
                        TouchRenderEvent.FinishStatus.Cancel -> {
                            popupWindowProvider.dismissAllPopupWindow()
                            cancelAudioRecord()
                        }
                    }
                }
            }
            pressToTalkListener.touchRenderEvent = touchRenderEvent
            setOnTouchListener(pressToTalkListener)
        }


        keyboardHeightProvider.setKeyboardHeightObserver { height, orientation ->
            binding.input.inputTextLayout.marginParams.bottomMargin = height
            binding.input.inputTextLayout.requestLayout()
            updateKeyboardHeightStyle(height != 0)
        }
        keyboardHeightProvider.start()
    }

    val audioRecorder = AudioRecorder()
    @Volatile private var isAudioRecording = false //如果正在录音，不允许其他点击操作。

    private fun startAudioRecord(onSuccess: (pcmData: ByteArray) -> Unit, onTimeOut: () -> Unit) {
        fuStaViewModel.stopSpeech() //开始录音时取消播报
        audioRecorder.createDefaultAudio()
        audioRecorder.setRecordStreamListener(object : AudioRecorder.RecordStreamListener {
            private var isCallTimeout = false

            override fun onStart() {
                isAudioRecording = true
            }

            override fun onRecording(duration: Long) {
                var time: String
                var second = (duration / 1000)
                val minute = (second / 60).toInt()
                second = second - minute * 60
                time = (java.lang.String.format(Locale.getDefault(), "%02d", minute) + ":"
                        + java.lang.String.format(Locale.getDefault(), "%02d", second))
                activity?.runOnUiThread { //临时处理方式
                    popupWindowProvider.refreshRecordDuration(time)
                }
                if (duration >= 60 * 1000 && !isCallTimeout) { //超时直接结束
                    activity?.runOnUiThread { //临时处理方式
                        isCallTimeout = true
                        onTimeOut()
                    }
                }
            }

            override fun onFinish(duration: Long, pcmData: ByteArray) {
                if (!isCancelAudioRecord) {
                    onSuccess(pcmData)
                }
                audioRecorder.setRecordStreamListener(null)
                isAudioRecording = false
            }

            override fun onError(error: String?) {
                isAudioRecording = false
            }
        })
        audioRecorder.startRecord()

    }

    private var isCancelAudioRecord = false

    private fun finishAudioRecord() {
        isCancelAudioRecord = false
        audioRecorder.stopRecord()
    }

    private fun cancelAudioRecord() {
        isCancelAudioRecord = true
        audioRecorder.stopRecord()
    }

    //region 没啥用途的 UI 逻辑

    private fun setInputAudioRenderBtnStyle(isRender: Boolean) {
        binding.input.inputAudioRenderBtn.apply {
            if (isRender) {
                setBackgroundResource(R.drawable.bg_speech_input_audio_hover)
                text = "松开发送"
            } else {
                setBackgroundResource(R.drawable.bg_speech_input_audio_normal)
                text = "按住说话"
            }
        }
    }

    private val popupWindowProvider by lazy { PopupWindowProvider(requireContext(), binding.root) }

    interface TouchRenderEvent {
        fun onStart()

        fun onCancelTip(isCancel: Boolean)

        fun onFinish(status: FinishStatus)

        enum class FinishStatus {
            Success, TooShort, TooLong, Cancel
        }
    }

    private class PressToTalkListener(var touchRenderEvent: TouchRenderEvent? = null) : View.OnTouchListener {
        private val mLongPressDuration: Int = ViewConfiguration.getLongPressTimeout()
        private val mVerticalOffset: Int = 200 //随便写的
        private var downTimestamp: Long = -1L
        private var mDownY = 0
        private var isCancel = false

        var isForceFinishAudioRecord = false
        var isCallFinish = false

        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            val actionMasked: Int = event.getActionMasked()
            when (actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isForceFinishAudioRecord = false
                    isCallFinish = false
                    mDownY = event.getRawY().roundToInt()
                    downTimestamp = System.currentTimeMillis()
                    touchRenderEvent?.onStart()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isForceFinishAudioRecord) {
                        if (!isCallFinish) {
                            touchRenderEvent?.onFinish(TouchRenderEvent.FinishStatus.TooLong)
                            isCallFinish = true
                        }
                        return true
                    }
                    val status = when {
                        isCancel -> TouchRenderEvent.FinishStatus.Cancel
                        System.currentTimeMillis() - downTimestamp < mLongPressDuration -> TouchRenderEvent.FinishStatus.TooShort
                        else -> TouchRenderEvent.FinishStatus.Success
                    }

                    touchRenderEvent?.onFinish(status)
                    downTimestamp = -1L
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isForceFinishAudioRecord) {
                        if (!isCallFinish) {
                            touchRenderEvent?.onFinish(TouchRenderEvent.FinishStatus.TooLong)
                            isCallFinish = true
                        }
                        return true
                    }
                    val rawY = event.getRawY().roundToInt()
                    isCancel = wantToCancel(mDownY, rawY)
                    touchRenderEvent?.onCancelTip(isCancel)
                }
                else -> {}
            }
            return true
        }

        private fun wantToCancel(downY: Int, moveY: Int): Boolean {
            return Math.abs(downY - moveY) > mVerticalOffset
        }
    }

    //endregion 没啥用途的 UI 逻辑

    //endregion 文字输入、语音输入功能。

    //region 推荐技能功能

    private val recommendList = mutableListOf<Pair<String, (String) -> Unit>>()
    private var timer: Timer? = null

    private fun initRecommendView() {
        binding.recommendFlowLayout.apply {
            setMaxLine(2)
        }
        val action: (String) -> Unit = action@{
            if (isAudioRecording) return@action
            fuStaViewModel.sendTextMessage(it)
        }
        fuStaViewModel.recommendSkillListLiveData.observe(viewLifecycleOwner) {
            val fullRecommendList = it.map { it.name }.shuffled()
            recommendList.clear()
            for (i in 0 .. 5) {
                fullRecommendList.getOrNull(i)?.let {
                    recommendList.add(it to action)
                }
            }
            refreshRecommendView()
        }
        timer = timer("refreshRecommendView", false, 0, 30 * 1000) {
            fuStaViewModel.postRecommendSkillListLiveData()
        }

        refreshRecommendView()
    }

    private fun refreshRecommendView() {
        binding.recommendFlowLayout.apply {
            removeAllViews()
            recommendList.forEach { (text, action) ->
                val view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_layout_flow, binding.recommendFlowLayout, false) as TextView
                view.text = text
                view.setOnClickListener {
                    action(text)
                }
                binding.recommendFlowLayout.addView(view)
            }
        }
    }

    //endregion 推荐技能功能


    //region 控制菜单

    private lateinit var skillView: SkillView
    private lateinit var avatarManagerView: AvatarManagerView
    private lateinit var animationView: AnimationView
    private lateinit var emotionView: EmotionView
    private lateinit var toneView: ToneView
    private lateinit var backgroundView: BackgroundView

    private fun initControlView() {
        val controlView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_interaction_control, null)
        val controlBinding = LayoutInteractionControlBinding.bind(controlView)

        skillView = createSkillView()
        avatarManagerView = createAvatarManagerView()
        animationView = createAnimationView()
        emotionView = createEmotionView()
        toneView = createToneView()
        backgroundView = createBackgroundView()

        fun setExpandView(view: View) {
            controlBinding.expandLayout.removeAllViews()
            controlBinding.expandLayout.addView(view)
        }

        val modeViewList = listOf(controlBinding.modeSkill, controlBinding.modeAvatar, controlBinding.modeAnimation, controlBinding.modeEmotion, controlBinding.modeTone, controlBinding.modeBackground)
        fun selectModeView(modeView: View) {
            modeViewList.forEach {
                it.isSelected = false
            }
            modeView.isSelected = true
        }

        fun selectControl(controlPage: InteractionControlPage) {
            when(controlPage) {
                InteractionControlPage.Skill -> {
                    setExpandView(skillView)
                    selectModeView(controlBinding.modeSkill)
                }
                InteractionControlPage.Avatar -> {
                    setExpandView(avatarManagerView)
                    selectModeView(controlBinding.modeAvatar)
                }
                InteractionControlPage.Animation -> {
                    setExpandView(animationView)
                    selectModeView(controlBinding.modeAnimation)
                }
                InteractionControlPage.Emotion -> {
                    setExpandView(emotionView)
                    selectModeView(controlBinding.modeEmotion)
                }
                InteractionControlPage.Tone -> {
                    setExpandView(toneView)
                    selectModeView(controlBinding.modeTone)
                    toneView.setDefaultItem(fuStaViewModel.getVoice()) //选中时更新选中的音色
                }
                InteractionControlPage.Background -> {
                    setExpandView(backgroundView)
                    selectModeView(controlBinding.modeBackground)
                }
            }
        }

        controlBinding.modeSkill.setOnClickListener {
            selectControl(InteractionControlPage.Skill)
        }
        controlBinding.modeAvatar.setOnClickListener {
            selectControl(InteractionControlPage.Avatar)
        }
        controlBinding.modeAnimation.setOnClickListener {
            viewModel.flushAnimationPageDownloadState()
            selectControl(InteractionControlPage.Animation)
        }
        controlBinding.modeEmotion.setOnClickListener {
            selectControl(InteractionControlPage.Emotion)
        }
        controlBinding.modeTone.setOnClickListener {
            selectControl(InteractionControlPage.Tone)
        }
        controlBinding.modeBackground.setOnClickListener {
            selectControl(InteractionControlPage.Background)
        }

        selectControl(InteractionControlPage.Skill)


        binding.controlBtn.setOnClickListener {
            if (isAudioRecording) return@setOnClickListener
            when(pageStatus) {
                InteractionPageStatus.Default, InteractionPageStatus.History -> {
                    binding.expandLayout.removeAllViews()
                    binding.expandLayout.addView(controlView)
                    pageStatus = InteractionPageStatus.Control
                }
                InteractionPageStatus.Control -> {
                    binding.expandLayout.removeAllViews()
                    pageStatus = InteractionPageStatus.Default
                }
            }
        }
    }

    private fun createSkillView(): SkillView {
        val skillView = SkillView(requireContext())

        fuStaViewModel.skillListLiveData.observe(viewLifecycleOwner) {
            skillView.fillData(it) {
                fuStaViewModel.sendTextMessage(it)
            }
            skillView.requestLayout()
        }
        return skillView
    }

    private fun createAvatarManagerView(): AvatarManagerView {
        val avatarManagerView = AvatarManagerView(requireContext()).apply {
            onItemClick = {
                fuStaViewModel.stopSpeech() //切换形象时取消播报
                fuAvatarManagerViewModel.smartSwitchAvatar(it.id){
                    DevSceneManagerRepository.setAvatarDefaultAnimation(DevAvatarManagerRepository.getCurrentAvatarInfo()!!.avatar, DevAvatarManagerRepository.getCurrentAvatarInfo()!!.gender())
                } //加载形象。如果已准备好则直接加载，否则从云端下载。
                viewModel.onSwitchAvatar(it.id)
            }
            onItemDelete = {
                fuAvatarManagerViewModel.removeAvatar(it.id) //删除形象
            }
        }
        val avatarWrapper = FuAvatarContainer(mutableListOf(), mutableListOf())
        fuAvatarManagerViewModel.avatarCollectionLiveData.observe(viewLifecycleOwner) {
            val fuAvatarContainerParser = FuAvatarContainerParser()
            val wrapperList = DevAvatarManagerRepository.mapAvatar {
                fuAvatarContainerParser.parserAvatarInfoToFuAvatarWrapper(it)
            }
            avatarWrapper.avatarList.apply {
                clear()
                addAll(wrapperList)
            }
            avatarManagerView.syncAvatarContainer(avatarWrapper)
        }
        fuAvatarManagerViewModel.avatarSelectLiveData.observe(viewLifecycleOwner) {
            avatarWrapper.selectId.apply {
                clear()
                it?.let { add(it) }
            }
            avatarManagerView.syncAvatarContainer(avatarWrapper) //同步最新的形象容器至 UI
            fuStaViewModel.setSTAControlAvatar(it) //同步形象到 STA
            fuStaViewModel.applyAnimationConfig(it) //将动画配置运用到该 Avatar
        }
        return avatarManagerView
    }

    private fun createAnimationView(): AnimationView {
        val animationView = AnimationView(requireContext())
        lifecycleScope.launchWhenResumed {
            viewModel.animationUiState.collect {
                animationView.collectUiState(it)
            }
        }
        animationView.setClickListener {
            lifecycleScope.launchWhenResumed {
                viewModel.playAnimation(it)
            }
        }
        return animationView
    }

    private fun createEmotionView(): EmotionView {
        val emotionView = EmotionView(requireContext())
        lifecycleScope.launchWhenResumed {
            viewModel.emotionUiState.collect {
                emotionView.collectUiState(it)
            }
        }
        emotionView.setClickListener {
            lifecycleScope.launchWhenResumed {
                viewModel.playEmotionAnimation(it)
            }
        }
        return emotionView
    }

    private fun createToneView(): ToneView {
        val toneView = ToneView(requireContext()).apply {
            eventListener = object : ToneView.EventListener {
                override fun onItemClick(voice: InteractionVoiceResult.Data.Voice) {
                    selectItem = voice
                    fuStaViewModel.stopSpeech() //切换音色时取消播报
                    fuStaViewModel.setVoice(voice.id)
                }

                override fun onAudition(voice: InteractionVoiceResult.Data.Voice) {
                    fuStaViewModel.auditionVoice("如沐春风的意思就是，今天的风吹到身上很舒服，就像见到你一样。", voice.id)
                }
            }
        }
        fuStaViewModel.voiceListLiveData.observe(viewLifecycleOwner) {
            toneView.setDefaultItem(fuStaViewModel.getVoice())
            toneView.fillData(it)
        }
        fuStaViewModel.auditionEventLiveData.observe(viewLifecycleOwner) {
            when(it.status) {
                FuStaViewModel.AuditionEvent.Status.Start -> {
                    toneView.startAudition(it.auditionId)
                }
                FuStaViewModel.AuditionEvent.Status.Finish -> {
                    toneView.endAudition()
                }
            }
        }
        return toneView
    }

    private val appConfig by lazy {
        SPStorageFieldImpl(requireContext(), "AppConfig")
    }

    private fun getBackgroundListConfig(): MutableList<String> {
        return appConfig.getAsString("backgroundJson").let {
            if (it.isBlank()) {
                emptyArray()
            } else {
                FuDI.getCustom<IFuJsonParser>().parse(it, Array<String>::class.java)
            }
        }.let {
            it.toMutableList()
        }
//        return appConfig.getAsStringSet("background").toMutableSet().toMutableList()
    }

    private fun saveBackgroundListConfig(list: List<String>) {
        list.toTypedArray().let {
            FuDI.getCustom<IFuJsonParser>().toJson(it)
        }.let {
            appConfig.set("backgroundJson", it)
        }
    }

    private val backgroundList by lazy {
        getBackgroundListConfig()
    }

    private fun createBackgroundView() : BackgroundView {
        val backgroundView = BackgroundView(requireContext()).apply {
            fillData(backgroundList)
            selectDefaultItem()
            eventListener = object : BackgroundView.EventListener {
                override fun onItemClick(backgroundSource: BackgroundSource) {
                    selectItem = backgroundSource
                    when (backgroundSource) {
                        is BackgroundSource.Default -> {
                            val bitmap =
                                BitmapFactory.decodeResource(requireContext().resources, backgroundSource.drawableId)
                            viewModel.setCustomSceneBackground(bitmap)
                        }
                        is BackgroundSource.User -> {
                            val bitmap = FUMediaUtils.loadBitmap(backgroundSource.path, FUDensityUtils.getScreenWidth( ), FUDensityUtils.getScreenHeight( ) )
                            if (bitmap == null) {
                                ToastUtils.showFailureToast(requireContext(), "解析图片失败！")
                                return
                            }
                            viewModel.setCustomSceneBackground(bitmap)
                        }
                    }
                }

                override fun onAddItemClick() {
                    if (backgroundView.getListSize() >= 21) {
                        ToastUtils.showFailureToast(requireContext(), "最多只能添加20张背景图")
                        return
                    }
                    openSelectPhotoPage()
                }

                override fun onItemRemove(position: Int, userPosition: Int, backgroundSource: BackgroundSource) {
                    (backgroundSource as? BackgroundSource.User)?.let { userSource ->
                        backgroundList.removeAt(userPosition)
                        saveBackgroundListConfig(backgroundList)
                    }
                    if (selectItem == backgroundSource) {
                        val selectPosition = if (position == 1) 1 else position - 1
                        backgroundView.selectIndex(selectPosition)
                    }

                }
            }
        }
        return backgroundView
    }

    //endregion 控制菜单


    //region 选择图片
    private val IMAGE_REQUEST_CODE = 0x102

    private fun openSelectPhotoPage() {
        val intent2 = Intent()
        intent2.addCategory(Intent.CATEGORY_OPENABLE)
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        intent2.type = "image/*"
        intent2.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        if (Build.VERSION.SDK_INT < 19) {
            intent2.action = Intent.ACTION_GET_CONTENT
        } else {
            intent2.action = Intent.ACTION_OPEN_DOCUMENT
        }
        startActivityForResult(
            intent2,
            IMAGE_REQUEST_CODE
        )
    }

    private fun onSelectPhoto(path: String) {
        FuLog.debug("onSelectPhoto: $path")
        backgroundList.add(path)
        saveBackgroundListConfig(backgroundList)
        backgroundView.addUserImport(path)
        backgroundView.selectUserItem(path)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val filePath = FUFileUtils.getAbsolutePathByUri(  data.data!!)
            if (filePath == null) {
                ToastUtils.showFailureToast(requireContext(), "所选图片文件不存在。")
                return
            }
            val file = File(filePath)
            if (file.exists()) {
                onSelectPhoto(filePath)
            } else {
                ToastUtils.showFailureToast(requireContext(), "所选图片文件不存在。")
            }
        }
    }

    //endregion 选择图片

    //region 历史对话功能

    private val historyMessageList = mutableListOf<ChatMessage>()
    private val historyAdapter = ChatAdapter(historyMessageList)


    private fun initHistoryView() {
        val historyView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_interaction_history, null)
        val historyRecyclerView = historyView.findViewById<RecyclerView>(R.id.chat_list)
        historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = historyAdapter

            setHasFixedSize(true)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            val hSpace = 32.px
            val vSpace = 16.px
            addItemDecoration(SpaceItemDecoration(hSpace, vSpace))
        }
        fuStaViewModel.historyNewMessageLiveData.observe(viewLifecycleOwner) {
            it.forEach {
                addHistoryMessage(it.first, it.second)
            }
        }

        binding.historyBtn.setOnClickListener {
            if (isAudioRecording) return@setOnClickListener
            when(pageStatus) {
                InteractionPageStatus.Default, InteractionPageStatus.Control -> {
                    binding.expandLayout.removeAllViews()
                    binding.expandLayout.addView(historyView)
                    historyRecyclerView.scrollToPosition(historyRecyclerView.adapter!!.itemCount - 1)
                    pageStatus = InteractionPageStatus.History
                }
                InteractionPageStatus.History -> {
                    binding.expandLayout.removeAllViews()
                    pageStatus = InteractionPageStatus.Default
                }
            }
        }
    }

    private fun addHistoryMessage(message: String, isSelf: Boolean) {
        fun addSenseChat(chatMessage: ChatMessage) {
            // 获取最后一条信息的时间戳，然后和当前的作对比，大于一分钟就显示当前时间
            val itemCount: Int = historyMessageList.size
            if (itemCount > 0) {
                val lastChatMessage: ChatMessage = historyMessageList.get(itemCount - 1)
                if (chatMessage.is1MinLaterThan(lastChatMessage)) {
                    val chatTime = ChatMessage.getTimeMessage()
                    historyMessageList.add(chatTime)
                }
            } else {
                val chatTime = ChatMessage.getTimeMessage()
                historyMessageList.add(chatTime)
            }
            historyMessageList.add(chatMessage)
        }

        addSenseChat(ChatMessage(if(isSelf) ChatMessage.FROM_USER else ChatMessage.FROM_NLP, message))
        historyAdapter.notifyDataSetChanged()
    }

    //endregion 历史对话功能


    private fun initRenderer() {
        ptaRenderer.apply {
            bindGLTextureView(binding.glTextureView)
            BindRendererListenerUseCase(ptaRenderer, listener = object : BindRendererListenerUseCase.Listener {
                override fun surfaceState(isAlive: Boolean) {
                    if (isAlive) {
                        requestRender()
                    } else {
                        viewModel.tempCancelAnimTask()
                    }
                }

                override fun onRenderDataPrepared(inputData: FURenderInputData) {
                    fuStaViewModel.syncSpeechStatus()
                }

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
        fuAvatarManagerViewModel.loadingState.observe(viewLifecycleOwner) {
            onLoadingState(it)
        }
        fuAvatarManagerViewModel.exceptionEvent.observe(viewLifecycleOwner) {
            onExceptionEvent(it)
            fuAvatarManagerViewModel.finishExceptionEvent()
        }
    }

    private fun inject() {
        viewModel.drawFinishLiveData.observe(viewLifecycleOwner) {
            val avatarId = DevAvatarManagerRepository.getCurrentAvatarId()
            if (avatarId!= null) {
                fuAvatarManagerViewModel.smartSwitchAvatar(avatarId)
            } else {
                ToastUtils.showFailureToast(requireContext(), "找不到要加载的形象")
            }
        }
        fuStaViewModel.isSTAServerInitSuccessLiveData.observe(viewLifecycleOwner) {
            if (it) {
                fuStaViewModel.requestHomePage()
                fuStaViewModel.requestVoiceList()
                fuStaViewModel.requestSkill()
            } else {
                ToastUtils.showFailureToast(requireContext(), "STA服务初始化失败，请重试。")
            }
        }
//        fuAvatarManagerViewModel.cloudAvatarPrepareLiveData.observe(viewLifecycleOwner) { //形象所需的资源下载完成后，显示该形象
//            fuAvatarManagerViewModel.autoSwitchAvatar(it)
//        }
        fuStaViewModel.animationConfigLiveData.observe(viewLifecycleOwner) {
            DevAvatarManagerRepository.getCurrentAvatarId()?.let { id ->
                fuStaViewModel.applyAnimationConfig(id)
            }
        }
        fuStaViewModel.staErrorTipLiveData.observe(viewLifecycleOwner) {
            ToastUtils.showFailureToast(requireContext(), "播报服务异常：${it}")
        }
    }

    private fun injectDownload() {
//        fuAvatarManagerViewModel.loadingCloudLiveData.observe(viewLifecycleOwner) {
//            when(it) {
//                is FuLoadingCloudState.Start -> {
//                    downloadingDialog.show()
//                }
//                is FuLoadingCloudState.Finish -> {
//                    downloadingDialog.dismiss()
//                }
//            }
//        }
//        val requestErrorRetryDialog = FuDemoRetryDialog(requireContext(), "").apply {
//            callback = object : FuDemoRetryDialog.Callback {
//                override fun onCancel() {
//                    dismiss()
//                }
//
//                override fun onFinish() { //网络错误下的重试按钮要执行的操作
//                    fuAvatarManagerViewModel.autoInitDefaultAvatar(false)
//                    dismiss()
//                }
//            }
//            create()
//        }
//        fuAvatarManagerViewModel.requestErrorInfoLiveData.observe(viewLifecycleOwner) { //网络请求异常
//            val type = it.type
//            when(type) {
//                is FuRequestErrorInfo.Type.Retry -> {
//                    requestErrorRetryDialog.tipText = type.tip
//                    if (!requestErrorRetryDialog.isShowing) {
//                        requestErrorRetryDialog.show()
//                    }
//                }
//                is FuRequestErrorInfo.Type.Error -> {
//                    ToastUtils.showFailureToast(requireContext(), "遇到异常：${type.tip}。/n请稍后重试")
//                }
//            }
//        }
//        val downloadFileRetryDialog = FuDemoRetryDialog(requireContext(), "").apply {
//            create()
//        }
//        fuAvatarManagerViewModel.downloadStatusLiveData.observe(viewLifecycleOwner) { //网络下载状态
//            val status = it.downloadStatus
//            when(status) {
//                is DownloadStatus.Start -> {
//                    downloadingDialog.show()
//                }
//                is DownloadStatus.Progress -> {
//                    downloadingDialog.updateText("下载 ${status.downloadCount}/${status.totalCount}")
//                }
//                is DownloadStatus.Error -> {
//                    downloadFileRetryDialog.tipText = "下载异常：${status.errorMsg}"
//                    downloadFileRetryDialog.callback = object : FuDemoRetryDialog.Callback {
//                        override fun onCancel() {
//                            downloadFileRetryDialog.dismiss()
//                        }
//
//                        override fun onFinish() { //网络错误下的重试按钮要执行的操作
//                            fuAvatarManagerViewModel.autoSwitchAvatar(it.avatarId)
//                            downloadFileRetryDialog.dismiss()
//                        }
//                    }
//                    if (!downloadFileRetryDialog.isShowing) {
//                        downloadFileRetryDialog.show()
//                    }
//                    downloadingDialog.dismiss()
//                }
//                is DownloadStatus.Finish -> {
//                    downloadingDialog.dismiss()
//                }
//            }
//        }
    }


    private fun updatePageStatusStyle(pageStatus: InteractionPageStatus) {
        var isShowExpandLayout = true //是否显示拓展面板。
        when(pageStatus) {
            InteractionPageStatus.Default -> {
                binding.historyBtn.setImageResource(R.drawable.icon_interaction_history_nor)
                binding.controlBtn.setImageResource(R.drawable.icon_interaction_function_nor)
                isShowExpandLayout = false
            }
            InteractionPageStatus.History -> {
                binding.historyBtn.setImageResource(R.drawable.icon_interaction_history_sel)
                binding.controlBtn.setImageResource(R.drawable.icon_interaction_function_nor)
            }
            InteractionPageStatus.Control -> {
                binding.historyBtn.setImageResource(R.drawable.icon_interaction_history_nor)
                binding.controlBtn.setImageResource(R.drawable.icon_interaction_function_sel)
            }
        }

        if (isShowExpandLayout) {
            binding.historyBtn.apply {
                marginParams.bottomMargin = 320.px
                requestLayout()
            }
        } else {
            binding.historyBtn.apply {
                marginParams.bottomMargin = 222.px
                requestLayout()
            }
        }
    }

    private fun updateKeyboardHeightStyle(isHintOtherLayout: Boolean) {
        if (isHintOtherLayout) {
            binding.recommendFlowLayout.gone()
            binding.historyBtn.gone()
            binding.controlBtn.gone()
        } else {
            binding.recommendFlowLayout.visible()
            binding.historyBtn.visible()
            binding.controlBtn.visible()
        }
    }

    private var isAvatarExecuteCompleted = false

    private fun setAvatarLoadListener() {
        DevDrawRepository.setAvatarEvent(object : DevDrawRepository.AvatarEvent {
            override fun onAvatarLoaded(avatar: Avatar) {
                isAvatarExecuteCompleted = true
            }
        })
    }

    private fun onAvatarShow() {
        FuLog.info("Avatar show on screen.")
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()
        if (ignoreSTAModel) return
        fuStaViewModel.stopSpeech()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        keyboardHeightProvider.close()
        timer?.cancel()
        timer = null
        DevDrawRepository.clearAvatarEvent()
        ptaRenderer.release()
        if (ignoreSTAModel) return
        fuStaViewModel.removeAllAvatarAnimationConfig()
        fuStaViewModel.release()
    }

}