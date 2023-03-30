package com.faceunity.app_ptag.ui.edit.entity.state

import com.faceunity.app_ptag.ui.edit.filter.FilterGroup

/**
 * 编辑页需要由 ViewModel 管理的 Avatar 相关数据
 */
data class EditPageUiState(
    val gender: String,
    val isAvatarPrepare: Boolean,
    val filterGroup: FilterGroup
) {
    companion object {
        fun default() = EditPageUiState("", false, FilterGroup())
    }
}
