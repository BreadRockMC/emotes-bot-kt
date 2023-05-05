package dev.kosmx.discordBot.brigadier

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import dev.kosmx.discordBot.BotEventHandler
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import kotlin.system.exitProcess

object Commands {
    val dispatcher = CommandDispatcher<DiscordCommandSource>()

    init {
        dispatcher.register(literal<DiscordCommandSource>("help")
            .then(argument<DiscordCommandSource, String>("function", word())
                .executes { ctx ->
                    when(StringArgumentType.getString(ctx, "function").lowercase()) {
                        "postemote" -> "Please use /postemote slash command\nIt will send a preview before sending the embed to the server" // bad idea
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

        dispatcher.register(literal<DiscordCommandSource>("logs").executes { ctx ->
            ctx.source.event.channel.sendMessage("https://emotes.kosmx.dev/guide/logs").queue()
            0
        })


        dispatcher.register(literal<DiscordCommandSource>("stop")
            .then(argument<DiscordCommandSource, Boolean>("restart", BoolArgumentType.bool())
                .executes { ctx ->
                    stopCommand(ctx, BoolArgumentType.getBool(ctx, "restart"))
                }
            )
            .executes(::stopCommand)
        )
    }

    @Suppress("SameReturnValue")
    private fun stopCommand(ctx: CommandContext<DiscordCommandSource>, restart: Boolean = false): Int {
        if (ctx.source.user.idLong.toULong() in BotEventHandler.config.botAdmins) {
            ctx.source.event.message.reply("Exiting").queue {
                exitProcess(if (restart) 0 else 4)
            }
        } else {
            ctx.source.event.message.reply("You don't have permission to use this command").queue()
        }
        return 1
    }
}

data class DiscordCommandSource(val user: User, val event: MessageReceivedEvent)
