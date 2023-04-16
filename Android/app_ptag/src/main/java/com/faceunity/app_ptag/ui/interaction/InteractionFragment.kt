package com.faceunity.app_ptag.ui.interaction

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.agora.gpt.ConvertClient
import com.agora.gpt.ConvertListener
import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.compat.SPStorageFieldImpl
import com.faceunity.app_ptag.databinding.InteractionFragmentBinding
import com.faceunity.app_ptag.databinding.LayoutInteractionControlBinding
import com.faceunity.app_ptag.ui.home.entity.state.ExceptionEvent
import com.faceunity.app_ptag.ui.home.entity.state.LoadingState
import com.faceunity.app_ptag.ui.interaction.entity.AnimationConfig
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
import com.faceunity.app_ptag.ui.interaction.weight.lego.LegoView
import com.faceunity.app_ptag.ui.interaction.weight.lego.ModeView
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
import com.faceunity.app_ptag.view_model.FuPreviewViewModel
import com.faceunity.app_ptag.view_model.FuStaViewModel
import com.faceunity.app_ptag.weight.DownloadingDialog
import com.faceunity.app_ptag.weight.FuDemoRetryDialog
import com.faceunity.app_ptag.weight.avatar_manager.AvatarManagerDialog
import com.faceunity.app_ptag.weight.avatar_manager.AvatarManagerView
import com.faceunity.app_ptag.weight.avatar_manager.entity.FuAvatarContainer
import com.faceunity.app_ptag.weight.avatar_manager.entity.FuAvatarWrapper
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
import com.faceunity.pta.pta_core.widget.TouchView
import com.faceunity.toolbox.file.FUFileUtils
import com.faceunity.toolbox.media.FUMediaUtils
import com.faceunity.toolbox.utils.FUDensityUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeout
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*
import java.util.Collections.addAll
import kotlin.collections.HashMap
import kotlin.concurrent.timer
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.properties.Delegates


class InteractionFragment : Fragment() {

    private var _binding: InteractionFragmentBinding? = null
    private val binding get() = _binding!!

    private val fuAvatarManagerViewModel by viewModels<FuAvatarManagerViewModel>()
    private val avatarWrapper = FuAvatarContainer(mutableListOf(), mutableListOf())
    private lateinit var avatarManagerDialog: AvatarManagerDialog
    private val fuStaViewModel by viewModels<FuStaViewModel>()
    private val fuPreviewViewModel by viewModels<FuPreviewViewModel>()
    private val viewModel: InteractionViewModel by viewModels()

    private val ptaRenderer = CreateRendererUseCase.build().apply {
        RendererBindLifecycleUseCase(this).execute(lifecycle)
    }

    private val downloadingDialog: DownloadingDialog by lazy {
        DownloadingDialog(requireContext())
    }
    private var ignoreSTAModel = false

    private var pageStatus: InteractionPageStatus by Delegates.observable(InteractionPageStatus.Default) { _, old, new ->
        //updatePageStatusStyle(new)
    }

    private val chatMessageList = mutableListOf<Pair<String, Boolean>>()
    private val chatMessage = MutableLiveData<Pair<String, Boolean>>()
    private var localUid: Int = 0
    private var remoteUid: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InteractionFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var isSendtoRemote = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initBottomInputView()
        initControlView()
        initHistoryView()
        initRenderer()
        initAvatarManagerDialog()
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

