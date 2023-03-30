package com.faceunity.app_ptag.ui.home.dev

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryBundleModel
import com.faceunity.app_ptag.view_model.FuAvatarManagerViewModel
import com.faceunity.app_ptag.weight.DownloadingDialog
import com.faceunity.app_ptag.weight.dev_setting.entity.DevSetting
import com.faceunity.fupta.cloud_download.entity.CloudConfig

/**
 * 空的实现
 */
class EmptyDevBuilder : IDevBuilder {
    override fun enableDevConfig(): Boolean = false

    override fun buildHomeDevMenu(context: Context): List<DevSetting> = emptyList()

    override fun buildTempTest(): List<DevSetting> = emptyList()

    override fun getDefaultCloudConfig(): CloudConfig? = null

    override fun hookCheckBundle() = Unit

    override fun isShowFps(): Boolean? = null

    override fun isNotLimitFps(): Boolean? = null

    override fun showItemInfoDialog(model: SubCategoryBundleModel, context: Context): Boolean? = null

    override fun showAvatarInfoDialog(avatarId: String, context: Context): Boolean? = null

    override fun buildRandomAvatar(
        lifecycleScope: LifecycleCoroutineScope,
        context: Context,
        downloadingDialog: DownloadingDialog,
        fuAvatarManagerViewModel: FuAvatarManagerViewModel
    ) = Unit
}