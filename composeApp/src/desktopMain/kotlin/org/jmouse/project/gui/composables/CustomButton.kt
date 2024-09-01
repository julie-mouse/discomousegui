package org.jmouse.project.gui.composables

import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jmouse.project.gui.theme.pastelBG
import org.jmouse.project.gui.theme.pastelGrey
import org.jmouse.project.gui.theme.pastelPurple

@Composable
fun CustomButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick,
        modifier,
        colors = buttonColors(
            disabledBackgroundColor = pastelGrey,
            backgroundColor = pastelPurple,
            contentColor = pastelPurple
        ),
        enabled = enabled
    ) {
        Text(text, color = pastelBG)
    }
}