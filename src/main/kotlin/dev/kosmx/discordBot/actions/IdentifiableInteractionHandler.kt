package dev.kosmx.discordBot.actions

import dev.kosmx.discordBot.BotEventHandler
import net.dv8tion.jda.api.events.Event

open class IdentifiableInteractionHandler<T: Event>(val id: String, val function: IdentifiableInteractionHandler<T>.(T) -> Unit = {}): (T) -> Unit {
    override fun invoke(event: T) {
        function(event)
    }
}

class IdentifiableList<T : IdentifiableInteractionHandler<E>, E: Event>(private val list: MutableList<T> = mutableListOf()): List<T> by list {
    private var map: Map<String, T>? = null
    operator fun plusAssign(t: T) {
        map = null
        list += t
    }

    operator fun invoke(id: String?, event: E, type: String) {
        if (map == null) map = list.associateBy { it.id }

        map!![id?.split(":")?.get(0)]?.invoke(event)
            ?: BotEventHandler.LOGGER.error("executed $type event was not found: $id")
    }

}

