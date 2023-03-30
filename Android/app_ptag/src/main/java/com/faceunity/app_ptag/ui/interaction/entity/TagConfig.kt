package com.faceunity.app_ptag.ui.interaction.entity

/**
 *
 */
class TagConfig(
    val animation: List<Animation>
) {

    data class Animation(
        val tag: String,
        val pathMap: Map<String, String>
    )
}