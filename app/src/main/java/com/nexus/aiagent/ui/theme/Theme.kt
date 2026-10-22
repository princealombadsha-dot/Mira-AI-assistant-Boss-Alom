package com.nexus.aiagent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NexusBackground = Color(0xFF0A0A0A)
val NexusSurface = Color(0xFF141414)
val NexusCard = Color(0xFF1E1E1E)
val NexusRed = Color(0xFFFF1744)
val NexusCrimson = Color(0xFFD50000)
val NexusNeonGreen = Color(0xFF00E676)
val NexusCyan = Color(0xFF00E5FF)
val NexusTextPrimary = Color(0xFFFFFFFF)
val NexusTextSecondary = Color(0xFFB0B0B0)

private val DarkColorScheme = darkColorScheme(
    primary = NexusRed,
    secondary = NexusCyan,
    tertiary = NexusNeonGreen,
    background = NexusBackground,
    surface = NexusSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = NexusTextPrimary,
    onSurface = NexusTextPrimary
)

@Composable
fun NexusAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
