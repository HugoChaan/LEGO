package com.faceunity.app_ptag.ui.interaction.tag

/**
 *
 */
abstract class FuTagParser {

    abstract fun accept(tag: String) : Boolean

    abstract fun parse(tag: String)
}