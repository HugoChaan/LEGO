package com.faceunity.app_ptag.use_case.render_kit

import android.graphics.Bitmap
import com.faceunity.app_ptag.ui.edit.expand.facepup.FuBodyShapePreviewHelper
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.core.avatar.model.Scene
import com.faceunity.core.entity.FUColorRGBData
import com.faceunity.core.enumeration.FURenderFormatEnum
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.pta.pta_core.model.AvatarInfo
import java.io.File

/**
 * 根据 Avatar 的当前配置，通过后台渲染的方式生成一个大头照，并更新相关预览图。
 */
class SaveAvatarIconUseCase(
    val shotSceneUseCase: ShotSceneUseCase,
    val switchCameraUseCase: SwitchCameraUseCase
) {

    suspend fun execute(avatarInfo: AvatarInfo) {
        avatarInfo.run {
            execute(avatar, gender(), avatarLogoPath)
        }
    }

    suspend fun execute(avatar: Avatar, gender: String, iconPath: String, customScene: (Scene.() -> Unit)? = null) {

        val scene = DevDrawRepository.currentScene?.clone() ?: Scene()
        scene.apply {
            rendererConfig.setRenderFormat(FURenderFormatEnum.RGBA16)
            if (scene.getBackground() == null) {
                setBackgroundColor(FUColorRGBData(244, 247, 255))
            }
            switchCameraUseCase.getBundle("lianxin", gender).let {
                cameraAnimation.setAnimation(it)
            }
            customScene?.invoke(this)
        }

        val useAvatar = avatar.clone().apply {
            getDefaultBodyShapeParams().forEach { (key, value) ->
                deformation.setDeformation(key, value)
            }
            //关闭高跟鞋的身高影响
            dynamicBone.setEnableHighHeelBoneTransform(false)
        }
        scene.removeAllAvatar()
        scene.addAvatar(useAvatar)
        val bitmap = shotSceneUseCase(ShotSceneUseCase.Params(scene)).getOrThrow()
        File(iconPath).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
        }
    }

    /**
     * 得到默认的捏形数据。
     * 用于在生成图标时重置身高数据，防止相机对不准头。
     * @return 捏形数据。如果获取失败返回 emptyMap。
     */
    fun getDefaultBodyShapeParams(): Map<String, Float> = kotlin.runCatching {
        FuBodyShapePreviewHelper.headKey.associate { it to 0f }
    }.getOrDefault(emptyMap())
}