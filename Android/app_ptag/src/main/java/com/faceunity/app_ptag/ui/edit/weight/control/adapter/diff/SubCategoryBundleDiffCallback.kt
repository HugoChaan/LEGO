package com.faceunity.app_ptag.ui.edit.weight.control.adapter.diff

import androidx.recyclerview.widget.DiffUtil
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.SubBundleWrapper

/**
 *
 */
object SubCategoryBundleDiffCallback : DiffUtil.ItemCallback<SubBundleWrapper>() {
    override fun areItemsTheSame(oldItem: SubBundleWrapper, newItem: SubBundleWrapper): Boolean {
        if (newItem.model.type != oldItem.model.type) return false
        return newItem.model.fileId == oldItem.model.fileId
    }

    override fun areContentsTheSame(oldItem: SubBundleWrapper, newItem: SubBundleWrapper): Boolean {
        if (newItem.model.type != oldItem.model.type) return false
        return newItem.model.fileId == oldItem.model.fileId
                && newItem.isSelect == oldItem.isSelect
                && newItem.downloadStyle == oldItem.downloadStyle
    }

}