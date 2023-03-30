package com.faceunity.app_ptag.compat

import com.faceunity.fupta.cloud_download.FormatFailure
import com.faceunity.fupta.cloud_download.entity.api.FuCommonResult
import com.faceunity.fupta.cloud_download.parser.IFuAPIParser
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.lang.reflect.Type

/**
 * 使用 GSON 实现的 JSON 解析。接入者可更换为自己的实现。
 * 返回了标准格式 Result。
 */
class GsonJsonRobustParser : IFuAPIParser {
    val gson = Gson()
    override fun <T> parse(json: String, clazz: Class<T>): Result<T> {
        return try {
            Result.success(gson.fromJson(json, clazz))
        } catch (ex: Throwable) {
            Result.failure(FormatFailure(ex, json))
        }
    }

    override fun <T> parse(json: String, typeOfT: Type): Result<T> {
        return try {
            Result.success(gson.fromJson(json, typeOfT))
        } catch (ex: Throwable) {
            Result.failure(FormatFailure(ex, json))
        }
    }

    override fun parseCommon(json: String): Result<FuCommonResult> {
        return try {
            val jsonObj = JsonParser.parseString(json).asJsonObject
            val code = jsonObj.get("code").asInt
            val msg = jsonObj.get("message").asString
            val data = jsonObj.get("data").let {
                if (it.isJsonObject) it.asJsonObject.toString()
                else ""
            }
            Result.success(FuCommonResult(code, data, msg))
        } catch (ex: Throwable) {
            Result.failure(FormatFailure(ex, json))
        }
    }

    override fun toJson(obj: Any): Result<String> {
        return try {
            Result.success(gson.toJson(obj))
        } catch (ex: Throwable) {
            Result.failure(FormatFailure(ex, obj.toString()))
        }
    }

}