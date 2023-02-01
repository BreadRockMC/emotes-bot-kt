package dev.kosmx.discordBot.actions.templateMatcher

import dev.kosmx.discordBot.BotEventHandler
import dev.kosmx.discordBot.EventResult
import dev.kosmx.discordBot.actions.IdentifiableInteractionHandler
import dev.kosmx.discordBot.command.SlashCommand
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.time.Duration.Companion.minutes

typealias AttachmentConfig = Pair<String, InputStream>

object PatternMatcher {
    @OptIn(ExperimentalSerializationApi::class)
    val patterns: MutableList<Pattern> = File("patterns.json").takeIf { it.isFile }?.inputStream()?.use { input ->
        Json.decodeFromStream(input)
    } ?: mutableListOf()

    val attachmentMap = mutableMapOf<String, Pair<Instant, AttachmentConfig>>()

    private val json = Json { prettyPrint = true }

    @OptIn(ExperimentalSerializationApi::class)
    operator fun invoke(bot: BotEventHandler) {
        bot.messageReceivedEvent[60] = event@{ event ->
            if (event.author.id == event.jda.selfUser.id) return@event EventResult.PASS
            for (pattern in patterns) {
                if (pattern.regex.containsMatchIn(event.message.contentRaw)) {
                    event.message.reply(pattern.message).queue()
                    return@event EventResult.CONSUME
                }
            }
            EventResult.PASS
        }

        bot.ownerServerCommands += SlashCommand("reloadAutoReply".lowercase(), "Reloads the auto reply config") {
            patterns.clear()
            patterns.addAll(File("patterns.json").inputStream().use { input ->
                Json.decodeFromStream(input)
            })
            it.reply("Config reloaded").queue()
        }

        bot.ownerServerCommands += object :
            SlashCommand("addAutoReply".lowercase(), "Set a regex and add the replied message to auto-reply database") {
            val pattern = option("pattern", "regex pattern", OptionType.STRING, OptionMapping::getAsString)
            val msg = option(
                "reply",
                "reply, you'll be able to edit it in a modal",
                OptionType.STRING,
                OptionMapping::getAsString
            )
            val attachment =
                option("attachment", "optional attachment file", OptionType.ATTACHMENT, OptionMapping::getAsAttachment)

            override fun invoke(event: SlashCommandInteractionEvent) {
                event[attachment]?.let { attachmentMap[event.id] = Clock.System.now() to ( it.fileName to it.proxy.download().get()) }
                Modal.create("autoreply:${event.id}", "Auto reply").apply {
                    addActionRow(TextInput.create("msg", "Reply message", TextInputStyle.PARAGRAPH).also {
                        it.placeholder = event[msg]
                        //it.setRequiredRange()
                    }.build())
                    addActionRow(
                        TextInput.create("regex", "Matcher regex", TextInputStyle.SHORT)
                            .also { it.placeholder = event[pattern] }.build()
                    ).build()
                        .let { event.replyModal(it).queue() }
                }
            }
        }

        bot.modalEvents += IdentifiableInteractionHandler("autoreply") { event ->
            val id = event.modalId.split(":")[1]
            val attachment = attachmentMap[id]?.second
            val regex = event.getValue("regex")!!.asString
            val msg = event.getValue("msg")!!.asString
            patterns += Pattern(regex, msg, attachment?.second?.let { Base64.getEncoder().encodeToString(it.readAllBytes()) }, attachment?.first)
            File("patterns.json").outputStream().use { json.encodeToStream(patterns, it) }
            event.reply("Success!").setEphemeral(true).queue()
        }

        bot.maintainEvent += object : () -> Unit {
            var lastMaintained = Clock.System.now()

            override fun invoke() {
                if (lastMaintained + 5.minutes < Clock.System.now()) {
                    val now = Clock.System.now()
                    lastMaintained = now
                    attachmentMap.entries.removeIf { (_, pair) -> pair.first + 15.minutes > now }
                }
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
