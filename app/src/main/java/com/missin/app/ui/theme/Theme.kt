package com.missin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MinimalistColorScheme = darkColorScheme(
    primary = BoneWhite,
    onPrimary = MatteCharcoal,
    secondary = MutedEmerald,
    onSecondary = BoneWhite,
    background = MatteCharcoal,
    onBackground = BoneWhite,
    surface = MatteCharcoal,
    onSurface = BoneWhite,
    outline = BorderDark
)

@Composable
fun MissInTheme(
    darkTheme: Boolean = true, // Ignored, always strict dark minimalist
    dynamicColor: Boolean = false, // Ignored
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MinimalistColorScheme,
        typography = Typography,
        content = content
    )
}
