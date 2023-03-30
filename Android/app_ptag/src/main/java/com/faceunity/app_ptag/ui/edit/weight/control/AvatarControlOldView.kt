package com.faceunity.app_ptag.ui.edit.weight.control

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ViewControlAvatarOldBinding
import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryModel
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.*
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.*
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.visible

/**
 * Created on 2021/7/22 0022 16:17.
 */
class AvatarControlOldView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val holder = AvatarControlViewHolder()

    private val binding: ViewControlAvatarOldBinding

    private val masterAdapter = MasterCategoryAdapter(holder.eventListener)
    private val minorAdapter = MinorCategoryAdapter(holder.eventListener)
    private val subAdapter = SubCategoryBundleAdapter(holder.eventListener)
    private val subColorAdapter = SubCategoryColorAdapter(holder.eventListener)
    private val subConfigAdapter = SubCategoryConfigAdapter(holder.eventListener)

    init {
        binding = ViewControlAvatarOldBinding.inflate(LayoutInflater.from(context), this, true)
        initView()
        holder.bindCategoryUiState(object : AvatarControlCategoryUiState {
            override fun notifyMasterList(list: List<MasterWrapper>) {
                masterAdapter.submitList(list)
            }

            override fun notifyMinorList(list: List<MinorWrapper>) {
                minorAdapter.submitList(list)
            }

            override fun notifyBundleList(list: List<SubBundleWrapper>) {
                if (binding.subCategoryView.adapter != subAdapter) {
                    binding.subCategoryView.adapter = subAdapter
                }
                subAdapter.submitList(list)
            }

            override fun notifyColorList(list: List<SubColorWrapper>) {
                if (binding.subCategoryView.adapter != subColorAdapter) {
                    binding.subCategoryView.adapter = subColorAdapter
                }
                subColorAdapter.submitList(list)
            }

            override fun notifyConfigList(list: List<SubConfigWrapper>) {
                if (binding.subCategoryView.adapter != subConfigAdapter) {
                    binding.subCategoryView.adapter = subConfigAdapter
                }
                subConfigAdapter.submitList(list)
            }

            override fun updateThemeColor(color: Int) {
                minorAdapter.tintColor = color
            }
        })
    }

    private fun initView() {
        binding.masterCategoryView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = masterAdapter
        }
        binding.minorCategoryView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = minorAdapter
            (itemAnimator as? SimpleItemAnimator)?.apply {
                addDuration = 0
                changeDuration = 0
                moveDuration = 0
                removeDuration = 0
                supportsChangeAnimations = false

            }
        }
        binding.subCategoryView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = subAdapter
            (itemAnimator as? SimpleItemAnimator)?.apply {
                addDuration = 0
                changeDuration = 0
                moveDuration = 0
                removeDuration = 0
                supportsChangeAnimations = false

            }
        }
        binding.historyBack.setOnClickListener {
            holder.controlListener?.onHistoryBackClick()
        }
        binding.historyForward.setOnClickListener {
            holder.controlListener?.onHistoryForwardClick()
        }
        binding.historyReset.setOnClickListener {
            holder.controlListener?.onHistoryResetClick()
        }
        binding.facepupBtn.setOnClickListener {
            if (holder.getCurrentMinorType() == SubCategoryModel.Type.Config) {
                holder.controlListener?.onBodyShapeClick()
                return@setOnClickListener
            }
            val currentMinorCategory = holder.getCurrentMinor() ?: return@setOnClickListener
            val fileId = holder.getCurrentBundle()?.model?.fileId
            holder.controlListener?.onFacepupClick(currentMinorCategory.model.key, fileId)
        }
    }


    fun controlStyleRecord(action: StyleRecordInterface.() -> Unit) {
        action(subAdapter)
        subAdapter.notifyDataSetChanged()
    }


    /**
     * 显示编辑菜单
     */
    fun visibleControl() {
        binding.masterCategoryView.visible()
        binding.minorCategoryView.visible()
        binding.subCategoryView.visible()
        binding.historyBack.visible()
        binding.historyForward.visible()
        binding.historyReset.visible()
    }

    /**
     * 隐藏编辑菜单
     */
    fun goneControl() {
        binding.masterCategoryView.gone()
        binding.minorCategoryView.gone()
        binding.subCategoryView.gone()
        binding.facepupBtn.gone()
        binding.historyBack.gone()
        binding.historyForward.gone()
        binding.historyReset.gone()
    }

    fun visibleFacepupBtn() {
        binding.facepupBtn.visible()
    }

    fun goneFacepupBtn() {
        binding.facepupBtn.gone()
    }

    fun setHistoryBackEnable(enable: Boolean) {
        binding.historyBack.isEnabled = enable
        binding.historyBack.setImageResource(if (enable) R.drawable.icon_withdraw_nor else R.drawable.icon_withdraw_disabled)
    }

    fun setHistoryForwardEnable(enable: Boolean) {
        binding.historyForward.isEnabled = enable
        binding.historyForward.setImageResource(if (enable) R.drawable.icon_forword_nor else R.drawable.icon_forword_disabled)
    }

    fun setHistoryResetEnable(enable: Boolean) {
        binding.historyReset.isEnabled = enable
        binding.historyReset.setImageResource(if (enable) R.drawable.icon_clear_nor else R.drawable.icon_clear_disabled)
    }
}