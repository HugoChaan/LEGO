package com.faceunity.app_ptag.ui.interaction.tag.parser

import com.faceunity.app_ptag.ui.interaction.tag.FuTagManager
import com.faceunity.app_ptag.ui.interaction.tag.FuTagParser
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.core.entity.FUAnimationBundleData
import com.faceunity.core.entity.FUEmotionBundleData
import com.faceunity.core.faceunity.FUSceneKit
import com.faceunity.editor_ptag.data_center.FuDevDataCenter
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.util.FuLog
import kotlinx.coroutines.*

/**
 *
 */
class FuAnimationTagParser(
    val avatar: Avatar,
    val gender: String
) : FuTagParser() {

    override fun accept(tag: String): Boolean {
        return FuTagManager.tagAnimMap.containsKey(tag)
    }

    override fun parse(tag: String) {
        val animation = FuTagManager.tagAnimMap[tag] ?: return
        val animFileId = animation.pathMap[gender] ?: return
        playAnim(
            FuDevDataCenter.resourceManager.path.ossBundle(animFileId),
            avatar
        ) {
            DevSceneManagerRepository.setAvatarDefaultAnimation(
                avatar,
                DevSceneManagerRepository.filterGenderDefaultSceneFirst(gender)!!.sceneResource
            )
        }
    }


    companion object {

        var currentJob: Job? = null

        fun playAnim(path: String, avatar: Avatar, onPlayEnd: () -> Unit) {
            FUSceneKit.getInstance().executeGLAction({
                avatar.animation.playAnimation(
                    FUAnimationBundleData(
                        path = path,
                        repeatable = false
                    ),
                    false
                )
                currentJob?.cancel()
                currentJob = GlobalScope.launch {
                    withTimeout(20_000) {
                        avatar.animationGraph.setAnimationGraphParam("DefaultAnimNodeProgress", 0f, false)
                        while (isActive) {
                            val progress =
                                avatar.animationGraph.getAnimationGraphParamFloat("DefaultAnimNodeProgress")
                                    ?: return@withTimeout
                            if (progress >= 0.9999) {
                                onPlayEnd()
                                return@withTimeout
                            }
                            delay(100)
                        }
                    }

                }
            })

        }

        fun playEmotionAnim(path: String, avatar: Avatar, onPlayEnd: () -> Unit) {
            val fuEmotionBundleData = FUEmotionBundleData(
                path = path,
                repeatable = false
            )
            FUSceneKit.getInstance().executeGLAction({
                avatar.animation.playAnimation(
                    fuEmotionBundleData,
                    false
                )
                GlobalScope.launch {
                    withTimeout(20_000) {
                        avatar.animationGraph.setAnimationGraphParam("HeadAnimNodeProgress", 0f, false)
                        while (isActive) {
                            val progress =
                                avatar.animationGraph.getAnimationGraphParamFloat("HeadAnimNodeProgress")
                                    ?: return@withTimeout
                            if (progress >= 0.9999) {
                                avatar.animation.removeAnimation(fuEmotionBundleData)
                                onPlayEnd()
                                return@withTimeout
                            }
                            delay(100)
                        }
                    }

                }
            })

        }

        fun cancelAllTask() {
            currentJob?.cancel()
        }
    }
}