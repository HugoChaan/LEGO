package com.faceunity.app_ptag.edit_cloud

import com.faceunity.app_ptag.edit_cloud.entity.FuUpdateEditDataState
import kotlinx.coroutines.flow.Flow

/**
 * 编辑组件云端配置 的接口
 */
interface IEditCloudControl {

    suspend fun requestEditData(itemListVersion: String): Flow<FuUpdateEditDataState>
}