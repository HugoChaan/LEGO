package com.faceunity.app_ptag.ui.edit.expand.download

import com.faceunity.app_ptag.ui.edit.expand.download.entity.DownloadTask
import com.faceunity.app_ptag.ui.edit.expand.download.entity.DownloadTaskContainer

/**
 * 下载任务的调度器。它分为 group 和 task 两个概念。目前的策略是同一个 group 里只会执行最后设置的 task 的 onAction，被取消的 task 会调用 onCancel。
 */
class DownloadDispatcher {

    private val taskContainer = DownloadTaskContainer()

    /**
     * 添加一个任务，该任务耗时，当执行完成后会回调[DownloadTask.onAction]，但该回调可能因为任务调度取消执行。
     */
    fun addTask(groupId: String, taskId: String, task: DownloadTask) {
        taskContainer.removeByGroupId(groupId)
        taskContainer.put(groupId, taskId, task)
    }

    fun useTask(groupId: String, taskId: String): DownloadTask? {
        val task = taskContainer.get(groupId, taskId)?.second
        if (task != null) {
            taskContainer.remove(groupId, taskId)
            return task
        }
        return null
    }

    /**
     * 触发对应任务的回调。
     */
    fun triggerTask(groupId: String, taskId: String) {
        taskContainer.get(groupId, taskId)?.second?.onAction()
        taskContainer.remove(groupId, taskId)
    }

    /**
     * 取消指定的任务。
     */
    fun cancelTask(groupId: String, taskId: String) {
        taskContainer.remove(groupId, taskId)
    }

    fun cancelGroupTask(groupId: String) {
        taskContainer.removeByGroupId(groupId)
    }

    /**
     * 取消全部的任务。
     */
    fun cancelAllTask() {
        taskContainer.clear()
    }

    /**
     * 打印任务数据。调试用。
     */
    fun printTask(): String {
        return taskContainer.print()
    }

}