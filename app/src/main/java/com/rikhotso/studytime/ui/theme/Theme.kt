package com.rikhotso.studytime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.rikhotso.studytime.data.Settings
import com.rikhotso.studytime.data.ThemeMode

/** Read the studytime palette anywhere in the UI with StudyTheme.colors. */
object StudyTheme {
    val colors: StudyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStudyColors.current
}

/** True when the user asked for animations to be kept to a minimum. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Whether [mode] means the dark palette right now. System follows the phone's own setting. */
@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
    ThemeMode.System -> isSystemInDarkTheme()
}

/**
 * Wraps the app. The palette comes from the user's settings (light, dark or match phone, accent
 * colour, high contrast) and is also fed into Material 3, so built-in pieces such as text fields,
 * sliders and pickers match. Large text scales every text style.
 */
@Composable
fun StudytimeTheme(settings: Settings = Settings(), content: @Composable () -> Unit) {
    val darkTheme = isDark(settings.themeMode)
    // Remembered so that an unrelated change to the saved data does not rebuild the whole theme.
    val c = remember(darkTheme, settings.accent, settings.highContrast) { studyColors(darkTheme, settings.accent, settings.highContrast) }
    val typography = remember(settings.largeText) { studyTypography(if (settings.largeText) 1.15f else 1f) }
    val scheme = remember(c) { colorSchemeFor(c) }
    CompositionLocalProvider(LocalStudyColors provides c, LocalReduceMotion provides settings.reduceMotion) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

private fun colorSchemeFor(c: StudyColors): ColorScheme =
    if (c.isDark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.muted, onPrimaryContainer = c.foreground,
            secondary = c.primary, onSecondary = c.onPrimary,
            secondaryContainer = c.secondary, onSecondaryContainer = c.onSecondary,
            tertiary = c.accent, onTertiary = c.onAccent,
            tertiaryContainer = c.accent, onTertiaryContainer = c.onAccent,
            background = c.background, onBackground = c.foreground,
            surface = c.background, onSurface = c.foreground,
            surfaceVariant = c.muted, onSurfaceVariant = c.mutedForeground,
            surfaceContainerLowest = c.background, surfaceContainerLow = c.card,
            surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerHighest = c.muted,
            outline = c.border, outlineVariant = c.border,
            error = c.destructive, onError = c.onPrimary,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.muted, onPrimaryContainer = c.foreground,
            secondary = c.primary, onSecondary = c.onPrimary,
            secondaryContainer = c.secondary, onSecondaryContainer = c.onSecondary,
            tertiary = c.accent, onTertiary = c.onAccent,
            tertiaryContainer = c.accent, onTertiaryContainer = c.onAccent,
            background = c.background, onBackground = c.foreground,
            surface = c.background, onSurface = c.foreground,
            surfaceVariant = c.muted, onSurfaceVariant = c.mutedForeground,
            surfaceContainerLowest = c.card, surfaceContainerLow = c.card,
            surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerHighest = c.muted,
            outline = c.border, outlineVariant = c.border,
            error = c.destructive, onError = c.onPrimary,
        )
    }
