package com.faceunity.app_ptag.ui.edit.expand.download.entity

/**
 * 以 groupId 和 taskId 为 key 的一个 Map。
 */
class DownloadTaskContainer {
    private val taskMap = mutableMapOf<Key, DownloadTask>()

    fun put(groupId: String, taskId: String, task: DownloadTask) {
        val findTask = get(groupId, taskId)
        if (findTask == null) {
            taskMap.put(Key(groupId, taskId), task)
        } else {
            findTask.second.onCancel()
            taskMap[findTask.first] = task
        }
    }

    fun contains(groupId: String, taskId: String): Boolean {
        return taskMap.filter { it.key.groupId == groupId && it.key.taskId == taskId }.isNotEmpty()
    }

    fun get(groupId: String, taskId: String): Pair<Key, DownloadTask>? {
        return taskMap.filter { it.key.groupId == groupId && it.key.taskId == taskId }.toList().firstOrNull()
    }

    fun getByGroupId(groupId: String): List<DownloadTask> {
        return taskMap.filter { it.key.groupId == groupId }.map { it.value }
    }

    fun getByTaskId(taskId: String): DownloadTask? {
        return taskMap.filter { it.key.taskId == taskId }.map { it.value }.firstOrNull()
    }

    fun remove(groupId: String, taskId: String) {
        taskMap.filter { it.key.groupId == groupId && it.key.taskId == taskId }.forEach {
            it.value.onCancel()
            taskMap.remove(it.key)
        }
    }

    fun removeByGroupId(groupId: String) {
        taskMap.filter { it.key.groupId == groupId }.forEach {
            it.value.onCancel()
            taskMap.remove(it.key)
        }
    }

    fun removeByTaskId(taskId: String) {
        taskMap.filter { it.key.taskId == taskId }.forEach {
            it.value.onCancel()
            taskMap.remove(it.key)
        }
    }

    fun clear() {
        taskMap.forEach {
            it.value.onCancel()
        }
        taskMap.clear()
    }

    fun print(): String {
        return taskMap.keys.toString()
    }

    data class Key(
        val groupId: String,
        val taskId: String
    )
}