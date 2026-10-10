package com.rikhotso.studytime.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.rikhotso.studytime.data.Accent
import com.rikhotso.studytime.data.PastelColor

/**
 * The studytime palette. The names mirror the design tokens of the web version
 * (background, card, primary, muted and so on), so a colour means the same thing everywhere.
 */
@Immutable
data class StudyColors(
    val background: Color,
    val foreground: Color,
    val card: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val muted: Color,
    val mutedForeground: Color,
    val accent: Color,
    val onAccent: Color,
    val destructive: Color,
    val border: Color,
    val isDark: Boolean,
)

val LightStudyColors = StudyColors(
    background = Color(0xFFFAF9F4),
    foreground = Color(0xFF352749),
    card = Color(0xFFFFFFFF),
    primary = Color(0xFFB297D8),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFD5E8F6),
    onSecondary = Color(0xFF25597E),
    muted = Color(0xFFF4F1EB),
    mutedForeground = Color(0xFF776490),
    accent = Color(0xFFF5D6E0),
    onAccent = Color(0xFF913051),
    destructive = Color(0xFFE87373),
    border = Color(0xFFEDE8DE),
    isDark = false,
)

val DarkStudyColors = StudyColors(
    background = Color(0xFF221B2D),
    foreground = Color(0xFFF4F1EB),
    card = Color(0xFF2C2239),
    primary = Color(0xFFB297D8),
    onPrimary = Color(0xFF22192E),
    secondary = Color(0xFF324B5D),
    onSecondary = Color(0xFFCEE3F3),
    muted = Color(0xFF3B3149),
    mutedForeground = Color(0xFFBEB6A7),
    accent = Color(0xFF6E3548),
    onAccent = Color(0xFFF5D6E0),
    destructive = Color(0xFFD14747),
    border = Color(0xFF453956),
    isDark = true,
)

val LocalStudyColors = staticCompositionLocalOf { LightStudyColors }

/** The soft version of each accent, used for buttons and highlights. */
val Accent.color: Color
    get() = when (this) {
        Accent.Lavender -> Color(0xFFB297D8)
        Accent.Sky -> Color(0xFF8DBDE3)
        Accent.Blush -> Color(0xFFE8A0B8)
        Accent.Mint -> Color(0xFF7CCBB1)
        Accent.Peach -> Color(0xFFEFA98A)
        Accent.Lilac -> Color(0xFFC69BDB)
    }

/** A deeper version of each accent that white text is easy to read on, for high contrast. */
val Accent.deepColor: Color
    get() = when (this) {
        Accent.Lavender -> Color(0xFF6F52A8)
        Accent.Sky -> Color(0xFF2F6FA3)
        Accent.Blush -> Color(0xFFB0456B)
        Accent.Mint -> Color(0xFF2E7D64)
        Accent.Peach -> Color(0xFFB45A35)
        Accent.Lilac -> Color(0xFF8B4FA6)
    }

/** The palette for the current settings. High contrast darkens text and outlines and deepens the accent. */
fun studyColors(dark: Boolean, accent: Accent, highContrast: Boolean): StudyColors {
    val base = if (dark) DarkStudyColors else LightStudyColors
    return when {
        !highContrast -> base.copy(primary = accent.color)
        dark -> base.copy(
            primary = accent.color,
            foreground = Color(0xFFFFFFFF),
            mutedForeground = Color(0xFFE6DFEE),
            border = Color(0xFF8A7BA0),
            destructive = Color(0xFFFF8A8A),
        )
        else -> base.copy(
            primary = accent.deepColor,
            foreground = Color(0xFF1F1530),
            mutedForeground = Color(0xFF4A3B63),
            border = Color(0xFFB9ADC9),
            destructive = Color(0xFFB3261E),
        )
    }
}

/** Dark text for anything sitting on a solid pastel, in both light and dark themes. */
val Ink = Color(0xFF352749)

/** The full strength pastel, used for dots, avatars and colour swatches. */
val PastelColor.solid: Color
    get() = when (this) {
        PastelColor.Lavender -> Color(0xFFB89FDB)
        PastelColor.BabyBlue -> Color(0xFFACD1EC)
        PastelColor.Blush -> Color(0xFFF1CBD7)
        PastelColor.Butter -> Color(0xFFF6E1AC)
        PastelColor.Mint -> Color(0xFFACE2D0)
        PastelColor.Lilac -> Color(0xFFD7BFE3)
        PastelColor.Peach -> Color(0xFFF4D0BE)
        PastelColor.Sky -> Color(0xFFB3E0EF)
    }

/** A see-through tint of the pastel, used as the background of rows and tiles. */
val PastelColor.soft: Color
    get() = solid.copy(alpha = 0.28f)
