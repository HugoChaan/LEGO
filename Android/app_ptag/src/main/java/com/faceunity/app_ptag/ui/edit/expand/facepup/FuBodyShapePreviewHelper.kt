package com.faceunity.app_ptag.ui.edit.expand.facepup

import com.faceunity.core.avatar.model.Avatar

/**
 * 为了解决捏形后部分页面相机与头部不对齐的辅助类。
 *
 */
object FuBodyShapePreviewHelper {

    /**
     * 半身相机时，为了屏蔽腿部捏形造成的高度变化需要设置为 0 的字段。
     */
    val halfBodyKey = listOf<String>(
        "highHeeledShoesRoot_type0",
        "highHeeledShoesRoot_type1",
        "highHeeledShoesRoot_type2",
        "highShoes_type0",
        "highShoes_type1",
        "highShoes_type2",
        "height_short",
        "height_high",
        "legs_length_short",
        "legs_length_long",
        "upperleg_length_short",
        "upperleg_length_long",
        "lowerleg_length_short",
        "lowerleg_length_long"
    )

    /**
     * 头部相机时，为了屏蔽腿部捏形造成的高度变化需要设置为 0 的字段。
     */
    val headKey = listOf<String>(
        "highHeeledShoesRoot_type0",
        "highHeeledShoesRoot_type1",
        "highHeeledShoesRoot_type2",
        "highShoes_type0",
        "highShoes_type1",
        "highShoes_type2",
        "height_short",
        "height_high",
        "waist_length_short",
        "waist_length_long",
        "legs_length_short",
        "legs_length_long",
        "upperleg_length_short",
        "upperleg_length_long",
        "lowerleg_length_short",
        "lowerleg_length_long"
    )

    enum class Type {
        HalfBody, Head
    }

    private fun getControlList(type: Type) = when(type) {
        Type.HalfBody -> halfBodyKey
        Type.Head -> headKey
    }

    fun getCurrentInfo(type: Type, avatar: Avatar): Map<String, Float> {
        val list = getControlList(type)
        return list.associate { it to (avatar.deformation.getDeformationCache()[it] ?: 0f) }
    }

    fun setInfoByDefault(type: Type, avatar: Avatar) {
        val list = getControlList(type)
        list.forEach { key ->
            avatar.deformation.setDeformation(key, 0f)
        }
    }

    fun setInfoByInput(avatar: Avatar, params: Map<String, Float>) {
        params.forEach{ (key, value) ->
            avatar.deformation.setDeformation(key, value)
        }
    }
}