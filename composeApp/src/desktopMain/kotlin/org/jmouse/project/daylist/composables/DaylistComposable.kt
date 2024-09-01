package org.jmouse.project.daylist.composables

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DaylistCard(
    funnyAdjectives: String,
    timeOfDay: String,
    weekDay: String,
    genres: String
){
    val imageWidth = 1024.dp
    val imageHeight = 512.dp

    Box (
        Modifier.size(imageWidth, imageHeight)
    ){
        Image(
            painter = painterResource(imagePathByTime(timeOfDay)),
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
        Text(
            text = "discomouse-list",
            fontSize = 25.sp,
            color = Color.White,
            fontWeight = FontWeight.Light,
            fontStyle = FontStyle.Italic,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        )
        Text(
            text = "$funnyAdjectives $weekDay $timeOfDay",
            fontSize = 85.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        )
        Text(
            text = "based on $genres",
            fontSize = 25.sp,
            fontWeight = FontWeight.Light,
            fontStyle = FontStyle.Italic,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
        )
    }

}

fun imagePathByTime(time: String?): String {
    return when (time) {
        "morning" -> "images/inputs/MorningList.jpg"
        "afternoon" -> "images/inputs/AfternoonList.jpg"
        "evening" -> "images/inputs/EveningList.jpg"
        "night" -> "images/inputs/NightList.jpg"
        else -> "images/inputs/AfternoonList.jpg"
    }
}

@Composable
@Preview
fun PreviewDaylistCard() {
    DaylistCard(
        funnyAdjectives = "forest cottagecore",
        weekDay = "saturday",
        timeOfDay = "night",
        genres = "indie, alternative, pop"
    )
}