package com.faceunity.app_ptag.ui.edit.entity.state

/**
 *
 */
data class EditHistoryUiState(
    val backCount: Int,
    val forwardCount: Int,
    val historyCount: Int
) {
    val canBack: Boolean = backCount != 0

    val canForward: Boolean = forwardCount != 0

    val canReset: Boolean = !((historyCount == 1) && backCount == 0)

    companion object {
        fun default() = EditHistoryUiState(
            0, 0, 1
        )
    }
}
