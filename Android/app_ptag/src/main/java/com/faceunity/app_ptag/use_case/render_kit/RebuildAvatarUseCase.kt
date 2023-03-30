package com.faceunity.app_ptag.use_case.render_kit

import com.faceunity.editor_ptag.use_case.cloud_platform.FilterLostBundleUseCase
import com.faceunity.editor_ptag.use_case.render_kit.CreateAvatarUseCase
import com.faceunity.editor_ptag.use_case.render_kit.DecodeAvatarJsonUseCase
import com.faceunity.pta.pta_core.model.AvatarInfo

/**
 * 重建 AvatarInfo 中的 Avatar。
 * 为了解决页面切换时，一些状态没有被正确关闭导致的异常。（比如从驱动页回来，没有正确完整地关闭相关配置时，Avatar 可能有效果异常）
 */
class RebuildAvatarUseCase(
    private val decodeAvatarJsonUseCase: DecodeAvatarJsonUseCase,
    private val createAvatarUseCase: CreateAvatarUseCase,
    private val filterLostBundleUseCase: FilterLostBundleUseCase
) {

    operator fun invoke(avatarInfo: AvatarInfo) = execute(avatarInfo)

    fun execute(avatarInfo: AvatarInfo): Result<Unit> {
        try {
            val decodeAvatarInfo =
                decodeAvatarJsonUseCase(DecodeAvatarJsonUseCase.Params(avatarInfo.avatarJson)).getOrThrow()
            val ignoreBundleList = filterLostBundleUseCase(decodeAvatarInfo.fuAvatarInfo.bundles.toList()).getOrThrow()
            val avatar =
                createAvatarUseCase(CreateAvatarUseCase.Params(decodeAvatarInfo, ignoreBundleList.toList())).getOrThrow()
            avatarInfo.updateAvatar(avatar)
            return Result.success(Unit)
        } catch (ex: Throwable) {
            return Result.failure(ex)
        }
    }
}