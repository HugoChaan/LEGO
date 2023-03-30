package com.faceunity.app_ptag.ui.edit.weight.control.adapter.diff

import androidx.recyclerview.widget.DiffUtil
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.SubColorWrapper

/**
 *
 */
object SubCategoryColorDiffCallback : DiffUtil.ItemCallback<SubColorWrapper>() {
    override fun areItemsTheSame(oldItem: SubColorWrapper, newItem: SubColorWrapper): Boolean {
        if (newItem.model.type != oldItem.model.type) return false
        return newItem.model.key == oldItem.model.key
    }

    override fun areContentsTheSame(oldItem: SubColorWrapper, newItem: SubColorWrapper): Boolean {
        if (newItem.model.type != oldItem.model.type) return false
        return newItem.model.key == oldItem.model.key
                && newItem.isSelect == oldItem.isSelect
    }

}