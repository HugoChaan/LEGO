package com.faceunity.app_ptag.ui.edit.entity.control.wrapper

import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryBundleModel
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.StyleRecordInterface

/**
 *
 */
data class SubBundleWrapper(
    val model: SubCategoryBundleModel,
    val isSelect: Boolean,
    val downloadStyle: StyleRecordInterface.DownloadStyle
)
