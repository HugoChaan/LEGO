package com.faceunity.app_ptag.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.FuDevInitializeWrapper
import com.faceunity.app_ptag.ui.interaction.network.InteractionCloudSyncRepository
import com.faceunity.app_ptag.ui.interaction.network.entity.AnimationConfigExpand
import com.faceunity.app_ptag.ui.interaction.network.entity.InteractionSkillResult
import com.faceunity.app_ptag.ui.interaction.network.entity.InteractionVoiceResult
import com.faceunity.app_ptag.ui.interaction.tag.FuTagManager
import com.faceunity.app_ptag.ui.interaction.tag.parser.FuAnimationTagParser
import com.faceunity.app_ptag.use_case.FuUseCaseFactory
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.core.entity.FUAnimationBundleData
import com.faceunity.core.enumeration.FULogicNodeSwitchEnum
import com.faceunity.editor_ptag.business.sta.STAServiceStreamOverlayCacheControlImpl
import com.faceunity.editor_ptag.business.sta.SpeechToAnimationWrapper
import com.faceunity.editor_ptag.business.sta.callback.AsrNlpTtsCallback
import com.faceunity.editor_ptag.business.sta.callback.NlpTtsCallback
import com.faceunity.editor_ptag.business.sta.callback.SpeechCallback
import com.faceunity.editor_ptag.business.sta.config.InitSpeechConfig
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.editor_ptag.util.safeCollect
import com.faceunity.fupta.cloud_download.ICloudPlatformSyncAPI
import com.faceunity.fupta.cloud_download.ServiceCodeThrowable
import com.faceunity.fupta.sta.interaction.entity.FuAsrNlpTtsResult
import com.faceunity.fupta.sta.interaction.entity.FuNlpTtsResult
import com.faceunity.pta.pta_core.model.AvatarInfo
import com.faceunity.toolbox.utils.FURandomUtils
import kotlinx.coroutines.launch

/**
 * 语音驱动的辅助 ViewModel
 */
class FuStaViewModel : ViewModel() {
    private val notFindInitConfig = FuDevDataCenter.staControl == null
    private val staControl: SpeechToAnimationWrapper
        get() = FuDevDataCenter.staControl!!

    private val interactionCloudRepo: InteractionCloudSyncRepository by lazy {
        FuDI.getCustom()
    }

    private var controlAvatar: Avatar? = null
    private var gender: String = ""
    private var voice: String = ""
    private var defaultMaleVoice = "Kenny"
    private var defaultFemaleVoice = "Aiqi"
    private var animationConfig: AnimationConfigExpand? = null
    private var isRelease = false

    private val _isSTAServerInitSuccessLiveData = MutableLiveData<Boolean>()
    val isSTAServerInitSuccessLiveData: LiveData<Boolean> get() = _isSTAServerInitSuccessLiveData

    private val _animationConfigLiveData = MutableLiveData<AnimationConfigExpand>()
    val animationConfigLiveData: LiveData<AnimationConfigExpand> get() = _animationConfigLiveData

    //不想持有 ChatMessage，所以暂时未在 ViewModel 里存储消息数据。
    private val _historyNewMessageLiveData = MutableLiveData<List<Pair<String, Boolean>>>()
    val historyNewMessageLiveData: LiveData<List<Pair<String, Boolean>>> get() = _historyNewMessageLiveData

    private val _staErrorTipLiveData = MutableLiveData<String>()
    val staErrorTipLiveData: LiveData<String> get() = _staErrorTipLiveData

    private val _skillListLiveData = MutableLiveData<List<InteractionSkillResult.Data.Skill>>()
    val skillListLiveData: LiveData<List<InteractionSkillResult.Data.Skill>> get() = _skillListLiveData

    private val _recommendSkillListLiveData = MutableLiveData<List<InteractionSkillResult.Data.RecommendSkill>>()
    val recommendSkillListLiveData: LiveData<List<InteractionSkillResult.Data.RecommendSkill>> get() = _recommendSkillListLiveData

