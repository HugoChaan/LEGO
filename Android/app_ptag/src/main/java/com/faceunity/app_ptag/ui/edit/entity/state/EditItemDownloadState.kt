package com.faceunity.app_ptag.ui.edit.entity.state

/**
 *
 */
sealed class EditItemDownloadState(val fileIdList: List<String>) {
    class Start(fileIdList: List<String>) : EditItemDownloadState(fileIdList)
    class Success(fileIdList: List<String>) : EditItemDownloadState(fileIdList)
    class Error(fileIdList: List<String>) : EditItemDownloadState(fileIdList)
    companion object {
        fun Default() = Success(emptyList())
    }
}
