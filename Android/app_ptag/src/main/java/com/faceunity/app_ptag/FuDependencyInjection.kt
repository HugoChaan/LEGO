package com.faceunity.app_ptag

import android.content.Context
import com.faceunity.app_ptag.compat.*
import com.faceunity.app_ptag.data_center.FuCloudResourceLoader
import com.faceunity.app_ptag.data_center.FuCloudSpaceResourcePath
import com.faceunity.app_ptag.edit_cloud.EditCloudControlImpl
import com.faceunity.app_ptag.edit_cloud.EditCloudRepository
import com.faceunity.app_ptag.edit_cloud.IEditCloudControl
import com.faceunity.app_ptag.ui.home.dev.IDevBuilderInstance
import com.faceunity.app_ptag.ui.interaction.network.InteractionCloudSyncRepository
import com.faceunity.editor_ptag.business.cloud.download.IDownloadControl
import com.faceunity.editor_ptag.business.cloud.interfaces.FuZipInterface
import com.faceunity.editor_ptag.business.pta.CloudPhotoToAvatarSyncControlImpl
import com.faceunity.editor_ptag.business.pta.IPhotoToAvatarSyncControl
import com.faceunity.editor_ptag.business.sta.ISTARenderControl
import com.faceunity.editor_ptag.business.sta.ISTAServiceControl
import com.faceunity.editor_ptag.business.sta.STARenderControlImpl
import com.faceunity.editor_ptag.business.sta.STAServiceStreamOverlayCacheControlImpl
import com.faceunity.editor_ptag.business.sta.config.STARenderConfig
import com.faceunity.editor_ptag.business.sta.media_play.IMediaPlay
import com.faceunity.editor_ptag.business.sta.media_play.MediaPlayImpl
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.data_center.FuTokenObserver
import com.faceunity.editor_ptag.parser.IFuJsonParser
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.fupta.avatar_data.parser.IResourceParser
import com.faceunity.fupta.cloud_download.*
import com.faceunity.fupta.cloud_download.entity.CloudConfig
import com.faceunity.fupta.cloud_download.parser.IFuAPIParser
import com.faceunity.fupta.cloud_download.util.NetRequest
import com.faceunity.fupta.sta.interaction.SocketStaInteractionDataSource
import com.faceunity.fupta.sta.interaction.StaInteractionDataSource
import com.faceunity.pta.pta_core.data_build.FuResourceLoader
import com.faceunity.pta.pta_core.data_build.FuResourceManager
import com.faceunity.pta.pta_core.data_build.FuResourcePath
import com.faceunity.pta.pta_core.interfaces.FuStorageFieldInterface
import com.faceunity.toolbox.log.FuLogInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.timer

/**
 * Koin 的相关依赖注入。
 */
object FuDependencyInjection: KoinComponent {

    /**
     * 基础的必要的工具类
     */
    val utilsModel = module {
        single<FuLogInterface> { FuLog } //日志
        single<NetRequest> { OkHttpNetRequest().apply { fuLog = get() } } //网络请求

        single<IResourceParser> { GsonResourceParserImpl() } //JSON 解析
        single<IFuJsonParser> { GsonJsonParser() } //JSON 解析
        factory<FuStorageFieldInterface> { (name: String) -> SPStorageFieldImpl(get(), name) } //持久化存储配置
    }

    /**
     * 资源管理，必要。
     */
    val resourceModel = module {
        single<FuResourcePath> { (space: String) -> FuCloudSpaceResourcePath(get(), space) } //资源路径配置
        single<FuResourceLoader> { FuCloudResourceLoader(get()) } //资源的加载配置
        single { FuResourceManager(get(), get()) } //统一的资源管理器
    }

    /**
     * 云端功能所需的依赖
     */
    val cloudModel = module {
        single<SaveInterface> { (space: String) -> CloudSaveInterface(get(), space, get()) } //存储资源的状态
        single<CheckInterface> { CloudCheckInterface() } //校验文件完整性
        single { CloudResourceManager(get(), get(), get()) } //资源管理辅助类
        factory<IDownloadControl> { FuDownloadHelperImpl() } //下载功能的实现类

        //新的同步接口
        single<IFuAPIParser> { GsonJsonRobustParser() } //Result 格式的 JSON 解析器
        single { (config: CloudConfig) -> CloudSyncRepository(get(), get(), config, get()) } binds arrayOf(ICloudPlatformSyncAPI::class, ICloudTaskSyncAPI::class) //使用协程的云服务网络接口
        single<IPhotoToAvatarSyncControl> { CloudPhotoToAvatarSyncControlImpl(get()) } //使用协程的云端形象生成网络接口

    }

    /**
     * 语音播报、问答等功能所需的依赖
     */
    val staModel = module {
        single { (socketUrl: String) -> SocketStaInteractionDataSource(get(), socketUrl) } bind StaInteractionDataSource::class //互动服务的能力实现
        factory<IMediaPlay> { MediaPlayImpl() } //音频播放
        factory<ISTARenderControl> { STARenderControlImpl(get(), STARenderConfig(isParseTextProcess = true)) } //STA 的渲染控制类
        factory<ISTAServiceControl> { STAServiceStreamOverlayCacheControlImpl(get()) } //STA 服务的控制类
        single<InteractionCloudSyncRepository> { InteractionCloudSyncRepository(get(), get(), get(), FuDevInitializeWrapper.defaultCloudConfig.domain) }
    }

