package com.faceunity.app_ptag.ui.edit.entity

import com.faceunity.app_ptag.ui.edit.entity.control.MinorCategoryModel
import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryBundleModel
import com.faceunity.app_ptag.ui.edit.filter.GenderFilter

/**
 *
 */
class GenderProFilter(vararg keys: GenderFilterKey?) : GenderFilter(*keys) {

    override fun filter(any: Any?): GenderFilterKey? {
        var filter = super.filter(any)
        if (filter == null) {
            if (any is SubCategoryBundleModel) {
                val genderValue = any.filter.filter(tag())
                filter = when(genderValue) {
                    "male" -> GenderFilterKey.JustMale
                    "female" -> GenderFilterKey.JustFemale
                    else -> GenderFilterKey.All
                }
            } else if (any is MinorCategoryModel) {
                val genderValue = any.filter.filter(tag())
                filter = when(genderValue) {
                    "male" -> GenderFilterKey.JustMale
                    "female" -> GenderFilterKey.JustFemale
                    else -> GenderFilterKey.All
                }
            }
        }
        return filter
    }

    override fun tag() = "gender"
}