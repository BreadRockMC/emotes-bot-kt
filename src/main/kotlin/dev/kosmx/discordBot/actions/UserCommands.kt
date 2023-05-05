package dev.kosmx.discordBot.actions

import dev.kosmx.discordBot.BotEventHandler
import dev.kosmx.discordBot.command.SlashCommand
import dev.kosmx.discordBot.command.SlashOptionType
import dev.kosmx.discordBot.getTextChannelById
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.jsoup.Jsoup
import java.awt.Color

fun initUserCommands(bot: BotEventHandler) {

    bot.ownerServerCommands += object : SlashCommand("logs", "How do I send logs?") {
        override fun invoke(event: SlashCommandInteractionEvent) {
            event.interaction.reply(bot.config.logLink).queue()
        }
    }


    bot.ownerServerCommands += object : SlashCommand("postemote", "Send an emote embed to the channel") {
        val emoteTitle = option("title", "title", SlashOptionType.STRING).required()
        val jsonFile = option("jsonfile", "jsonFile", SlashOptionType.ATTACHMENT).required()
        val image = option("image", "image", SlashOptionType.ATTACHMENT)
        val emoteDescription = option("description", "description", SlashOptionType.STRING)

        override fun invoke(event: SlashCommandInteractionEvent) {
            val fileStream = event[jsonFile].proxy.download().get()
            try {
                val anim = AnimationSerializing.deserializeAnimation(fileStream)
                //if deserialization fails, the uploaded JSON is invalid
                //TODO: get relevant data of emote (e.g.: name) from the JSON file
            }
            catch (e: Exception) {
                event.reply("The JSON you uploaded is invalid!").setEphemeral(true).queue()
                return
            }
            val embed = EmbedBuilder().apply {
                setAuthor(event.interaction.member?.effectiveName)
                setColor(Color.decode(bot.config.embedColor))
                setTitle(event[emoteTitle], event[jsonFile].url)
                setDescription(event[emoteDescription])
                setFooter("Click the title to download!")
                setImage(event[image]?.url)
            }
            val message = event.replyEmbeds(embed.build()).setEphemeral(true)
            message.addActionRow(
                Button.primary("postemote", "Post #here"),
                Button.success("uploademote", "Post in #emote-list"),
                Button.danger("cancelmodal", "Cancel")
            ).queue()
        }
    }

    bot.ownerServerCommands += object : SlashCommand("postEmoteUrl".lowercase(), "Send an emote embed to the channel, using emotes.kosmx.dev url") {
        val url = option("url", "url", SlashOptionType.STRING).required()

        override fun invoke(event: SlashCommandInteractionEvent) {
            val pattern = Regex("^(http(s)?://)?emotes.kosmx.dev/e(motes)?/(\\d+)(/)?\$")
            val emoteId: Int? = pattern.find(event[url])?.groupValues?.get(4)?.toInt()
            if (emoteId != null) {
                val response =
                    Jsoup.connect("https://emotes.kosmx.dev/e/$emoteId").userAgent("emotes-bot.kt").ignoreHttpErrors(true).execute()

                val doc = response.takeIf { it.statusCode() == 200 }?.parse()
                    ?: kotlin.run { event.reply("can't access emote").setEphemeral(true).queue(); return@invoke }

                val meta = doc.getElementsByTag("meta")
                val properties = meta.fold(object {
                    var author: String? = null
                    var name: String? = null
                    var description: String? = null
                    var iconUrl: String? = null
                }) { acc, element ->
                    when(element.attr("property")) {
                        "og:image" -> acc.iconUrl = element.attr("abs:content")
                        "og:description" -> acc.description = element.attr("content")
                        "og:title" -> acc.name = element.attr("content")
                    }
                    if (element.attr("name") == "author") acc.author = element.attr("content")
                    return@fold acc
                }

                if (properties.name != null && properties.author != null && properties.description != null) {
                    val emoteUrl = "https://emotes.kosmx.dev/e/${emoteId}/bin"

                    val embed = EmbedBuilder().apply {
                        setAuthor(event.interaction.member?.effectiveName)
                        setColor(Color.decode(bot.config.embedColor))
                        setTitle(properties.name, emoteUrl)
                        setDescription(properties.description)
                        setFooter("Click the title to download!")
                        setImage(properties.iconUrl)
                    }
                    val message = event.replyEmbeds(embed.build()).setEphemeral(true)
                    message.addActionRow(
                        Button.primary("postemote", "Post #here"),
                        Button.success("uploademote", "Post in #emote-list"),
                        Button.danger("cancelmodal", "Cancel")
                    ).queue()
                } else {
                    event.reply("something went wrong").setEphemeral(true).queue()
                }
            }
        }
    }

    bot.buttonEvents += IdentifiableInteractionHandler("postemote") { event ->
        event.run {
            val origEmbed = interaction.message.embeds[0]
            interaction.editComponents().queue()
            interaction.hook.editOriginal("Sent to channel.").queue()
            channel.asTextChannel().sendMessageEmbeds(origEmbed).queue()
        }
    }
    bot.buttonEvents += IdentifiableInteractionHandler("uploademote") { event ->
        event.run {
            val origEmbed = interaction.message.embeds[0]
            interaction.editComponents().queue()
            interaction.hook.editOriginal("Uploaded.").queue()
            val target = event.jda.getTextChannelById(bot.config.emoteChannel)

            //TODO: upload logic
            target?.sendMessageEmbeds(origEmbed)?.queue()
        }
    }
    bot.buttonEvents += IdentifiableInteractionHandler("cancelmodal") { event ->
        event.interaction.editComponents().queue()
    }
}