package com.faceunity.app_ptag.view_model.entity

import com.faceunity.editor_ptag.business.cloud.CloudException

/**
 *
 */
sealed class FuDownloadAvatarState {
    object Start : FuDownloadAvatarState()
    class ConfigFinish(val avatarId: String) : FuDownloadAvatarState()
    class ConfigFailed(val avatarId: String, val cloudException: CloudException) : FuDownloadAvatarState()
}
