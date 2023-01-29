package dev.kosmx.discordBot.actions.templateMatcher

import dev.kosmx.discordBot.BotEventHandler
import dev.kosmx.discordBot.EventResult
import dev.kosmx.discordBot.command.SlashCommand
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.*

object PatternMatcher {
    @OptIn(ExperimentalSerializationApi::class)
    val patterns: List<Pattern> = File("patterns.json").inputStream().use { input ->
        Json.decodeFromStream(input)
    }


    operator fun invoke(bot: BotEventHandler) {
        bot.messageReceivedEvent[60] = event@{ event ->
            for (pattern in patterns) {
                if (pattern.regex.matches(event.message.contentRaw)) {
                    event.message.reply(pattern.message).queue()
                    return@event EventResult.CONSUME
                }
            }
            EventResult.PASS
        }

        bot.ownerServerCommands += object : SlashCommand("addAutoReply".lowercase(), "Set a regex and add the replied message to auto-reply database") {
            val pattern = option("pattern", "regex pattern", OptionType.STRING, OptionMapping::getAsString).required()

            override fun invoke(event: SlashCommandInteractionEvent) {
                //if (event )
            }
        }
    }
}

@Serializable
class Pattern(private val pattern: String, private val response: String, private val base64Embed: String?, private val embedName: String?) {
    @Transient
    val regex: Regex = Regex(pattern)

    private val embed: InputStream? by lazy {
        base64Embed?.let {
            ByteArrayInputStream(Base64.getDecoder().decode(it))
        }
    }

    val message: MessageCreateData
        get() = MessageCreateBuilder().apply {
            setContent(response)
            embed?.let { setFiles(FileUpload.fromData(it, embedName!!)) }
        }.build()
}
