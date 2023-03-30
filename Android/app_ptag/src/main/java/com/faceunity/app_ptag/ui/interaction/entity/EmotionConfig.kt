package com.faceunity.app_ptag.ui.interaction.entity

/**
 *
 */
data class EmotionConfig(val map: Map<String, List<Item>>) {

    data class Item(
        val name: String,
        val path: String,
        val icon: String
    )
}