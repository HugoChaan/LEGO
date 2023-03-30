package com.faceunity.app_ptag.edit_cloud.entity

/**
 *
 */
sealed class FuUpdateEditDataState {
    object Uninitialized : FuUpdateEditDataState()
    object Start : FuUpdateEditDataState()
    object NotNeedUpdate: FuUpdateEditDataState()
    data class NotifyUpdateSuccess(val name: String) : FuUpdateEditDataState()
    data class NotifyUpdateFailure(val name: String, val throwable: Throwable) : FuUpdateEditDataState()
    class Finish(): FuUpdateEditDataState()
    data class CloudFailure(val throwable: Throwable) : FuUpdateEditDataState()
    data class Failure(val throwable: Throwable) : FuUpdateEditDataState()
}
