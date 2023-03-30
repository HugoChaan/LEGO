package com.faceunity.app_ptag.ui.edit.weight.control

import com.faceunity.app_ptag.ui.edit.entity.control.MinorCategoryModel
import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryBundleModel
import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryColorModel
import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryConfigModel

/**
 * 用于外部的 [AvatarControlView] 业务事件监听
 */
interface AvatarControlListener {
    /**
     * 当切换二级菜单
     */
    fun onMinorSelect(item: MinorCategoryModel)

    /**
     * 当普通条目被选择
     */
    fun onNormalItemClick(item: SubCategoryBundleModel)

    /**
     * 当颜色条目被选择
     */
    fun onColorItemClick(item: SubCategoryColorModel)

    /**
     * 当配置条目被选择
     */
    fun onConfigItemClick(item: SubCategoryConfigModel)

    /**
     * 当捏脸按钮被选择
     * @param groupKey 当前的捏脸类别名
     * @param fileId 当前被选中的道具。根据业务需求，捏脸属性在业务上是绑在该道具上的。
     */
    fun onFacepupClick(groupKey: String, fileId: String?)

    /**
     * 当捏形按钮被选择
     */
    fun onBodyShapeClick()

    /**
     * 当历史-回退
     */
    fun onHistoryBackClick()

    /**
     * 当历史-前进
     */
    fun onHistoryForwardClick()


    fun onHistoryResetClick()
}