package dev.kosmx.discordBot.command

import dev.kosmx.discordBot.actions.IdentifiableInteractionHandler
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData


open class SlashCommand(name: String, val description: String, configure: SlashCommandData.() -> Unit = {}, private val execute: SlashCommand.(SlashCommandInteractionEvent) -> Unit = {})
    : IdentifiableInteractionHandler<SlashCommandInteractionEvent>(id = name) {
    val name: String
        get() = id

    private val options = mutableListOf<Option<*>>()
    operator fun <T> SlashCommandInteractionEvent.get(option: Option<T>): T? = option[this]
    operator fun <T> SlashCommandInteractionEvent.get(option: RequiredOption<T>): T = option[this]
    operator fun <T> SlashCommandInteractionEvent.get(option: DefaultOption<T>): T = option[this]

    override operator fun invoke(event: SlashCommandInteractionEvent) = execute(event)

    open val configure: SlashCommandData.() -> Unit = {
        configure()
        this@SlashCommand.options.forEach { option ->
            addOption(option.optionType.type, option.name, option.description, option.required)
        }
    }

    open fun autoCompleteAction(event: CommandAutoCompleteInteractionEvent) {

    }

    //operator fun invoke(event: SlashCommandInteractionEvent) = invoke(event)

    protected fun <T> option(name: String, description: String, optionType: SlashOptionType<T>) = Option(name, description, optionType).also { options += it }

    open inner class Option<T> internal constructor(val name: String, val description: String, val optionType: SlashOptionType<T>) {
        open val required: Boolean = false

        open operator fun get(event: SlashCommandInteractionEvent): T? = event.getOption(name, optionType.optionMapping)
        fun required() = RequiredOption(name, description, optionType).also { options[options.size - 1] = it }

        fun default(default: T) = DefaultOption(name, description, optionType, default).also { options[options.size - 1] = it }
    }

    inner class RequiredOption<T> internal constructor(name: String, description: String, optionType: SlashOptionType<T>): Option<T>(name, description,optionType) {
        override val required: Boolean = true

        override fun get(event: SlashCommandInteractionEvent): T {
            return super.get(event) ?: error("Required argument missing")
        }
    }
    inner class DefaultOption<T> internal constructor(name: String, description: String, optionType: SlashOptionType<T>, private val default: T): Option<T>(name, description,optionType) {

        override fun get(event: SlashCommandInteractionEvent): T {
            return super.get(event) ?: default
        }
    }
}

