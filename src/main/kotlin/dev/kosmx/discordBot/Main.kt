package dev.kosmx.discordBot

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

    val configFile: String by parser.option(ArgType.String, fullName = "configFile", shortName = "c").default("bot.config")

    parser.parse(args)

    val config = Json.decodeFromStream<BotConfig>(File(configFile).inputStream())
}