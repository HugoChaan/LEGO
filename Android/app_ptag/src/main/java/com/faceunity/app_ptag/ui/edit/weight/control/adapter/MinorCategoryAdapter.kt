package com.faceunity.app_ptag.ui.edit.weight.control.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ItemMinorCategoryBinding
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.MinorWrapper
import com.faceunity.app_ptag.ui.edit.weight.control.AvatarControlHolderEvent
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.diff.MinorCategoryDiffCallback

/**
 * Created on 2021/7/22 0022 17:13.


 */
internal class MinorCategoryAdapter(
    private val eventListener: AvatarControlHolderEvent
) : ListAdapter<MinorWrapper, MinorCategoryAdapter.MinorViewHolder>(MinorCategoryDiffCallback) {

    var tintColor: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MinorViewHolder {
        return MinorViewHolder(
            ItemMinorCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            eventListener
        )
    }

    override fun onBindViewHolder(holder: MinorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MinorViewHolder(
        private val binding: ItemMinorCategoryBinding,
        private val eventListener: AvatarControlHolderEvent
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MinorWrapper) {
            val (model, isSelect) = item
            val context = binding.root.context
            binding.icon.apply {
                if (isSelect) {
                    val color = tintColor ?: Color.DKGRAY
                    (background as? GradientDrawable)?.let {
                        it.setColor(color)
                    }
                    Glide.with(this)
                        .load(model.selectIconPath.path)
                        .into(this)
                } else {
                    val color = ContextCompat.getColor(context, R.color.item_bg)
                    (background as? GradientDrawable)?.let {
                        it.setColor(color)
                    }
                    Glide.with(this)
                        .load(model.iconPath.path)
                        .into(this)
                }

            }


            binding.root.setOnClickListener {
                eventListener.switchMinor(item)
            }
        }
    }
}