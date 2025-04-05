package org.jmouse.project.discomouse

import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jmouse.project.discomouse.commands.AnonMsgCommand
import org.jmouse.project.discomouse.commands.FxURLCommand
import org.jmouse.project.discomouse.commands.sendWakeUpMessage
import org.jmouse.project.util.COMMAND_ANONMSG
import org.jmouse.project.util.COMMAND_FXURL
import org.jmouse.project.util.COMMAND_FXURL2
import java.util.ResourceBundle

class DiscomouseBot {
    private var kord: Kord? = null
    private lateinit var fxUrlCommand: FxURLCommand

    fun start(scope: CoroutineScope) {
        if (kord != null) return

        scope.launch(Dispatchers.IO) {
            try {
                val bundle = ResourceBundle.getBundle("Strings")
                kord = Kord(System.getenv("DISCO_MOUSE_KEY"))

                fxUrlCommand = FxURLCommand(bundle, kord!!)
                val anonMsgCommand = AnonMsgCommand(bundle, kord!!)

                fxUrlCommand.register()
                anonMsgCommand.register()

                kord!!.on<GuildChatInputCommandInteractionCreateEvent> {
                    when (interaction.command.rootName) {
                        COMMAND_FXURL, COMMAND_FXURL2 -> fxUrlCommand.execute(interaction)
                        COMMAND_ANONMSG -> anonMsgCommand.execute(interaction)
                    }
                }

//                kord!!.on<MessageCreateEvent> {
//                    if (message.author?.isBot != false) return@on
//
//                    with(message.content) {
//                        when {
//                            startsWith("/${COMMAND_FXURL}") || startsWith("/${COMMAND_FXURL2}") ->
//                                fxUrlCommand.fxUrlTextBased(message)
//
//                            else -> return@on
//                        }
//                    }
//                }

                kord!!.login {
                    @OptIn(PrivilegedIntent::class)
                    intents = Intents(
                        Intent.GuildMessages,
                        Intent.Guilds,
                        Intent.MessageContent,
                        Intent.GuildMembers
                    )

                    launch {
                        sendWakeUpMessage(kord!!)
                    }
                }

                sendWakeUpMessage(kord!!)
            } catch (e: Exception) {
                println("Discomouse fell off the wheel because of this error: ${e.message}")
            }
        }
    }

    fun stop(scope: CoroutineScope) {
        kord?.let {
            scope.launch {
                it.shutdown()
                kord = null
            }
        }

        fxUrlCommand.shutdown()
    }
}