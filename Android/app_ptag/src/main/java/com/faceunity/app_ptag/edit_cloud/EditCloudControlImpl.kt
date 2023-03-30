package com.faceunity.app_ptag.edit_cloud

import com.faceunity.app_ptag.edit_cloud.entity.FuCentreConfig
import com.faceunity.app_ptag.edit_cloud.entity.FuUpdateEditDataState
import com.faceunity.editor_ptag.business.cloud.interfaces.FuZipInterface
import com.faceunity.editor_ptag.parser.IFuJsonParser
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.pta.pta_core.data_build.FuResourceManager
import com.faceunity.pta.pta_core.interfaces.FuStorageFieldInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * ps：因为需求变动，scene_list.json 不再从该接口获取，而使用 App assets 内置的。
 */
class EditCloudControlImpl(
    val editCloudRepository: EditCloudRepository,
    val fuStorageFieldInterface: FuStorageFieldInterface,
    val jsonParser: IFuJsonParser,
    val resourceManager: FuResourceManager,
    val zip: FuZipInterface
) : IEditCloudControl {
    private val FileKey = "FuCentreConfig"
    private val VersionKey = "FuCentreConfigVersion"

    /**
     * Demo 的版本控制。因为一些奇怪的原因不能直接传版本号，直接写死，视情况更新。
     */
    private val UiVersion = "1.6.0"

    override suspend fun requestEditData(itemListVersion: String) : Flow<FuUpdateEditDataState> {
        return flow {
            emit(FuUpdateEditDataState.Start)
            editCloudRepository.centreConfig(itemListVersion, UiVersion).onSuccess {
                if (it.code != 0) {
                    emit(FuUpdateEditDataState.CloudFailure(Throwable("api code:${it.code},message:${it.message}")))
                    return@onSuccess
                } else if (it.data == null) {
                    emit(FuUpdateEditDataState.CloudFailure(Throwable("api data == null")))
                    return@onSuccess
                }
                val cacheString = fuStorageFieldInterface.getAsString(FileKey)
                val oldVersion = fuStorageFieldInterface.getAsString(VersionKey)
                val oldConfig = if (cacheString.isNotBlank()) {
                    jsonParser.parse(cacheString, FuCentreConfig::class.java)
                } else {
                    null
                }

                val updateList = if (oldVersion != itemListVersion || hasFileNotExist()) { //有文件不存在，或者版本不一致，则全量更新
                    getUpdateResource(it)
                } else {
                    checkUpdate(it, oldConfig)
                }
                if (updateList.isEmpty()) {
                    emit(FuUpdateEditDataState.NotNeedUpdate)
                    return@onSuccess
                }
                var isSuccess = true
                updateList.forEach { item ->
                    when(item.type) {
                        CentreConfigType.EditItemList -> {
                            editCloudRepository.loadContent(item.url).runCatching {
                                resourceManager.fastWriteString(this.getOrThrow()) { appEditItemList() }
                            }.onSuccess {
                                emit(FuUpdateEditDataState.NotifyUpdateSuccess(item.type.name))
                            }.onFailure {
                                emit(FuUpdateEditDataState.NotifyUpdateFailure(item.type.name, it))
                                isSuccess = false
                            }
                        }
                        CentreConfigType.ColorList -> {
                            editCloudRepository.loadContent(item.url).runCatching {
                                resourceManager.fastWriteString(this.getOrThrow()) { appEditColorList() }
                            }.onSuccess {
                                emit(FuUpdateEditDataState.NotifyUpdateSuccess(item.type.name))
                            }.onFailure {
                                emit(FuUpdateEditDataState.NotifyUpdateFailure(item.type.name, it))
                                isSuccess = false
                            }
                        }
                        CentreConfigType.EditConfig -> {
                            editCloudRepository.loadContent(item.url).runCatching {
                                resourceManager.fastWriteString(this.getOrThrow()) { appEditConfig() }
                            }.onSuccess {
                                emit(FuUpdateEditDataState.NotifyUpdateSuccess(item.type.name))
                            }.onFailure {
                                emit(FuUpdateEditDataState.NotifyUpdateFailure(item.type.name, it))
                                isSuccess = false
                            }
                        }
                        CentreConfigType.EditItemConfig -> {
                            editCloudRepository.loadContent(item.url).runCatching {
                                resourceManager.fastWriteString(this.getOrThrow()) { appEditItemConfigList() }
                            }.onSuccess {
                                emit(FuUpdateEditDataState.NotifyUpdateSuccess(item.type.name))
                            }.onFailure {
                                emit(FuUpdateEditDataState.NotifyUpdateFailure(item.type.name, it))
                                isSuccess = false
                            }
                        }
//                        CentreConfigType.SceneList -> {
//                            editCloudRepository.loadContent(item.url).runCatching {
//                                resourceManager.fastWriteString(this.getOrThrow()) { appSceneList() }
//                            }.onSuccess {
//                                emit(FuUpdateEditDataState.NotifyUpdateSuccess(item.type.name))
//                            }.onFailure {
//                                emit(FuUpdateEditDataState.NotifyUpdateFailure(item.type.name, it))
//                                isSuccess = false
//                            }
//                        }
                        CentreConfigType.Assets -> {
                            val zipFile = File(resourceManager.resourcePath.appAssets, "Assets.zip")
                            editCloudRepository.downloadFile(item.url, zipFile).mapCatching { file ->
                                zip.unZipSync(file, file.parentFile!!)
                            }.onSuccess {
                                emit(FuUpdateEditDataState.NotifyUpdateSuccess(item.type.name))
                            }.onFailure {
                                emit(FuUpdateEditDataState.NotifyUpdateFailure(item.type.name, it))
                                isSuccess = false
                            }
                        }
                    }
                }
                if (isSuccess) {
                    fuStorageFieldInterface.set(FileKey, jsonParser.toJson(it))
                    fuStorageFieldInterface.set(VersionKey, itemListVersion)
                }

                emit(FuUpdateEditDataState.Finish())

            }.onFailure {
                emit(FuUpdateEditDataState.CloudFailure(it))
            }
        }.catch {
            emit(FuUpdateEditDataState.Failure(it))
        }.flowOn(Dispatchers.IO)
    }

    private fun checkUpdate(centreConfig: FuCentreConfig, oldConfig: FuCentreConfig?) : List<UpdateResource> {
        if (oldConfig == null || oldConfig.data == null) return getUpdateResource(centreConfig)
        val updateList = mutableListOf<UpdateResource>()
        val oldData: FuCentreConfig.Data = oldConfig.data
        centreConfig.data?.run {
            FuLog.debug("editItemListTimestamp:${editItemListTimestamp},old:${oldData.editItemListTimestamp}")
            if (editItemListTimestamp != oldData.editItemListTimestamp) {
                updateList.add(UpdateResource(CentreConfigType.EditItemList, editItemListPath))
            }
            if (colorListTimestamp != oldData.colorListTimestamp) {
                updateList.add(UpdateResource(CentreConfigType.ColorList, colorListPath))
            }
            if (editConfigTimestamp != oldData.editConfigTimestamp) {
                updateList.add(UpdateResource(CentreConfigType.EditConfig, editConfigPath))
            }
            if (editItemConfigListTimestamp != oldData.editItemConfigListTimestamp && editItemConfigListPath != null) {
                updateList.add(UpdateResource(CentreConfigType.EditItemConfig, editItemConfigListPath))
            }
//            if (sceneListTimestamp != oldData.sceneListTimestamp) {
//                updateList.add(UpdateResource(CentreConfigType.SceneList, sceneListPath))
//            }
            if (assetsTimestamp != oldData.assetsTimestamp) {
                updateList.add(UpdateResource(CentreConfigType.Assets, assetsPath))
            }
        }
        return updateList
    }

    private fun getUpdateResource(centreConfig: FuCentreConfig) : List<UpdateResource> {
        val updateList = mutableListOf<UpdateResource>()
        centreConfig.data?.run {
            updateList.add(UpdateResource(CentreConfigType.EditItemList, editItemListPath))
            updateList.add(UpdateResource(CentreConfigType.ColorList, colorListPath))
            updateList.add(UpdateResource(CentreConfigType.EditConfig, editConfigPath))
            if (editItemConfigListPath != null) {
                updateList.add(UpdateResource(CentreConfigType.EditItemConfig, editItemConfigListPath))
            }
//            updateList.add(UpdateResource(CentreConfigType.SceneList, sceneListPath))
            updateList.add(UpdateResource(CentreConfigType.Assets, assetsPath))
        }
        return updateList
    }

    /**
     * 有文件不存在的情况下，全量下载
     */
    private fun hasFileNotExist() : Boolean {
        val isAllExist = resourceManager.fastIsExist { appEditItemList() }
                && resourceManager.fastIsExist { appEditColorList() }
                && resourceManager.fastIsExist { appEditConfig() }
                && resourceManager.fastIsExist { appSceneList() }
        return !isAllExist
    }

    private data class UpdateResource(val type: CentreConfigType, val url: String)

    private enum class CentreConfigType {
        EditItemList, ColorList, EditConfig, SceneList, Assets, EditItemConfig
    }
}