    /**
     * 客户端资源的相关依赖
     */
    val clientAssetsModel = module {
        factory<FuZipInterface> { FuZipImpl() } //解压缩
        single { (domain: String) -> EditCloudRepository(get(),domain, get()) } //客户端配置的云服务接口
        factory<IEditCloudControl> { EditCloudControlImpl(get(), get { parametersOf("EditCloud") }, get(), get(), get()) } //客户端配置的云服务业务封装
    }

    /**
     * 聚合接口，一键调用。
     * 不需要全部依赖的，按需加载即可。
     */
    val allModel = utilsModel + resourceModel + cloudModel + staModel + clientAssetsModel

    /**
     * 初始化全部依赖。
     * 传 null 则不初始化对应模块
     * 因为 SDK 未使用 Koin，故需要手动地将相关依赖设置进 [FuDevDataCenter] 中。
     */
    fun init(
        cloudConfig: CloudConfig?,
        socketUrl: String?
    ) {
        initBasic()
        initResourceManager(cloudConfig?.mode?.text ?: "")
        if (cloudConfig != null) {
            initCloud(cloudConfig)
        }
        if (socketUrl != null) {
            initSTA(socketUrl)
        }
        if (cloudConfig != null) {
            initClientAssets(cloudConfig.domain)
        }
    }

    /**
     * 初始化必要的依赖
     */
    fun initBasic() {
        FuDevDataCenter.initResourceParser(get())
    }

    fun initResourceManager(space: String) {
        //命名空间，为了能切换不同的环境。不需要直接设置 ""
        val useSpace = if (space == "release") "" else space
        get<FuResourcePath> { parametersOf(useSpace) }
        get<SaveInterface> { parametersOf(useSpace) }
        FuDevDataCenter.resourceManager = get()
    }

    /**
     * 初始化云端的依赖
     */
    fun initCloud(cloudConfig: CloudConfig) {
        val cloudSyncRepository = get<CloudSyncRepository> { parametersOf(cloudConfig) }

        FuDevDataCenter.initCloudResourceManager(
            get()
        )
        FuDevDataCenter.initPTAControl(
            get()
        )
        //添加一个每 55 分钟刷新 Token 的任务。避免 Token 过期。
        timer("refreshToken", true, 55 * 60 * 1000L, 55 * 60 * 1000L) {
            GlobalScope.launch(Dispatchers.IO) {
                cloudSyncRepository.requestToken().onSuccess {
                    val token = it.data?.value
                    if (token != null) {
                        FuDevDataCenter.tokenObserver.updateToken(token)
                    }
                    FuLog.info("refreshToken success.current token:${FuDevDataCenter.tokenObserver.getToken()},refresh time: ${FuDevDataCenter.tokenObserver.getTokenUpdateTimestamp().let { Date(it) }}")
                }.onFailure {
                    FuLog.error("refreshToken failed:$it")
                }
            }
        }
        IDevBuilderInstance.hookCheckBundle()
    }

    /**
     * 初始化语音互动的依赖
     */
    fun initSTA(socketUrl: String) {
        val staInteraction = get<SocketStaInteractionDataSource>{ parametersOf(socketUrl) }
        FuDevDataCenter.tokenObserver.addTokenObserver(object : FuTokenObserver.TokenObserver("socket") {
            override fun onTokenUpdate(token: String) {
                staInteraction.socket.setToken(token)
            }
        })

        FuDevDataCenter.initSTAControl(
            get(), get()
        )
    }


    /**
     * 初始化客户端配置的依赖
     */
    fun initClientAssets(editDomain: String) {
        val editCloudRepository = get<EditCloudRepository> { parametersOf(editDomain) }
        FuDevDataCenter.tokenObserver.addTokenObserver(object : FuTokenObserver.TokenObserver("edit") {
            override fun onTokenUpdate(token: String) {
                editCloudRepository.token = token
            }
        })
    }


    //region 辅助函数

    fun getContext() = get<Context>()

    /**
     * 得到一个之前申明过的依赖。
     */
    inline fun <reified T : Any> getCustom() = get<T>()

    fun getStorageField(name: String = "Default") = get<FuStorageFieldInterface> { parametersOf(name) }

    private val _memoryCache: MutableMap<String, Any> = ConcurrentHashMap()

    fun <T : Any> getMemoryProperty(key: String, defaultValue: T): T {
        return _memoryCache[key] as? T ?: defaultValue
    }

    fun <T : Any> getMemoryProperty(key: String): T? {
        return _memoryCache[key] as? T
    }

    fun setMemoryProperty(key: String, value: Any) {
        _memoryCache[key] = value
    }

    fun deleteMemoryProperty(key: String) {
        _memoryCache.remove(key)
    }

    //endregion
}

typealias FuDI = FuDependencyInjection