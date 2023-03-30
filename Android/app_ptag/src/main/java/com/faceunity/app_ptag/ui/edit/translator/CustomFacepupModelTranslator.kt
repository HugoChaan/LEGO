package com.faceunity.app_ptag.ui.edit.translator

import com.faceunity.app_ptag.ui.edit.entity.facepup.CustomFacepupIcon
import com.faceunity.app_ptag.ui.edit.entity.facepup.CustomFacepupModel
import com.faceunity.fupta.facepup.FacePupManager
import com.faceunity.fupta.facepup.entity.origin.FacepupMeshTranslation
import com.faceunity.fupta.facepup.entity.tier.FacepupGeneralTier

/**
 *
 */
class CustomFacepupModelTranslator {

    /**
     * 根据给的数据源，构造出一个符合实际业务需求的数据结构。
     * @param isTranCustom 是否需要读取语言文件里的 custom 字段。目前的业务下专家模式为 false，可视实际情况修改。
     */
    fun buildCustom(
        generalTierGroup: FacepupGeneralTier.Group,
        translation: FacepupMeshTranslation,
        facepupMap: Map<String, Float>,
        customIcon: CustomFacepupIcon,
        isTranCustom: Boolean = true
    ): CustomFacepupModel {
        val partList = mutableListOf<CustomFacepupModel.CustomPart>()
        generalTierGroup.partList.forEach { part ->
            val partKey = part.partKey
            val sliderList = mutableListOf<CustomFacepupModel.CustomSlider>()

            part.sliderList.forEach { slider ->
                val sliderValue = FacePupManager.facePupParamValueToScale(
                    slider.controlItem.first().let { facepupMap[it] ?: 0f },
                    slider.controlItem.second().let { facepupMap[it] ?: 0f }
                )
                val sliderName = if (isTranCustom) {
                    translation.tranSlider(slider.type, partKey)
                } else {
                    translation.tranDefaultSlider(slider.type)
                }
                sliderList.add(CustomFacepupModel.CustomSlider(slider, sliderName, sliderValue))
            }
            partList.add(CustomFacepupModel.CustomPart(part, translation.tran(partKey), customIcon.iconMap[partKey] ?: emptyList(), sliderList))
        }

        return CustomFacepupModel(generalTierGroup, partList)
    }
}