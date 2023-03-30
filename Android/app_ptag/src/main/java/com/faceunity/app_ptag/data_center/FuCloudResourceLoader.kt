package com.faceunity.app_ptag.data_center

import android.content.Context
import com.faceunity.editor_ptag.data_center.FuDefaultResourceLoader
import com.faceunity.pta.pta_core.data_build.FuResourceLoader

/**
 * 一套简单的资源加载器。
 * 目前依据 Demo 中的简单场景，根据 `download` 目录来分配交给 Assets 解析或者 File 解析。
 * 接入者可更换为自己的实现。
 */
class FuCloudResourceLoader(val context: Context) : FuResourceLoader {
    private val assetLoader = FuDemoAssetResourceLoader(context)
    private val fileLoader = FuDefaultResourceLoader()

    private fun isInAsset(path: String) : Boolean {
        //TODO
        if (path.contains("download")) {
            return false
        }
        return true
    }

    override fun loadString(path: String): String {
        return if (isInAsset(path)) {
            assetLoader.loadString(path)
        } else {
            fileLoader.loadString(path)
        }
    }

    override fun loadByteArray(path: String): ByteArray {
        return if (isInAsset(path)) {
            assetLoader.loadByteArray(path)
        } else {
            fileLoader.loadByteArray(path)
        }
    }

    override fun listFile(path: String): List<String> {
        return if (isInAsset(path)) {
            assetLoader.listFile(path)
        } else {
            fileLoader.listFile(path)
        }
    }

    override fun isExist(path: String): Boolean {
        return if (isInAsset(path)) {
            assetLoader.isExist(path)
        } else {
            fileLoader.isExist(path)
        }
    }

    override fun writeStringToPath(content: String, path: String) {
        if (isInAsset(path)) {
            assetLoader.writeStringToPath(content, path)
        } else {
            fileLoader.writeStringToPath(content, path)
        }
    }

    override fun writeByteArrayToPath(content: ByteArray, path: String) {
        if (isInAsset(path)) {
            assetLoader.writeByteArrayToPath(content, path)
        } else {
            fileLoader.writeByteArrayToPath(content, path)
        }
    }

    override fun delete(path: String): Boolean {
        return if (isInAsset(path)) {
            assetLoader.delete(path)
        } else {
            fileLoader.delete(path)
        }
    }
}