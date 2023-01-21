package dev.kosmx.discordBot.brigadier

import dev.kosmx.discordBot.BotEventHandler
import dev.kosmx.discordBot.EventResult
import net.dv8tion.jda.api.entities.emoji.Emoji
import java.lang.Exception

object BrigadierConnector {
    operator fun invoke(bot: BotEventHandler) {
        bot.messageReceivedEvent[10] = { event ->
            try {
                if (event.message.contentRaw.startsWith(bot.config.commandStart) && event.message.contentRaw.length > bot.config.commandStart.length) {
                    //bot.LOGGER.info("Executing command: ${event.message.contentRaw}")
                    Commands.dispatcher.execute(
                        event.message.contentRaw.drop(bot.config.commandStart.length),
                        DiscordCommandSource(event.author, event)
                    )

                    EventResult.CONSUME
                }
            } catch (e: Exception) {
                event.message.addReaction(Emoji.fromUnicode("U+2753")).queue() // note invalid command but don't annoy anyone
            }
            EventResult.PASS
        }
    }
}