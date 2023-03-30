package com.faceunity.app_ptag.ui.interaction.network.entity

import com.faceunity.app_ptag.ui.interaction.entity.AnimationConfig
import com.faceunity.app_ptag.ui.interaction.entity.EmotionConfig

data class InteractionAnimationResult(
    val code: Int,
    val `data`: Data,
    val message: String
) {
    data class Data(
        val AnimationConfig: Map<String, List<AnimationConfig.Item>>,
        val EmoticonConfig: Map<String, List<EmotionConfig.Item>>
    )
}