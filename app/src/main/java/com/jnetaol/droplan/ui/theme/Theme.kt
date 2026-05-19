package com.jnetaol.droplan.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DLPrimary,
    onPrimary = Color.Black,
    primaryContainer = DLPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = DLSecondary,
    onSecondary = Color.Black,
    tertiary = DLAccent,
    onTertiary = Color.Black,
    background = DLBackground,
    onBackground = DLTextPrimary,
    surface = DLSurface,
    onSurface = DLTextPrimary,
    surfaceVariant = DLSurfaceVariant,
    onSurfaceVariant = DLTextSecondary,
    error = DLError,
    onError = Color.White,
    outline = DLTextMuted
)

@Composable
fun DropLANTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, typography = Typography(), content = content)
}
