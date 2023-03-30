package com.faceunity.app_ptag.view_model.entity

/**
 *
 */
sealed class FuSwitchAvatarState(open val avatarId: String) {
    class Success(avatarId: String) : FuSwitchAvatarState(avatarId)
    data class Failure(override val avatarId: String, val errorMsg: String) : FuSwitchAvatarState(avatarId)
}
