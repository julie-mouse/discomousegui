package org.jmouse.project.discomouse.commands

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.create.embed

suspend fun sendWakeUpMessage(bot: Kord) {
    val channelsToMessage = listOf(
        Snowflake(System.getenv("LAURENCE_GENERAL"))
    )

    if (System.getenv("HEADLESS")?.toBoolean() == true) {
        val wakeUpGif = "https://tenor.com/baUmC.gif"

        channelsToMessage.forEach { channelId ->
            val channel = bot.getChannelOf<TextChannel>(channelId)
            channel?.createMessage(wakeUpGif)
        }
    }
}