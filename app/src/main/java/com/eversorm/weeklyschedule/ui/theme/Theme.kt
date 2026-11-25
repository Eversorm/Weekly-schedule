package com.eversorm.weeklyschedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/*
 * One accent color (a calm blue), used for: today's day, reached goals, checkboxes, buttons.
 * Light/dark follows the phone's system setting automatically.
 */

private val Accent = Color(0xFF1F6F8B)
private val AccentLight = Color(0xFF8CCFE6)
private val AccentTint = Color(0xFFC4E7F3)
private val AccentDeep = Color(0xFF004D63)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentTint,
    onPrimaryContainer = Color(0xFF001F29),
    // Material uses "secondary" for some components (e.g. tonal buttons, selected chips):
    // we map it to the same accent so there is really only one accent color.
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = AccentTint,
    onSecondaryContainer = Color(0xFF001F29),
)

private val DarkColors = darkColorScheme(
    primary = AccentLight,
    onPrimary = Color(0xFF003545),
    primaryContainer = AccentDeep,
    onPrimaryContainer = AccentTint,
    secondary = AccentLight,
    onSecondary = Color(0xFF003545),
    secondaryContainer = AccentDeep,
    onSecondaryContainer = AccentTint,
)

/** Slightly larger text than the Material default, for readability. */
private val AppTypography = Typography().run {
    copy(
        bodyLarge = bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
        bodyMedium = bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
        titleMedium = titleMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
        labelLarge = labelLarge.copy(fontSize = 16.sp),
    )
}

@Composable
fun WeeklyScheduleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
