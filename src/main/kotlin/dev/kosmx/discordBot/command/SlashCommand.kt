package dev.kosmx.discordBot.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData


open class SlashCommand(val name: String, val description: String, configure: SlashCommandData.() -> Unit = {}, private val execute: SlashCommand.(SlashCommandInteractionEvent) -> Unit = {}) {
    private val options = mutableListOf<Option<*>>()
    operator fun <T> SlashCommandInteractionEvent.get(option: Option<T>): T? = option[this]
    operator fun <T> SlashCommandInteractionEvent.get(option: RequiredOption<T>): T = option[this]
    operator fun <T> SlashCommandInteractionEvent.get(option: DefaultOption<T>): T = option[this]

    open operator fun invoke(event: SlashCommandInteractionEvent) = execute(event)

    val configure: SlashCommandData.() -> Unit = {
        configure()
        this@SlashCommand.options.forEach { option ->
            addOption(option.optionType, option.name, option.description, option.required)
        }
    }

    //operator fun invoke(event: SlashCommandInteractionEvent) = invoke(event)

    protected fun <T> option(name: String, description: String, optionType: OptionType, resolver: (OptionMapping) -> T) = Option(name, description, optionType, resolver).also { options += it }

    open inner class Option<T> internal constructor(val name: String, val description: String, val optionType: OptionType, private val resolver: (OptionMapping) -> T) {
        open val required: Boolean = false

        open operator fun get(event: SlashCommandInteractionEvent): T? = event.getOption(name, resolver)
        fun required() = RequiredOption(name, description, optionType, resolver).also { options[options.size - 1] = it }

        fun default(default: T) = DefaultOption(name, description, optionType, resolver, default).also { options[options.size - 1] = it }
    }

    inner class RequiredOption<T> internal constructor(name: String, description: String, optionType: OptionType, resolver: (OptionMapping) -> T): Option<T>(name, description,optionType, resolver) {
        override val required: Boolean = true

        override fun get(event: SlashCommandInteractionEvent): T {
            return super.get(event) ?: error("Required argument missing")
        }
    }
    inner class DefaultOption<T> internal constructor(name: String, description: String, optionType: OptionType, resolver: (OptionMapping) -> T, private val default: T): Option<T>(name, description,optionType, resolver) {

        override fun get(event: SlashCommandInteractionEvent): T {
            return super.get(event) ?: default
        }
    }

}

