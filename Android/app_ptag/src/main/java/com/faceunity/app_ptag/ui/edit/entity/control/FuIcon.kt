package com.faceunity.app_ptag.ui.edit.entity.control

/**
 *
 */
data class FuIcon(val path: String, val type: Type) {

    enum class Type{
        File,
        Url,
        Assets,
        Null
    }

    companion object {
        fun buildNull() = FuIcon("", Type.Null)
    }
}