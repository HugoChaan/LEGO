package com.faceunity.app_ptag.use_case.download

import com.faceunity.core.avatar.model.Avatar
import com.faceunity.editor_ptag.use_case.UseCaseCoroutines
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadBundleUseCase
import com.faceunity.editor_ptag.use_case.render_kit.CreateAvatarUseCase
import com.faceunity.editor_ptag.use_case.render_kit.DecodeAvatarJsonUseCase
import com.faceunity.editor_ptag.util.FuResourceCheck
import com.faceunity.fupta.cloud_download.CloudResourceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last

/**
 * 解析 avatar.json，并对相关资源下载，构造出一个 Avatar。
 * - 对于下架的资源，会进行忽略处理；
 * - 仅根据 avatar.json 构造 Avatar，不包含动画；
 */
class BuildAvatarByJsonUseCase(
    private val decodeAvatarJsonUseCase: DecodeAvatarJsonUseCase,
    private val downloadBundleUseCase: DownloadBundleUseCase,
    private val createAvatarUseCase: CreateAvatarUseCase,
    private val cloudResourceManager: CloudResourceManager
): UseCaseCoroutines<BuildAvatarByJsonUseCase.Params, Avatar>() {


    override suspend fun execute(params: Params): Result<Avatar> {
        val (avatarJson, downloadParser) = params
        //解析 avatar.json，转为一个结构体。
        val decodeAvatarInfo = decodeAvatarJsonUseCase(
            DecodeAvatarJsonUseCase.Params(avatarJson)
        ).getOrElse {
            return Result.failure(it)
        }
        /** 忽略的资源。用于当下架了某些资源时，将这部分资源当作不存在来构造 Avatar。 */
        val ignoreBundleList = mutableListOf<String>()
        val checkBundleList = decodeAvatarInfo.fuAvatarInfo.bundles
        val bundleStatus =
            FuResourceCheck.checkBundleStatus(checkBundleList.toList(), cloudResourceManager)
        if (bundleStatus.isNotPrepare) { //如果有资源需要下载
            val downloadState = downloadBundleUseCase(bundleStatus.allDownload).let {
                downloadParser?.invoke(it) ?: it.last()
            }
            //如果下载失败，当它是因为资源下架导致的，则进行一次重试。
            if (!downloadState.isSuccess) {
                //下载未成功时，如果是开始下载失败（即：下载信息找不到，资源不存在或下架），将这部分资源加入忽略列表。
                if (downloadState is DownloadBundleUseCase.State.StartFailed) {
                    ignoreBundleList.addAll(downloadState.lostFileIdList)
                    //对忽略掉的这些 Bundle 刷新状态，标记为丢失。
                    FuResourceCheck.updateBundleStatus(ignoreBundleList, cloudResourceManager)
                    //忽略掉下架资源后再尝试下一次其他资源。
                    val retryBundleList = bundleStatus.allDownload.toMutableList().apply {
                        removeAll(ignoreBundleList)
                    }
                    downloadBundleUseCase(retryBundleList).let {
                        downloadParser?.invoke(it) ?: it.last()
                    }.let {
                        if (!it.isSuccess) return Result.failure(Throwable("download failed:$it"))
                    }
                } else {
                    //遇到下载失败直接抛出异常。
                    return Result.failure(Throwable("download failed:$downloadState"))
                }
            }
        }

        //构造一个 Avatar。
        val avatar = createAvatarUseCase(
            CreateAvatarUseCase.Params(decodeAvatarInfo, ignoreBundleList)
        ).getOrElse {
            return Result.failure(it)
        }
        return Result.success(avatar)
    }

    data class Params(
        /**
         * Avatar 对应的 json 配置文件
         */
        val avatarJson: String,
        /**
         * 解析下载进度的回调。可选。
         */
        val downloadParser: (suspend Flow<DownloadBundleUseCase.State>.() -> DownloadBundleUseCase.State)? = null
    )
}