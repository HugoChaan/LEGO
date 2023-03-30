package com.faceunity.app_ptag.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceunity.core.entity.FUBundleData
import com.faceunity.core.enumeration.FUAIHuman3DSceneStateEnum
import com.faceunity.core.enumeration.FUAIHumanFootModeTypeEnum
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.store.DevAIRepository
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.pta.pta_core.data_build.FuBundleDataBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI 能力的封装 ViewModel
 * - AR 驱动 的开关
 * - 面部驱动 的开关
 * - 身体驱动 的开关
 */
class FuDriveViewModel : ViewModel() {

    private var mCacheSceneBackground: FUBundleData? = null

    private val arMask: Map<String, FUBundleData?> by lazy {
        fun buildArMaskBundle(gender: String): FUBundleData? {
            return DevSceneManagerRepository.filterGenderDefaultSceneFirst(gender)?.let {
                it.sceneResource.ARMask
            }?.let {
                FuBundleDataBuilder.buildFuBundleData(
                    FuDevDataCenter.resourcePath.ossBundle(it),
                    it
                )
            }
        }

        listOf("male", "female").associate { it to buildArMaskBundle(it) }
    }
    private val lightBundle: Map<String, FUBundleData?> by lazy {
        fun buildLightBundle(type: String): FUBundleData? {
            return DevSceneManagerRepository.getDefaultOrNull()?.let {
                when(type) {
                    "ar" -> it.sceneResource.lightARMask
                    else -> it.sceneResource.light
                }
            }?.let {
                FuBundleDataBuilder.buildFuBundleData(
                    FuDevDataCenter.resourcePath.ossBundle(it),
                    it
                )
            }
        }

        listOf("normal", "ar").associate { it to buildLightBundle(it) }
    }


    fun openArMode() {
        val scene = DevDrawRepository.getSceneOrNull()
        if (scene == null) {
            FuLog.warn("openArMode: scene is null, break.")
            return
        }
        mCacheSceneBackground = scene.getBackground()
        scene.setBackground(null)
        runOnOrigin {
            DevAIRepository.openArMode(scene)
        }
    }

    fun closeArMode() {
        val scene = DevDrawRepository.getSceneOrNull()
        if (scene == null) {
            FuLog.warn("closeArMode: scene is null, break.")
            return
        }
        mCacheSceneBackground?.let {
            scene.setBackground(it)
        }
        runOnOrigin {
            DevAIRepository.closeArMode(scene)
        }
    }

    fun openArModeWithMask() {
        val scene = DevDrawRepository.getSceneOrNull()
        if (scene == null) {
            FuLog.warn("openArMode: scene is null, break.")
            return
        }
        mCacheSceneBackground = scene.getBackground()
        scene.setBackground(null)
        DevAvatarManagerRepository.getCurrentAvatarInfo()?.let { avatarInfo ->
            val gender = avatarInfo.gender()
            avatarInfo.avatar.addComponent(arMask[gender]!!)
            scene.setLightingBundle(lightBundle["ar"])
            scene.rendererConfig.setEnableFakeShadow(true)
        }
        runOnOrigin {
            DevAIRepository.openArMode(scene)
        }
    }

    fun closeArModeWithMask() {
        val scene = DevDrawRepository.getSceneOrNull()
        if (scene == null) {
            FuLog.warn("closeArMode: scene is null, break.")
            return
        }
        mCacheSceneBackground?.let {
            scene.setBackground(it)
        }
        DevAvatarManagerRepository.getCurrentAvatarInfo()?.let { avatarInfo ->
            val gender = avatarInfo.gender()
            avatarInfo.avatar.removeComponent(arMask[gender]!!)
            scene.setLightingBundle(lightBundle["normal"])
            scene.rendererConfig.setEnableFakeShadow(false)
        }
        runOnOrigin {
            DevAIRepository.closeArMode(scene)
        }
    }

    fun openFaceTrack() {
        val scene = DevDrawRepository.getSceneOrNull()
        if (scene == null) {
            FuLog.warn("openFaceTrack: scene is null, break.")
            return
        }
        runOnOrigin {
            DevAIRepository.openFaceTrack(scene)
        }

    }

    fun closeFaceTrack() {
        val scene = DevDrawRepository.getSceneOrNull()
        if (scene == null) {
            FuLog.warn("closeFaceTrack: scene is null, break.")
            return
        }
        runOnOrigin {
            DevAIRepository.closeFaceTrack(scene)
        }
    }

    fun openBodyFullTrack() {
        val scene = DevDrawRepository.getSceneOrNull()
        if (scene == null) {
            FuLog.warn("openBodyFullTrack: scene is null, break.")
            return
        }
        runOnOrigin {
            DevAIRepository.openBodyTrack(scene, FUAIHuman3DSceneStateEnum.FULL)
        }

    }

    fun openBodyHalfTrack() {
        val scene = DevDrawRepository.getSceneOrNull()
        if (scene == null) {
            FuLog.warn("openBodyHalfTrack: scene is null, break.")
            return
        }
        runOnOrigin {
            DevAIRepository.openBodyTrack(scene, FUAIHuman3DSceneStateEnum.HALF)
        }

    }

    fun closeBodyTrack() {
        val scene = DevDrawRepository.getSceneOrNull()
        if (scene == null) {
            FuLog.warn("closeBodyTrack: scene is null, break.")
            return
        }
        runOnOrigin {
            DevAIRepository.closeBodyTrack(scene)
        }
    }

    fun setBodyFollowModeFix() {
        val avatar = DevAvatarManagerRepository.getCurrentAvatarInfo()?.avatar ?: return
        DevAIRepository.setBodyFollowMode(avatar, FUAIHumanFootModeTypeEnum.FIXED)
    }

    fun setBodyFollowModeStage() {
        val avatar = DevAvatarManagerRepository.getCurrentAvatarInfo()?.avatar ?: return
        DevAIRepository.setBodyFollowMode(avatar, FUAIHumanFootModeTypeEnum.STAGE)
    }

    fun release() {
        DevDrawRepository.getSceneOrNull()?.let {
            DevAIRepository.release(it)
        }
        mCacheSceneBackground = null
    }

    private fun runOnOrigin(block: () -> Unit) {
        block()
    }

    private fun runOnCoroutine(block: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                block()
            }
        }
    }

}