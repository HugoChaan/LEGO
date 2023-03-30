package com.faceunity.app_ptag.ui.interaction.weight.animation

import com.faceunity.app_ptag.ui.edit.weight.control.adapter.StyleRecordInterface
import com.faceunity.app_ptag.ui.interaction.entity.AnimationConfig

/**
 * 与 [AnimationView] 关联的所有 UI 状态，它的显示都由该类控制。
 */
data class AnimationUiState(
    val list: List<AnimationConfig.Item>,
    val downloadStateMap: Map<String, StyleRecordInterface.DownloadStyle>,
    val selectItem: AnimationConfig.Item?
) {
    companion object {
        val default = AnimationUiState(emptyList(), emptyMap(), null)
    }
}