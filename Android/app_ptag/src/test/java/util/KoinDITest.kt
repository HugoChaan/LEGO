package util

import com.faceunity.app_ptag.FuDevInitializeWrapper
import com.faceunity.app_ptag.compat.GsonJsonRobustParser
import com.faceunity.app_ptag.compat.OkHttpNetRequest
import com.faceunity.fupta.cloud_download.CloudSyncRepository
import com.faceunity.fupta.cloud_download.ICloudPlatformSyncAPI
import com.faceunity.fupta.cloud_download.ICloudTaskSyncAPI
import com.faceunity.fupta.cloud_download.parser.IFuAPIParser
import com.faceunity.fupta.cloud_download.util.NetRequest
import com.faceunity.toolbox.log.FuLogInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.koin.core.component.KoinComponent
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

/**
 *
 */
object KoinDITest : KoinComponent {
    /**
     * 基础的必要的工具类
     */
    val utilsModel = module {
        single<FuLogInterface> { UnitTestLog(true) } //日志
        single<NetRequest> { OkHttpNetRequest().apply { fuLog = get() } } //网络请求

        single<IFuAPIParser> { GsonJsonRobustParser() } //JSON 解析
    }

    val cloudModel = module {
        single { FuDevInitializeWrapper.testCloudConfig }
        single { TestCoroutineDispatcher() } binds arrayOf(CoroutineDispatcher::class, CoroutineContext::class)
        factory {
            CloudSyncRepository(
                get(),
                get(),
                get(),
                get(),
                get()
            )
        } binds arrayOf(ICloudPlatformSyncAPI::class, ICloudTaskSyncAPI::class, CloudSyncRepository::class)
    }
}