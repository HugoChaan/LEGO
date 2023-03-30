package com.faceunity.app_ptag.ui.home.dev

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.faceunity.app_ptag.ui.edit.entity.control.SubCategoryBundleModel
import com.faceunity.app_ptag.view_model.FuAvatarManagerViewModel
import com.faceunity.app_ptag.weight.DownloadingDialog
import com.faceunity.app_ptag.weight.dev_setting.entity.DevSetting
import com.faceunity.fupta.cloud_download.entity.CloudConfig

/**
 * 用于内部调试的一些额外逻辑。故抽象成接口，对外版本中不包含相关实现。
 */
interface IDevBuilder {
    /**
     * 是否开启调试模式
     */
    fun enableDevConfig(): Boolean

    fun buildHomeDevMenu(context: Context): List<DevSetting>
    fun buildTempTest(): List<DevSetting>

    /**
     * 得到指定的云端配置
     */
    fun getDefaultCloudConfig(): CloudConfig?

    /**
     * 添加调试资源逻辑
     */
    fun hookCheckBundle()

    /**
     * 是否显示帧率
     */
    fun isShowFps(): Boolean?

    /**
     * 是否不限制帧率
     */
    fun isNotLimitFps(): Boolean?

    /**
     * 显示道具的调试信息
     */
    fun showItemInfoDialog(model: SubCategoryBundleModel, context: Context): Boolean?

    /**
     * 显示形象的调试信息
     */
    fun showAvatarInfoDialog(avatarId: String, context: Context): Boolean?

    /**
     * 生成一个随机形象
     */
    fun buildRandomAvatar(
        lifecycleScope: LifecycleCoroutineScope,
        context: Context,
        downloadingDialog: DownloadingDialog,
        fuAvatarManagerViewModel: FuAvatarManagerViewModel
    )
}

/**
 * 调试功能的默认实例
 */
object IDevBuilderInstance: IDevBuilder by EmptyDevBuilder()