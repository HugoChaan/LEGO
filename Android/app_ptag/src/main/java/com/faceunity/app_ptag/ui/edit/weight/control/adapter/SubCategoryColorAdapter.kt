package com.faceunity.app_ptag.ui.edit.weight.control.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ItemSubCategoryColorBinding
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.SubColorWrapper
import com.faceunity.app_ptag.ui.edit.weight.control.AvatarControlHolderEvent
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.diff.SubCategoryColorDiffCallback
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.visible

/**
 * Created on 2021/7/22 0022 17:13.
 */
internal class SubCategoryColorAdapter(
    private val eventListener: AvatarControlHolderEvent
) : ListAdapter<SubColorWrapper, SubCategoryColorAdapter.SubColorViewHolder>(SubCategoryColorDiffCallback),
    StyleRecordInterface by StyleRecordHelper() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubColorViewHolder {
        return SubColorViewHolder(
            ItemSubCategoryColorBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            eventListener
        )
    }

    override fun onBindViewHolder(holder: SubColorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubColorViewHolder(
        private val binding: ItemSubCategoryColorBinding,
        private val eventListener: AvatarControlHolderEvent
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SubColorWrapper) {
            val (model, isSelect) = item
            binding.background.apply {
                (drawable as? GradientDrawable)?.let {
                    it.setColor(ContextCompat.getColor(context, R.color.item_bg))
                }
            }
            binding.color.apply {
                if (isSelect) {
                    binding.color.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .start()
                } else {
                    binding.color.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .start()
                }
//                ImageViewCompat.setImageTintList(binding.color, ColorStateList.valueOf(item.color))

                (drawable as? GradientDrawable)?.let {
                    it.setColor(model.color.color())
                }

            }

            binding.selectIcon.apply {
                if (isSelect) {
                    visible()
                } else {
                    gone()
                }

            }

            binding.root.setOnClickListener {
                eventListener.clickColor(item)
            }
        }
    }
}