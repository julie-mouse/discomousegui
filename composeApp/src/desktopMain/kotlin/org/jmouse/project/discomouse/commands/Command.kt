package org.jmouse.project.discomouse.commands

import dev.kord.core.Kord
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import java.util.ResourceBundle

interface Command {
    val bundle: ResourceBundle
    val bot: Kord
    val name: String
    val description: String
    suspend fun register()
    suspend fun execute(interaction: GuildChatInputCommandInteraction)
}