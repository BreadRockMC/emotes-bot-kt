package dev.kosmx.discordBot

import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val token: String,
    val botName: String = "Emotes bot"
)
