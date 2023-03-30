package com.faceunity.app_ptag.ui.edit.entity.control

import com.faceunity.fupta.facepup.entity.config.FacePupConfig

/**
 *
 */
class SubCategoryConfigModel(
    val filePath: String,
    val name: String,
    val icon: FuIcon,
    val facePupConfig: FacePupConfig?
) : SubCategoryModel(Type.Config) {

    override fun id() = filePath

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubCategoryConfigModel) return false
        if (!super.equals(other)) return false

        if (filePath != other.filePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + filePath.hashCode()
        return result
    }

    override fun toString(): String {
        return "SubCategoryConfigModel(filePath='$filePath', name='$name', icon=$icon, facePupConfig=$facePupConfig)"
    }


}