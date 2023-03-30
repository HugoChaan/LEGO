package com.faceunity.app_ptag.ui.edit.entity.control

import com.faceunity.fupta.avatar_data.entity.parser.FuFilter

/**
 * Created on 2021/7/23 0023 10:59.


 */
data class MinorCategoryModel(
    val key: String,
    val name: String,
    val iconPath: FuIcon,
    val selectIconPath: FuIcon,
    val filter: FuFilter,
    val subList: MutableList<SubCategoryModel>,
    var subColorList: MutableList<SubCategoryColorModel>?
) {

}