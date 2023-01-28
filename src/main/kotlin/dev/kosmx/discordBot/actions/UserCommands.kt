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
            val embed = EmbedBuilder()
            embed.setAuthor(event.interaction.member?.effectiveName)
            embed.setColor(Color(15105570))
            embed.setDescription(event[message])
            event.interaction.replyEmbeds(embed.build()).queue()
        }
    }
}