package com.faceunity.app_ptag.compat

import android.content.Context
import com.faceunity.editor_ptag.parser.IFuJsonParser
import com.faceunity.fupta.cloud_download.SaveInterface
import com.faceunity.fupta.cloud_download.entity.CloudResourceContainer
import java.io.File

/**
 *
 */
class CloudSaveInterface(
    context: Context,
    space: String,
    val fuJsonParser: IFuJsonParser
) : SaveInterface {
    val file = File(context.getExternalFilesDir(null), "${space.run { if (isBlank()) "" else "/$this" }}/CloudResourceContainer.json")

    override fun load(): CloudResourceContainer {
        try {
            val text = file.readText()
            return fuJsonParser.parse(text, CloudResourceContainer::class.java)
        } catch (ex: Exception) {
            return CloudResourceContainer()
        }

    }

    override fun save(cloudResourceContainer: CloudResourceContainer) {
        val json = fuJsonParser.toJson(cloudResourceContainer)
        file.writeText(json)
    }
}