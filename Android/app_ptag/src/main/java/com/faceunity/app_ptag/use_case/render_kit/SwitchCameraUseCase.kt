package com.faceunity.app_ptag.use_case.render_kit

import com.faceunity.core.entity.FUAnimationBundleData
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.store.DevSceneManagerRepository

/**
 * 根据 scene_list.json 里配置的相机名称，切换为对应的相机。
 */
class SwitchCameraUseCase {

    fun execute(cameraName: String, durationTime: Float = 0.5f) {
        val scene = DevDrawRepository.getSceneOrNull() ?: return
        val animation = getBundle(cameraName) ?: return
        scene.cameraAnimation.setAnimation(animation, durationTime)
    }

    fun getBundle(cameraName: String, gender: String? = null): FUAnimationBundleData? {
        val sceneInfo = if (gender != null) {
            DevSceneManagerRepository.filterGenderDefaultSceneFirst(gender)
        } else {
            DevSceneManagerRepository.getDefaultOrNull()
        }
        return sceneInfo?.run {
            sceneResource.cameraList?.firstOrNull { it.name == cameraName }?.path
        }?.run {
            FuDevDataCenter.resourceManager.path.ossBundle(this)
        }?.let {
            FUAnimationBundleData(it)
        }
    }
}