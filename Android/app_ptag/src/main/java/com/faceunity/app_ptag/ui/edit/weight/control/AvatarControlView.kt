package com.faceunity.app_ptag.ui.edit.weight.control

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ViewControlAvatarBinding
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.*
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.*
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.visible

/**
 * Created on 2021/7/22 0022 16:17.


 */
class AvatarControlView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val holder = AvatarControlViewHolder()

    private val binding: ViewControlAvatarBinding

    private val masterAdapter = MasterCategoryAdapter(holder.eventListener)
    private val minorAdapter = MinorCategoryAdapter(holder.eventListener)
    private val subAdapter = SubCategoryBundleAdapter(holder.eventListener)
    private val subColorAdapter = SubCategoryColorAdapter(holder.eventListener)

    init {
        binding = ViewControlAvatarBinding.inflate(LayoutInflater.from(context), this, true)
        initView()
        holder.bindCategoryUiState(object : AvatarControlCategoryUiState {
            override fun notifyMasterList(list: List<MasterWrapper>) {
                masterAdapter.submitList(list)
            }

            override fun notifyMinorList(list: List<MinorWrapper>) {
                minorAdapter.submitList(list)
            }

            override fun notifyBundleList(list: List<SubBundleWrapper>) {
                subAdapter.submitList(list)
            }

            override fun notifyColorList(list: List<SubColorWrapper>) {
                subColorAdapter.submitList(list)
            }

            override fun notifyConfigList(list: List<SubConfigWrapper>) {

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
            layoutManager = GridLayoutManager(context, 5)
            adapter = subAdapter
            (itemAnimator as? SimpleItemAnimator)?.apply {
                addDuration = 0
                changeDuration = 0
                moveDuration = 0
                removeDuration = 0
                supportsChangeAnimations = false

            }
        }
        binding.subColorCategoryView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = subColorAdapter
        }
        binding.historyBack.apply {
            setOnClickListener {
                holder.controlListener?.onHistoryBackClick()
            }
        }
        binding.historyForward.setOnClickListener {
            holder.controlListener?.onHistoryForwardClick()
        }
        binding.historyReset.setOnClickListener {
            holder.controlListener?.onHistoryResetClick()
        }
        binding.facepupBtn.setOnClickListener {
            val currentMinorCategory = holder.getCurrentMinor() ?: return@setOnClickListener
            val fileId = holder.getCurrentBundle()?.model?.fileId
            holder.controlListener?.onFacepupClick(currentMinorCategory.model.key, fileId)
        }
    }


//    /**
//     * 根据传入的筛选规则，显示符合筛选条件的数据
//     */
//    fun filterGenderList(inputFilterGroup: FilterGroup) {
//        filterGroup = inputFilterGroup
//        val masterCategoryModel = masterCategoryList?.getOrNull(masterAdapter.getNowSelectPosition())
//        minorCategoryList = filterGroup.filter(masterCategoryModel?.minorList).filter { filterGroup.filter(it.subList).isNotEmpty() }.toMutableList()
//        val minorCategoryModel = minorCategoryList?.getOrNull(minorAdapter.getNowSelectPosition())
//        subCategoryList = filterGroup.filter(minorCategoryModel?.subList)
//
////        minorCategoryModel?.getSelectedItem()?.let {
////            val selectSubIndex = subCategoryList?.indexOf(it)
////            subAdapter.recordClickSafe(selectSubIndex)
////            subAdapter.notifyClickItemChanged(subAdapter)
////        }
//        minorAdapter.submitList(minorCategoryList)
//        subAdapter.submitList(subCategoryList)
//    }
//
//    override fun onMasterClick(item: MasterCategoryModel, position: Int) {
//        updateMasterEvent(item, position)
//    }
//
//    override fun onMinorClick(item: MinorCategoryModel, position: Int) {
//        updateMinorEvent(item, position)
//
//        minorAdapter.recordClickSafe(position)
//        minorAdapter.notifyClickItemChanged(minorAdapter)
//
//        controlListener?.onMinorSelect(item)
//
//    }
//
//    override fun onSubClick(item: SubCategoryModel, position: Int) {
//        updateSubEvent(item, position)
//
//        when(item) {
//            is SubCategoryNormalModel -> {
//                controlListener?.onNormalItemClick(item)
//            }
//            is SubCategoryColorModel -> {
//                controlListener?.onColorItemClick(item)
//            }
//        }
//
//    }
//
//    private fun updateMasterEvent(item: MasterCategoryModel, position: Int) {
//        masterAdapter.apply {
//            submitList(masterCategoryList?.toList())
//            recordClickSafe(position)
//            notifyClickItemChanged(masterAdapter)
//        }
//
//        minorAdapter.tintColor = item.tintColor()
//        subAdapter.tintColor = item.tintColor()
//        minorAdapter.resetClickRecord()
//        minorCategoryList = filterGroup.filter(item.minorList).filter { filterGroup.filter(it.subList).isNotEmpty() }.toMutableList()
//        val index = minorAdapter.getDefaultPosition(true)
//        minorCategoryList?.getOrNull(index)?.let {
//            updateMinorEvent(it, index)
//            controlListener?.onMinorSelect(it)
//        }
//    }
//
//    private fun updateMinorEvent(item: MinorCategoryModel, position: Int) {
//        minorAdapter.apply {
//            submitList(minorCategoryList?.toList())
////            recordClickSafe(minorCategoryList?.indexOf(item))
////            notifyClickItemChanged(minorAdapter)
//        }
//
//        subCategoryList = filterGroup.filter(item.subList)
//        subAdapter.resetClickRecord()
////        val selectSubIndex = item.getSelectedItem()?.let {
////            subCategoryList?.indexOf(it)
////        }
//        val selectSubIndex = position
//        subAdapter.submitList(subCategoryList, object : Runnable {
//            override fun run() {
//                binding.subCategoryView.apply {
//                    scrollToPosition(0)
//                    (layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
//                }
//            }
//        })
//        if (selectSubIndex != null) {
//            subAdapter.recordClick(selectSubIndex)
////            subAdapter.notifyClickItemChanged(subAdapter)
//        }
//        subColorCategoryList = item.subColorList
//        if (subColorCategoryList != null) {
//            subColorAdapter.submitList(subColorCategoryList as List<SubCategoryModel>?)
//        } else {
//            subColorAdapter.submitList(emptyList())
//        }
//
//    }
//
//    private fun updateSubEvent(item: SubCategoryModel, position: Int) {
//        when(item) {
//            is SubCategoryNormalModel -> {
//                subAdapter.apply {
//                    submitList(subCategoryList?.toList())
//                    recordClickSafe(position)
//                    notifyClickItemChanged(subAdapter)
//                }
//            }
//            is SubCategoryColorModel -> {
//                subColorAdapter.apply {
//                    submitList(subColorCategoryList?.toList())
//                    recordClickSafe(position)
//                    notifyClickItemChanged(subColorAdapter)
//                }
//            }
//        }
//
//    }

//    fun notifySelectItem(selectItemList: List<String>) {
//        if (subCategoryList?.all { it is SubCategoryColorModel } == true) {
//            return
//        }
//        val needNotifyIndexList = mutableListOf<Int>().apply {
//            add(subAdapter.getNowSelectPosition())
//            add(subAdapter.getLastSelectPosition())
//        }
//        val selectIndexList = mutableListOf<Int>()
//        subCategoryList?.forEachIndexed { index, subCategoryModel ->
//            if (subCategoryModel is SubCategoryNormalModel && subCategoryModel.fileId in selectItemList) {
//                selectIndexList.add(index)
//            }
//        }
//        selectIndexList.forEach {
//            subAdapter.recordClick(it)
//            subAdapter.notifyItemChanged(it)
//        }
//        if (selectIndexList.isEmpty()) {
//            subAdapter.recordClick(-1)
//        }
//        needNotifyIndexList.forEach {
//            subAdapter.notifyItemChanged(it)
//        }
//
//    }
//
//    fun notifySelectColorItem(selectColorMap: Map<String, FUColorRGBData>) {
//        val needNotifyIndexList = mutableListOf<Int>().apply {
//            add(subColorAdapter.getNowSelectPosition())
//            add(subColorAdapter.getLastSelectPosition())
//        }
//        val selectIndexList = mutableListOf<Int>()
//        val colorMap = selectColorMap.map {
//            it.key to it.value.let {
//                rgb(
//                    it.red.toInt(),
//                    it.green.toInt(),
//                    it.blue.toInt()
//                )
//            }
//        }.toMap()
//        subColorCategoryList?.forEachIndexed { index, subCategoryModel ->
//            if (subCategoryModel is SubCategoryColorModel && colorMap.any { it.value == subCategoryModel.color.color() && it.key == subCategoryModel.key }) {
//                selectIndexList.add(index)
//            }
//        }
//        selectIndexList.forEach {
//            subColorAdapter.recordClick(it)
//            subColorAdapter.notifyItemChanged(it)
//        }
//        if (selectIndexList.isEmpty()) {
//            subColorAdapter.recordClick(-1)
//        }
//        needNotifyIndexList.forEach {
//            subColorAdapter.notifyItemChanged(it)
//        }
//    }
//
//    private fun rgb(
//        red: Int,
//        green: Int,
//        blue: Int
//    ): Int {
//        return -0x1000000 or (red shl 16) or (green shl 8) or blue
//    }

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