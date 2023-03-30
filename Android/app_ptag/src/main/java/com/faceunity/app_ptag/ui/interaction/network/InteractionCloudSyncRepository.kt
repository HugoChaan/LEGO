package com.faceunity.app_ptag.ui.interaction.network

import com.faceunity.app_ptag.ui.interaction.network.entity.*
import com.faceunity.app_ptag.util.expand.merge
import com.faceunity.fupta.cloud_download.parser.IFuAPIParser
import com.faceunity.fupta.cloud_download.parser.checkCode
import com.faceunity.fupta.cloud_download.parser.result
import com.faceunity.fupta.cloud_download.util.NetRequest
import com.faceunity.fupta.cloud_download.util.RequestWrapper
import com.faceunity.toolbox.log.FuLogInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 *
 */
class InteractionCloudSyncRepository(
    val fuLog: FuLogInterface,
    val apiParser: IFuAPIParser,
    val request: NetRequest,
    val domain:String,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    var token: String = ""

    suspend fun voiceList(): Result<InteractionVoiceResult> = withContext(defaultDispatcher) {
        val requestWrapper = RequestWrapper(domain + "/api/demo/config/voicelist").apply {
            setQueryParams(
                "token" to token
            )
        }
        request.request(requestWrapper).merge {
            apiParser.parse(it, InteractionVoiceResult::class.java)
        }.merge {
            (it.code to it.message).checkCode().result(it)
        }
    }

    suspend fun skill(): Result<InteractionSkillResult> = withContext(defaultDispatcher) {
        val requestWrapper = RequestWrapper(domain + "/api/demo/config/skill").apply {
            setQueryParams(
                "token" to token
            )
        }
        request.request(requestWrapper).merge {
            apiParser.parse(it, InteractionSkillResult::class.java)
        }.merge {
            (it.code to it.message).checkCode().result(it)
        }
    }

    suspend fun homePage(): Result<InteractionHomePageResult> = withContext(defaultDispatcher) {
        val requestWrapper = RequestWrapper(domain + "/api/demo/config/homepage").apply {
            setQueryParams(
                "token" to token
            )
        }
        request.request(requestWrapper).merge {
            apiParser.parse(it, InteractionHomePageResult::class.java)
        }.merge {
            (it.code to it.message).checkCode().result(it)
        }
    }

    suspend fun animation(): Result<InteractionAnimationResult> = withContext(defaultDispatcher) {
        val requestWrapper = RequestWrapper(domain + "/api/demo/config/animation").apply {
            setQueryParams(
                "token" to token
            )
        }
        request.request(requestWrapper).merge {
            apiParser.parse(it, InteractionAnimationResult::class.java)
        }.merge {
            (it.code to it.message).checkCode().result(it)
        }
    }

    suspend fun actionTag(): Result<InteractionActionTagResult> = withContext(defaultDispatcher) {
        val requestWrapper = RequestWrapper(domain + "/api/demo/config/action-tag").apply {
            setQueryParams(
                "token" to token
            )
        }
        request.request(requestWrapper).merge {
            apiParser.parse(it, InteractionActionTagResult::class.java)
        }.merge {
            (it.code to it.message).checkCode().result(it)
        }
    }

}