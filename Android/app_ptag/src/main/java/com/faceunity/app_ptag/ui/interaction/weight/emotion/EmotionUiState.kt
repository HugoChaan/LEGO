package com.faceunity.app_ptag.ui.interaction.weight.emotion

import com.faceunity.app_ptag.ui.edit.weight.control.adapter.StyleRecordInterface
import com.faceunity.app_ptag.ui.interaction.entity.EmotionConfig

/**
 * 与 [AnimationView] 关联的所有 UI 状态，它的显示都由该类控制。
 */
data class EmotionUiState(
    val list: List<EmotionConfig.Item>,
    val downloadStateMap: Map<String, StyleRecordInterface.DownloadStyle>,
    val selectItem: EmotionConfig.Item?
) {
    companion object {
        val default = EmotionUiState(emptyList(), emptyMap(), null)
    }
}