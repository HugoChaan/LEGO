package com.faceunity.app_ptag.use_case

import com.faceunity.app_ptag.FuDI
import com.faceunity.app_ptag.use_case.download.BuildAvatarByJsonUseCase
import com.faceunity.app_ptag.use_case.render_kit.*
import com.faceunity.app_ptag.use_case.renderer.PhotoRecordUseCase
import com.faceunity.editor_ptag.cache.FuCacheResource
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.use_case.cloud_platform.*
import com.faceunity.editor_ptag.use_case.edit.ComponentModifyCheckUseCase
import com.faceunity.editor_ptag.use_case.edit.SaveAvatarJsonUseCase
import com.faceunity.editor_ptag.use_case.edit.WearBundleUseCase
import com.faceunity.editor_ptag.use_case.render_kit.CreateAvatarUseCase
import com.faceunity.editor_ptag.use_case.render_kit.DecodeAvatarJsonUseCase
import com.faceunity.editor_ptag.use_case.render_kit.VerifyAvatarFileExistUseCase
import com.faceunity.editor_ptag.use_case.render_kit.VerifySceneFileExistUseCase

/**
 * 一个 UseCase 的构造工厂。
 * 依赖于 [FuDependencyInjection] 的正确初始化，和一些缓存的正确配置。
 * 该类为快速开发使用，接入者可更精细化管理。
 */
object FuUseCaseFactory {
    val downloadAvatarConfigUseCase
        get() = DownloadAvatarConfigUseCase(
            FuDI.getCustom(),
            FuDI.getCustom(),
            FuDI.getCustom()
        )

    val initCloudPlatformUseCase
        get() = InitCloudPlatformUseCase(
            FuDI.getCustom()
        )

    val requestItemListUseCase
        get() = RequestItemListUseCase(
            FuDI.getCustom(),
            FuDI.getCustom()
        )

    val downloadBundleUseCase
        get() = DownloadBundleUseCase(
            FuDI.getCustom(),
            FuDI.getCustom(),
            FuDI.getCustom(),
            FuDI.getCustom(),
            FuDevDataCenter.bundleMetaManager
        )

    val decodeAvatarJsonUseCase
        get() = DecodeAvatarJsonUseCase()

    val createAvatarUseCase
        get() = CreateAvatarUseCase(
            FuDI.getCustom(),
            FuDevDataCenter.defaultAvatarConfigurator!!,
            FuDevDataCenter.bundleMetaManager,
            FuCacheResource.controllerConfigBundle!!
        )

    val filterLostBundleUseCase
        get() = FilterLostBundleUseCase(FuDevDataCenter.getCloudResourceManager())

    val rebuildAvatarUseCase
        get() = RebuildAvatarUseCase(
            decodeAvatarJsonUseCase,
            createAvatarUseCase,
            filterLostBundleUseCase
        )

    val downloadAvatarBundleUseCase
        get() = DownloadAvatarBundleUseCase(
            downloadBundleUseCase,
            FuDevDataCenter.getCloudResourceManager()
        )


    val smartCreateSceneUseCase
        get() = SmartCreateSceneUseCase()


    val verifyAvatarFileExistUseCase
        get() = VerifyAvatarFileExistUseCase(FuDI.getCustom(), FuDI.getCustom())

    val verifySceneFileExistUseCase
        get() = VerifySceneFileExistUseCase(FuDI.getCustom(), FuDI.getCustom())

    val componentModifyCheckUseCase
        get() = ComponentModifyCheckUseCase(
            FuDevDataCenter.resourceManager,
            FuDevDataCenter.bundleMetaManager
        )
    val wearBundleUseCase
        get() = WearBundleUseCase(
            FuDevDataCenter.resourceManager
        )

    val saveAvatarJsonUseCase
        get() = SaveAvatarJsonUseCase(
            FuDevDataCenter.resourceManager
        )

    val randomAvatarUseCase
        get() = RandomAvatarUseCase(
            downloadBundleUseCase,
            componentModifyCheckUseCase,
            wearBundleUseCase,
            FuDevDataCenter.defaultAvatarConfigurator!!,
            FuDevDataCenter.bundleMetaManager,
            FuDevDataCenter.resourceManager,
            FuCacheResource.controllerConfigBundle!!
        )

    val buildAvatarByJsonUseCase
        get() = BuildAvatarByJsonUseCase(
            decodeAvatarJsonUseCase,
            downloadBundleUseCase,
            createAvatarUseCase,
            FuDevDataCenter.getCloudResourceManager()
        )

    val photoRecordUseCase
        get() = PhotoRecordUseCase()

    val shotSceneUseCase
        get() = ShotSceneUseCase(
            photoRecordUseCase
        )

    val switchCameraUseCase
        get() = SwitchCameraUseCase()

    val saveAvatarIconUseCase
        get() = SaveAvatarIconUseCase(
            shotSceneUseCase,
            switchCameraUseCase
        )

    val uploadAvatarConfigUseCase
        get() = UploadAvatarConfigUseCase(
            FuDI.getCustom()
        )
}