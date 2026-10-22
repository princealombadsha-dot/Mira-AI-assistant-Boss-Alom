package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.ThemeMode

private val EmeraldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF00532B),
    onPrimaryContainer = EmeraldTertiary,
    secondary = EmeraldSecondary,
    onSecondary = Color(0xFF00354E),
    secondaryContainer = Color(0xFF004D71),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = EmeraldTertiary,
    background = EmeraldBackground,
    onBackground = EmeraldOnSurface,
    surface = EmeraldSurface,
    onSurface = EmeraldOnSurface,
    surfaceVariant = EmeraldSurfaceVariant,
    onSurfaceVariant = EmeraldOnSurfaceVariant
)

private val SunsetColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    onPrimary = Color(0xFF452B00),
    primaryContainer = Color(0xFF633F00),
    onPrimaryContainer = Color(0xFFFFDFA0),
    secondary = SunsetSecondary,
    onSecondary = Color(0xFF432C00),
    tertiary = SunsetTertiary,
    background = SunsetBackground,
    onBackground = SunsetOnSurface,
    surface = SunsetSurface,
    onSurface = SunsetOnSurface,
    surfaceVariant = SunsetSurfaceVariant,
    onSurfaceVariant = Color(0xFFD6D3D1)
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF004D68),
    onPrimaryContainer = Color(0xFFC2E8FF),
    secondary = MidnightSecondary,
    onSecondary = Color(0xFF1E2678),
    tertiary = MidnightTertiary,
    background = MidnightBackground,
    onBackground = MidnightOnSurface,
    surface = MidnightSurface,
    onSurface = MidnightOnSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.EMERALD_CYBER,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.EMERALD_CYBER -> EmeraldColorScheme
        ThemeMode.SUNSET_GOLD -> SunsetColorScheme
        ThemeMode.MIDNIGHT_AMOLED -> MidnightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
