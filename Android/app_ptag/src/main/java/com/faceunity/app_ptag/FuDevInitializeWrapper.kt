package com.faceunity.app_ptag

import android.content.Context
import com.faceunity.app_ptag.ui.home.dev.IDevBuilderInstance
import com.faceunity.core.avatar.model.Scene
import com.faceunity.core.entity.FUPostProcessMirrorParamData
import com.faceunity.core.enumeration.FURenderFormatEnum
import com.faceunity.core.faceunity.FUAIKit
import com.faceunity.core.faceunity.FURenderKit
import com.faceunity.core.faceunity.FURenderManager
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.util.FuElapsedTimeChecker
import com.faceunity.fupta.cloud_download.entity.CloudConfig
import com.faceunity.fupta.cloud_download.entity.CloudMode
import com.faceunity.toolbox.file.FUFileUtils
import com.faceunity.toolbox.utils.FULogger
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.io.File

/**
 * SDK 所有初始化相关的代码聚合
 */
object FuDevInitializeWrapper {

    val defaultCloudConfig by lazy {
        CloudConfig(
            domain = "https://apigateway.faceunity.com",
            apiKey = "8UV113NeECvtpNAEMhLzbW",
            apiSecret = "jwuS6yMxWEL6j4eV63jbgo",
            appId = "WbzLhMEANptvCEeN311VU8",
            mode = CloudMode.Release
        )
    }

    fun initSDK(context: Context) {
        startKoin {
            androidLogger(Level.ERROR) //https://github.com/InsertKoinIO/koin/issues/1188
            androidContext(context)
            modules(FuDependencyInjection.allModel)
        }

        FuDependencyInjection.init(
            cloudConfig = defaultCloudConfig,
            socketUrl = "wss://avatarx-websocket.faceunity.com"
        )

        FURenderManager.setCoreDebug(FULogger.LogLevel.WARN) //图形算法相关日志
        FURenderManager.setKitDebug(FULogger.LogLevel.DEBUG) //客户端核心库相关日志
        FURenderManager.registerFURender(getAuthPack())
        FURenderKit.getInstance().setStorageCacheDirectory(getCacheDir(context, "shader"))
        FUAIKit.getInstance().setAICacheDirectory(getCacheDir(context, "ai"))
        FUFileUtils.readByteArrayByPath(FuDevDataCenter.resourcePath.sdkEngineAssetsBundle())?.let {
            FURenderManager.createRenderContext(it)
        }
        initBusiness(context)
    }

    fun getAuthPack(): ByteArray {
        return authpack.A()
    }

    private fun getCacheDir(context: Context, name: String): String {
        val path = (context.getExternalFilesDir(null) ?: context.filesDir).path + "/GraphicsCache/$name/"
        File(path).let { file ->
            if (!file.exists()) {
                file.mkdirs()
            }
        }
        return path
    }

    /**
     * 一些业务功能。根据实际情况选择。
     */
    private fun initBusiness(context: Context) {
        FuElapsedTimeChecker.enable = false //开启耗时统计
    }

    /**
     * 统一对 RenderKit 做初始化配置
     */
    fun initRenderKit() {
        FURenderKit.getInstance().apply {
            createGraphicDevice()
            bindGLThread()
        }
        DevDrawRepository.onRenderKitInit()
    }


    /**
     * 统一对 Scene 做初始化配置
     */
    val sceneCustom: Scene.() -> Unit = {
        rendererConfig.setRenderFormat(FURenderFormatEnum.R11G11B10) //设置色彩空间为 RGB，不带 alpha
        rendererConfig.setPostProcessMirrorParam(FUPostProcessMirrorParamData( // 开启地面反射
            maxTransparency = 0.7f,
            maxDistance = 30f
        ))
    }

    /**
     * 统一对 RenderKit 做释放配置
     */
    fun releaseRenderKit() {
        FURenderKit.getInstance().release()
        DevDrawRepository.onRenderKitRelease()
    }

    class NeedConfigError: Error() {
        override fun toString(): String {
            return "请联系相芯人员获取此处需要的配置"
        }
    }

    inline fun 待填写(): Nothing = throw NeedConfigError()
}