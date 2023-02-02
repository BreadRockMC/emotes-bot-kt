package dev.kosmx.discordBot.actions.templateMatcher

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.jsoup.Jsoup

object FileDownloader {
    private val mclogsPattern = Regex("https://mclo\\.gs/(?<match>[^/\\\\{}\\[\\]]+)\$") // with group
    private val pastebinPattern = Regex("https://pastebin\\.com/(raw/)?(?<match>[^/\\\\{}\\[\\]]+)\$") // with group 2

    /**
     * Attempt to parse message for log attachments and mclo.gs, pastebin links
     */
    fun getContent(event: MessageReceivedEvent): List<String> {
        val attachedLogs =  event.message.attachments.filter {
            it.fileName.endsWith(".log") ||
                    it.fileName.startsWith("crash-") &&
                    it.fileName.endsWith(".txt")
        }.map {
            String(it.proxy.download().get().readAllBytes(), Charsets.UTF_8)
        }.filter { it.isNotBlank() }
        return attachedLogs + lookForMCLogs(event.message.contentRaw) + lookForPastebin(event.message.contentRaw)
    }

    private fun lookForPastebin(msg: String): List<String> =
        pastebinPattern.findAll(msg).mapNotNull { matchResult ->
            matchResult.groups["match"]?.value
        }.mapNotNull {id ->
            val result = Jsoup.connect("https://pastebin.com/raw/$id").userAgent("kosmx.dev, emotes-bot").execute()
            return@mapNotNull if (result.statusCode() == 200) {
                result.body() // hope I won't get flagged
            } else {
                null
            }
        }.toList()

    private fun lookForMCLogs(msg: String): List<String> =
        mclogsPattern.findAll(msg).mapNotNull { matchResult ->
            matchResult.groups["match"]?.value
        }.mapNotNull { id ->
            val result = Jsoup.connect("https://api.mclo.gs/1/raw/$id").userAgent("kosmx.dev, emotes-bot").execute()
            return@mapNotNull if (result.statusCode() == 200 && result.contentType()
                    ?.startsWith("text/plain") == true
            ) {
                result.body()
            } else null
        }.toList()

}