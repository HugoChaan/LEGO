package com.faceunity.app_ptag.ui.edit.expand.download.entity

/**
 *
 */
interface DownloadTask {
    fun onAction()

    fun onCancel()

    companion object {
        fun empty() = object : DownloadTask {
            override fun onAction() {

            }

            override fun onCancel() {

            }

        }
    }
}