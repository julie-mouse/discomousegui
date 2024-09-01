package org.jmouse.project.discomouse.commands

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import kotlinx.coroutines.flow.firstOrNull
import org.jmouse.project.util.ANONMSG_MSG
import org.jmouse.project.util.ANONMSG_USER
import org.jmouse.project.util.COMMAND_ANONMSG
import java.util.*

class AnonMsgCommand(
    override val bundle: ResourceBundle,
    override val bot: Kord
) : Command {
    override val name: String = COMMAND_ANONMSG
    override val description: String = bundle.getString("anon_msg_description")

    override suspend fun register() {
        bot.createGuildChatInputCommand(Snowflake(System.getenv("CRIMBUS_SNOWFLAKE")), name, description) {
            user(ANONMSG_USER, bundle.getString("anon_msg_user_description")) {
                required = true
            }
            string(ANONMSG_MSG, bundle.getString("anon_msg_message_description")) {
                required = true
            }
        }
    }

    override suspend fun execute(interaction: GuildChatInputCommandInteraction) {
        val response = interaction.deferEphemeralResponse()
        val user = interaction.command.users[ANONMSG_USER]
        val message = interaction.command.strings[ANONMSG_MSG]

        var tellUser = "Message sent!"

        val thread = interaction.guild.activeThreads.firstOrNull { it.owner == user } ?: interaction.guild.cachedThreads.firstOrNull{it.owner == user}
        if (thread != null) {
            thread.createMessage("${user!!.mention} $message")
        } else {
            tellUser = "They don't have a thread yet!"
        }

        response.respond {
            content = tellUser
        }
    }
}