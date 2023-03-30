package com.faceunity.app_ptag.compat

import com.faceunity.editor_ptag.business.cloud.interfaces.FuZipInterface
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File
import kotlin.concurrent.thread

/**
 * 压缩包解压工具。接入者可更换为自己的实现。
 * use:<https://github.com/srikanth-lingala/zip4j>
 */
class FuZipImpl : FuZipInterface {
    override fun makeZip(folder: File, outFile: File, listener: FuZipInterface.Listener?) {
        try {
            if (outFile.exists()) outFile.delete()
            var progress: ProgressMonitor? = null
            val zipFile = ZipFile(outFile)
            zipFile.apply {
                progress = progressMonitor
                isRunInThread = true
                addFolder(folder)
            }
            syncProgress(progress, listener, outFile)
        } catch (ex: Exception) {
            listener?.onError(ex)
        }
    }

    override fun unZip(file: File, outFolder: File, listener: FuZipInterface.Listener?) {
        try {
            var progress: ProgressMonitor? = null
            var projectID: String? = file.nameWithoutExtension
            ZipFile(file).apply {
                progress = progressMonitor
                isRunInThread = true
                extractAll(outFolder.path)
                this.fileHeaders.filter { it.fileName.split(File.separator).size == 2 && it.isDirectory }.forEach {
                    projectID = it.fileName
                }

            }
            syncProgress(progress, listener, File(outFolder, projectID))
        } catch (ex: Exception) {
            listener?.onError(ex)
        }
    }

    override fun unZipSync(file: File, outFolder: File) {
        ZipFile(file).apply {
            extractAll(outFolder.path)
        }
    }

    private fun syncProgress(progress: ProgressMonitor?, listener: FuZipInterface.Listener? = null, zipFile: File) {
        progress?.let {
            thread {
                listener?.onStart()
                while (it.state != ProgressMonitor.State.READY) {
                    listener?.onProgress(it.percentDone, it.fileName)
                    Thread.sleep(100)
                }
                when(it.result) {
                    ProgressMonitor.Result.SUCCESS -> {
                        listener?.onSuccess(zipFile)
                    }
                    ProgressMonitor.Result.ERROR -> {
                        listener?.onError(it.exception)
                    }
                    ProgressMonitor.Result.CANCELLED -> {
                        listener?.onCancel()
                    }
                }
            }

        }
    }
}