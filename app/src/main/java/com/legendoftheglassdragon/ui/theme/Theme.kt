package com.legendoftheglassdragon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TerminalColorScheme = darkColorScheme(
    primary = AnsiColor.Cyan,
    onPrimary = AnsiColor.Black,
    secondary = AnsiColor.IceBlue,
    onSecondary = AnsiColor.Black,
    error = AnsiColor.BrightRed,
    background = AnsiColor.Black,
    onBackground = AnsiColor.LightGray,
    surface = AnsiColor.PanelBlack,
    onSurface = AnsiColor.LightGray,
)

@Composable
fun LegendOfTheGlassDragonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = TerminalTypography,
        content = content,
    )
}
