package com.faceunity.app_ptag.compat

import com.faceunity.fupta.avatar_data.entity.resource.*
import com.faceunity.fupta.avatar_data.parser.IHookResourceParser
import com.faceunity.fupta.avatar_data.parser.IResourceParser
import com.faceunity.fupta.cloud_download.FormatFailure
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

/**
 * 一个资源解析器的实现类。
 * 采用了 GSON 作为解析，开发者可自行用对应的解析方案。
 */
class GsonResourceParserImpl(
    private val hookParser: IHookResourceParser = object : IHookResourceParser {}
) : IResourceParser {
    private val gson = Gson()

    override fun parseAvatar(json: String): AvatarResource {
        return gson.fromJson(json, AvatarResource::class.java).let { hookParser.hookAvatar(json, it) }
    }

    override fun parseItemList(json: String): ItemListResource {
        try {
            return gson.fromJson(json, ItemListResource::class.java).let { hookParser.hookItemList(json, it) }
        } catch (throwable: Throwable) {
            throw FormatFailure(throwable, json, "ItemList 配置错误")
        }
    }

    override fun parseProject(json: String): ProjectResource {
        return gson.fromJson(json, ProjectResource::class.java).let { hookParser.hookProject(json, it) }
    }

    override fun parseEditConfig(json: String): EditConfigResource {
        val jsonObj = JsonParser.parseString(json).asJsonObject

        val sourceJsonObj = jsonObj.getAsJsonObject("source")
        val sourceMap = gson.fromJson<Map<String, EditConfigResource.Source.SourceItem>>(
            sourceJsonObj,
            object : TypeToken<Map<String, EditConfigResource.Source.SourceItem>>() {}.type
        )
        val source = EditConfigResource.Source(sourceMap)

        val colorKeysJsonObj = jsonObj.getAsJsonObject("color_keys")
        val colorKeysMap = gson.fromJson<Map<String, List<String>>>(
            colorKeysJsonObj,
            object : TypeToken<Map<String, List<String>>>() {}.type
        )
        val colorKeys = EditConfigResource.ColorKeys(colorKeysMap)

        val map = mutableMapOf<String, List<String>>()
        val keySet = jsonObj.keySet().minus(setOf("source", "color_keys"))
        keySet.forEach { key ->
            val jsonArray = jsonObj.getAsJsonArray(key)
            val strings = gson.fromJson(jsonArray, Array<String>::class.java)
            map[key] = strings.toList()
        }

        return EditConfigResource(map, colorKeys, source).let { hookParser.hookEditConfig(json, it) }
    }

    override fun parseEditCustomConfig(json: String): EditCustomConfigResource {
        val jsonObj = JsonParser.parseString(json).asJsonObject

        val sourceJsonObj = jsonObj.getAsJsonObject("source")
        val sourceMap = mutableMapOf<String, EditCustomConfigResource.Source.SourceItem>()
        sourceJsonObj.keySet().forEach { key ->
            val itemJsonObj = sourceJsonObj.getAsJsonObject(key)
            val itemMap = gson.fromJson<Map<String, Any?>>(
                itemJsonObj,
                object : TypeToken<Map<String, Any?>>() {}.type
            )
            sourceMap[key] = EditCustomConfigResource.Source.SourceItem(itemMap)
        }

        val source = EditCustomConfigResource.Source(sourceMap)

        val colorKeysJsonObj = jsonObj.getAsJsonObject("color_keys")
        val colorKeysMap = gson.fromJson<Map<String, List<String>>>(
            colorKeysJsonObj,
            object : TypeToken<Map<String, List<String>>>() {}.type
        )
        val colorKeys = EditCustomConfigResource.ColorKeys(colorKeysMap)

        val map = mutableMapOf<String, List<String>>()
        val keySet = jsonObj.keySet().minus(setOf("source", "color_keys"))
        keySet.forEach { key ->
            val jsonArray = jsonObj.getAsJsonArray(key)
            val strings = gson.fromJson(jsonArray, Array<String>::class.java)
            map[key] = strings.toList()
        }

        return EditCustomConfigResource(map, colorKeys, source)
    }

    override fun parseEditItemList(json: String): EditItemListResource {
        val jsonObj = JsonParser.parseString(json).asJsonObject

        val mapObj = jsonObj.getAsJsonObject("map")
        val map = gson.fromJson<Map<String, List<EditItemListResource.Item>>>(
            mapObj,
            object : TypeToken<Map<String, List<EditItemListResource.Item>>>() {}.type
        )
        val version = jsonObj.get("version").let {
            if (it == null || it.isJsonNull) {
                -1
            } else {
                it.asInt
            }
        }
        return EditItemListResource(map, version).let { hookParser.hookEditItemList(json, it) }
    }

    override fun parseEditCustomItemList(json: String): EditCustomItemListResource {
        val jsonObj = JSONObject(json)

        val mapObj = jsonObj.getJSONObject("map")
        val map = mutableMapOf<String, List<EditCustomItemListResource.Item>>()
        mapObj.keys().forEach { key ->
            val itemArray = mapObj.optJSONArray(key)
            val itemList = mutableListOf<EditCustomItemListResource.Item>()
            for (i in 0 until itemArray.length()) {
                val item = itemArray.getJSONObject(i)

                val itemMap = mutableMapOf<String, Any?>()
                item.keys().forEach {
                    itemMap[it] = item.get(it)
                }
                val customItem = EditCustomItemListResource.Item(itemMap)
                itemList.add(customItem)
            }

            map[key] = itemList
        }
        val version = jsonObj.optInt("version")
        return EditCustomItemListResource(map, version)
    }

    override fun parseEditColorList(json: String): EditColorListResource {
        val jsonObj = JsonParser.parseString(json).asJsonObject

        val map = gson.fromJson<Map<String, List<EditColorListResource.Color>>>(
            jsonObj,
            object : TypeToken<Map<String, List<EditColorListResource.Color>>>() {}.type
        )
        return EditColorListResource(map).let { hookParser.hookEditColorList(json, it) }
    }

    override fun parseEditItemConfigList(json: String): EditItemConfigListResource {
        return gson.fromJson(json, EditItemConfigListResource::class.java)
    }

    override fun parseSceneList(json: String): SceneListResource {
        return gson.fromJson(json, SceneListResource::class.java).let { hookParser.hookSceneList(json, it) }
    }

}