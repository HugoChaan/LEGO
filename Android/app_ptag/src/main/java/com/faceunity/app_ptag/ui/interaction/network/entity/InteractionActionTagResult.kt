package com.faceunity.app_ptag.ui.interaction.network.entity

import com.faceunity.app_ptag.ui.interaction.entity.TagConfig

data class InteractionActionTagResult(
    val code: Int,
    val `data`: Data,
    val message: String
) {
    data class Data(
        val TagConfig: TagConfig
    )
}