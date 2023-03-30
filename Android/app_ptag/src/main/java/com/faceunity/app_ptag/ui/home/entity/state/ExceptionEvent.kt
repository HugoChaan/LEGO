package com.faceunity.app_ptag.ui.home.entity.state

/**
 *
 */
sealed class ExceptionEvent {
    object Nothing : ExceptionEvent()
    data class FailedToast(val content: String) : ExceptionEvent()
    data class RetryDialog(val content: String, val retryBlock: () -> Unit) : ExceptionEvent()
}
