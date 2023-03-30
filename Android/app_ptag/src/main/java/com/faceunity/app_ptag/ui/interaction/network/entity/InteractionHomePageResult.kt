package com.faceunity.app_ptag.ui.interaction.network.entity

data class InteractionHomePageResult(
    val code: Int,
    val `data`: Data?,
    val message: String
) {
    data class Data(
        val list: List<Item>
    ) {
        data class Item(
            val IdleState: List<IdleStateClass>,
            val ListenState: List<Any>,
            val TalkState: List<TalkStateClass>,
            val animationList: List<Animation>,
            val default: List<Default>,
            val filter: Filter,
            val homePage: HomePage,
            val touch: List<Any>
        ) {
            data class IdleStateClass(
                val name: String
            )

            data class TalkStateClass(
                val name: String
            )

            data class Animation(
                val name: String,
                val path: String
            )

            data class Default(
                val name: String
            )

            data class Filter(
                val gender: String
            )

            data class HomePage(
                val firstAppear: List<FirstAppear>,
                val normalAppear: List<NormalAppear>
            ) {
                data class FirstAppear(
                    val aName: String,
                    val text: String
                )

                data class NormalAppear(
                    val aName: String,
                    val text: String
                )
            }
        }
    }
}