package com.faceunity.app_ptag.ui.edit.weight.control

import com.faceunity.app_ptag.ui.edit.entity.control.ControlModel
import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryBundleModel
import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryColorModel
import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryConfigModel
import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.*
import com.faceunity.app_ptag.ui.edit.entity.state.SelectedUiState
import com.faceunity.app_ptag.ui.edit.filter.FilterGroup
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.StyleRecordInterface
import com.faceunity.pta.pta_core.util.expand.equalsDelta

/**
 *
 */
class AvatarControlViewHolder {

    private var masterCategoryList: List<MasterWrapper> = mutableListOf()
    private var minorCategoryList: List<MinorWrapper> = mutableListOf()
    private var subCategoryBundleList: List<SubBundleWrapper> = mutableListOf()
    private var subCategoryColorList: List<SubColorWrapper> = mutableListOf()
    private var subCategoryConfigList: List<SubConfigWrapper> = mutableListOf()

    private var currentMaster: MasterWrapper? = null
    private var currentMinor: MinorWrapper? = null
    private var currentColor: SubColorWrapper? = null
    private var currentBundle: SubBundleWrapper? = null
    private var currentConfig: SubConfigWrapper? = null
    fun getCurrentMaster() = currentMaster
    fun getCurrentMinor() = currentMinor
    fun getCurrentColor() = currentColor
    fun getCurrentBundle() = currentBundle
    fun getCurrentMinorType() = getCurrentMinor()?.model?.subList?.firstOrNull()?.type

    private var selectedUiState: SelectedUiState? = null
    private var filterGroup: FilterGroup? = null

    internal var controlListener: AvatarControlListener? = null
    fun bindControlListener(controlListener: AvatarControlListener) {
        this.controlListener = controlListener
    }

    private var categoryUiState: AvatarControlCategoryUiState? = null
    internal fun bindCategoryUiState(categoryUiState: AvatarControlCategoryUiState) {
        this.categoryUiState = categoryUiState
    }

    internal val eventListener: AvatarControlHolderEvent = object : AvatarControlHolderEvent {
        override fun switchMaster(masterWrapper: MasterWrapper) {
            currentMaster = masterWrapper
            masterCategoryList = masterCategoryList.map { it.copy(
                isSelect = masterWrapper == it
            ) }
            buildMinorList()

            categoryUiState?.updateThemeColor(masterWrapper.model.tintColor())
            categoryUiState?.notifyMasterList(masterCategoryList)
            //按下一级菜单后，需要联动点击二级菜单
            minorCategoryList.firstOrNull()?.let {
                switchMinor(it)
            }
        }

        override fun switchMinor(minorWrapper: MinorWrapper) {
            currentMinor = minorWrapper
            minorCategoryList = minorCategoryList.map { it.copy(
                isSelect = minorWrapper == it
            ) }
//            buildBundleList()
//            buildColorList()
            buildBundleOrColorList()

            controlListener?.onMinorSelect(minorWrapper.model)
            categoryUiState?.notifyMinorList(minorCategoryList)
        }

        override fun clickColor(subColorWrapper: SubColorWrapper) {
            currentColor = subColorWrapper
            subCategoryColorList = subCategoryColorList.map { it.copy(
                isSelect = subColorWrapper == it
            ) }

            controlListener?.onColorItemClick(subColorWrapper.model)
            categoryUiState?.notifyColorList(subCategoryColorList)
        }

        override fun clickBundle(subBundleWrapper: SubBundleWrapper) {
            currentBundle = subBundleWrapper
            subCategoryBundleList = subCategoryBundleList.map { it.copy(
                isSelect = subBundleWrapper == it
            ) }

            controlListener?.onNormalItemClick(subBundleWrapper.model)
            categoryUiState?.notifyBundleList(subCategoryBundleList)
        }

        override fun clickConfig(subConfigWrapper: SubConfigWrapper) {
            currentConfig = subConfigWrapper
            subCategoryConfigList = subCategoryConfigList.map { it.copy(
                isSelect = subConfigWrapper == it
            ) }
            controlListener?.onConfigItemClick(subConfigWrapper.model)
            categoryUiState?.notifyConfigList(subCategoryConfigList)
        }

    }

