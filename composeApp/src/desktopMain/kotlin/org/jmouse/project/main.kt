package org.jmouse.project

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.*
import org.jmouse.project.discomouse.ConsoleOutputStream
import org.jmouse.project.discomouse.DiscomouseBot
import org.jmouse.project.discomouse.ParsedLog
import org.jmouse.project.discomouse.parseLogMessage
import org.jmouse.project.gui.composables.TerminalComposable
import org.jmouse.project.gui.theme.pastelBG
import org.jmouse.project.gui.theme.pastelGreen
import org.jmouse.project.gui.theme.pastelPurple
import org.jmouse.project.gui.theme.pastelRed
import org.slf4j.event.Level
import java.io.PrintStream

fun main() {
    if (System.getenv("HEADLESS")?.toBoolean() == true) {
        println("Launching Headless!")
        val bot = DiscomouseBot()
        runBlocking {
            bot.start(this)
        }
        return
    }

    application {
        var isBotRunning by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val bot = remember { DiscomouseBot() }

        var isPrettyMode by remember { mutableStateOf(true) }

        val consoleBuffer = remember { mutableStateListOf<ParsedLog>() }
        val listState = rememberLazyListState()

        fun startBot(scope: CoroutineScope, bot: DiscomouseBot) {
            consoleBuffer.add(ParsedLog.print("Bot Started!"))
            if (!isBotRunning) {
                isBotRunning = true
                scope.launch(Dispatchers.IO) {
                    try {
                        bot.start(scope)
                        while (isBotRunning) {
                            delay(6000000)
                            consoleBuffer.add(ParsedLog.print("Bot health check"))
                        }
                    } catch (e: Exception) {
                        isBotRunning = false
                        consoleBuffer.add(
                            ParsedLog.print(
                                message = "Discomouse fell off the wheel due to this error: ${e.message}",
                                level = Level.WARN
                            )
                        )
                    } finally {
                        isBotRunning = false
                    }
                }
            }
        }

        fun stopBot(scope: CoroutineScope, bot: DiscomouseBot) {
            bot.stop(scope)
            isBotRunning = false
            consoleBuffer.add(ParsedLog.print("Bot Stopped!"))
        }

        // Function to parse raw log lines into ParsedLog objects and add them to the consoleBuffer
        fun updateParsedLogs(rawLog: String) {
            val parsedLog = parseLogMessage(rawLog)
            if (parsedLog != null) {
                consoleBuffer.add(parsedLog) // Add the parsed log to the mutable list
            }
        }

        LaunchedEffect(Unit) {
            val printStream = PrintStream(ConsoleOutputStream { line -> updateParsedLogs(line) }, true)
            System.setOut(printStream)
            System.setErr(printStream)
            startBot(scope, bot)
        }

        Window(
            state = rememberWindowState(),
            undecorated = true,
            transparent = true,
            onCloseRequest = ::exitApplication
        ) {
            Box(Modifier.background(color = pastelBG)){
                Column {
                    WindowDraggableArea {
                        CustomToolbar(
                            isBotRunning,
                            isPrettyMode,
                            {toggledTo -> isPrettyMode = toggledTo},
                            ::exitApplication,
                            {startBot(scope, bot)},
                            {stopBot(scope, bot)}
                        )
                    }
                    Spacer(Modifier.background(color = Color.Gray).fillMaxWidth().height(1.dp))
                    TerminalComposable(consoleBuffer, listState, isPrettyMode)
                }
            }
        }
    }
}

@Composable
fun CustomToolbar(
    isBotRunning: Boolean,
    isPrettyMode: Boolean,
    onTogglePrettyMode: (Boolean) -> Unit,
    closeAction: () -> Unit,
    startBot: () -> Unit,
    stopBot: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(color = pastelBG)
            .padding(vertical = 3.dp)
    ) {
        IconButton(onClick = closeAction) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
        }
        Text("DiscomouseGUI", color = Color.White, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)

        Spacer(Modifier.weight(1f))

        if (isBotRunning) {
            IconButton(onClick = {stopBot()}) {
                Icon(Icons.Filled.Square, contentDescription = "Stop", tint = pastelRed)
            }
        } else {
            IconButton(onClick = { startBot() }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Start", tint = pastelGreen)
            }
        }

        Text("Pretty:", color = Color.White, modifier = Modifier.padding(6.dp))
        Switch(
            checked = isPrettyMode,
            onCheckedChange = {
                onTogglePrettyMode(it)
            },
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = Color.White,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray,
                checkedTrackColor = pastelPurple
            )
        )
    }
}

@Preview
@Composable
fun AppPreview() {
    val prettyMode = false
    val listState = rememberLazyListState()
    val consoleBuffer = remember { mutableStateListOf<ParsedLog>() }
    //MessageBubble(parseLogMessage("02:17:47.456 [main] DEBUG [R]:[KTOR]:[ExclusionRequestRateLimiter] -- [REQUEST]:POST:/{application.id}/commands params:[{application.id}=1165781408644666666] body:{\"name\":\"fx\",\"type\":1,\"description\":\"Attempts to fix the embed from your link!\",\"options\":[{\"type\":3,\"name\":\"link\",\"description\":\"The link with a bad embed.\",\"required\":true}]}"))
    consoleBuffer.add(ParsedLog.print("Hello World!"))
    consoleBuffer.add(ParsedLog.print("We're Hello World-ing even harder!", Level.WARN))
    consoleBuffer.add(ParsedLog.print("We've Hello World-ed WAY too hard! Someone pull the plug before it's too late!", Level.ERROR))

    Box(Modifier.fillMaxSize().background(color = pastelBG)) {
        Column {
            CustomToolbar(true, prettyMode, {}, {}, {}, {})
            Spacer(Modifier.background(color = Color.Gray).fillMaxWidth().height(1.dp))
            TerminalComposable(consoleBuffer, listState, isPrettyMode = prettyMode)
        }
    }
}