package dev.kosmx.discordBot.actions

import dev.kosmx.discordBot.BotEventHandler
import dev.kosmx.discordBot.command.SlashCommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.awt.Color

fun initUserCommands(bot: BotEventHandler) {

    bot.ownerServerCommands += object : SlashCommand("test", "testing testing 123") {
        val message = option("message", "message", OptionType.STRING, OptionMapping::getAsString).required()

        override fun invoke(event: SlashCommandInteractionEvent) {
            val embed = EmbedBuilder().apply {
                setAuthor(event.interaction.member?.effectiveName)
                setColor(Color.getColor(bot.config.embedColor))
                setDescription(event[message])
            }
            event.interaction.replyEmbeds(embed.build()).queue()
        }
    }

    bot.ownerServerCommands += object : SlashCommand("logs", "How do I send logs?") {
        override fun invoke(event: SlashCommandInteractionEvent) {
            event.interaction.reply(bot.config.logLink).queue()
        }
    }

    bot.ownerServerCommands += object : SlashCommand("postemote", "Send an emote embed to the channel") {
        val emoteTitle = option("title", "title", OptionType.STRING, OptionMapping::getAsString).required()
        val jsonFile = option("jsonfile", "jsonFile", OptionType.ATTACHMENT, OptionMapping::getAsAttachment).required()
        val image = option("image", "image", OptionType.ATTACHMENT, OptionMapping::getAsAttachment)
        val emoteDescription = option("description", "description", OptionType.STRING, OptionMapping::getAsString)

        override fun invoke(event: SlashCommandInteractionEvent) {
            val embed = EmbedBuilder().apply {
                setAuthor(event.interaction.member?.effectiveName)
                setColor(Color.decode(bot.config.embedColor))
                setTitle(event[emoteTitle], event[jsonFile].url)
                setDescription(event[emoteDescription])
                setFooter("Click the title to download!")
                setImage(event[image]?.url)
            }
            event.replyEmbeds(embed.build()).queue()
        }
    }
}