    private val _voiceListLiveData = MutableLiveData<List<InteractionVoiceResult.Data.Voice>>()
    val voiceListLiveData: LiveData<List<InteractionVoiceResult.Data.Voice>> get() = _voiceListLiveData

    private val _auditionEventLiveData = MutableLiveData<AuditionEvent>()
    val auditionEventLiveData: LiveData<AuditionEvent> get() = _auditionEventLiveData

    private val downloadBundleUseCase by lazy {
        FuUseCaseFactory.downloadBundleUseCase
    }

    /**
     * 当前播报的 ID。
     * 根据目前需求，仅对当前播报的内容进行处理，取消掉之前的播报 ID。
     */
    private var currentSpeechId: String? = null

    /**
     * 记录指定的播报 ID 是否有动作标签。
     */
    private val speechIsIncludeLabelMap = mutableMapOf<String, Boolean>()

    /**
     * 初始化 STA 渲染所需要的数据
     */
    fun initSTARender() {
        if (notFindInitConfig()) return
        staControl.initWrapper(
            InitSpeechConfig(
                auth = FuDevInitializeWrapper.getAuthPack(),
                data_bs = "fusta/defaultBSConfig.bin",
                data_decoder = "fusta/data_decoder.bin",
                data_map = "", //未用到
                vta_align = "" //未用到
            )
        )
        DevAvatarManagerRepository.getCurrentAvatarId()?.let {
            setSTAControlAvatar(it)
        }
    }

    /**
     * 初始化 STA 服务所需要的数据。
     * 依赖 [FuAvatarManagerViewModel.initCloudAvatar]，需对应方法申请到 Token 后用于该服务。
     */
    fun initSTAServer() {
        if (notFindInitConfig()) return
        isRelease = false
        if (FuDevDataCenter.tokenObserver.getToken() == null) {
            requestToken()
        }
        _isSTAServerInitSuccessLiveData.postValue(true)
    }

    private fun requestToken(onSuccess: () -> Unit = {}) {
        if (notFindInitConfig()) return
        viewModelScope.launch {
            FuDI.getCustom<ICloudPlatformSyncAPI>().requestToken().onSuccess {
                val token = it.data?.value
                if (token != null) {
                    FuDevDataCenter.tokenObserver.updateToken(token)
                    onSuccess()
                }
            }.onFailure {
                FuLog.error("request token failed, error: ${it}")
            }
        }
    }

    fun setSTAControlAvatar(avatarId: String) {
        if (notFindInitConfig()) return
        val avatarInfo = DevAvatarManagerRepository.getAvatarInfo(avatarId)
        if (avatarInfo == null) {
            FuLog.warn("setSTAControlAvatar: avatarId $avatarId not found")
            return
        }
        gender = avatarInfo.gender()
        syncVoiceByGender()
        controlAvatar = avatarInfo.avatar
        staControl.setControlAvatar(avatarInfo.avatar)

        //每次切换形象后，为 Tag 解析服务设置一个动作解析器
        val animationTagParser = FuAnimationTagParser(controlAvatar!!, gender)
        FuTagManager.clearParser()
        FuTagManager.addParser(animationTagParser)
    }

    fun requestHomePage() {
        if (notFindInitConfig()) return
        if (interactionCloudRepo.token.isBlank()) {
            FuLog.warn("requestHomePage: token is empty")
            return
        }
        viewModelScope.launch {
            interactionCloudRepo.homePage().onSuccess {
                if (it.data == null) {
                    FuLog.warn("requestHomePage: data is null")
                    return@onSuccess
                }
                val animationConfigExpand = AnimationConfigExpand.parse(it)
                animationConfig = animationConfigExpand
                _animationConfigLiveData.postValue(animationConfigExpand)
            }.onFailure {
                if (it is ServiceCodeThrowable.TokenThrowable) {
                    requestToken {
                        requestHomePage()
                    }
                } else {
                    FuLog.error(it.toString())
                }
            }
        }
    }

