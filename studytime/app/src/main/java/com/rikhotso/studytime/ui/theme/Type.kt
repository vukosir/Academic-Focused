package com.rikhotso.studytime.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rikhotso.studytime.R

/** Quicksand for headings and numbers, Nunito for everything else. Both ship in res/font. */
val Quicksand = FontFamily(
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_semibold, FontWeight.SemiBold),
    Font(R.font.quicksand_bold, FontWeight.Bold),
)

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private fun heading(size: Int, scale: Float, weight: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = Quicksand,
    fontWeight = weight,
    fontSize = (size * scale).sp,
    lineHeight = (size * scale * 1.25f).sp,
    letterSpacing = (-0.01).em,
)

private fun body(size: Int, scale: Float, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Nunito,
    fontWeight = weight,
    fontSize = (size * scale).sp,
    lineHeight = (size * scale * 1.45f).sp,
)

/** Every text style in the app. [scale] is above 1 when the user switched on large text. */
fun studyTypography(scale: Float = 1f) = Typography(
    displaySmall = heading(40, scale),
    headlineLarge = heading(30, scale),
    headlineMedium = heading(24, scale),
    headlineSmall = heading(20, scale),
    titleLarge = heading(18, scale, FontWeight.SemiBold),
    titleMedium = heading(16, scale, FontWeight.SemiBold),
    titleSmall = body(14, scale, FontWeight.SemiBold),
    bodyLarge = body(16, scale),
    bodyMedium = body(14, scale),
    bodySmall = body(12, scale),
    labelLarge = body(14, scale, FontWeight.SemiBold),
    labelMedium = body(12, scale, FontWeight.Medium),
    labelSmall = body(11, scale, FontWeight.Medium),
)

val StudyTypography = studyTypography()
