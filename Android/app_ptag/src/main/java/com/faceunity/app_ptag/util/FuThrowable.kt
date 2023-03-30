package com.faceunity.app_ptag.util

/**
 *
 */
class CanNotFindRenderAvatarThrowable : Throwable() {
    override fun toString(): String {
        return "当前没有正在渲染的形象"
    }
}