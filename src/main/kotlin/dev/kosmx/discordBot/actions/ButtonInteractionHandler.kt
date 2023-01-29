package dev.kosmx.discordBot.actions

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

open class ButtonInteractionHandler(val id: String, val function: ButtonInteractionHandler.(ButtonInteractionEvent) -> Unit = {}): (ButtonInteractionEvent) -> Unit {
    override fun invoke(event: ButtonInteractionEvent) {
        function(event)
    }
}
