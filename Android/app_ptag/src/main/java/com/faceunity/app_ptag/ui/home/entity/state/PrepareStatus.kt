package com.faceunity.app_ptag.ui.home.entity.state

/**
 *
 */
sealed class PrepareStatus {
    object Init : PrepareStatus()
    object Success: PrepareStatus()
    object Failed: PrepareStatus()

    fun isNotFinish() = this == Init
}
