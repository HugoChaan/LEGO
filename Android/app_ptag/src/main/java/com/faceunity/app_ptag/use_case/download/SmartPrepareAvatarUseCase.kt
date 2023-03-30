package com.faceunity.app_ptag.use_case.download

import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.use_case.FuUseCaseFactory
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadBundleUseCase
import com.faceunity.editor_ptag.util.FuResourceCheck
import com.faceunity.pta.pta_core.model.AvatarInfo
import kotlinx.coroutines.flow.Flow

/**
 * 分析 Avatar 的状态，在 avatar.json 就绪的情况下，对需要下载的 Bundle 进行下载，构造出一个可以显示的 Avatar，并将其更新至 AvatarInfo
 */
class SmartPrepareAvatarUseCase {

    companion object {
        suspend operator fun invoke(
            avatarId: String,
            downloadParser: suspend Flow<DownloadBundleUseCase.State>.() -> DownloadBundleUseCase.State
        ) = SmartPrepareAvatarUseCase().executeNow(avatarId, downloadParser)
    }

    private val buildAvatarByJsonUseCase = FuUseCaseFactory.buildAvatarByJsonUseCase

    private val decodeAvatarJsonUseCase = FuUseCaseFactory.decodeAvatarJsonUseCase
    private val downloadBundleUseCase = FuUseCaseFactory.downloadBundleUseCase
    private val createAvatarUseCase = FuUseCaseFactory.createAvatarUseCase

    suspend fun executeNow(
        avatarId: String,
        downloadParser: suspend Flow<DownloadBundleUseCase.State>.() -> DownloadBundleUseCase.State
    ): Result<AvatarInfo> {
        return try {
            execute(avatarId, downloadParser)
        } catch (ex: Throwable) {
            Result.failure(ex)
        }
    }

    suspend fun execute(
        avatarId: String,
        downloadParser: suspend Flow<DownloadBundleUseCase.State>.() -> DownloadBundleUseCase.State
    ): Result<AvatarInfo> {
        val avatarInfo = DevAvatarManagerRepository.getAvatarInfo(avatarId)
        if (avatarInfo == null) return Result.failure(IllegalArgumentException("AvatarManager not find avatar id $avatarId."))
        if (avatarInfo.state == AvatarInfo.State.PrepareId) {
            //如果 avatar.json 还未下载则抛出异常。下载 avatar.json 不是该类的职责。
            return Result.failure(IllegalStateException("$avatarId should download avatar.json first"))
        }
        if (avatarInfo.state == AvatarInfo.State.PrepareConfig) {
            //如果该 AvatarInfo 还未构造 Avatar，则构造。
            buildAvatar(
                avatarInfo.avatarJson,
                avatarInfo.gender(),
                downloadParser
            ).onSuccess {
                avatarInfo.prepareAvatar(it)
            }.onFailure {
                return Result.failure(it)
            }

        }

        //Avatar 所用到的资源状态
        val avatarBundleStatus =
            FuResourceCheck.checkAvatarBundleStatus(avatarInfo.avatar, FuDI.getCustom())
        //Scene 所用到的资源状态
        val sceneBundleStatus =
            DevSceneManagerRepository.filterGenderDefaultSceneFirst(avatarInfo.gender())?.let {
                it.getUsedBundleFileIdList() + it.getAllAnimation().map { it.path }
            }?.let {
                FuResourceCheck.checkBundleStatus(it, FuDI.getCustom())
            }
        val bundleStatus = avatarBundleStatus.merge(sceneBundleStatus)
        if (bundleStatus.isNotPrepare) { //有资源未就绪则下载相关资源。
            val downloadFlow = downloadBundleUseCase(bundleStatus.allDownload)
            val downloadState = downloadParser(downloadFlow).also {
                if (it is DownloadBundleUseCase.State.StartFailed) { //如果有下架资源，则重试一次
                    downloadParser(downloadBundleUseCase(bundleStatus.allDownload - it.lostFileIdList))
                } else it
            }
            if (!downloadState.isSuccess) {
                if (downloadState is DownloadBundleUseCase.State.StartFailed) { //在数据都存在已经加载好的时候，Avatar 会已构建，而它缺失的话，就需要重新 Build
                    buildAvatar(
                        avatarInfo.avatarJson,
                        avatarInfo.gender(),
                        downloadParser
                    ).onSuccess {
                        avatarInfo.prepareAvatar(it)
                    }.onFailure {
                        return Result.failure(it)
                    }
                } else {
                    return Result.failure(Throwable("download failed:$downloadState"))
                }
            }
        }
        return Result.success(avatarInfo)
    }

    /**
     * 根据 avatar.json，生成一个 Avatar。
     */
    private suspend fun buildAvatar(
        avatarJson: String,
        gender: String,
        downloadParser: suspend Flow<DownloadBundleUseCase.State>.() -> DownloadBundleUseCase.State
    ): Result<Avatar> {
        return buildAvatarByJsonUseCase(BuildAvatarByJsonUseCase.Params(
            avatarJson = avatarJson,
            downloadParser = downloadParser
        )).onSuccess {
            //给 Avatar 绑上动画。
            DevSceneManagerRepository.filterGenderDefaultSceneFirst(gender)?.run {
                DevSceneManagerRepository.setSceneConfig(it, sceneResource)
            }
        }
    }

    /**
     * 对应 SmartPrepareAvatarUseCase 的流程注释
     */
    private val 加载形象流程 = """
        val avatarInfo = getAvatarInfo()
        when(avatarInfo.state) {
            只有 ID -> 异常状态不处理
            只有 avatar.json -> {
                //需要先获取 avatar.json bundle_list 里道具对应的 meta 信息，才能正确创建出 Avatar。
                val decodeInfo = 解析avatar.json
                if(decodeInfo.bundleList需要下载) {
                    下载对应bundleList
                }
                val avatar = 创建Avatar(decodeInfo)
                绑定Avatar对应的动画
                avatarInfo.state = Avatar就绪
                执行Avatar就绪后的逻辑
            }
            Avatar就绪 -> {
                //判断 Avatar 和 Scene 的资源就绪状态，将其一并下载。
                //当Avatar存在可选更新时，目前直接更新后显示。如果需要预显示则自行修改相关逻辑。
                if(Avatar的Bundle需要下载) {
                    下载对应Bundle
                }
                显示Avatar
            }
        }
    """.trimIndent()
}