package com.faceunity.app_ptag.use_case.renderer

import com.faceunity.app_ptag.FuDevInitializeWrapper
import com.faceunity.app_ptag.ui.home.dev.IDevBuilderInstance
import com.faceunity.app_ptag.util.FPSChecker
import com.faceunity.core.entity.FURenderInputData
import com.faceunity.core.entity.FURenderOutputData
import com.faceunity.core.entity.FURendererTextureConfig
import com.faceunity.core.renderer.base.FUAbstractRenderer
import com.faceunity.core.renderer.entity.FUDrawFrameMatrix
import com.faceunity.core.renderer.enumeration.FURendererModeEnum
import com.faceunity.core.renderer.impl.FUCustomRenderer
import com.faceunity.core.renderer.infe.OnGLRendererListener

/**
 * 统一的处理 Render 回调的 UseCase。
 * 接入者可根据实际情况，参考该类拓展自己的业务。
 */
class BindRendererListenerUseCase {

    companion object {
        operator fun invoke(ptaRenderer: FUAbstractRenderer, params: Params = Params(), listener: Listener = object : Listener {}) = BindRendererListenerUseCase().execute(ptaRenderer, params, listener)
    }

    operator fun invoke(ptaRenderer: FUAbstractRenderer, params: Params = Params(), listener: Listener = object : Listener {}) = execute(ptaRenderer, params, listener)

    fun execute(ptaRenderer: FUAbstractRenderer, params: Params = Params(), listener: Listener = object : Listener {}) {
        ptaRenderer.bindListener(object : OnGLRendererListener {
            private val fpsChecker = FPSChecker().apply {
                enable = params.isCheckFps
                print = { fps, renderTime ->
                    if (params.isCheckFps) {
                        listener.fpsPrint(fps, renderTime)
                    }
                }
            }

            override fun onSurfaceCreated() {
                FuDevInitializeWrapper.initRenderKit()
                listener.surfaceState(true)

                listener.onSurfaceCreated()
            }

            override fun onSurfaceChanged(width: Int, height: Int) {
                val fps = if (IDevBuilderInstance.isNotLimitFps() ?: false) {
                    200
                } else {
                    30
                }
                if (params.isDynamicResolution) {
                    when (ptaRenderer) { //渲染依据实际分辨率设置
                        is FUCustomRenderer -> ptaRenderer.setEmptyTextureConfig(FURendererTextureConfig(width, height, fps))
                    }
                }

                listener.onSurfaceChanged(width, height)
            }

            override fun onRenderDataPrepared(inputData: FURenderInputData) {
                if (params.isCheckFps) {
                    fpsChecker.markRenderBefore()
                }

                listener.onRenderDataPrepared(inputData)
            }

            override fun onRenderAfter(
                outputData: FURenderOutputData,
                drawMatrix: FUDrawFrameMatrix
            ) {

                listener.onRenderAfter(outputData, drawMatrix)
            }

            override fun onDrawFrameAfter() {
                if (params.isCheckFps) {
                    fpsChecker.benchmarkFPS()
                }

                listener.onDrawFrameAfter()
            }

            override fun onGLContextDestroy() {
                listener.surfaceState(false)
                listener.onGLContextDestroy()
                FuDevInitializeWrapper.releaseRenderKit()
            }

        })
    }

    data class Params(
        /**
         * 进行 FPS 耗时统计。结果会回调给 [Listener.fpsPrint]
         */
        var isCheckFps: Boolean = false,
        /**
         * 动态分辨率。根据视图的宽高设置分辨率。
         */
        var isDynamicResolution: Boolean = true,
    )

    interface Listener {
        fun onSurfaceCreated() {}
        fun onSurfaceChanged(width: Int, height: Int) {}
        fun onRenderDataPrepared(inputData: FURenderInputData) {}
        fun onRenderAfter(outputData: FURenderOutputData, drawMatrix: FUDrawFrameMatrix) {}
        fun onDrawFrameAfter() {}
        fun onGLContextDestroy() {}

        fun surfaceState(isAlive: Boolean) {}

        fun fpsPrint(fps: Double, renderTime: Double) {}
    }
}