    fun requestVoiceList() {
        if (notFindInitConfig()) return
        if (interactionCloudRepo.token.isBlank()) {
            FuLog.warn("requestVoiceList: token is empty")
            return
        }
        viewModelScope.launch {
            interactionCloudRepo.voiceList().onSuccess {
                if (it.data == null) {
                    FuLog.warn("requestVoiceList: data is null")
                    return@onSuccess
                }
                defaultMaleVoice = it.data.defaultMaleVoice
                defaultFemaleVoice = it.data.defaultFemaleVoice
                syncVoiceByGender()
                _voiceListLiveData.postValue(it.data.voiceList)
            }.onFailure {
                if (it is ServiceCodeThrowable.TokenThrowable) {
                    requestToken {
                        requestHomePage()
                    }
                } else {
                    FuLog.error(it.toString())
                }
            }
        }
    }

    fun requestSkill() {
        if (notFindInitConfig()) return
        if (interactionCloudRepo.token.isBlank()) {
            FuLog.warn("requestSkill: token is empty")
            return
        }
        viewModelScope.launch {
            interactionCloudRepo.skill().onSuccess {
                it.data.nlp.apply { //对于相芯自带的 Demo，需要手动设置两个 NLP 参数。
                    (staControl.staServiceControl as? STAServiceStreamOverlayCacheControlImpl)?.let { control ->
                        control.setNlpConfig(STAServiceStreamOverlayCacheControlImpl.NlpConfig(
                            nlpSupplier = nlpSupplier,
                            nlpRobotID = nlpRobotID
                        ))
                    }
                }
                _recommendSkillListLiveData.postValue(it.data.recommendSkills)
                _skillListLiveData.postValue(it.data.skills)
            }.onFailure {
                if (it is ServiceCodeThrowable.TokenThrowable) {
                    requestToken {
                        requestHomePage()
                    }
                } else {
                    FuLog.error(it.toString())
                }
            }
        }
    }

    fun postRecommendSkillListLiveData() {
        if (notFindInitConfig()) return
        if (_recommendSkillListLiveData.value != null) {
            _recommendSkillListLiveData.postValue(_recommendSkillListLiveData.value)
        }
    }

    fun setVoice(voice: String) {
        this.voice = voice
    }

    fun getVoice(): String {
        return voice
    }

    fun getGender(): String {
        return gender
    }

    private fun syncVoiceByGender() {
        if (notFindInitConfig()) return
        fun setGenderVoice() {
            voice = if (gender == "male") {
                defaultMaleVoice
            } else {
                defaultFemaleVoice
            }
        }
        if (voice.isBlank()) {
            setGenderVoice()
        } else {
            if (voice == defaultMaleVoice || voice == defaultFemaleVoice) {
                setGenderVoice()
            }
        }
    }

