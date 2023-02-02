package dev.kosmx.discordBot

import dev.kosmx.discordBot.actions.MailBot
import dev.kosmx.discordBot.actions.Tags
import dev.kosmx.discordBot.actions.initAdminCommands
import dev.kosmx.discordBot.actions.initUserCommands
import dev.kosmx.discordBot.actions.templateMatcher.PatternMatcher
import dev.kosmx.discordBot.brigadier.BrigadierConnector
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    val parser = ArgParser("Emotes bot")

    val configFile: String by parser.option(ArgType.String, fullName = "configFile", shortName = "c").default("bot.config.json")

    parser.parse(args)
    BotEventHandler.LOGGER.info("")

    val config = File(configFile).inputStream().use { input ->
        Json.decodeFromStream<BotConfig>(input)
    }

    initAdminCommands(BotEventHandler)
    initUserCommands(BotEventHandler)
    BrigadierConnector(BotEventHandler)
    PatternMatcher(BotEventHandler)
    MailBot(BotEventHandler)
    Tags(BotEventHandler)

    BotEventHandler.start(config)

}
