package com.faceunity.app_ptag.edit_cloud.entity

data class FuCentreConfig(
    val code: Int,
    val `data`: Data?,
    val message: String
) {
    data class Data(
        val assetsPath: String,
        val assetsTimestamp: Long?,
        val colorListPath: String,
        val colorListTimestamp: Long?,
        val editConfigPath: String,
        val editConfigTimestamp: Long?,
        val editItemListPath: String,
        val editItemListTimestamp: Long?,
        val sceneListPath: String?,
        val sceneListTimestamp: Long?,
        val editItemConfigListPath: String?,
        val editItemConfigListTimestamp: Long?
    )
}