    private fun downloadBundle(fileIdList: List<String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            downloadBundleUseCase(fileIdList).safeCollect {
                FuLog.info("download Animation Status:$it")
            }
            onSuccess()
        }
    }


    fun stopSpeech() {
        if (notFindInitConfig()) return
        staControl.cancelAllSpeech()
    }

    fun auditionVoice(text: String, inputVoice: String = voice) {
        if (notFindInitConfig()) return
        val uuid = FURandomUtils.randomID()
        currentSpeechId?.let { staControl.cancelTask(it) }
        currentSpeechId = uuid
        staControl.startSpeech(text, inputVoice, id = uuid, speechCallback = object : SpeechCallback {
            override fun onStart(avatar: Avatar?) {
                _auditionEventLiveData.postValue(AuditionEvent(inputVoice, AuditionEvent.Status.Start))
            }

            override fun onCancel(avatar: Avatar?) {
                _auditionEventLiveData.postValue(AuditionEvent(inputVoice, AuditionEvent.Status.Finish))
            }

            override fun onFinish(avatar: Avatar?) {
                _auditionEventLiveData.postValue(AuditionEvent(inputVoice, AuditionEvent.Status.Finish))
            }
        })
    }


    fun sendTextMessage(inputContent: String) {
        if (notFindInitConfig()) return
        addHistoryMessage(inputContent, true)
        val uuid = FURandomUtils.randomID()
        currentSpeechId?.let { staControl.cancelTask(it) }
        currentSpeechId = uuid
        staControl.startChat(inputContent, voice, id = uuid, speechCallback = object : SpeechCallback {
            /**
             * 本次播报是否需要切换成 Talk 状态。
             * 当该播报含动作标签时 false；不含动作标签时 true。
             */
            private var needSwitchTalk = true

            override fun onStart(avatar: Avatar?) {
                if (speechIsIncludeLabelMap[uuid] == true) {
                    needSwitchTalk = false
                }
                if (needSwitchTalk) {
                    avatar?.animationGraph?.switchLogicNode(FULogicNodeSwitchEnum.TALK)
                }
            }

            override fun onFinish(avatar: Avatar?) {
                speechIsIncludeLabelMap.remove(uuid)
                if (needSwitchTalk && avatar != null) {
//                    avatar?.animationGraph?.switchLogicNode(FULogicNodeSwitchEnum.IDLE)
                    DevSceneManagerRepository.setAvatarDefaultAnimation(avatar, gender)
                }
            }

            override fun onPhonemeAction(content: String) {
                FuTagManager.action(content)
            }
        }, nlpTtsCallback = object : NlpTtsCallback {
            override fun onSuccess(result: FuNlpTtsResult) {
                speechIsIncludeLabelMap[result.ack.uuid] = result.ack.includeLabel
                addHistoryMessage(result.ack.nlpText, false)
            }

            override fun onError(error: Throwable) {
                _staErrorTipLiveData.postValue(error.message)
            }
        })
    }

    fun sendAudioMessage(audioData: ByteArray) {
        if (notFindInitConfig()) return
        val uuid = FURandomUtils.randomID()
        currentSpeechId?.let { staControl.cancelTask(it) }
        currentSpeechId = uuid
        staControl.startChat(audioData, voice, id = uuid, speechCallback = object : SpeechCallback {
            /**
             * 本次播报是否需要切换成 Talk 状态。
             * 当该播报含动作标签时 false；不含动作标签时 true。
             */
            private var needSwitchTalk = true

            override fun onStart(avatar: Avatar?) {
                if (speechIsIncludeLabelMap[uuid] == true) {
                    needSwitchTalk = false
                }
                if (needSwitchTalk) {
                    avatar?.animationGraph?.switchLogicNode(FULogicNodeSwitchEnum.TALK)
                }
            }

            override fun onFinish(avatar: Avatar?) {
                if (needSwitchTalk && avatar != null) {
//                    avatar?.animationGraph?.switchLogicNode(FULogicNodeSwitchEnum.IDLE)
                    DevSceneManagerRepository.setAvatarDefaultAnimation(avatar, gender)
                }
            }

            override fun onPhonemeAction(content: String) {
                FuTagManager.action(content)
            }
        }, asrNlpTtsCallback = object : AsrNlpTtsCallback {
            override fun onSuccess(result: FuAsrNlpTtsResult) {
                speechIsIncludeLabelMap[result.ack.uuid] = result.ack.includeLabel
                addHistoryMessageList(listOf(
                    result.ack.asrText to true,
                    result.ack.nlpText to false
                ))
            }

            override fun onError(error: Throwable) {
                _staErrorTipLiveData.postValue(error.message)
            }
        })
    }

    private fun addHistoryMessageList(msgList: List<Pair<String, Boolean>>) {
        _historyNewMessageLiveData.postValue(msgList)
    }

    private fun addHistoryMessage(message: String, isSelf: Boolean) {
        _historyNewMessageLiveData.postValue(listOf(Pair(message, isSelf)))
    }

    fun applyAnimationConfig(avatarId: String) {
        if (notFindInitConfig()) return
        val avatarInfo = DevAvatarManagerRepository.getAvatarInfo(avatarId)
        if (avatarInfo == null) {
            FuLog.warn("applyAnimationConfig failed: can't find avatarId=$avatarId.")
            return
        }
        val config = animationConfig?.genderConfig(avatarInfo.gender())
        if (config == null) {
            FuLog.warn("applyAnimationConfig failed: animationConfig is null, may need to call requestHomePage().")
            return
        }
//        val idleAnimationFileIdList = config.idleState.map { it.path }
//        downloadBundle(idleAnimationFileIdList) {
//            if (isRelease) return@downloadBundle
//            avatarInfo.avatar.apply {
//                idleAnimationFileIdList.forEach {
//                    animation.addAnimation(FUAnimationBundleData(
//                        path = FuDevDataCenter.resourcePath.ossBundle(it),
//                        name = it,
//                        nodeName = "IdleState",
//                        repeatable = false,
//                        probability = 1
//                    ))
//                }
//                animationGraph.switchLogicNode(FULogicNodeSwitchEnum.IDLE)
//            }
//        }
        val talkAnimationFileIdList = config.talkState.map { it.path }
        downloadBundle(talkAnimationFileIdList) {
            if (isRelease) return@downloadBundle
            avatarInfo.avatar.apply {
                talkAnimationFileIdList.forEach {
                    animation.addAnimation(FUAnimationBundleData(
                        path = FuDevDataCenter.resourcePath.ossBundle(it),
                        name = it,
                        nodeName = "TalkState",
                        repeatable = false,
                        probability = 1
                    ))
                }
            }
        }
    }

    fun removeAllAvatarAnimationConfig() {
        if (notFindInitConfig()) return
        DevAvatarManagerRepository.mapAvatar {
            if (it.state == AvatarInfo.State.PrepareAvatar) {
                removeAnimationConfig(it.id)
            }
        }
    }

    fun removeAnimationConfig(avatarId: String) {
        if (notFindInitConfig()) return
        val avatarInfo = DevAvatarManagerRepository.getAvatarInfo(avatarId)
        if (avatarInfo == null) {
            FuLog.warn("removeAnimationConfig failed: can't find avatarId=$avatarId.")
            return
        }
        val config = animationConfig?.genderConfig(avatarInfo.gender())
        if (config == null) {
            FuLog.warn("removeAnimationConfig failed: animationConfig is null.")
            return
        }
        val idleAnimationFileIdList = config.idleState.map { it.path }
        FuLog.warn("removeAnimationConfig:$idleAnimationFileIdList")
        avatarInfo.avatar.animation.getAnimations().filter {
            it.nodeName == "IdleState" && it.name in idleAnimationFileIdList
        }.let {
            avatarInfo.avatar.animation.removeAnimation(it)
        }
        val talkAnimationFileIdList = config.talkState.map { it.path }
        FuLog.warn("removeAnimationConfig:$talkAnimationFileIdList")
        avatarInfo.avatar.animation.getAnimations().filter {
            it.nodeName == "TalkState" && it.name in talkAnimationFileIdList
        }.let {
            avatarInfo.avatar.animation.removeAnimation(it)
        }
    }

    fun syncSpeechStatus() {
        if (notFindInitConfig()) return
        staControl.syncSpeechStatus()
    }

    fun release() {
        if (notFindInitConfig()) return
        isRelease = true
        staControl.cancelAllSpeech()
        staControl.releaseWrapper()
        FuDevDataCenter.tokenObserver.removeTokenObserverByName("sta")
    }

    private fun notFindInitConfig(): Boolean {
        if (notFindInitConfig) {
            FuLog.warn("未在 FuDevInitializeWrapper 配置 STA 服务。")
        }
        return notFindInitConfig
    }


    data class AuditionEvent(val auditionId: String, val status: Status) {

        enum class Status {
            Start,
            Finish
        }
    }
}