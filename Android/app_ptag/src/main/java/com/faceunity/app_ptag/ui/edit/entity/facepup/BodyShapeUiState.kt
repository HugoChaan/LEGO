package com.faceunity.app_ptag.ui.edit.entity.facepup

import com.faceunity.fupta.facepup.entity.bodyshape.BodyShapeItem
import com.faceunity.fupta.facepup.entity.bodyshape.BodyShapeModel
import kotlin.math.absoluteValue

/**
 *
 */
data class BodyShapeUiState(
    val groupList: List<Group>,
    val eventType: EventType
) {

    companion object {

        fun empty() = BodyShapeUiState(emptyList(), EventType.Init)

        fun build(model: BodyShapeModel, tran: TranSliderValue? = null): BodyShapeUiState {
            val groupList = mutableListOf<Group>()
            model.forEach{ (groupName, itemList) ->
                val sliderList = mutableListOf<Slider>()
                itemList.forEach { item ->
                    val slider = Slider(item, item.name, tran?.invoke(item) ?: 0f)
                    sliderList.add(slider)
                }
                groupList.add(Group(groupName, sliderList))
            }
            return BodyShapeUiState(groupList, EventType.Init)
        }

    }

    data class Group(
        val name: String,
        val sliderList: List<Slider>
    ) {
        val hasChanged: Boolean = sliderList.any { it.hasChanged }
    }

    data class Slider(
        val bodyShapeItem: BodyShapeItem,
        val name: String,
        val value: Float
    ) {
        val hasChanged: Boolean = value.absoluteValue >= 0.005f
    }

    enum class EventType{
        Init, Slider, Reset
    }
}

class TranSlider {

    /**
     * 根据当前 Avatar 身上的骨骼参数，和该滑条的属性，计算出在滑条上应该显示的值
     * @return 滑条上的值，取值 -1 到 1
     */
    fun modelToUi(item: BodyShapeItem, facePupMap: Map<String, Float>): Float {
        var printValue = 0f
        item.keyMore.forEachIndexed { i, key ->
            val deformation = facePupMap[key] ?: 0f
            val scale = deformation / (item.weightMore?.getOrNull(i) ?: 1f)
            val printScale = scale / item.keyMore.size
            printValue += printScale
        }
        item.keyLess.forEachIndexed { i, key ->
            val deformation = facePupMap[key] ?: 0f
            val scale = deformation / (item.weightLess?.getOrNull(i) ?: 1f)
            val printScale = scale / item.keyLess.size
            printValue -= printScale
        }
        if (printValue > 1) {
            printValue = 1f
        } else if (printValue < -1) {
            printValue = -1f
        }
        return printValue
    }

    /**
     * 根据 UI 上滑条的值，计算出应该传给 Avatar 的骨骼参数
     */
    fun uiToModel(item: BodyShapeItem, value: Float): Map<String, Float> {
        val deformationMap = mutableMapOf<String, Float>()
        val absValue = value.absoluteValue
        if(value >= 0) {
            item.keyMore.forEachIndexed { i, key ->
                deformationMap[key] = (item.weightMore?.getOrNull(i) ?: 1f) * absValue
            }
            item.keyLess.forEach { key ->
                deformationMap[key] = 0f
            }
        } else {
            item.keyLess.forEachIndexed { i, key ->
                deformationMap[key] = (item.weightLess?.getOrNull(i) ?: 1f) * absValue
            }
            item.keyMore.forEach { key ->
                deformationMap[key] = 0f
            }
        }
        return deformationMap
    }
}

/**
 * 将 [BodyShapeItem] 当前应该显示值的 计算公式
 */
typealias TranSliderValue = (BodyShapeItem) -> Float
