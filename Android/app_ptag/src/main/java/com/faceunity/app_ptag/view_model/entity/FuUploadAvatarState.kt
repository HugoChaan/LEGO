package com.faceunity.app_ptag.view_model.entity

/**
 *
 */
sealed class FuUploadAvatarState {


    class Success(val avatarId: String) : FuUploadAvatarState()

    class Error(val errorInfo: String) : FuUploadAvatarState()

}
