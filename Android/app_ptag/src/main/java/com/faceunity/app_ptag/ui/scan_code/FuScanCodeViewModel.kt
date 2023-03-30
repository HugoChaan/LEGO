package com.faceunity.app_ptag.ui.scan_code

import androidx.lifecycle.ViewModel
import com.faceunity.app_ptag.use_case.FuUseCaseFactory
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadAvatarConfigUseCase

/**
 *
 */
class FuScanCodeViewModel: ViewModel() {

    private val downloadAvatarConfigUseCase by lazy {
        FuUseCaseFactory.downloadAvatarConfigUseCase
    }

    fun avatarIdIsExist(avatarId: String): Boolean {
        return DevAvatarManagerRepository.getAvatarInfo(avatarId) != null
    }

    suspend fun downloadAvatarConfig(avatarId: String): Result<Unit> {
        return downloadAvatarConfigUseCase(DownloadAvatarConfigUseCase.Params(avatarId)).onSuccess {
            val newAvatarInfo =
                DevAvatarManagerRepository.createAvatarInfoUseConfig(avatarId)
            if (newAvatarInfo == null) {
                return Result.failure(Throwable("Avatar 配置读取异常"))
            }
            DevAvatarManagerRepository.addAvatarAndSwitch(newAvatarInfo)
        }
    }
}