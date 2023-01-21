package dev.kosmx.discordBot.actions

import dev.kosmx.discordBot.BotEventHandler
import dev.kosmx.discordBot.command.SlashCommand
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

fun initAdminCommands(bot: BotEventHandler) {

    bot.ownerServerCommands += object : SlashCommand("msg", "message user or channel") {
        val message = option("message", "message", OptionType.STRING, OptionMapping::getAsString).required()
        val user = option("user", "user to send message", OptionType.USER, OptionMapping::getAsUser)
        val channel = option("channel", "channel to send message", OptionType.CHANNEL, OptionMapping::getAsChannel)
        val attachment =
            option("attachment", "optional attachment to send", OptionType.ATTACHMENT, OptionMapping::getAsAttachment)

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

    bot.ownerServerCommands += object : SlashCommand("setMailTo".lowercase(), "Sets the mailbot function endpoint to specified channel") {
        val channel = option("channel", description = "channel", OptionType.CHANNEL, OptionMapping::getAsChannel).required()

        override fun invoke(event: SlashCommandInteractionEvent) {
            if (event.member?.hasPermission(Permission.ADMINISTRATOR) != true) {
                return
            }
            bot.config.mailChannel = event[channel].idLong.toULong()
            bot.config.save()
            event.reply("Saved!").queue()
        }
    }

    bot.ownerServerCommands += object : SlashCommand("setCommandSequence".lowercase(), "Sets the command char") {
        val sequence = option("sequence", description = "start sequence, normally special chars", OptionType.STRING, OptionMapping::getAsString).required()

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