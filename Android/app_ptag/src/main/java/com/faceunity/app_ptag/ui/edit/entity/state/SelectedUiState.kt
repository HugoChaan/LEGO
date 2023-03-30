package com.faceunity.app_ptag.ui.edit.entity.state

import com.faceunity.app_ptag.ui.edit.entity.control.FuColor
import com.faceunity.core.avatar.model.Avatar

/**
 *
 */
data class SelectedUiState(
    /**
     * 选中的颜色。key：颜色的 key；value：身上的颜色。
     */
    val color: Map<String, FuColor>,
    /**
     * 选中的道具。选中的道具的 fileId。
     */
    val bundle: Set<String>,
    /**
     * 捏脸的当前数据
     */
    val facePupMap: Map<String, Float>
) {

    companion object {
        fun empty() = SelectedUiState(emptyMap(), emptySet(), emptyMap())

        fun build(avatar: Avatar) = SelectedUiState(
            color = avatar.color.getColorCache().mapValues {
                FuColor(it.value.red, it.value.green, it.value.blue, it.value.alpha.toFloat())
            },
            bundle = avatar.getComponents().map { it.fileId }.filterNotNull().toSet(),
            facePupMap = avatar.deformation.getDeformationCache()
        )
    }
}