    fun bindData(controlModel: ControlModel) {
        masterCategoryList = controlModel.masterList.mapIndexed { index, item ->
            MasterWrapper(item, index == 0)
        }
        currentMaster = masterCategoryList.firstOrNull()
        eventListener.switchMaster(currentMaster!!)
    }

    fun notifySelectUiState(selectedUiState: SelectedUiState) {
        this.selectedUiState = selectedUiState
        buildBundleOrColorList()
    }

    fun notifyFilterGroup(filterGroup: FilterGroup) {
        this.filterGroup = filterGroup
        //在设置筛选组后，要对可能涉及到的数据刷新一次。
        buildMinorList()
        buildBundleOrColorList()
    }

    private fun isSelectColor(model: SubCategoryColorModel): Boolean {
        if (selectedUiState == null) return false
        val currentColorKey = model.key
        val selectColor = selectedUiState!!.color[currentColorKey]
        return model.color == selectColor
    }

    private fun isSelectBundle(model: SubCategoryBundleModel): Boolean {
        if (selectedUiState == null) return false
        val selectBundle = selectedUiState!!.bundle
        return model.fileId in selectBundle
    }

    private fun isSelectConfig(model: SubCategoryConfigModel): Boolean {
        if (selectedUiState == null) return false
        val config = model.facePupConfig ?: return false
        val facePupMap = selectedUiState!!.facePupMap
        config.bone_controllers.forEach { (key, value) ->
            val nowValue = facePupMap[key] ?: 0f
            if (!nowValue.equalsDelta(value)) {
                return false
            }
        }
        return true
    }

    private fun buildMinorList() {
        val originList = currentMaster?.model?.minorList ?: return
        val filterList = filterGroup?.filter(originList)
            ?.filter {
                filterGroup?.filter(it.subList)?.isNotEmpty() == true
                        || it.subColorList != null
            } ?: originList
        minorCategoryList = filterList.mapIndexed { index, item ->
            val isSelect = if (currentMinor != null) {
                currentMinor?.model == item
            } else {
                index == 0
            }
            MinorWrapper(item, isSelect)
        }
        categoryUiState?.notifyMinorList(minorCategoryList)
    }

    private fun buildBundleOrColorList() {
        if (currentMinor?.model?.subList?.isNotEmpty() == true) {
            if (currentMinor?.model?.subList?.firstOrNull() is SubCategoryConfigModel) {
                buildConfigList()
                return
            }
            buildBundleList()
        } else {
            buildColorList()
        }
    }

    private fun buildBundleList() {
        val originList = currentMinor?.model?.subList?.map { it as? SubCategoryBundleModel }?.filterNotNull() ?: return
        val filterList = filterGroup?.filter(originList) ?: originList
        subCategoryBundleList = filterList.map {
            val isSelect = isSelectBundle(it)
            SubBundleWrapper(it, isSelect, StyleRecordInterface.DownloadStyle.Normal).also {
                currentBundle = it
            }
        }
//        subCategoryBundleList.map { "BundleWrapper[fileId:${it.model.fileId};select:${it.isSelect};style:${it.downloadStyle.name}]" }.joinToString("\n").let {
//            FuLog.debug("buildBundleList():$it")
//        }
        categoryUiState?.notifyBundleList(subCategoryBundleList)
    }

    private fun buildColorList() {
        val originList = currentMinor?.model?.subColorList
        if (originList == null) {
            categoryUiState?.notifyColorList(emptyList())
            return
        }
        subCategoryColorList = originList.map {
            val isSelect = isSelectColor(it)
            SubColorWrapper(it, isSelect).also {
                if (isSelect) currentColor = it
            }
        }
        categoryUiState?.notifyColorList(subCategoryColorList)
    }

    private fun buildConfigList() {
        val originList = currentMinor?.model?.subList?.map { it as? SubCategoryConfigModel }?.filterNotNull() ?: return
        val filterList = filterGroup?.filter(originList) ?: originList
        subCategoryConfigList = filterList.map {
            val isSelect = isSelectConfig(it)
            SubConfigWrapper(it, isSelect, StyleRecordInterface.DownloadStyle.Normal).also {
                if (isSelect) currentConfig = it
            }
        }
        categoryUiState?.notifyConfigList(subCategoryConfigList)
    }
}