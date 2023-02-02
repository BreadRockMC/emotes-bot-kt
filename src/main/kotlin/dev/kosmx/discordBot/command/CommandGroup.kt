package dev.kosmx.discordBot.command

import dev.kosmx.discordBot.BotEventHandler
import dev.kosmx.discordBot.actions.IdentifiableList
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.*
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationMap
import net.dv8tion.jda.api.utils.data.DataObject
import java.util.function.Predicate

class CommandGroup(name: String, description: String, configure: SlashCommandData.() -> Unit = {}) : SlashCommand(name, description, configure) {

    override val configure: SlashCommandData.() -> Unit
        get() = SlashCommandConfig@{
            super.configure(this)
            this.addSubcommands(commands.map { command ->
                SubcommandData(command.name, command.description).apply {
                    command.configure(SubcommandDataWrapper(this, this@SlashCommandConfig))
                }
            })
        }

    private val commands = IdentifiableList<SlashCommand>()

    operator fun plusAssign(command: SlashCommand) {
        commands += command
    }

    override fun invoke(event: SlashCommandInteractionEvent) {
        commands[event.subcommandName]?.bindAndInvoke(event) ?: BotEventHandler.LOGGER.error("executed command event was not found: ${event.fullCommandName}")
    }

    override fun autoCompleteAction(event: CommandAutoCompleteInteractionEvent) {
        commands[event.subcommandName!!]?.autoCompleteAction(event) ?: run { event.replyChoiceStrings("error").queue() }
    }

    class SubcommandDataWrapper(private val sub: SubcommandData, private val group: SlashCommandData) : SlashCommandData {
        override fun toData(): DataObject = sub.toData()

        override fun setLocalizationFunction(localizationFunction: LocalizationFunction): SlashCommandData {
            error("Function not supported")
        }

        override fun setName(name: String) = apply { sub.name = name }

        override fun setNameLocalization(locale: DiscordLocale, name: String): SlashCommandData = apply {
            sub.setNameLocalization(locale, name)
        }

        override fun setNameLocalizations(map: MutableMap<DiscordLocale, String>): SlashCommandData = apply {
            sub.setNameLocalizations(map)
        }

        override fun setDefaultPermissions(permission: DefaultMemberPermissions): SlashCommandData {
            error("Can't set permission on sub-command")
        }

        override fun setGuildOnly(guildOnly: Boolean): SlashCommandData {
            error("Can't set guild-only property on sub-command")
        }

        override fun setNSFW(nsfw: Boolean): SlashCommandData {
            error("Can't set NSFW property on sub-command")
        }

        override fun getName(): String = sub.name

        override fun getNameLocalizations(): LocalizationMap = sub.nameLocalizations

        override fun getType(): Command.Type = Command.Type.SLASH

        override fun getDefaultPermissions(): DefaultMemberPermissions = group.defaultPermissions

        override fun isGuildOnly(): Boolean = group.isGuildOnly

        override fun isNSFW(): Boolean = group.isNSFW
        override fun setDescription(description: String): SlashCommandData = apply {
            sub.description = description
        }

        override fun setDescriptionLocalization(locale: DiscordLocale, description: String): SlashCommandData = apply {
            sub.setDescriptionLocalization(locale, description)
        }

        override fun setDescriptionLocalizations(map: MutableMap<DiscordLocale, String>): SlashCommandData = apply {
            sub.setDescriptionLocalizations(map)
        }

        override fun getDescription(): String = sub.description

        override fun getDescriptionLocalizations(): LocalizationMap = sub.descriptionLocalizations

        override fun removeOptions(condition: Predicate<in OptionData>): Boolean = sub.removeOptions(condition)

        override fun removeSubcommands(condition: Predicate<in SubcommandData>): Boolean {
            //error("Can't remove subcommands on subcommands")
            return false
        }

        override fun removeSubcommandGroups(condition: Predicate<in SubcommandGroupData>): Boolean {
            return false
        }

        override fun getSubcommands(): MutableList<SubcommandData> = mutableListOf()

        override fun getSubcommandGroups(): MutableList<SubcommandGroupData> = mutableListOf()

        override fun getOptions(): MutableList<OptionData> = sub.options

        override fun addOptions(vararg options: OptionData?): SlashCommandData = apply {
            sub.addOptions(*options)
        }

        override fun addSubcommands(vararg subcommands: SubcommandData?): SlashCommandData {
            error("Not yet implemented")
        }

        override fun addSubcommandGroups(vararg groups: SubcommandGroupData?): SlashCommandData {
            error("Not yet implemented")
        }

    }
}
