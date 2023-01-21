package dev.kosmx.discordBot.brigadier

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.builder.LiteralArgumentBuilder.*
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object Commands {
    val dispatcher = CommandDispatcher<DiscordCommandSource>()

    init {
        dispatcher.register(literal<DiscordCommandSource?>("help")
            .then(argument<DiscordCommandSource?, String?>("function", word())
                .executes { ctx ->
                    when(StringArgumentType.getString(ctx, "function").lowercase()) {
                        "postemote" -> "This function is not yet implemented, ask <@303577390616150018>" // bad idea
                        else -> "Unknown function :C"
                    }.let {
                        ctx.source.event.message.reply(it).queue()
                    }
                    1 // redstone level :D
                })
            .executes { ctx ->
                ctx.source.event.message.reply("""
                    This is the new discord bot, check slash commands!
                        logs - link `https://emotes.kosmx.dev/guide/logs`
                """.trimIndent()).queue()
                1
            }
        )
        dispatcher.register(literal<DiscordCommandSource?>("logs").executes { ctx ->
            ctx.source.event.channel.sendMessage("https://emotes.kosmx.dev/guide/logs").queue()
            0
        })
    }
}

data class DiscordCommandSource(val user: User, val event: MessageReceivedEvent)
