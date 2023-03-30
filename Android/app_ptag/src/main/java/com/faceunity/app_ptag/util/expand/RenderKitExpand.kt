package com.faceunity.app_ptag.util.expand

import com.faceunity.core.avatar.model.Scene

/**
 *
 */

fun Scene?.printInfo() = if (this != null) {
    """
    Scene(background:${getBackground()}、light:${getLightingBundle()}、camera：${cameraAnimation.getAnimation()})
""".trimIndent()
} else {
    "null"
}