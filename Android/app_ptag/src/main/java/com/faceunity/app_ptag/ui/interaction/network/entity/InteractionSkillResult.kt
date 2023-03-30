package com.faceunity.app_ptag.ui.interaction.network.entity

data class InteractionSkillResult(
    val code: Int,
    val `data`: Data,
    val message: String
) {
    data class Data(
        val nlp: Nlp,
        val recommendSkills: List<RecommendSkill>,
        val scenes: List<Scene>,
        val skills: List<Skill>
    ) {
        data class Nlp(
            val defaultScene: String,
            val nlpRobotID: String,
            val nlpSupplier: String
        )

        data class RecommendSkill(
            val id: String,
            val name: String
        )

        data class Scene(
            val id: String,
            val scene: String
        )

        data class Skill(
            val list: List<Item>,
            val scene: String
        ) {
            data class Item(
                val id: String,
                val name: String
            )
        }
    }
}