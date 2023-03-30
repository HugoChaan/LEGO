package com.faceunity.app_ptag.use_case.renderer

import com.faceunity.app_ptag.ui.home.dev.IDevBuilderInstance
import com.faceunity.core.camera.entity.FUCameraConfig
import com.faceunity.core.entity.FURendererTextureConfig
import com.faceunity.core.renderer.enumeration.FURendererModeEnum
import com.faceunity.core.renderer.impl.FUCustomRenderer

/**
 * 统一的创建不同场景下 Render 的 UseCase。
 * 接入者可根据实际情况，参考该类创建自己的 Render。
 */
class CreateRendererUseCase {

    companion object {
        fun build() = CreateRendererUseCase().execute()

        fun buildLessRam() = CreateRendererUseCase().execute(FURendererModeEnum.RAM)

        fun buildCamera() = CreateRendererUseCase().buildCameraRenderer()
    }

    operator fun invoke() = execute()

    fun execute(mode: FURendererModeEnum = FURendererModeEnum.REPLY): FUCustomRenderer {
        val fps = if (IDevBuilderInstance.isNotLimitFps() ?: false) {
            200
        } else {
            30
        }
        return FUCustomRenderer(FURendererTextureConfig(1, 1, fps), mode)
    }

    fun buildCameraRenderer(): FUCustomRenderer {
        val fps = if (IDevBuilderInstance.isNotLimitFps() ?: false) {
            200
        } else {
            30
        }
        return FUCustomRenderer(FUCameraConfig().apply { cameraFPS = fps },FURendererModeEnum.REPLY)
    }
}