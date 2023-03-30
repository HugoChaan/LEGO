package com.faceunity.app_ptag.compat

import com.faceunity.download.FUDownloadKit
import com.faceunity.download.entity.FUDownloadSource
import com.faceunity.download.infe.OnFUDownloadListener
import com.faceunity.editor_ptag.business.cloud.download.IDownloadControl
import com.faceunity.editor_ptag.util.FuElapsedTimeChecker
import java.io.File

/**
 * 使用相芯提供的下载工具库实现 Bundle 下载。接入者可更换为自己的实现。
 */
class FuDownloadHelperImpl : IDownloadControl {

    init {
        FUDownloadKit.getInstance().apply {
            setLogEnable(false)
            setMaxDownloadChannel(5) //设置最大并发下载数量
        }
    }

    override fun downloadGroup(
        downloadSourceList: List<IDownloadControl.DownloadSource>,
        groupListener: IDownloadControl.GroupDownloadListener
    ) {
        FuElapsedTimeChecker.start("downloadGroup|${downloadSourceList.map { it.url }}")
        val successSourceList = mutableListOf<IDownloadControl.DownloadSource>()
        val errorSourceList = mutableListOf<IDownloadControl.DownloadSource>()
        downloadSourceList.forEach {
            FUDownloadKit.getInstance().addTask(toFuDownloadSource(it), object : OnFUDownloadListener {
                override fun onCanceled(taskId: Long, downloadBytes: Long, totalBytes: Long) {
                    groupListener.onTaskError(it)
                    errorSourceList.add(it)
                }

                override fun onError(taskId: Long, httpStatus: Int, errorCode: Int, msg: String?) {
                    groupListener.onTaskError(it)
                    errorSourceList.add(it)
                    groupListener.onError("code:$errorCode,msg:$msg")
                }

                override fun onFinished(taskId: Long, file: File) {
                    groupListener.onTaskSuccess(it)
                    successSourceList.add(it)
                    groupListener.onProgress(successSourceList.size, downloadSourceList.size)
                    if (successSourceList.size + errorSourceList.size == downloadSourceList.size) {
                        FuElapsedTimeChecker.end("downloadGroup|${downloadSourceList.map { it.url }}")
                        if (errorSourceList.size == 0) {
                            groupListener.onSuccessFinished()
                        } else {
                            groupListener.onErrorFinished(errorSourceList)
                        }
                    }
                }

                override fun onPrepared(taskId: Long) {

                }

                override fun onProgress(taskId: Long, downloadBytes: Long, totalBytes: Long) {
                   }

                override fun onStart(taskId: Long, totalBytes: Long) {
                    groupListener.onTaskStart(it)
                }
            })
        }
        groupListener.onProgress(successSourceList.size, downloadSourceList.size)
    }

    override fun downloadSingle(
        downloadSource: IDownloadControl.DownloadSource,
        singleListener: IDownloadControl.SingleDownloadListener
    ) {
        FuElapsedTimeChecker.start("downloadSingle|${downloadSource.url}")
        FUDownloadKit.getInstance().addTask(toFuDownloadSource(downloadSource), object : OnFUDownloadListener {
            override fun onCanceled(taskId: Long, downloadBytes: Long, totalBytes: Long) {
            }

            override fun onError(taskId: Long, httpStatus: Int, errorCode: Int, msg: String?) {
                singleListener.onError(msg)
            }

            override fun onFinished(taskId: Long, file: File) {
                FuElapsedTimeChecker.end("downloadSingle|${downloadSource.url}")
                singleListener.onFinished()
            }

            override fun onPrepared(taskId: Long) {
            }

            override fun onProgress(taskId: Long, downloadBytes: Long, totalBytes: Long) {
                singleListener.onProgress(downloadBytes, totalBytes)
            }

            override fun onStart(taskId: Long, totalBytes: Long) {
            }
        })
    }

    override fun cancelTask(downloadSourceList: List<IDownloadControl.DownloadSource>) {
        downloadSourceList.mapNotNull { sourceMap[it] }.map { it.roomTaskId }.map {
            FUDownloadKit.getInstance().cancelTask(it)
        }
    }

    override fun cancelAllTask() {
        FUDownloadKit.getInstance().cancelAllTask()
        sourceMap.clear()
    }

    private val sourceMap = mutableMapOf<IDownloadControl.DownloadSource, FUDownloadSource>()

    private fun toFuDownloadSource(downloadSource: IDownloadControl.DownloadSource): FUDownloadSource {
        val fuDownloadSource = FUDownloadSource(downloadSource.url, downloadSource.savePath)
        sourceMap[downloadSource] = fuDownloadSource
        return fuDownloadSource
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789abcdef".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        var j = 0
        var v: Int
        while (j < bytes.size) {
            v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            j++
        }
        return String(hexChars)
    }
}