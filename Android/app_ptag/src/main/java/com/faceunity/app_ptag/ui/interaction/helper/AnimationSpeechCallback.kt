package com.faceunity.app_ptag.ui.interaction.helper

import com.faceunity.core.avatar.model.Avatar
import com.faceunity.core.entity.FUAnimationBundleData
import com.faceunity.core.enumeration.FULogicNodeSwitchEnum
import com.faceunity.editor_ptag.business.sta.callback.SpeechCallback

/**
 * 一个用于在语音播报时播放一个指定动画的 [SpeechCallback]，在完成播放后会恢复。
 */
class AnimationSpeechCallback(val animationPath: String) : SpeechCallback {
    val animName = animationPath.split("/").last()

    override fun onStart(avatar: Avatar?) {
        avatar?.let {
            val animation = FUAnimationBundleData(
                path = animationPath,
                name = animName,
                nodeName = "TalkState",
                repeatable = true
            )
            it.animation.addAnimation(animation)
            it.animationGraph.switchLogicNode(FULogicNodeSwitchEnum.TALK)
        }
    }

    override fun onFinish(avatar: Avatar?) {
        avatar?.let {
            it.animation.getAnimations().filter { it.name == animName }.forEach { anim ->
                it.animation.removeAnimation(anim)
            }
            it.animationGraph.switchLogicNode(FULogicNodeSwitchEnum.IDLE)
        }
    }
}