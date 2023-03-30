package com.faceunity.app_ptag.use_case.render_kit

import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.pta.pta_core.model.AvatarInfo

/**
 * 检查该 Avatar 对应的完整状态
 */
class SmartCheckAvatarStateUseCase {

    companion object {
        operator fun invoke(avatarId: String) : AvatarState = SmartCheckAvatarStateUseCase().execute(avatarId)
    }

    operator fun invoke(avatarId: String) : AvatarState = execute(avatarId)

    fun execute(avatarId: String) : AvatarState {
        val avatarInfo =
            DevAvatarManagerRepository.getAvatarInfo(avatarId)
        if (avatarInfo == null) {
            //TODO 将来可用再从本地文件查询一次该 id 是否存在，完善健壮性。
            return AvatarState.LackDir
        }
        val resourceManager = FuDevDataCenter.resourceManager
        if (avatarInfo.state == AvatarInfo.State.PrepareId) {
            //检查是否存在 avatar.json，如果存在则刷新状态
            if (resourceManager.loader.isExist(avatarInfo.avatarJsonPath)) {
                val isFlushSuccess =
                    DevAvatarManagerRepository.flushAvatarInfoUseConfig(avatarInfo)
                if (isFlushSuccess) {
//                    return AvatarState.Normal
                } else {
                    return AvatarState.LackJson
                }
            } else {
                return AvatarState.LackJson
            }
        }
        if (avatarInfo.state == AvatarInfo.State.PrepareConfig) {
            if (!resourceManager.loader.isExist(avatarInfo.avatarLogoPath)) {
                return AvatarState.LackIcon
            }
        }
        return AvatarState.Normal
    }

    sealed class AvatarState {
        /**
         * 本地不存在对应文件夹。通常为导入的形象。
         */
        object LackDir : AvatarState()

        /**
         * 本地不存在对应的 avatar.json。为初始化或者导入的形象。
         */
        object LackJson : AvatarState()

        /**
         * 本地不存在对应的 avatar.png。
         */
        object LackIcon: AvatarState()

        /**
         * 该 ID 的文件夹、json、icon 就绪。
         */
        object Normal: AvatarState()
    }
}