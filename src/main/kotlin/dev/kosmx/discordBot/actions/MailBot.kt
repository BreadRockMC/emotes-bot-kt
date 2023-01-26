package dev.kosmx.discordBot.actions

import dev.kosmx.discordBot.BotEventHandler
import dev.kosmx.discordBot.EventResult
import dev.kosmx.discordBot.getTextChannelById
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder

object MailBot {

    private val cache: MutableList<CacheEntry> = mutableListOf()
    private data class CacheEntry(val source: Long, val targetChannel: Long, val targetMessage: Long, val incoming: Boolean)

    private fun cache(entry: CacheEntry) {
        cache.add(0, entry)
        if (cache.size > 40) {
            cache.removeLast()
        }
    }

    private fun messageFromFormatted(content: String, authorId: String, noteSource: Boolean): String =
        if (noteSource) {
            """
                Message from <@$authorId>;
                $content
            """.trimIndent()
        } else content

    private fun sendMail(message: Message, targetChannel: MessageChannel, noteSource: Boolean = false): EventResult {

        MessageCreateBuilder().apply {
            setContent(messageFromFormatted(message.contentRaw, message.author.id, noteSource))

            message.attachments.map { attachment ->
                val data = attachment.proxy.download().get()
                FileUpload.fromData(data, attachment.fileName).apply {
                    description = attachment.description
                    if (attachment.isSpoiler) asSpoiler()
                }
            }.let {
                setFiles(it)
            }
        }.build().let {
            targetChannel.sendMessage(it).queue { sentMessage ->
                cache(CacheEntry(message.idLong, targetChannel.idLong, sentMessage.idLong, noteSource))
            }
            message.addReaction(Emoji.fromUnicode("U+1F4E9")).queue()
        }


        return EventResult.CONSUME
    }

    operator fun invoke(bot: BotEventHandler) {
        bot.messageReceivedEvent[100] = { event ->
            if (event.channelType.isGuild || event.author.idLong == event.jda.selfUser.idLong) {
                EventResult.PASS
            } else {
                event.jda.getTextChannelById(bot.config.mailChannel)?.let {
                    sendMail(event.message, it, true)
                } ?: EventResult.PASS
            }
        }

        bot.messageUpdateEvent[60] = { event ->
            if ((!event.channelType.isGuild || event.channel.idLong.toULong() == bot.config.mailChannel) && event.author.idLong != event.jda.selfUser.idLong) {
                cache.find { (from) -> from == event.message.idLong }?.let { (_, channelId, targetId, incoming) ->

                    (if (incoming)event.jda.getTextChannelById(channelId) else event.jda.getPrivateChannelById(channelId))

                        ?.let { mailChannel ->
                        mailChannel.retrieveMessageById(targetId).queue { targetMessage: Message? ->
                            if (targetMessage == null) return@queue
                            MessageEditBuilder.fromMessage(targetMessage).apply {
                                setContent(messageFromFormatted(event.message.contentRaw, event.author.id, !event.channelType.isGuild))
                            }.build().let { msg ->
                                mailChannel.editMessageById(targetId, msg).queue()
                            }
                        }
                    }
                }
                EventResult.CONSUME
            } else EventResult.PASS
        }

        bot.messageReceivedEvent[40] = { event ->
            if (event.channel.idLong == bot.config.mailChannel.toLong()) {
                event.message.referencedMessage?.let { referenceMessage ->
                    if (referenceMessage.author.idLong == event.jda.selfUser.idLong && referenceMessage.mentions.users.size > 0) {
                        referenceMessage.mentions.users[0].openPrivateChannel().queue { targetChannel ->
                            sendMail(message = event.message, targetChannel = targetChannel)
                        }
                    }
                }
            }
            EventResult.PASS
        }
    }
}

