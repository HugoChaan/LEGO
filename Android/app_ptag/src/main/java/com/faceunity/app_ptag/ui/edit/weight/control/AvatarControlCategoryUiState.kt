package com.faceunity.app_ptag.ui.edit.weight.control

import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.*

/**
 *
 */
internal interface AvatarControlCategoryUiState {
    fun notifyMasterList(list: List<MasterWrapper>)

    fun notifyMinorList(list: List<MinorWrapper>)

    fun notifyBundleList(list: List<SubBundleWrapper>)

    fun notifyColorList(list: List<SubColorWrapper>)

    fun notifyConfigList(list: List<SubConfigWrapper>)

    fun updateThemeColor(color: Int)
}