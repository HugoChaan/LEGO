package com.faceunity.app_ptag.ui.edit.entity.control.wrapper

import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryConfigModel
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.StyleRecordInterface

/**
 *
 */
data class SubConfigWrapper(
    val model: SubCategoryConfigModel,
    val isSelect: Boolean,
    val downloadStyle: StyleRecordInterface.DownloadStyle
)
