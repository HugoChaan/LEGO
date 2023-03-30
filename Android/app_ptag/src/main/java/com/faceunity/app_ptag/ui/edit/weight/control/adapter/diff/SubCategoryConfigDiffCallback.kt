package com.faceunity.app_ptag.ui.edit.weight.control.adapter.diff

import androidx.recyclerview.widget.DiffUtil
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.SubConfigWrapper

/**
 *
 */
object SubCategoryConfigDiffCallback : DiffUtil.ItemCallback<SubConfigWrapper>() {
    override fun areItemsTheSame(oldItem: SubConfigWrapper, newItem: SubConfigWrapper): Boolean {
        if (newItem.model.type != oldItem.model.type) return false
        return newItem.model.filePath == oldItem.model.filePath
    }

    override fun areContentsTheSame(oldItem: SubConfigWrapper, newItem: SubConfigWrapper): Boolean {
        if (newItem.model.type != oldItem.model.type) return false
        return newItem.model.filePath == oldItem.model.filePath
                && newItem.isSelect == oldItem.isSelect
                && newItem.downloadStyle == oldItem.downloadStyle
    }

}