package com.faceunity.app_ptag.data_center

import android.content.Context
import com.faceunity.pta.pta_core.data_build.FuResourcePath

/**
 * 用于参考的 [FuResourcePath] 实现类。对应方法的路径如无修改可不重写。
 * [space] 用于在不同的环境下有不同的目录，实现环境隔离。无此需求可不管。
 */
class FuCloudSpaceResourcePath(val context: Context, val space: String = "") : FuResourcePath(
    ossAssets = context.getExternalFilesDir(null)?.path + "${space.run { if (isBlank()) "" else "/$this" }}/download",
    appAssets = context.getExternalFilesDir(null)?.path + "${space.run { if (isBlank()) "" else "/$this" }}/download/" + "AppAssets",
    binAssets = "pta_kit"
) {

    override fun appSceneList(): String {
        return "AppAssets/scene_list.json" //使用 Demo 中 assets 下预置的 json
    }

    override fun avatarBundleConfig(): String {
        return "AppAssets/avatar_bundle_config.json" //使用 Demo 中 assets 下预置的 json
    }

//    override fun appAnimationConfig() = "AppAssets/sta/AnimationConfig.json" //使用 Demo 中 assets 下预置的 json
//
//    override fun appEmotionConfig() = "AppAssets/sta/EmotionConfig.json" //使用 Demo 中 assets 下预置的 json
//
//    override fun appTagConfig() = "AppAssets/sta/TagConfig.json" //使用 Demo 中 assets 下预置的 json

    override fun avatarSourceDirList(): List<String> =
//        listOf("AppAssets/DemoAvatarList") +
        super.avatarSourceDirList()

//    override fun appEditConfig(): String {
//        return "AppAssets/edit_config.json"
//    }

}