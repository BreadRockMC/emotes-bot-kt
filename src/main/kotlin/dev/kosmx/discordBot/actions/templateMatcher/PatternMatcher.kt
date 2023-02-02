package dev.kosmx.discordBot.actions.templateMatcher

import dev.kosmx.discordBot.BotEventHandler
import dev.kosmx.discordBot.EventResult
import dev.kosmx.discordBot.actions.IdentifiableInteractionHandler
import dev.kosmx.discordBot.command.SlashCommand
import dev.kosmx.discordBot.command.SlashOptionType
import dev.kosmx.discordBot.maintainEvent
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
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

            val contents = FileDownloader.getContent(event)
            for (pattern in patterns) {
                if (!pattern.onlyOnFile && pattern.regex.containsMatchIn(event.message.contentRaw) ||
                    contents.find { pattern.regex.containsMatchIn(it) } != null
                ) {
                    event.message.reply(pattern.message).queue()
                    return@event EventResult.CONSUME
                }
            }
            EventResult.PASS
        }

        bot.ownerServerCommands += SlashCommand("reloadAutoReply".lowercase(), "Reloads the auto reply config", {
            defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)
        }) {
            patterns.clear()
            patterns.addAll(File("patterns.json").inputStream().use { input ->
                Json.decodeFromStream(input)
            })
            it.reply("Config reloaded").queue()
        }

        bot.ownerServerCommands += object :
            SlashCommand("addAutoReply".lowercase(), "Set a regex and add the replied message to auto-reply database", configure = {
                defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)
            }) {
            val pattern = option("pattern", "regex pattern", SlashOptionType.STRING)
            val msg = option("reply", "reply, you'll be able to edit it in a modal", SlashOptionType.STRING)
            val attachment = option("attachment", "optional attachment file", SlashOptionType.ATTACHMENT)
            val onlyOnFile by option("onFile".lowercase(), "Only match log files, default=TRUE", SlashOptionType.BOOLEAN).default(true)

            override fun invoke(event: SlashCommandInteractionEvent) {
                event[attachment]?.let {
                    attachmentMap[event.id] = Clock.System.now() to (it.fileName to it.proxy.download().get())
                }
                Modal.create("autoreply:${event.id}:$onlyOnFile", "Auto reply").apply {
                    addActionRow(TextInput.create("msg", "Reply message", TextInputStyle.PARAGRAPH).also {
                        it.value = event[msg]
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
            val onlyOnFile = event.modalId.split(":")[2].toBoolean()
            val attachment = attachmentMap[id]?.second
            val regex = event.getValue("regex")!!.asString
            val msg = event.getValue("msg")!!.asString
            patterns += Pattern(regex, msg, attachment?.second?.let { Base64.getEncoder().encodeToString(it.readAllBytes()) }, attachment?.first, onlyOnFile)
            File("patterns.json").outputStream().use { json.encodeToStream(patterns, it) }
            event.reply("Success!").setEphemeral(true).queue()
        }

        bot.maintainEvent += attachmentMap.maintainEvent()
    }
}

@Serializable
class Pattern(private val pattern: String, private val response: String, private val base64Embed: String?, private val embedName: String?, val onlyOnFile: Boolean = true) {
    @Transient
    val regex: Regex = Regex(pattern, option = RegexOption.MULTILINE)

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