        ConvertClient.getInstance().init(activity)
        ConvertClient.getInstance().isDebugMode = true
        ConvertClient.getInstance()
            .setChatGPTListener(object : ConvertListener {
                override fun onVoice2Text(index: Int, text: String?) {
                    Log.i("LEGO", "onVoice2Text: text=$text")
                    //ToastUtils.showToastLong(requireContext(), "Ask: " + text!!)
                    val mText = text ?: return

                    // 发出去
                    if (enableRemoteChatMode && isSendtoRemote) {
                        val msg: MutableMap<String?, Any?> = HashMap()
                        msg["cmd"] = "syncSTTResult"
                        msg["uid"] = localUid
                        msg["text"] = mText
                        val jsonMsg = JSONObject(msg)
                        ConvertClient.getInstance().sendStreamMessage(jsonMsg)
                    }

                    // 存在本地历史记录内
                    //chatMessageList.add(Pair(mText, true))
                    chatMessage.postValue(Pair(mText, true))
                }

                override fun onText2Voice(index: Int, pcmFilePath: String?) {
                    Log.i("LEGO", "onText2Voice: pcmFilePath=$pcmFilePath")
                }

                override fun onQues2AnsSuccess(index: Int, question: String?, answer: String?) {
                    //问chatGPT回调结果
                    Log.i("LEGO", "question=$question,answer=$answer")
                    val ans = answer ?: return
                    //ToastUtils.showToastLong(requireContext(), "Ans: " + ans!!)

                    //chatMessageList.add(Pair("乐高: $ans", false))
                    chatMessage.postValue(Pair("乐高: $ans", false))

                    if (enableTTs) {
                        if (FuDevDataCenter.staControl == null) {
                            ToastUtils.showFailureToast(requireContext(), "未配置语音驱动服务")
                            return
                        }
                        fuStaViewModel.auditionVoice(ans)
                    }
                }

                override fun onFailure(errorCode: Int, message: String?) {
                    //各种错误回调
                    Log.i("LEGO", "onFailure ,message=$message")
                }

                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    localUid = uid
                }

                override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
                    Log.i("LEGO", "onStreamMessage")
                    val jsonMsg: JSONObject
                    val messageData = data ?: return
                    try {
                        val strMsg = String(messageData)
                        jsonMsg = JSONObject(strMsg)
                        if (jsonMsg.getString("cmd") == "syncSTTResult") {
                            val uid = jsonMsg.getInt("uid")
                            val text = jsonMsg.getString("text")

                            // 存在本地历史记录内
                            //chatMessageList.add(Pair("$uid: $text", false))
                            chatMessage.postValue(Pair("$uid: $text", false))
                        }
                    } catch (exp: JSONException) {
                        Log.e("LEGO", "onStreamMessage:$exp")
                    }
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    remoteUid = uid
                }
            })

        chatMessage.observe(viewLifecycleOwner) {
            addHistoryMessage(it.first, it.second)
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
                //Navigation.findNavController(it).popBackStack() //返回上一个页面
                //fuAvatarManagerViewModel.requestAvatarContainer()
                //avatarManagerDialog.show()
                findNavController().navigate(R.id.editFragment)
            }
        }
        binding.shutUpBtn.apply {
            setOnClickListener {
                fuStaViewModel.stopSpeech()
            }
        }
        binding.touchView.apply {
            setOnClickListener {
                if (pageStatus != InteractionPageStatus.Default) {
                    //binding.expandLayout.removeAllViews()
                    //pageStatus = InteractionPageStatus.History
                } else {
                    KeyboardUtils.hideSoftInput(requireContext(), binding.input.inputEditText)
                }
            }

            setTouchListener(object : TouchView.OnTouchListener {
                private var lastDeltaX = 0f

                override fun onScale(scale: Float) {
                    Log.d("LEGO", "onScale" + scale)
                    fuPreviewViewModel.scaleAvatar(scale)
                }

                override fun onMove(deltaX: Float, deltaY: Float) {
                    lastDeltaX = deltaX
//                    fuPreviewViewModel.rotateAvatar(deltaX * (binding.glTextureView.width))
//                    fuPreviewViewModel.cancelRollAvatar()
                    fuPreviewViewModel.moveVerticalAvatar(-deltaY) //Android 与 OpenGL 坐标系上下相反，故为负
                }

                override fun onClick() {
                    fuPreviewViewModel.scaleAvatar(0.002069354f)
                    fuPreviewViewModel.moveVerticalAvatar(-10f)
                }

                override fun onUp() {
                    if (lastDeltaX.absoluteValue > 0.001) { //如果手势离开屏幕时高于一定的速度,则触发惯性滚动
                        fuPreviewViewModel.rollAvatar(lastDeltaX)
                    }
                }
            })
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
        binding.input.inputSttRenderBtn.isEnabled = false
        binding.input.inputSttRenderBtn.apply {
            setBackgroundResource(R.drawable.bg_speech_input_audio_hover)
            val pressToTalkListener = PressToTalkListener()

            val touchRenderEvent = object : TouchRenderEvent {
                override fun onStart() {
                    isSendtoRemote = true
                    setInputSTTRenderBtnStyle(true)
                    popupWindowProvider.showRecordPopupWindow(null)
                    ConvertClient.getInstance().isAutoVoice2Text = false
                    ConvertClient.getInstance().startVoice2Text()
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
                    setInputSTTRenderBtnStyle(false)
                    when (status) {
                        TouchRenderEvent.FinishStatus.Success -> {
                            popupWindowProvider.dismissAllPopupWindow()
                            ConvertClient.getInstance().flushVoice2Text()
                            //finishAudioRecord()
                        }
                        TouchRenderEvent.FinishStatus.TooShort -> {
                            popupWindowProvider.showWarnPopupWindow("说话时间太短")
                            postDelayed(object : Runnable { //临时处理方式
                                override fun run() {
                                    popupWindowProvider.dismissAllPopupWindow()
                                }
                            }, 1500)
                            ConvertClient.getInstance().stopVoice2Text()
                            //cancelAudioRecord()
                        }
                        TouchRenderEvent.FinishStatus.TooLong -> {
                            popupWindowProvider.showWarnPopupWindow("说话时间太长")
                            postDelayed(object : Runnable { //临时处理方式
                                override fun run() {
                                    popupWindowProvider.dismissAllPopupWindow()
                                }
                            }, 1500)
                            ConvertClient.getInstance().stopVoice2Text()
                            //finishAudioRecord()
                        }
                        TouchRenderEvent.FinishStatus.Cancel -> {
                            popupWindowProvider.dismissAllPopupWindow()
                            ConvertClient.getInstance().stopVoice2Text()
                            //cancelAudioRecord()
                        }
                    }
                }
            }
            pressToTalkListener.touchRenderEvent = touchRenderEvent
            setOnTouchListener(pressToTalkListener)
        }
        binding.input.inputAudioRenderBtn.apply {
            setBackgroundResource(R.drawable.bg_speech_input_audio_hover)
            val pressToTalkListener = PressToTalkListener()

            val touchRenderEvent = object : TouchRenderEvent {
                override fun onStart() {
                    ConvertClient.getInstance().isAutoVoice2Text = true
                    isSendtoRemote = false
                    setInputAudioRenderBtnStyle(true)
                    popupWindowProvider.showRecordPopupWindow(null)
                    ConvertClient.getInstance().startVoice2Text()
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
                            ConvertClient.getInstance().flushVoice2Text()
                            //finishAudioRecord()
                        }
                        TouchRenderEvent.FinishStatus.TooShort -> {
                            popupWindowProvider.showWarnPopupWindow("说话时间太短")
                            postDelayed(object : Runnable { //临时处理方式
                                override fun run() {
                                    popupWindowProvider.dismissAllPopupWindow()
                                }
                            }, 1500)
                            ConvertClient.getInstance().stopVoice2Text()
                            //cancelAudioRecord()
                        }
                        TouchRenderEvent.FinishStatus.TooLong -> {
                            popupWindowProvider.showWarnPopupWindow("说话时间太长")
                            postDelayed(object : Runnable { //临时处理方式
                                override fun run() {
                                    popupWindowProvider.dismissAllPopupWindow()
                                }
                            }, 1500)
                            ConvertClient.getInstance().stopVoice2Text()
                            //finishAudioRecord()
                        }
                        TouchRenderEvent.FinishStatus.Cancel -> {
                            popupWindowProvider.dismissAllPopupWindow()
                            ConvertClient.getInstance().stopVoice2Text()
                            //cancelAudioRecord()
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
            //updateKeyboardHeightStyle(height != 0)
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
                text = "呼叫 LEGO"
            }
        }
    }

    private fun setInputSTTRenderBtnStyle(isRender: Boolean) {
        binding.input.inputSttRenderBtn.apply {
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


    //region 控制菜单
    private lateinit var legoView: LegoView
    private lateinit var modeView: ModeView
    private lateinit var skillView: SkillView
    private lateinit var avatarManagerView: AvatarManagerView
    private lateinit var animationView: AnimationView
    private lateinit var emotionView: EmotionView
    private lateinit var toneView: ToneView
    private lateinit var backgroundView: BackgroundView
    private lateinit var cameraView: TextureView
    private var enableRemoteChatMode = false
    private var enableSTT = false
    private var enableTTs = false
    private var mSystem: String = ""

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initControlView() {
        val controlView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_interaction_control, null)
        val controlBinding = LayoutInteractionControlBinding.bind(controlView)
        val fuPreviewViewModel by viewModels<FuPreviewViewModel>()

        legoView = createLegoView()
        legoView.setLegoViewListener(object : LegoView.OnLegoViewListener{
            override fun onJoinChannelButtonClick(channel: String) {
                ConvertClient.getInstance().joinChannel(channel)
            }

            override fun onLeaveChannelButtonClick() {
                ConvertClient.getInstance().leaveChannel()
            }

            override fun onLocalChatMode(enable: Boolean) {
                if (enable) {
                    ConvertClient.getInstance().enableLocalView(binding.cameraTextureView)
                } else {
                    ConvertClient.getInstance().enableLocalView(null)
                }
            }

            override fun onRemoteChatMode(enable: Boolean) {
                enableRemoteChatMode = enable
                ConvertClient.getInstance().enableRemoteView(binding.cameraTextureView, remoteUid)
            }

            override fun onSttMode(enable: Boolean) {
                enableSTT = enable
                binding.input.inputSttRenderBtn.isEnabled = enable
                binding.input.inputSttRenderBtn.apply {
                    if (enable) {
                        setBackgroundResource(R.drawable.bg_speech_input_audio_normal)
                    } else {
                        setBackgroundResource(R.drawable.bg_speech_input_audio_hover)
                    }
                }
                binding.input.inputAudioRenderBtn.apply {
                    if (enable) {
                        //setBackgroundResource(R.drawable.bg_speech_input_audio_normal)
                    } else {
                        setBackgroundResource(R.drawable.bg_speech_input_audio_hover)
                    }
                }
                ConvertClient.getInstance().enableSTT(enable)
            }

            override fun onAvatarShow(enable: Boolean) {
                binding.glTextureView.isVisible = enable
            }

            override fun onTTsMode(enable: Boolean) {
                enableTTs = enable
            }

            override fun onGPTMode(enable: Boolean) {
                ConvertClient.getInstance().setAutoVoice2Text(enable)
                binding.input.inputAudioRenderBtn.isEnabled = enable
                if (!enableSTT) return
                binding.input.inputAudioRenderBtn.apply {
                    if (enable) {
                        setBackgroundResource(R.drawable.bg_speech_input_audio_normal)
                    } else {
                        setBackgroundResource(R.drawable.bg_speech_input_audio_hover)
                    }
                }
            }

            override fun onSystemSetting(system: String) {
                mSystem = system
                ConvertClient.getInstance().setSystem(system)
            }
        })
        binding.input.inputAudioRenderBtn.isEnabled = false

        modeView = createModeView()
        modeView.setLegoViewListener(object : ModeView.OnLegoViewListener {
            override fun onButton1Click() {
                val bitmap =
                    BitmapFactory.decodeResource(requireContext().resources, R.drawable.interaction_bg1)
                viewModel.setCustomSceneBackground(bitmap)

                clearHistoryMessage()
            }

            override fun onButton2Click() {
                val bitmap =
                    BitmapFactory.decodeResource(requireContext().resources, R.drawable.lego_live_bg)
                viewModel.setCustomSceneBackground(bitmap)

                fuStaViewModel.stopSpeech() //切换音色时取消播报
                fuAvatarManagerViewModel.smartSwitchAvatar("19k8sTLbt"){
                    DevSceneManagerRepository.setAvatarDefaultAnimation(DevAvatarManagerRepository.getCurrentAvatarInfo()!!.avatar, DevAvatarManagerRepository.getCurrentAvatarInfo()!!.gender())
                } //加载形象。如果已准备好则直接加载，否则从云端下载。
                viewModel.onSwitchAvatar("19k8sTLbt")

                ConvertClient.getInstance().setSystem("我是一个推销员，我说话热情，我说话最多只说50个字, 我喜欢在句首说语气词“家人们！家人们！”，在句尾说“绝绝子～”“谁懂啊～”")

                lifecycleScope.launchWhenResumed {
                    viewModel.playAnimation(AnimationConfig.Item("wudao_20_25", "GAssets/animation/AsiaFemale/common/ani_AvatarX_afemale_wudao_20_25.bundle", ""))
                }
                Thread {
                    Thread.sleep(1100)
                    fuStaViewModel.setVoice("Laomei")
                    fuStaViewModel.auditionVoice("家人们， 家人们， 准备上链接啦！", "Laomei")
                }.start()

                clearHistoryMessage()
            }

            override fun onButton3Click() {
                val bitmap =
                    BitmapFactory.decodeResource(requireContext().resources, R.drawable.lego_ent_bg)
                viewModel.setCustomSceneBackground(bitmap)

                fuStaViewModel.stopSpeech() //切换音色时取消播报
                fuAvatarManagerViewModel.smartSwitchAvatar("1oqPeD4Tc"){
                    DevSceneManagerRepository.setAvatarDefaultAnimation(DevAvatarManagerRepository.getCurrentAvatarInfo()!!.avatar, DevAvatarManagerRepository.getCurrentAvatarInfo()!!.gender())
                } //加载形象。如果已准备好则直接加载，否则从云端下载。
                viewModel.onSwitchAvatar("1oqPeD4Tc")

                ConvertClient.getInstance().setSystem("我是一个相声演员，我说话最多只说20个字。我不会说“你”只会说“您. 我喜欢使用语气词“牛啊牛啊”“芜湖”“可不是嘛, 我喜欢使用倒装句，例如“您吃饭了吗”变为“吃饭了吗您”。")

                fuStaViewModel.stopSpeech() //切换音色时取消播报
                lifecycleScope.launchWhenResumed {
                    viewModel.playAnimation(AnimationConfig.Item("拜年", "GAssets/animation/AsiaFemale/common/ani_ptag_afemale_bainian.bundle", ""))
                }

                Thread {
                    Thread.sleep(1200)
                    fuStaViewModel.setVoice("Aikan")
                    fuStaViewModel.auditionVoice("我请您吃，蒸羊羔，蒸熊掌, 您付钱", "Aikan")
                }.start()

                clearHistoryMessage()
            }

            override fun onButton4Click() {
                val bitmap =
                    BitmapFactory.decodeResource(requireContext().resources, R.drawable.lego_class_bg)
                viewModel.setCustomSceneBackground(bitmap)

                fuStaViewModel.stopSpeech() //切换音色时取消播报
                fuAvatarManagerViewModel.smartSwitchAvatar("1tPk0t50D"){
                    DevSceneManagerRepository.setAvatarDefaultAnimation(DevAvatarManagerRepository.getCurrentAvatarInfo()!!.avatar, DevAvatarManagerRepository.getCurrentAvatarInfo()!!.gender())
                } //加载形象。如果已准备好则直接加载，否则从云端下载。
                viewModel.onSwitchAvatar("1tPk0t50D")

                fuStaViewModel.setVoice("Aiqi")
                fuStaViewModel.auditionVoice("同学们, 同学们， 我们要上课了，一二三，坐端正！", "Aiqi")

                clearHistoryMessage()
            }

            override fun onButton5Click() {
                val bitmap =
                    BitmapFactory.decodeResource(requireContext().resources, R.drawable.interaction_bg1)
                viewModel.setCustomSceneBackground(bitmap)

                fuStaViewModel.stopSpeech() //切换音色时取消播报
                fuAvatarManagerViewModel.smartSwitchAvatar("1YXjKMMzJ"){
                    DevSceneManagerRepository.setAvatarDefaultAnimation(DevAvatarManagerRepository.getCurrentAvatarInfo()!!.avatar, DevAvatarManagerRepository.getCurrentAvatarInfo()!!.gender())
                } //加载形象。如果已准备好则直接加载，否则从云端下载。
                viewModel.onSwitchAvatar("1YXjKMMzJ")

                fuStaViewModel.setVoice("Ailun")
                fuStaViewModel.auditionVoice("老板，接下来将由我协助您记录会议", "Ailun")

                clearHistoryMessage()
            }
        })


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

        val modeViewList = listOf(controlBinding.modeLego, controlBinding.modeSwitcher, controlBinding.modeSkill, controlBinding.modeAvatar, controlBinding.modeAnimation, controlBinding.modeEmotion, controlBinding.modeTone, controlBinding.modeBackground)
        fun selectModeView(modeView: View) {
            modeViewList.forEach {
                it.isSelected = false
            }
            modeView.isSelected = true
        }

        fun selectControl(controlPage: InteractionControlPage) {
            when(controlPage) {
                InteractionControlPage.Lego -> {
                    setExpandView(legoView)
                    selectModeView(controlBinding.modeLego)
                }
                InteractionControlPage.Mode -> {
                    setExpandView(modeView)
                    selectModeView(controlBinding.modeSwitcher)
                }
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

        controlBinding.modeLego.setOnClickListener {
            selectControl(InteractionControlPage.Lego)
        }
        controlBinding.modeSwitcher.setOnClickListener {
            selectControl(InteractionControlPage.Mode)
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

        selectControl(InteractionControlPage.Lego)

        binding.input.settingsBtn.setOnClickListener {
            if (isAudioRecording) return@setOnClickListener
            when(pageStatus) {
                InteractionPageStatus.Default, InteractionPageStatus.History -> {
                    binding.expandLayout.addView(controlView)
                    pageStatus = InteractionPageStatus.Control
                }
                InteractionPageStatus.Control -> {
                    binding.expandLayout.removeView(controlView)
                    pageStatus = InteractionPageStatus.History
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createLegoView(): LegoView {
        val legoView = LegoView(requireContext())
        return legoView
    }

    private fun createModeView(): ModeView {
        val modeView = ModeView(requireContext())
        return modeView
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
                Log.d("hugo", "id: " + it.id)
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
            Log.d("hugo", "name: " + it.name + " path: " + it.path + " icon: " + it.icon)
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
            Log.d("hugo", "name: " + it.name + " path: " + it.path + " icon: " + it.icon)
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
                    Log.d("hugo", "voice: " + voice.id)
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

    private lateinit var historyRecyclerView: RecyclerView
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
        this.historyRecyclerView = historyRecyclerView

        binding.expandLayout.addView(historyView)
        pageStatus = InteractionPageStatus.History
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

        //addSenseChat(ChatMessage(if(isSelf) ChatMessage.FROM_USER else ChatMessage.FROM_NLP, message))

        //addSenseChat(ChatMessage(if(isSelf) if (!isSendtoRemote) ChatMessage.SEND_TO_GPT else ChatMessage.FROM_USER else ChatMessage.FROM_NLP, message))
        if (isSelf) {
            if (isSendtoRemote) {
                addSenseChat(ChatMessage(ChatMessage.FROM_USER, message))
            } else {
                addSenseChat(ChatMessage(ChatMessage.FROM_USER_TO_GPT, message))
            }
        } else {
            addSenseChat(ChatMessage(ChatMessage.FROM_NLP, message))
        }
        historyAdapter.notifyDataSetChanged()
        historyRecyclerView.post {
            historyRecyclerView.scrollToPosition(historyRecyclerView.adapter!!.itemCount - 1)
        }
    }

    private fun clearHistoryMessage() {
        historyMessageList.clear()
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


//    private fun updatePageStatusStyle(pageStatus: InteractionPageStatus) {
//        var isShowExpandLayout = true //是否显示拓展面板。
//        when(pageStatus) {
//            InteractionPageStatus.Default -> {
//                binding.historyBtn.setImageResource(R.drawable.icon_interaction_history_nor)
//                binding.controlBtn.setImageResource(R.drawable.icon_interaction_function_nor)
//                isShowExpandLayout = false
//            }
//            InteractionPageStatus.History -> {
//                binding.historyBtn.setImageResource(R.drawable.icon_interaction_history_sel)
//                binding.controlBtn.setImageResource(R.drawable.icon_interaction_function_nor)
//            }
//            InteractionPageStatus.Control -> {
//                binding.historyBtn.setImageResource(R.drawable.icon_interaction_history_nor)
//                binding.controlBtn.setImageResource(R.drawable.icon_interaction_function_sel)
//            }
//        }
//
//        if (isShowExpandLayout) {
//            binding.historyBtn.apply {
//                marginParams.bottomMargin = 320.px
//                requestLayout()
//            }
//        } else {
//            binding.historyBtn.apply {
//                marginParams.bottomMargin = 222.px
//                requestLayout()
//            }
//        }
//    }
//
//    private fun updateKeyboardHeightStyle(isHintOtherLayout: Boolean) {
//        if (isHintOtherLayout) {
//            //binding.recommendFlowLayout.gone()
//            binding.historyBtn.gone()
//            binding.controlBtn.gone()
//        } else {
//            //binding.recommendFlowLayout.visible()
//            binding.historyBtn.visible()
//            binding.controlBtn.visible()
//        }
//    }

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
        binding.glTextureView.post {
            fuPreviewViewModel.scaleAvatar(0.002069354f)
            fuPreviewViewModel.moveVerticalAvatar(-10f)
        }
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
        //timer?.cancel()
        //timer = null
        DevDrawRepository.clearAvatarEvent()
        ptaRenderer.release()
        if (ignoreSTAModel) return
        fuStaViewModel.removeAllAvatarAnimationConfig()
        fuStaViewModel.release()
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

}