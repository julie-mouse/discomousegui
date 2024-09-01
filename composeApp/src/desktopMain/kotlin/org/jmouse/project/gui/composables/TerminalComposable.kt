package org.jmouse.project.gui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jmouse.project.discomouse.ParsedLog
import org.jmouse.project.gui.theme.*

@Composable
fun TerminalComposable(messages: List<ParsedLog>, listState: LazyListState, isPrettyMode: Boolean) {
    val isScrolledToBottom = remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val lastVisibileItemIndex = visibleItems.lastOrNull()?.index ?: 0
            lastVisibileItemIndex >= messages.lastIndex - 2
        }
    }

    LaunchedEffect(messages.size) {
        delay(50)
        if(isScrolledToBottom.value) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .padding(8.dp)
            .background(color = pastelFG, shape = RoundedCornerShape(8.dp))
            .fillMaxSize()
            .padding(3.dp)
    ) {
        items(messages) { message ->
            if (isPrettyMode) {
                MessageBubble(message)
            } else {
                // This should get some extra padding
                Bubble(message.rawLog, message.level, modifier = Modifier.padding(3.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(message: ParsedLog) {
    Column(Modifier.padding(3.dp)) {
        Text("${message.source} at ${message.timestamp}",
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Light,
            color = Color.LightGray,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 3.dp)
        )
        Bubble(message.shortMessage, message.level)
    }
}

@Composable
fun Bubble(text: String, level: String, modifier: Modifier = Modifier) {
    val color = when (level) {
        "WARN" -> pastelYellow
        "ERROR" -> pastelRed
        else -> pastelBlue
    }
    Box(
        modifier = modifier
            .background(color = color, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(text, color = pastelBG)
    }
}