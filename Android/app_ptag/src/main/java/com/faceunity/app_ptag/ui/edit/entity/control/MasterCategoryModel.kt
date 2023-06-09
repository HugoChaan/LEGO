package com.faceunity.app_ptag.ui.edit.entity.control

import android.graphics.Color

/**
 * Created on 2021/7/23 0023 10:59.


 */
data class MasterCategoryModel(
    val key: String,
    val name: String,
    val iconPath: FuIcon,
    val selectIconPath: FuIcon,
    val minorList: MutableList<MinorCategoryModel>
) {

    fun tintColor(): Int {
        return when(key) {
            "head" -> Color.parseColor("#7b58ff")
            "body" -> Color.parseColor("#5997ff")
            "upper" -> Color.parseColor("#3ebae4")
            "pants" -> Color.parseColor("#f68149")
            "shoe" -> Color.parseColor("#f8af49")
            "ornament" -> Color.parseColor("#69dbba")
            else -> Color.WHITE
        }
    }
}
