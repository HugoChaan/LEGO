package com.faceunity.app_ptag.compat

import com.faceunity.editor_ptag.parser.IFuJsonParser
import com.faceunity.fupta.cloud_download.entity.api.FuCommonResult
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.lang.reflect.Type

/**
 * 使用 GSON 实现的 JSON 解析。接入者可更换为自己的实现。
 */
class GsonJsonParser : IFuJsonParser {
    val gson = Gson()
    override fun <T> parse(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }

    override fun <T> parse(json: String, typeOfT: Type): T {
        return gson.fromJson(json, typeOfT)
    }

    override fun parseCommon(json: String): FuCommonResult {
        val jsonObj = JsonParser.parseString(json).asJsonObject
        val code = jsonObj.get("code").asInt
        val msg = jsonObj.get("message").asString
        val data = jsonObj.get("data").let {
            if (it.isJsonObject) it.asJsonObject.toString()
            else ""
        }
        return FuCommonResult(code, data, msg)
    }

    override fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }

}