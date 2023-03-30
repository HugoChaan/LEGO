package com.faceunity.app_ptag.use_case.render_kit

import com.faceunity.app_ptag.FuDI
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.core.entity.FUBundleData
import com.faceunity.core.entity.FUColorRGBData
import com.faceunity.editor_ptag.cache.FuCacheResource
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.store.DevEditRepository
import com.faceunity.editor_ptag.use_case.UseCaseCoroutines
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadBundleUseCase
import com.faceunity.editor_ptag.use_case.edit.ComponentModifyCheckUseCase
import com.faceunity.editor_ptag.use_case.edit.WearBundleUseCase
import com.faceunity.editor_ptag.util.safeCollect
import com.faceunity.fupta.avatar_data.entity.resource.EditColorListResource
import com.faceunity.fupta.avatar_data.parser.IResourceParser
import com.faceunity.pta.pta_core.data_build.FuResourceManager
import com.faceunity.pta.pta_core.util.FuBundleMetaManager
import com.faceunity.pta.pta_core.util.FuDefaultAvatarConfigurator
import com.faceunity.pta.pta_core.util.getBundleMeta
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

/**
 * 随机生成一个 Avatar
 */
class RandomAvatarUseCase(
    private val downloadBundleUseCase: DownloadBundleUseCase,
    private val componentModifyCheckUseCase: ComponentModifyCheckUseCase,
    private val wearBundleUseCase: WearBundleUseCase,
    private val defaultAvatarConfigurator: FuDefaultAvatarConfigurator,
    private val bundleMetaManager: FuBundleMetaManager,
    private val fuResourceManager: FuResourceManager,
    private val configBundle: FUBundleData,
) : UseCaseCoroutines<RandomAvatarUseCase.Params, Avatar>() {


    override suspend fun execute(params: Params): Result<Avatar> {
        val (gender, randomBundleNum, randomFacepupNum, isRandomColor, downloadParser) = params
        val avatar = Avatar(
            configBundle
        )
        val randomBundleList = randomBundleList(gender, randomBundleNum)
        downloadBundleUseCase(randomBundleList).run {
            if (downloadParser != null) {
                downloadParser(this)
            } else {
                safeCollect {}
            }
        }

        val bundleInfo = bundleMetaManager.getBundleMeta(randomBundleList).getOrElse {
            return Result.failure(it)
        }
        val fullList =
            defaultAvatarConfigurator.verifyAvatarJson(bundleInfo, configBundle)!!
//        DevDrawRepository.replaceCurrentAvatar(avatar)
        val checkResult = componentModifyCheckUseCase(ComponentModifyCheckUseCase.Params(avatar, fullList, true)).getOrElse {
            return Result.failure(it)
        }
        checkResult.needDownloadBundle(FuDevDataCenter.resourceManager).takeIf{ it.isNotEmpty() }?.run {
            downloadBundleUseCase(this).run {
                if (downloadParser != null) {
                    downloadParser(this)
                } else {
                    safeCollect {}
                }
            }
        }
        wearBundleUseCase(WearBundleUseCase.Params(avatar, checkResult)).getOrThrow()

        DevEditRepository.setFacepup(avatar, randomFacePup(randomFacepupNum))
        if (isRandomColor) {
            randomColor().forEach { (name, color) ->
                DevEditRepository.setColor(avatar, name, FUColorRGBData(color.r.toDouble(), color.g.toDouble(), color.b.toDouble(), color.intensity?.toDouble() ?: 255.0))
            }
        }

        //绑定动画配置
        fuResourceManager.fastLoadString { appGraphConfig() }
            ?.takeIf { it.isNotBlank() }
            ?.run {
                avatar.animationGraph.setAnimationGraph(this)
            }
        fuResourceManager.fastLoadString { appLogicConfig() }
            ?.takeIf { it.isNotBlank() }
            ?.run {
                avatar.animationGraph.setAnimationLogic(this)
            }

        return Result.success(avatar)
    }

    fun randomBundleList(gender: String, randomNum: Int): MutableList<String> {
        val baseBundleList = if (gender == "male") {
            listOf("GAssets/AsiaMale/head/BaseModelAmale_head.bundle",
                "GAssets/AsiaMale/body/BaseModelAmale_body.bundle")
        } else {
            listOf("GAssets/AsiaFemale/head/basemodelafemale_head.bundle",
                "GAssets/AsiaFemale/body/basemodelafemale_body.bundle")
        }
        val itemList = FuCacheResource.getItemListCacheMap().values.filter {
            if (it.config.tags == null) return@filter false
            val project = it.config.tags!!.project
            listOf("camera", "animation", "light", "ar_hair_mask").forEach {
                if (project.contains(it)) return@filter false
            }
            project.contains(gender)
        }
        val randomBundleList = baseBundleList.toMutableList()
        repeat(randomNum) {
            randomBundleList.add(itemList.random().path)
        }
        return randomBundleList
    }

    fun randomFacePup(randomNum: Int): Map<String, Float> {
        val facePupContainer = DevEditRepository.initFacePupContainer().getOrElse {
            return emptyMap()
        }
        val randomKey = facePupContainer.getAllGroupKey().map { facePupContainer.getAllFacePupKey(it) }.let {
            it.flatten()
        }.let {
            it.shuffled()
        }.let {
            it.subList(0, if (it.lastIndex > randomNum) randomNum else it.lastIndex)
        }
        return randomKey.associate { it to Random.nextFloat() }
    }

    fun randomColor(): List<Pair<String, EditColorListResource.Color>> {
        val editColorListText = FuDevDataCenter.fastLoadString { appEditColorList() }
        val editColorList = FuDI.getCustom<IResourceParser>().parseEditColorList(editColorListText)
        return editColorList.map.map { it.key to it.value.random() }
    }

    data class Params(
        val gender: String,
        val randomBundleNum: Int = 20,
        val randomFacepupNum: Int = 20,
        val isRandomColor: Boolean = true,
        val downloadParser: (suspend Flow<DownloadBundleUseCase.State>.() -> Unit)? = null
    )
}