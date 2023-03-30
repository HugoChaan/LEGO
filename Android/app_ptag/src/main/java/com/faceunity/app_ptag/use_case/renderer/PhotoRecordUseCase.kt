package com.faceunity.app_ptag.use_case.renderer

import android.graphics.Bitmap
import com.faceunity.core.entity.FURenderOutputData
import com.faceunity.core.media.photo.FUPhotoRecordHelper
import com.faceunity.editor_ptag.util.GL
import com.faceunity.toolbox.utils.FUGLUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 同步地将 outputData 转为 Bitmap 返回
 */
class PhotoRecordUseCase(
    private val glDispatcher: CoroutineDispatcher = Dispatchers.GL
) {

    suspend fun execute(outputData: FURenderOutputData, config: Config = Config()): Bitmap = withContext(glDispatcher) {
        suspendCancellableCoroutine { cont ->
            val photoRecordHelper = FUPhotoRecordHelper()
            photoRecordHelper.bindListener(object : FUPhotoRecordHelper.OnPhotoRecordingListener {
                override fun onRecordSuccess(bitmap: Bitmap?, tag: String) {
                    if (cont.isActive) {
                        if (bitmap != null) {
                            cont.resume(bitmap)
                        } else {
                            cont.resumeWithException(NullPointerException("FUPhotoRecordHelper return null"))
                        }
                    }
                }
            })
            val texture = outputData.texture
            if (texture == null) {
                if (cont.isActive) {
                    cont.resumeWithException(NullPointerException("outputData.texture is null"))
                }
                return@suspendCancellableCoroutine
            }
            val recordData = FUPhotoRecordHelper.RecordData(texture.texId, config.drawMatrix, FUGLUtils.IDENTITY_MATRIX, texture.width, texture.height).apply {
                isAlpha = config.isAlpha
            }
            photoRecordHelper.sendRecordingData(recordData)
        }
    }

    data class Config(
        val drawMatrix: FloatArray = FUGLUtils.IDENTITY_MATRIX,
        val isAlpha: Boolean = true
    )
}