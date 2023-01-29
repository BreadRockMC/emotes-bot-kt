package dev.kosmx.discordBot.actions

import dev.kosmx.discordBot.BotEventHandler
import net.dv8tion.jda.api.events.Event

open class IdentifiableInteractionHandler<T: Event>(val id: String, val function: IdentifiableInteractionHandler<T>.(T) -> Unit = {}): (T) -> Unit {
    override fun invoke(event: T) {
        function(event)
    }
}

class IdentifiableList<T : Event>(private val list: MutableList<IdentifiableInteractionHandler<T>> = mutableListOf()): List<IdentifiableInteractionHandler<T>> by list {
    private var map: Map<String, IdentifiableInteractionHandler<T>>? = null
    operator fun plusAssign(t: IdentifiableInteractionHandler<T>) {
        map = null
        list += t
    }

    operator fun invoke(id: String?, event: T, type: String) {
        if (map == null) map = list.associateBy { it.id }

        map!![id]?.invoke(event)
            ?: BotEventHandler.LOGGER.error("executed $type event was not found: $id")
    }

}

