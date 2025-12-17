package com.example.patienttracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimary,
    background = Background,
    surface = Surface,
    error = Error
)

private val DarkColors = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimary,
    background = Color.Black,
    surface = Color(0xFF121212),
    error = Error
)

@Composable
fun PatientTrackerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
