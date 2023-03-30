package com.faceunity.app_ptag.ui.edit.weight.control

import com.faceunity.app_ptag.ui.edit.entity.control.wrapper.*

/**
 * View 传递给 Holder 处理的 Event。
 */
internal interface AvatarControlHolderEvent {
    fun switchMaster(masterWrapper: MasterWrapper)

    fun switchMinor(minorWrapper: MinorWrapper)

    fun clickColor(subColorWrapper: SubColorWrapper)

    fun clickBundle(subBundleWrapper: SubBundleWrapper)

    fun clickConfig(subConfigWrapper: SubConfigWrapper)
}