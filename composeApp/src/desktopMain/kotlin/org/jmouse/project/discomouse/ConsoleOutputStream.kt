package org.jmouse.project.discomouse

import org.slf4j.event.Level
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ConsoleOutputStream(private val processLine: (String) -> Unit) : OutputStream() {
    private val stringBuilder = StringBuilder()

    override fun write(b: Int) {
        if (b == '\n'.code) {
            val line = stringBuilder.toString().trim()
            if(line.isNotEmpty()) {
                processLine(line)
            }
            stringBuilder.clear()
        } else {
            stringBuilder.append(b.toChar())
        }
    }

    override fun flush() {
        val line = stringBuilder.toString().trim()
        if (line.isNotEmpty()) {
            processLine(line)
        }
        stringBuilder.clear()
    }

    override fun close() {
        flush()
    }
}

data class ParsedLog(
    val timestamp: String,
    val level: String,
    val source: String,
    val message: String,
    val shortMessage: String,
    val rawLog: String
) {
    companion object {
        fun print(message: String, level: Level = Level.INFO): ParsedLog {
            val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
            return (
                ParsedLog(
                    timestamp,
                    level.toString(),
                    "DiscomouseApp",
                    message,
                    message.take(200),
                    "$timestamp $level [DiscomouseApp] -- $message"
                )
            )
        }
    }
}

fun parseLogMessage(rawLog: String): ParsedLog? {
    val regex = Regex("""(\d{2}:\d{2}:\d{2}\.\d{3})\s+(INFO|WARN|DEBUG|ERROR)\s+(\[.*?\]|\S+)\s+[-]{1,2}\s+(.*)""")
    val matchResult = regex.find(rawLog)

    if (matchResult == null) {
        val shortMessage = rawLog.take(200) + if (rawLog.length > 200) "..." else ""
        return ParsedLog("", "", "", rawLog, shortMessage, rawLog)
    }
    val (timestamp, level, source, message) = matchResult.destructured
    val shortMessage = message.take(200) + if (message.length > 200) "..." else ""

    return ParsedLog(timestamp, level, source, message, shortMessage, rawLog)
}