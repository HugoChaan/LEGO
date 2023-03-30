package com.faceunity.app_ptag.ui.edit.weight.control.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ItemSubCategoryBinding
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.SubConfigWrapper
import com.faceunity.app_ptag.ui.edit.weight.control.AvatarControlHolderEvent
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.diff.SubCategoryConfigDiffCallback
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.visible

/**
 * Created on 2021/7/22 0022 17:13.
 */
internal class SubCategoryConfigAdapter(
    private val eventListener: AvatarControlHolderEvent
) : ListAdapter<SubConfigWrapper, SubCategoryConfigAdapter.SubViewHolder>(
    SubCategoryConfigDiffCallback
),
    StyleRecordInterface by StyleRecordHelper() {

    var tintColor: Int? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubCategoryConfigAdapter.SubViewHolder {
        return SubViewHolder(
            ItemSubCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            eventListener
        )
    }

    override fun onBindViewHolder(holder: SubViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubViewHolder(
        private val binding: ItemSubCategoryBinding,
        private val eventListener: AvatarControlHolderEvent
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SubConfigWrapper) {
            val (model, isSelect, _) = item

            Glide.with(binding.icon)
                .load(model.icon.path)
                .error(R.drawable.icon_tab2_no_material)
                .into(binding.icon)

            if (isSelect) {
                binding.selectBg.visible()
            } else {
                binding.selectBg.gone()
            }

            binding.root.setOnClickListener {
                eventListener.clickConfig(item)
            }
        }
    }

}