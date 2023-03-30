package com.faceunity.app_ptag.use_case.render_kit

import com.faceunity.app_ptag.FuDI
import com.faceunity.core.avatar.model.Scene
import com.faceunity.editor_ptag.cache.FuCacheResource
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.use_case.render_kit.CreateSceneUseCase
import com.faceunity.editor_ptag.use_case.render_kit.VerifySceneFileExistUseCase

/**
 * 快捷创建一个 Scene 并使用。它会绑定 scene_list.json 的相关配置。
 * 依赖：FuCacheResource 的缓存；DevSceneManagerRepository 的初始化；对应配置的 Bundle 已存在。
 * 返回：是否成功创建并应用。
 */
class SmartCreateSceneUseCase {


    operator fun invoke() : Scene? = execute()

    /**
     * @param custom 自定义 Scene 配置。
     * @return 成功返回对应 Scene，失败返回 null。
     */
    fun execute() : Scene? {
        if (FuCacheResource.controllerConfigBundle == null || FuCacheResource.itemListText == null) {
            return null
        }
        val createSceneUseCase = CreateSceneUseCase()
        val scene = createSceneUseCase(CreateSceneUseCase.Params()).getOrThrow()

        val sceneInfo = DevSceneManagerRepository.getDefaultOrNull()
        sceneInfo?.run {
            DevSceneManagerRepository.setSceneConfig(scene, sceneInfo.sceneResource)
        }
        val verifySceneFileExistUseCase = VerifySceneFileExistUseCase(
            FuDI.getCustom(),
            FuDI.getCustom()
        )
        val verifyResult = verifySceneFileExistUseCase(VerifySceneFileExistUseCase.Params(scene)).getOrElse {
            return null
        }
        if (verifyResult.needDownloadFileIdList.isNotEmpty()) {
            return null
        }
        return scene
    }
}