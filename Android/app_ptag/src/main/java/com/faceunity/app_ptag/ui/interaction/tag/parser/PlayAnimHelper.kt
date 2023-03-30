package com.faceunity.app_ptag.ui.interaction.tag.parser

import com.faceunity.app_ptag.view_model.helper.Block
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.core.faceunity.FUSceneKit
import com.faceunity.editor_ptag.util.FuLog
import kotlinx.coroutines.*

/**
 * 一个相比 [FuAnimationTagParser] 更完善的动画播放监听。
 * 暂未完成对旧代码的替换。
 */
object PlayAnimHelper {
    val Tag = "PlayAnimHelper"

    var currentAnimJob: AnimJob? = null

    /**
     * 播放一个动画，它会对相同的任务忽略，对之前的任务如果没执行完会取消。
     * 接入者可视实际需求自定义其实现。
     */
    fun play(animJob: AnimJob) {
        print("try play Job{tag:${animJob.tag};type:${animJob.type}}")
        //如果新加入的任务是当前任务，则跳过该任务的执行
        if (animJob.tag != null && animJob.tag == currentAnimJob?.tag) {
            print("same Job,break.")
            return
        }
        //对当前任务进行取消
        if (currentAnimJob != null) {
            print("cancel currentJob, run endFun().")
        }
        currentAnimJob?.endFun?.invoke()
        currentAnimJob = animJob
        playTask(Task(
            avatar = animJob.avatar,
            onTaskStart = {
                print("${animJob.tag} onTaskStart()")
                animJob.playFun()
            },
            onPlayFinish = {
                print("${animJob.tag} onPlayFinish()")
                animJob.endFun()
            },
            onTaskFinish = {
                print("${animJob.tag} onTaskFinish()")
                currentAnimJob = null
            }
        ))
    }

    private fun playTask(task: Task) {
        GlobalScope.launch {
            try {
                task.onTaskStart()
                FUSceneKit.getInstance().executeGLAction({
                    task.avatar.animationGraph.setAnimationGraphParam("DefaultAnimNodeProgress", 0f, false)
                })
                var isLoop = true
                while (isActive && isLoop) {
                    FUSceneKit.getInstance().executeGLAction({
                        val progress =
                            task.avatar.animationGraph.getAnimationGraphParamFloat("DefaultAnimNodeProgress")
                        if (progress == null) {
                            isLoop = false
                            return@executeGLAction
                        }
                        if (progress >= 0.9999) {
                            task.onPlayFinish()
                            isLoop = false
                            return@executeGLAction
                        }
                    })

                    delay(100)
                }
            } finally {
                task.onTaskFinish()
            }

        }
    }

    private data class Task(
        val avatar: Avatar,
        val onTaskStart: Block,
        val onPlayFinish: Block,
        val onTaskFinish: Block
    )

    data class AnimJob(
        val playFun: () -> Unit,
        val endFun: () -> Unit = {},
        val avatar: Avatar,
        val tag: String? = null,
        val type: String? = null
    )

    private inline fun print(msg: String) {
        FuLog.warn("$Tag $msg")
    }
}