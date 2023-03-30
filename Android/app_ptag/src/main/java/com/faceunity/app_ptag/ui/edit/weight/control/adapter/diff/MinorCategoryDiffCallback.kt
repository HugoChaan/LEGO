package com.faceunity.app_ptag.ui.edit.weight.control.adapter.diff

import androidx.recyclerview.widget.DiffUtil
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.MinorWrapper

/**
 *
 */
object MinorCategoryDiffCallback : DiffUtil.ItemCallback<MinorWrapper>() {
    override fun areItemsTheSame(
        oldItem: MinorWrapper,
        newItem: MinorWrapper
    ): Boolean {
        return newItem.model.key == oldItem.model.key
    }

    override fun areContentsTheSame(
        oldItem: MinorWrapper,
        newItem: MinorWrapper
    ): Boolean {
        return newItem.model.key == oldItem.model.key
                && newItem.isSelect == oldItem.isSelect
    }

}