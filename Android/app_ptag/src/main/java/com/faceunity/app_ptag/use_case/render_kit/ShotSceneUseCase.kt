package com.faceunity.app_ptag.use_case.render_kit

import android.graphics.Bitmap
import com.faceunity.app_ptag.use_case.renderer.PhotoRecordUseCase
import com.faceunity.core.avatar.model.Scene
import com.faceunity.core.entity.FURenderInputData
import com.faceunity.core.entity.FUTexture
import com.faceunity.core.enumeration.FUTextureFormatEnum
import com.faceunity.core.faceunity.FURenderKit
import com.faceunity.core.faceunity.FUSceneKit
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.use_case.UseCaseCoroutines
import com.faceunity.editor_ptag.util.GL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 将传入的 Scene 用后台渲染的方式（不会影响到渲染画面），生成一个 Bitmap。
 * 通常用于更新头像、或者实时 icon。
 */
class ShotSceneUseCase(
    val photoRecordUseCase: PhotoRecordUseCase,
    ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val glDispatcher: CoroutineDispatcher = Dispatchers.GL
): UseCaseCoroutines<ShotSceneUseCase.Params, Bitmap>(ioDispatcher) {


    override suspend fun execute(parameters: Params): Result<Bitmap> {
        val (scene) = parameters
        val outputData = DevDrawRepository.useBackgroundScene(scene) {
            withContext(glDispatcher) {
                FUSceneKit.getInstance().setCurrentScene(scene, false)
                val outputData = FURenderKit.getInstance().renderWithInput(
                    FURenderInputData(
                        FUTexture(FUTextureFormatEnum.RGBA, 168, 168, 0)
                    )
                )
                DevDrawRepository.currentScene?.let {
                    FUSceneKit.getInstance().setCurrentScene(it, false)
                }
                outputData
            }
        }
        val bitmap = photoRecordUseCase.execute(outputData, PhotoRecordUseCase.Config())
        return Result.success(bitmap)
    }

    data class Params(
        val scene: Scene
    )
}