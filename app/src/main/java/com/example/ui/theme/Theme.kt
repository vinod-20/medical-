package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CleanMinimalColorScheme = lightColorScheme(
    primary = ClinicalBlue,
    secondary = LightBlueAccent,
    tertiary = LavenderAccent,
    background = MinimalBg,
    surface = SurfaceWhite,
    error = DiagnosticCrimson,
    onPrimary = SurfaceWhite,
    onSecondary = DarkBlueText,
    onTertiary = DeepLavenderText,
    onBackground = MinimalTextMain,
    onSurface = MinimalTextMain,
    errorContainer = CriticalRedBg,
    onErrorContainer = CriticalRedText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // disabled to lock Clean Minimalism palette
    content: @Composable () -> Unit,
) {
    // Fixed to CleanMinimalColorScheme to present a polished clinical workspace
    val colorScheme = CleanMinimalColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
