package dev.kosmx.discordBot

import net.dv8tion.jda.api.entities.channel.attribute.IGuildChannelContainer

fun IGuildChannelContainer.getTextChannelById(channelId: ULong) = getTextChannelById(channelId.toLong())