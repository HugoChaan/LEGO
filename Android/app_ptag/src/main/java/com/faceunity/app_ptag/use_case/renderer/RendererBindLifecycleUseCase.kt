package com.faceunity.app_ptag.use_case.renderer

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.faceunity.core.renderer.base.FUAbstractRenderer

/**
 * 绑定 Renderer 的生命周期相关方法
 */
class RendererBindLifecycleUseCase(
    private val renderer: FUAbstractRenderer
) {
    fun execute(lifecycle: Lifecycle) {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            private var isFragmentPause = false

            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                isFragmentPause = true
                renderer.pauseRender()
            }

            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                if (isFragmentPause) {
                    renderer.resumeRender()
                }
            }
        })
    }
}