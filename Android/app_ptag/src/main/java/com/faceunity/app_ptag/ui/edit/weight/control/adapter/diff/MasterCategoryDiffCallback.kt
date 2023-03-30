package com.faceunity.app_ptag.ui.edit.weight.control.adapter.diff

import androidx.recyclerview.widget.DiffUtil
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.MasterWrapper

/**
 *
 */
object MasterCategoryDiffCallback : DiffUtil.ItemCallback<MasterWrapper>() {
    override fun areItemsTheSame(
        oldItem: MasterWrapper,
        newItem: MasterWrapper
    ): Boolean {
        return newItem.model.key == oldItem.model.key
    }

    override fun areContentsTheSame(
        oldItem: MasterWrapper,
        newItem: MasterWrapper
    ): Boolean {
        return newItem.model.key == oldItem.model.key
                && newItem.isSelect == oldItem.isSelect
    }

}