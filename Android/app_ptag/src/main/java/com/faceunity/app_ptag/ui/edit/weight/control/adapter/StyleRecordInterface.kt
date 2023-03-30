package com.faceunity.app_ptag.ui.edit.weight.control.adapter

/**
 *
 */
interface StyleRecordInterface {

    fun downloadStyleSafe(fileId: String?) : DownloadStyle {
        if (fileId == null) return DownloadStyle.Normal
        return downloadStyle(fileId)
    }

    fun downloadStyle(fileId: String) : DownloadStyle

    fun notifyDownloadStart(fileId: String)

    fun notifyDownloadSuccess(fileId: String)

    fun notifyDownloadError(fileId: String)

    fun notifyDownloadStart(fileIdList: List<String>) {
        fileIdList.forEach { notifyDownloadStart(it) }
    }

    fun notifyDownloadSuccess(fileIdList: List<String>) {
        fileIdList.forEach { notifyDownloadSuccess(it) }
    }

    fun notifyDownloadError(fileIdList: List<String>) {
        fileIdList.forEach { notifyDownloadError(it) }
    }

    enum class DownloadStyle {
        Normal,
        NeedDownload,
        Downloading,
        Error
    }
}