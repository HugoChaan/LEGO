package com.faceunity.app_ptag.compat

import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.fupta.cloud_download.CheckInterface
import com.faceunity.toolbox.utils.FUMD5Utils
import java.io.File

/**
 *
 */
class CloudCheckInterface : CheckInterface {
    override fun fileIdIsExist(fileId: String): Boolean {
        return FuDevDataCenter.resourcePath.ossBundle(fileId)
            .let { FuDevDataCenter.resourceLoader.isExist(it) }
    }

    override fun fileIdHash(fileId: String, hashingAlgorithm: String): String? {
        if (!fileIdIsExist(fileId)) return null
        val filePath = FuDevDataCenter.resourcePath.ossBundle(fileId)
        return FUMD5Utils().getFileMD5String(File(filePath))
    }

    override fun fileIdHashIsMatch(
        fileId: String,
        hash: String,
        hashingAlgorithm: String
    ): Boolean {
        val filePath = FuDevDataCenter.resourcePath.ossBundle(fileId)
        try {
            val fileMD5String = FUMD5Utils().getFileMD5String(File(filePath))
            return fileMD5String == hash
        } catch (ex: java.lang.Exception) {
            return true
        }
    }
}