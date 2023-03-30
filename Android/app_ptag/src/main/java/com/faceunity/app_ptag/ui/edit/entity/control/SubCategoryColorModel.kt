package com.faceunity.app_ptag.ui.edit.entity.control


/**
 * Created on 2021/7/23 0023 14:12.


 */
data class SubCategoryColorModel(
    val key: String,
    val color: FuColor
) : SubCategoryModel(Type.Color) {
    override fun id() = key

}
