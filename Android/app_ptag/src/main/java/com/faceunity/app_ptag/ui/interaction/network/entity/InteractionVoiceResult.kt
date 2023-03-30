package com.faceunity.app_ptag.ui.interaction.network.entity

data class InteractionVoiceResult(
    val code: Int,
    val `data`: Data?,
    val message: String
) {
    data class Data(
        val defaultFemaleVoice: String,
        val defaultMaleVoice: String,
        val voiceList: List<Voice>
    ) {
        data class Voice(
            val format: String,
            val id: String,
            val name: String,
            val supplier: String
        )
    }
}