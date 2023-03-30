package com.faceunity.app_ptag.ui.interaction.network.entity

/**
 * 一个将服务器返回的 [InteractionHomePageResult] 转化为符合 App 方便使用的数据结构。
 */
class AnimationConfigExpand(
    val configList: List<Config>
) {

    fun genderConfig(gender: String): Config {
        return configList.first { it.filter["gender"] == gender }
    }

    fun maleConfig(): Config {
        return configList.first { it.filter["gender"] == "male" }
    }

    fun femaleConfig(): Config {
        return configList.first { it.filter["gender"] == "female" }
    }

    data class Config(
        val default: List<Animation>,
        val idleState: List<Animation>,
        val talkState: List<Animation>,
        val filter: Map<String, String>
    )


    data class Animation(
        val name: String,
        val path: String
    )

    companion object {
        fun parse(result: InteractionHomePageResult): AnimationConfigExpand {
            val configList = mutableListOf<Config>()
            result.data?.list?.forEach { item ->
                val animList = mutableListOf<Animation>()
                item.animationList.forEach { anim ->
                    animList.add(Animation(anim.name, anim.path))
                }

                val filter = mutableMapOf<String, String>()
                filter["gender"] = item.filter.gender

                val config = Config(
                    default = item.default.map { animList.first { a -> it.name == a.name } },
                    idleState = item.IdleState.map { animList.first { a -> it.name == a.name } },
                    talkState = item.TalkState.map { animList.first { a -> it.name == a.name } },
                    filter = filter
                )
                configList.add(config)
            }

            return AnimationConfigExpand(configList)
        }
    }
}