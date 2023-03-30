package com.faceunity.app_ptag.ui.interaction.tag

import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.ui.interaction.entity.TagConfig
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.parser.IFuJsonParser

/**
 *
 */
object FuTagManager {

    val tagConfig: TagConfig by lazy {
        FuDevDataCenter.fastLoadString { appTagConfig() }.let {
            FuDI.getCustom<IFuJsonParser>().parse(it, TagConfig::class.java)
        }
    }

    val tagAnimMap: Map<String, TagConfig.Animation> by lazy {
        tagConfig.animation.associate { it.tag to it }
    }

    private val parserList = mutableListOf<FuTagParser>()

    fun addParser(parser: FuTagParser) {
        parserList.add(parser)
    }

    fun removeParser(parser: FuTagParser) {
        parserList.remove(parser)
    }

    fun clearParser() {
        parserList.clear()
    }

    fun action(tag: String) {
        parserList.forEach {
            actionParser(tag, it)
        }
    }

    private fun actionParser(tag: String, parser: FuTagParser) {
        if (parser.accept(tag)) {
            parser.parse(tag)
        }
    }

}