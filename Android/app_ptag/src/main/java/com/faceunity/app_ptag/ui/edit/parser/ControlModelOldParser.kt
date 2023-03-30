package com.faceunity.app_ptag.ui.edit.parser

import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.ui.edit.entity.control.*
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.fupta.avatar_data.entity.parser.FuFilter
import com.faceunity.fupta.avatar_data.entity.resource.*
import com.faceunity.fupta.cloud_download.parser.IFuAPIParser
import com.faceunity.fupta.facepup.entity.config.FacePupConfig
import com.faceunity.pta.pta_core.repository.EditRepository
import com.faceunity.toolbox.log.FuLogInterface

/**
 * 将原始的编辑配置数据，转为一个符合 Demo 需求的 [ControlModel] 编辑菜单。
 * 接入者应参考其实现，构建符合自己业务需求的编辑菜单。
 * 旧的编辑菜单。将 Bundle 和 Color 分开构建。
 */
class ControlModelOldParser(
    private val fuLog: FuLogInterface
) : EditRepository.FUAvatarEditModelBuilder {
    override fun buildEditModel(
        editConfigResource: EditConfigResource,
        editCustomConfigResource: EditCustomConfigResource,
        editItemListResource: EditItemListResource,
        editCustomItemListResource: EditCustomItemListResource,
        editColorListResource: EditColorListResource,
        editItemConfigListResource: EditItemConfigListResource,
        itemListResource: ItemListResource
    ): Any {
        val controlModel = ControlModel(mutableListOf())
        editConfigResource.getTree().forEach { masterKey ->
            val masterSource = editConfigResource.source.map[masterKey]
            if (masterSource == null) {
                fuLog.error("$masterKey 未配置 source")
                return@forEach
            }
            val masterCategoryModel = buildMasterCategoryModel(masterKey, masterSource)
            controlModel.masterList.add(masterCategoryModel)

            editConfigResource.getMenu(masterKey).forEach { minorKey ->
                val minorSource = editConfigResource.source.map[minorKey]
                if (minorSource == null) {
                    fuLog.error("$masterKey 未配置 source")
                    return@forEach
                }

                val hasConfigMenu = minorKey == "bodyShape"
                if (hasConfigMenu) {
                    val configModelList = mutableListOf<SubCategoryConfigModel>()
                    editItemConfigListResource.bodyShape.forEach {
                        val configModel = buildSubCategoryConfigModel(it)
                        configModelList.add(configModel)
                    }
                    val minorCategoryModel = buildMinorCategoryModel(minorKey, minorSource)
                    masterCategoryModel.minorList.add(minorCategoryModel)
                    minorCategoryModel.subList.addAll(configModelList)
                    return@forEach
                }

                val hasColorMenu = editConfigResource.color_keys.map.containsKey(minorKey)

                if (hasColorMenu) {
                    val colorKey = editConfigResource.color_keys.map[minorKey]?.firstOrNull()
                    if (colorKey == null) {
                        fuLog.error("$minorKey 无法在 edit_config.json 的 color_keys 找到")
                        return@forEach
                    }
                    val colorList = editColorListResource.map[colorKey]
                    if (colorList == null) {
                        fuLog.error("$colorKey 无法在 edit_color_list.json 找到")
                        return@forEach
                    }
                    val colorModelList = mutableListOf<SubCategoryColorModel>()
                    colorList.forEach { colorItem ->
                        val subCategoryColorModel =
                            buildSubCategoryColorModel(colorKey, colorItem)
                        colorModelList.add(subCategoryColorModel)
                    }
                    val minorCategoryModel = buildMinorCategoryModel(minorKey, minorSource)
                    masterCategoryModel.minorList.add(minorCategoryModel)
                    minorCategoryModel.subColorList = colorModelList
                }


                val editItemList = editItemListResource.map[minorKey]
                if (editItemList == null) {
                    fuLog.error("$minorKey 无法在 edit_item_list.json 找到")
                    return@forEach
                }
                if (editItemList.isNotEmpty()) {
                    val minorCategoryModel = buildMinorCategoryModel(minorKey, minorSource)
                    masterCategoryModel.minorList.add(minorCategoryModel)
                    editItemList.forEach { editItem ->
                        val fileId = editItem.path
                        if (fileId == null) {
                            fuLog.error("$editItem 的 path 不能为空")
                            return@forEach
                        }
                        val subCategoryNormalModel =
                            buildSubCategoryNormalModel(fileId, editItem)
                        minorCategoryModel.subList.add(subCategoryNormalModel)
                    }
                }


            }
        }

        return controlModel
    }

    private fun buildMasterCategoryModel(masterKey: String, source: EditConfigResource.Source.SourceItem) : MasterCategoryModel {
        return MasterCategoryModel(
            key = masterKey,
            name = source.name,
            iconPath = parseEditConfigIcon(source.icon),
            selectIconPath = parseEditConfigIcon(source.selected_icon),
            minorList = mutableListOf()
        )
    }

    private fun parseEditConfigIcon(path: String): FuIcon {
        return FuIcon(
            path = path.replace("fu_asset://", "").let {
                FuDevDataCenter.resourceManager.path.appCustom(it)
            },
            FuIcon.Type.File
        )
    }

    private fun buildMinorCategoryModel(minorKey: String, source: EditConfigResource.Source.SourceItem): MinorCategoryModel {
        return MinorCategoryModel(
            key = minorKey,
            name = source.name,
            iconPath = parseEditConfigIcon(source.icon),
            selectIconPath = parseEditConfigIcon(source.selected_icon),
            filter = FuFilter.safeCreate(source.filter),
            subList = mutableListOf(),
            subColorList = null
        )
    }

    private fun buildSubCategoryNormalModel(fileId: String, item: EditItemListResource.Item): SubCategoryBundleModel {
        return SubCategoryBundleModel(
            fileId = fileId,
            iconPath = parseEditItemListIcon(item.icon_url),
            filter = FuFilter.safeCreate(item.filter),
        )
    }

    private fun buildSubCategoryColorModel(colorKey: String, colorItem: EditColorListResource.Color): SubCategoryColorModel {
        return SubCategoryColorModel(
            key = colorKey,
            color = FuColor(colorItem.r, colorItem.g, colorItem.b, colorItem.intensity),
        )
    }

    private fun buildSubCategoryConfigModel(configItem: EditItemConfigListResource.BodyShape): SubCategoryConfigModel {
        val filePath = FuDevDataCenter.resourceManager.path.appBodyShapeConfig(configItem.file)
        return SubCategoryConfigModel(
            name = configItem.name,
            icon = FuIcon(
                path = FuDevDataCenter.resourceManager.path.appBodyShapeIcon(configItem.icon),
                type = FuIcon.Type.File
            ),
            filePath = filePath,
            facePupConfig = FuDevDataCenter.fastLoadString { filePath }.let {
                FuDI.getCustom<IFuAPIParser>().parse(it, FacePupConfig::class.java)
            }.getOrNull()
        )
    }

    private fun parseEditItemListIcon(path: String?): FuIcon {
        if (path == null) return FuIcon("", FuIcon.Type.Null)
        return FuIcon(
            path = path,
            FuIcon.Type.Url
        )
    }
}