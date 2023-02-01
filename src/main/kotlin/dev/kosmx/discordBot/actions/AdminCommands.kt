package dev.kosmx.discordBot.actions

import dev.kosmx.discordBot.BotEventHandler
import dev.kosmx.discordBot.command.SlashCommand
import dev.kosmx.discordBot.command.SlashOptionType
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

fun initAdminCommands(bot: BotEventHandler) {

    bot.ownerServerCommands += object : SlashCommand("msg", "message user or channel") {
        val message = option("message", "message", SlashOptionType.STRING).required()
        val user = option("user", "user to send message", SlashOptionType.USER)
        val channel = option("channel", "channel to send message", SlashOptionType.CHANNEL)
        val attachment =
            option("attachment", "optional attachment to send", SlashOptionType.ATTACHMENT)

        override fun invoke(event: SlashCommandInteractionEvent) {
            if (event.member?.hasPermission(Permission.MODERATE_MEMBERS) != true) {
                return
            }
            val user = event[this.user]
            val channel = event[this.channel]
            if ((user != null) && (channel != null)) {
                event.reply("You can only send message to either user or channel, but not both").queue()
                return
            }


            val sendMsg: (MessageChannel) -> Unit = { ch ->
                MessageCreateBuilder().setContent(event[message]).also { message ->
                    event[attachment]?.let {
                        it.proxy.download().whenComplete { file, _ ->
                            message.setFiles(FileUpload.fromData(file, it.fileName).apply {
                                description = it.description
                            })
                            ch.sendMessage(message.build()).queue()
                        }
                    } ?: run { ch.sendMessage(message.build()).queue() }
                }
            }

            (user?.openPrivateChannel()?.queue(sendMsg)

                ?: channel?.takeIf { channel.type.isMessage }?.let {
                    channel.asTextChannel().apply(sendMsg)
                })
                ?.let {
                    event.reply("Message sent to <${
                        user?.let { "@${it.id}" } ?: "#${channel?.id}"
                    }>;\n${event[message]}").queue()
                }
                ?: event.reply("Can't send message, no valid target").setEphemeral(true).queue()
        }
    }

    @file:Suppress("DuplicatedCode")
    bot.ownerServerCommands += object : SlashCommand("setMailTo".lowercase(), "Sets the mailbot function endpoint to specified channel") {
        val channel = option("channel", description = "channel", SlashOptionType.CHANNEL).required()

        override fun invoke(event: SlashCommandInteractionEvent) {
            if (event.member?.hasPermission(Permission.ADMINISTRATOR) != true) {
                return
            }
            bot.config.mailChannel = event[channel].idLong.toULong()
            bot.config.save()
            event.reply("Saved!").queue()
        }
    }

    @file:Suppress("DuplicatedCode")
    bot.ownerServerCommands += object : SlashCommand("setEmoteTo".lowercase(), "Sets emote upload output to the specified channel") {
        val channel = option("channel", description = "channel", SlashOptionType.CHANNEL).required()

        override fun invoke(event: SlashCommandInteractionEvent) {
            if (event.member?.hasPermission(Permission.ADMINISTRATOR) != true) {
                return
            }
            bot.config.emoteChannel = event[channel].idLong.toULong()
            bot.config.save()
            event.reply("Saved!").queue()
        }
    }

    bot.ownerServerCommands += object : SlashCommand("setCommandSequence".lowercase(), "Sets the command char") {
        val sequence = option("sequence", description = "start sequence, normally special chars", SlashOptionType.STRING).required()

        override fun invoke(event: SlashCommandInteractionEvent) {
            if (event.member?.hasPermission(Permission.ADMINISTRATOR) != true) {
                return
            }
            bot.config.commandStart = event[sequence].trim()
            bot.config.save()
            event.reply("Command set to `${bot.config.commandStart}`.").queue()
        }
    }
}