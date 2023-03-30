package com.faceunity.app_ptag.ui.edit.weight.control.adapter

import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ItemMasterCategoryBinding
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.MasterWrapper
import com.faceunity.app_ptag.ui.edit.weight.control.AvatarControlHolderEvent
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.diff.MasterCategoryDiffCallback

/**
 * Created on 2021/7/22 0022 17:13.


 */
internal class MasterCategoryAdapter(
    private val eventListener: AvatarControlHolderEvent
) : ListAdapter<MasterWrapper, MasterCategoryAdapter.MasterViewHolder>(
    MasterCategoryDiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MasterViewHolder {
        return MasterViewHolder(
            ItemMasterCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            eventListener
        )
    }

    override fun onBindViewHolder(holder: MasterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MasterViewHolder(
        private val binding: ItemMasterCategoryBinding,
        private val eventListener: AvatarControlHolderEvent
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MasterWrapper) {
            val (model, isSelect) = item
            val context = binding.root.context
            binding.icon.apply {
                if (isSelect) {
                    (background as? GradientDrawable)?.let {
                        ObjectAnimator.ofArgb(it, "color", model.tintColor())
                            .setDuration(500)
                            .start()
                    }
                    Glide.with(this)
                        .load(model.selectIconPath.path)
                        .into(this)
                } else {
                    (background as? GradientDrawable)?.let {
                        ObjectAnimator.ofArgb(it, "color", ContextCompat.getColor(context, R.color.item_bg))
                            .setDuration(500)
                            .start()
                    }
                    Glide.with(this)
                        .load(model.iconPath.path)
                        .into(this)
                }

            }

            binding.root.setOnClickListener {
                eventListener.switchMaster(item)
            }
        }
    }
}