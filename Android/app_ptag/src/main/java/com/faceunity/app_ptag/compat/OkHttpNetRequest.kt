package com.faceunity.app_ptag.compat

import com.faceunity.app_ptag.BuildConfig
import com.faceunity.editor_ptag.util.FuElapsedTimeChecker
import com.faceunity.fupta.cloud_download.RequestFailure
import com.faceunity.fupta.cloud_download.ResponseCodeThrowable
import com.faceunity.fupta.cloud_download.util.NetCallback
import com.faceunity.fupta.cloud_download.util.NetDownloadCallback
import com.faceunity.fupta.cloud_download.util.NetRequest
import com.faceunity.fupta.cloud_download.util.RequestWrapper
import com.faceunity.toolbox.log.FuLogInterface
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * 基于 OkHttp 的 [NetRequest] 实现
 */
class OkHttpNetRequest(var fuLog: FuLogInterface? = null) : NetRequest {
    private val logging = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            fuLog?.debug(message)
        }
    }).apply {
        setLevel(if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder
//            .addInterceptor(logging)
            .readTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(32, 5, TimeUnit.MINUTES))
        builder.build()
    }

    override fun request(requestWrapper: RequestWrapper, netCallback: NetCallback) {
        FuElapsedTimeChecker.start("request|${formatUrl(requestWrapper)}")
        val requestBuilder: Request.Builder = buildRequestBuilder(requestWrapper)
        okHttpClient.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                FuElapsedTimeChecker.end("request|${formatUrl(requestWrapper)}")
                netCallback.onError(RequestFailure(e, "onFailure"))
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    FuElapsedTimeChecker.end("request|${formatUrl(requestWrapper)}")
                    netCallback.onError(ResponseCodeThrowable(response.code, "http state:${response.code}"))
                    return
                }
                FuElapsedTimeChecker.end("request|${formatUrl(requestWrapper)}")
                netCallback.onSuccess(response.body?.string() ?: "")
            }
        })
    }

    override fun request(requestWrapper: RequestWrapper): Result<String> {
        return try {
            FuElapsedTimeChecker.start("request|${formatUrl(requestWrapper)}")
            val requestBuilder: Request.Builder = buildRequestBuilder(requestWrapper)
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (response.code == 200) {
                Result.success(response.body?.string() ?: "")
            } else {
                Result.failure(ResponseCodeThrowable(response.code))
            }
        } catch (ex: Throwable) {
            Result.failure(RequestFailure(ex, "url:${formatUrl(requestWrapper)}"))
        } finally {
            FuElapsedTimeChecker.end("request|${formatUrl(requestWrapper)}")

        }
    }

    override fun download(
        requestWrapper: RequestWrapper,
        file: File,
        netDownloadCallback: NetDownloadCallback
    ) {
        FuElapsedTimeChecker.start("download|${formatUrl(requestWrapper)}")
        val requestBuilder: Request.Builder = buildRequestBuilder(requestWrapper)
        okHttpClient.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                netDownloadCallback.onError(RequestFailure(e))
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    FuElapsedTimeChecker.end("download|${formatUrl(requestWrapper)}")
                    netDownloadCallback.onError(ResponseCodeThrowable(response.code))
                    return
                }
                val isSuccess = responseToFile(response, file)
                if (isSuccess) {
                    FuElapsedTimeChecker.end("download|${formatUrl(requestWrapper)}")
                    netDownloadCallback.onSuccess(file)
                } else {
                    FuElapsedTimeChecker.end("download|${formatUrl(requestWrapper)}")
                    netDownloadCallback.onError(Exception("responseToFile error"))
                }
            }
        })
    }

    override fun download(requestWrapper: RequestWrapper, file: File): Result<File> {
        return try {
            FuElapsedTimeChecker.start("download|${formatUrl(requestWrapper)}")
            val requestBuilder: Request.Builder = buildRequestBuilder(requestWrapper)
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (response.code != 200) {
                return Result.failure(ResponseCodeThrowable(response.code))
            }
            val isSuccess = responseToFile(response, file)
            if (!isSuccess) {
                return Result.failure(Throwable("responseToFile error"))
            }
            Result.success(file)
        } catch (ex: Throwable) {
            Result.failure(ex)
        } finally {
            FuElapsedTimeChecker.end("download|${formatUrl(requestWrapper)}")

        }

    }

    /**
     * 构造一个完整的请求
     */
    private fun buildRequestBuilder(requestWrapper: RequestWrapper): Request.Builder {
        val requestBuilder = Request.Builder()
        var fullUrl = requestWrapper.url
        requestWrapper.queryParams?.also {
            fullUrl = parseUrl(requestWrapper.url, it)
        }
        requestBuilder.url(fullUrl)
        fuLog?.info("request fullUrl:$fullUrl")
        requestWrapper.method?.also {
            try {
                requestBuilder.method(it, null)
            } catch (e: Exception) {
                fuLog?.error("buildRequestBuilder method:$e")
            }
        }
        requestWrapper.header?.forEach { (key, value) ->
            requestBuilder.header(key, value.toString())
        }
        requestWrapper.formData?.also {
            val bodyBuilder = MultipartBody.Builder()
            it.forEach { (key, value) ->
                when {
                    value is File -> {
                        bodyBuilder.addFormDataPart(
                            key,
                            value.name,
                            value.asRequestBody(null)
                        )
                    }
                    else -> {
                        bodyBuilder.addFormDataPart(key, value.toString())
                    }
                }
            }
            bodyBuilder.setType(MultipartBody.FORM)
            requestBuilder.post(bodyBuilder.build())
            fuLog?.info("request formData body:${it}")
        }
        requestWrapper.rawBody?.also {
            requestBuilder.post(it.toRequestBody(requestWrapper.contentType?.toMediaTypeOrNull()))
            fuLog?.info("request rawBody:${it}")
        }
        return requestBuilder
    }

    /**
     * 解析为一个完整的 url
     */
    private fun parseUrl(url: String, queryParams: Map<String, Any>): String {
        val builder = url.toHttpUrlOrNull()?.newBuilder()
        if (builder == null) {
            fuLog?.error("can't parse $url to Http Url.break parse queryParams")
            return url
        }
        queryParams.forEach { (key, value) ->
            builder.addQueryParameter(key, value.toString())
        }
        return builder.build().toString()
    }

    private fun formatUrl(requestWrapper: RequestWrapper) : String {
        return requestWrapper.url.removePrefix("https://")
    }

    /**
     * 祖传代码，来自 com.faceunity.fupta.web.OkHttpUtils#downServiceBundle
     */
    private fun responseToFile(response: Response, file: File): Boolean {
        // Okhttp/Retofit 下载监听
        var `is`: InputStream? = null
        val buf = ByteArray(2048)
        var len = 0
        var fos: FileOutputStream? = null
        // 储存下载文件的目录
        try {
            `is` = response.body!!.byteStream()
            if (!file.exists()) file.createNewFile()
            fos = FileOutputStream(file)
            while (`is`.read(buf).also { len = it } != -1) {
                fos.write(buf, 0, len)
                //currentLen += len;
            }
            fos.flush()
            return true
        } catch (e: Exception) {
            fuLog?.error("", e)
        } finally {
            try {
                `is`?.close()
            } catch (e: IOException) {
            }
            try {
                fos?.close()
            } catch (e: IOException) {
            }
        }
        return false
    }

}