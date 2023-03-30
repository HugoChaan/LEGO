package com.faceunity.app_ptag.edit_cloud

import com.faceunity.app_ptag.edit_cloud.entity.FuCentreConfig
import com.faceunity.fupta.cloud_download.parser.IFuAPIParser
import com.faceunity.fupta.cloud_download.util.NetRequest
import com.faceunity.fupta.cloud_download.util.RequestWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 *
 */
class EditCloudRepository(
    val request: NetRequest,
    val domain:String,
    val jsonParser: IFuAPIParser,
    private val coroutineContext: CoroutineDispatcher = Dispatchers.IO
) {
    var token: String = ""

    suspend fun centreConfig(itemListVersion: String, uiVersion: String?): Result<FuCentreConfig> {
        val requestWrapper = RequestWrapper(domain + "/api/items/v2/centre-config").apply {
            setQueryParamsIgnoreNull(
                "token" to token,
                "version" to itemListVersion,
                "uiVersion" to uiVersion
            )
        }

        return withContext(coroutineContext) {
            request.request(requestWrapper).mapCatching { jsonParser.parse(it, FuCentreConfig::class.java).getOrThrow() }
        }
    }

    suspend fun loadContent(url: String): Result<String> {
        val requestWrapper = RequestWrapper(url)
        return withContext(coroutineContext) {
            request.request(requestWrapper)
        }
    }

    suspend fun downloadFile(url: String, file: File): Result<File> {
        val requestWrapper = RequestWrapper(url)
        return withContext(coroutineContext) {
            request.download(requestWrapper, file)
        }
    }

}