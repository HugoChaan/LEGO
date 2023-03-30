package com.faceunity.app_ptag.ui.build_avatar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.faceunity.app_ptag.ui.build_avatar.entity.FaceInfo
import com.faceunity.app_ptag.ui.build_avatar.util.FaceCheckUtil
import com.faceunity.core.camera.enumeration.FUCameraFacingEnum
import com.faceunity.core.entity.FUAIImageData
import com.faceunity.core.entity.FURenderInputData
import com.faceunity.core.enumeration.FUAIImageFormatEnum
import com.faceunity.core.enumeration.FUAIRotationModeEnum
import com.faceunity.core.faceunity.FUAIKit


class FuDemoBuildViewModel : ViewModel() {
    private val _tipMsgLiveData = MutableLiveData<String>()
    val tipMsgLiveData: LiveData<String>
        get() = _tipMsgLiveData
    private val _isCheckFacePassLiveData = MutableLiveData<Boolean>(false)
    val isCheckFacePassLiveData: LiveData<Boolean>
        get() = _isCheckFacePassLiveData

    fun onRenderPrepare(inputData: FURenderInputData) {
        val faceInfo: FaceInfo? = trackFace(inputData)
        parseFaceInfo(faceInfo, inputData.imageData?.width ?: 0, inputData.imageData?.height ?: 0)
    }

    private fun parseFaceInfo(faceInfo: FaceInfo?, width: Int, height: Int) {
        val msg: String
        var isCheckFacePass = false
        if (faceInfo == null) {
            msg = "未识别到人脸"
        } else if (faceInfo.faceCount != 1) {
            msg = "请保持1人"
        } else if (FaceCheckUtil.checkRotation(faceInfo.rotationData)) {
            msg = "请保持正面"
        } else if (FaceCheckUtil.checkFaceRect(faceInfo.faceRect, width, height)) {
            msg = "请将人脸对准屏幕中央"
        } else if (FaceCheckUtil.checkExpression(faceInfo.expressionData)) {
            msg = "请保持面部无夸张表情"
        } else {
            msg = "已就绪"
            isCheckFacePass = true
        }
        _tipMsgLiveData.postValue(msg)
        _isCheckFacePassLiveData.postValue(isCheckFacePass)
    }


    fun trackFace(inputData: FURenderInputData): FaceInfo? {
        val imageBuffer = inputData.imageData ?: return null


        val fuaiKit = FUAIKit.getInstance()
        val image = FUAIImageData(FUAIImageFormatEnum.NV21, imageBuffer.width, imageBuffer.height, imageBuffer.buffer, imageBuffer.width)
        val rot = when (getRotMode(inputData)) {
            0 -> FUAIRotationModeEnum.CCROT0
            1 -> FUAIRotationModeEnum.CCROT90
            2 -> FUAIRotationModeEnum.CCROT180
            3 -> FUAIRotationModeEnum.CCROT270
            else -> FUAIRotationModeEnum.CCROT0
        }
        val count: Int = fuaiKit.trackFace(image, rot, inputData.renderConfig.inputBufferMatrix)

        if (count >= 1) {
            val rotation = fuaiKit.getFaceProcessorRotation(0)
            val rotationData = if (rotation != null) floatArrayOf(rotation.x, rotation.y, rotation.z, rotation.w) else FloatArray(4)
            val faceRect = fuaiKit.getFaceProcessorFaceRect(0)
            val faceRectData = if (faceRect != null) floatArrayOf(faceRect.x, faceRect.y, faceRect.z, faceRect.w) else FloatArray(4)
            val expressionData = fuaiKit.getFaceProcessorExpression(0) ?: FloatArray(57)
            return FaceInfo(count, rotationData, faceRectData, expressionData)
        }
        return null
    }

    private fun getRotMode(inputData: FURenderInputData): Int {
        val renderConfig: FURenderInputData.FURenderConfig = inputData.renderConfig
        return if (renderConfig.cameraFacing == FUCameraFacingEnum.CAMERA_FRONT) {
            (renderConfig.inputOrientation + renderConfig.deviceOrientation + 90) % 360 / 90
        } else {
            (renderConfig.inputOrientation - renderConfig.deviceOrientation + 270) % 360 / 90
        }
    }
}