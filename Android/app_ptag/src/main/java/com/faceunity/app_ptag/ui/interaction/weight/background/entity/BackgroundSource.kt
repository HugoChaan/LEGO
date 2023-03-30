package com.faceunity.app_ptag.ui.interaction.weight.background.entity

/**
 *
 */
sealed class BackgroundSource {
    data class Default(val drawableId: Int) : BackgroundSource()
    data class User(val path: String) : BackgroundSource()
    class Add(val drawableId: Int) : BackgroundSource()
}