package com.faceunity.app_ptag.ui.edit.weight.control.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ItemSubCategoryBinding
import com.faceunity.app_ptag.ui.edit.entity.control.FuIcon
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.SubBundleWrapper
import com.faceunity.app_ptag.ui.edit.weight.control.AvatarControlHolderEvent
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.diff.SubCategoryBundleDiffCallback
import com.faceunity.app_ptag.ui.home.dev.IDevBuilderInstance
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.visible

/**
 * Created on 2021/7/22 0022 17:13.
 */
internal class SubCategoryBundleAdapter(
    private val eventListener: AvatarControlHolderEvent
) : ListAdapter<SubBundleWrapper, SubCategoryBundleAdapter.SubViewHolder>(SubCategoryBundleDiffCallback),
    StyleRecordInterface by StyleRecordHelper() {

    var tintColor: Int? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubCategoryBundleAdapter.SubViewHolder {
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
        fun bind(item: SubBundleWrapper) {
            val (model, isSelect, downloadStyle) = item
            val context = binding.root.context
            binding.name.text = if (model.iconPath.type != FuIcon.Type.Null) {
                ""
            } else {
                model.id().split("/").last().replace(".bundle", "")
            }
            binding.name.setTextColor(tintColor ?: Color.BLACK)
            if (isSelect) {
                binding.selectBg.visible()
            } else {
                binding.selectBg.gone()
            }
            when(downloadStyleSafe(model.fileId)) {
                StyleRecordInterface.DownloadStyle.Normal -> {
                    binding.downloadImageView.gone()
                }
                StyleRecordInterface.DownloadStyle.NeedDownload -> {
                    binding.downloadImageView.apply {
                        visible()
                        setImageResource(R.drawable.icon_tab2_download)
                        animation = null
                    }

                }
                StyleRecordInterface.DownloadStyle.Downloading -> {
                    binding.downloadImageView.apply {
                        visible()
                        setImageResource(R.drawable.icon_tab2_loading)
                        val anim = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f).apply {
                            repeatCount = -1
                            duration = 1000
                            interpolator = LinearInterpolator()
                        }
                        animation = anim
                    }
                }
                StyleRecordInterface.DownloadStyle.Error -> {
                    binding.downloadImageView.apply {
                        visible()
                        setImageResource(R.drawable.icon_tab2_download)
                    }
                }
            }

            val icon = model.iconPath
            when(icon.type) {
                FuIcon.Type.Url -> {
                    Glide.with(binding.icon)
                        .load(icon.path)
                        .error(R.drawable.icon_tab2_no_material)
                        .into(binding.icon)
                }
                FuIcon.Type.File -> {
                    Glide.with(binding.icon)
                        .load(icon.path)
                        .error(R.drawable.icon_tab2_no_material)
                        .into(binding.icon)
                }
                FuIcon.Type.Assets -> {
                    Glide.with(binding.icon)
                        .load("file:///android_asset/" + icon.path)
                        .error(R.drawable.icon_tab2_no_material)
                        .into(binding.icon)
                }
                FuIcon.Type.Null -> {
                    Glide.with(binding.icon)
                        .load(R.drawable.icon_tab2_no_material)
                        .into(binding.icon)
                }
            }

            binding.root.setOnClickListener {
                eventListener.clickBundle(item)
            }
            binding.root.setOnLongClickListener {
                return@setOnLongClickListener IDevBuilderInstance.showItemInfoDialog(model, context) ?: false
            }
        }
    }

}