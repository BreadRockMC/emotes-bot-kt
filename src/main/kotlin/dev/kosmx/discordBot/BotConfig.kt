package dev.kosmx.discordBot

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.File

@Serializable
data class BotConfig(
    val token: String,
    val botName: String = "Emotes bot",
    val initActivity: String = "with the old bot",
    val ownerServers: Set<ULong>,
    var mailChannel: ULong,
    var commandStart: String = "?",
    val botAdmins: Set<ULong>
) {

    @Transient
    private val json = Json { prettyPrint = true }

    @OptIn(ExperimentalSerializationApi::class)
    fun save() {
        File("bot.config.json").outputStream().use { output ->
            json.encodeToStream(this, output)
        }
    }
}