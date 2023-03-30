package com.faceunity.app_ptag.util.expand

import com.faceunity.core.entity.FUAnimationBundleData
import com.faceunity.core.entity.FUBundleData
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.pta.pta_core.data_build.FuBundleDataBuilder
import com.faceunity.pta.pta_core.data_build.FuResourcePath

/**
 * Factory 为了便携调用而增加的一些拓展函数。不需要可删除。
 */

/**
 * 调用者需要为 fileId，形如 “GAssets/AsiaMale/body/BaseModelAmale_body.bundle”。
 * 返回一个经过 [FuResourcePath] 配置的 Bundle 路径
 */
fun String.toFuBundlePath(): String = FuDevDataCenter.resourceManager.path.ossBundle(this)

/**
 * 调用者需要为 fileId，形如 “GAssets/AsiaMale/body/BaseModelAmale_body.bundle”。
 * 返回一个经过 [FuResourcePath] 配置的 FUBundleData
 */
fun String.toFuBundle(): FUBundleData = FuBundleDataBuilder.buildFuBundleData(
    path = this.toFuBundlePath(),
    fileId = this
)

/**
 * 调用者需要为 fileId，形如 “GAssets/AsiaMale/body/BaseModelAmale_body.bundle”。
 * 返回一个经过 [FuResourcePath] 配置的 FUAnimationBundleData
 */
fun String.toFuAnimationBundle(
    nodeName: String = "DefaultState",
    repeatable: Boolean = true
): FUAnimationBundleData = FUAnimationBundleData(
    path = this.toFuBundlePath(),
    nodeName = nodeName,
    repeatable = repeatable
)