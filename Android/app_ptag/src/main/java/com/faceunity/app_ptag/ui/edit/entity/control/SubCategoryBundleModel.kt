package com.faceunity.app_ptag.ui.edit.entity.control

import com.faceunity.fupta.avatar_data.entity.parser.FuFilter


data class SubCategoryBundleModel(
    val fileId: String,
    val iconPath: FuIcon,
    val filter: FuFilter,
) : SubCategoryModel(Type.Bundle) {
    override fun id() = fileId


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubCategoryBundleModel) return false
        if (!super.equals(other)) return false

        if (fileId != other.fileId) return false
        if (iconPath != other.iconPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fileId.hashCode()
        result = 31 * result + iconPath.hashCode()
        return result
    }


}
