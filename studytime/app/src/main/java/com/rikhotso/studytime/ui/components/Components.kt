@file:OptIn(ExperimentalLayoutApi::class)

package com.rikhotso.studytime.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.data.DayBar
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.initials
import com.rikhotso.studytime.ui.theme.Ink
import com.rikhotso.studytime.ui.theme.LocalReduceMotion
import com.rikhotso.studytime.ui.theme.Quicksand
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft
import com.rikhotso.studytime.ui.theme.solid
import kotlin.math.max

/** The building blocks every screen shares: brand mark, cards, buttons, rows and charts. */

val CardShape = RoundedCornerShape(28.dp)
val TileShape = RoundedCornerShape(20.dp)
val FieldShape = RoundedCornerShape(16.dp)

// ------------------------------------------------------------------ brand

/** The studytime mark: a rounded square in the three brand pastels holding a clock face over a smile. */
@Composable
fun StudytimeMark(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 64f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFC8B6F0), Color(0xFFA8C8E8), Color(0xFFF0C4D4)),
                start = Offset(8f * u, 8f * u),
                end = Offset(56f * u, 56f * u),
            ),
            topLeft = Offset(6f * u, 6f * u),
            size = Size(52f * u, 52f * u),
            cornerRadius = CornerRadius(16f * u, 16f * u),
        )
        drawCircle(Color.White.copy(alpha = 0.92f), radius = 14f * u, center = Offset(32f * u, 30f * u))
        val handColor = Color(0xFF9A86D6)
        val hands = Path().apply {
            moveTo(32f * u, 22f * u)
            lineTo(32f * u, 30f * u)
            lineTo(37f * u, 33f * u)
        }
        drawPath(hands, handColor, style = Stroke(width = 2.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(handColor, radius = 2f * u, center = Offset(32f * u, 30f * u))
        val smile = Path().apply {
            moveTo(22f * u, 44f * u)
            cubicTo(25f * u, 42f * u, 28f * u, 42f * u, 32f * u, 42f * u)
            cubicTo(36f * u, 42f * u, 39f * u, 42f * u, 42f * u, 44f * u)
        }
        drawPath(smile, Color.White, style = Stroke(width = 2.6f * u, cap = StrokeCap.Round))
    }
}

/** The lowercase wordmark: "study" in ink and "time" in lavender. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, fontSize: TextUnit = 24.sp) {
    val colors = StudyTheme.colors
    Text(
        text = buildAnnotatedString {
            append("study")
            withStyle(SpanStyle(color = colors.primary)) { append("time") }
        },
        modifier = modifier,
        color = colors.foreground,
        fontFamily = Quicksand,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        letterSpacing = (-0.02).em,
        maxLines = 1,
    )
}

@Composable
fun Logo(modifier: Modifier = Modifier, markSize: Dp = 32.dp, fontSize: TextUnit = 22.sp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StudytimeMark(size = markSize)
        Wordmark(fontSize = fontSize)
    }
}

// ------------------------------------------------------------------ icons and buttons

@Composable
fun StudyIcon(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    Icon(painter = painterResource(id), contentDescription = contentDescription, modifier = modifier, tint = tint)
}

enum class ButtonKind { Primary, Secondary, Ghost, Danger }

/** The one pill-shaped button used across the app. */
@Composable
fun StudyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    small: Boolean = false,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    enabled: Boolean = true,
) {
    val colors = StudyTheme.colors
    val container = when (kind) {
        ButtonKind.Primary -> colors.primary
        ButtonKind.Secondary -> colors.secondary
        else -> Color.Transparent
    }
    val content = when (kind) {
        ButtonKind.Primary -> colors.onPrimary
        ButtonKind.Secondary -> colors.onSecondary
        ButtonKind.Ghost -> colors.foreground
        ButtonKind.Danger -> colors.destructive
    }
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 1.dp, minHeight = if (small) 34.dp else 44.dp),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = container.alpha * 0.5f),
            disabledContentColor = content.copy(alpha = 0.5f),
        ),
        contentPadding = PaddingValues(horizontal = if (small) 14.dp else 22.dp, vertical = if (small) 6.dp else 10.dp),
    ) {
        if (leadingIcon != null) {
            StudyIcon(leadingIcon, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp))
            StudyIcon(trailingIcon, Modifier.size(16.dp))
        }
    }
}

@Composable
fun TrashButton(onClick: () -> Unit, description: String, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
        StudyIcon(
            R.drawable.ic_trash_2,
            Modifier.size(18.dp),
            tint = StudyTheme.colors.mutedForeground,
            contentDescription = description,
        )
    }
}

// ------------------------------------------------------------------ cards and rows

/** The white card with a soft pastel shadow that holds each section of a screen. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = StudyTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, CardShape, ambientColor = Color(0x33896BC2), spotColor = Color(0x33896BC2))
            .clip(CardShape)
            .background(colors.card)
            .border(1.dp, colors.border, CardShape)
            .padding(20.dp),
    ) {
        if (title != null || action != null) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title.orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.foreground,
                )
                if (action != null) action()
            }
        }
        content()
    }
}

/** A small figure with an icon, for example "Day streak 3". */
@Composable
fun StatCard(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    accent: PastelColor,
    modifier: Modifier = Modifier,
) {
    val colors = StudyTheme.colors
    Column(
        modifier
            .shadow(4.dp, CardShape, ambientColor = Color(0x33896BC2), spotColor = Color(0x33896BC2))
            .clip(CardShape)
            .background(colors.card)
            .border(1.dp, colors.border, CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(14.dp)).background(accent.soft),
                contentAlignment = Alignment.Center,
            ) {
                StudyIcon(icon, Modifier.size(16.dp), tint = colors.foreground.copy(alpha = 0.7f))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(value, style = MaterialTheme.typography.headlineMedium, color = colors.foreground, maxLines = 1)
    }
}

/** The gentle placeholder shown when a list has nothing in it yet. */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    Box(
        modifier.fillMaxWidth().clip(TileShape).background(colors.muted.copy(alpha = 0.6f)).padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground, textAlign = TextAlign.Center)
    }
}

/** A row tinted with one of the pastels, used for subjects, classes, friends and sessions. */
@Composable
fun SoftRow(color: PastelColor, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().clip(TileShape).background(color.soft).padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun ColorDot(color: PastelColor, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color.solid))
}

/** A round badge with the first two letters of a name. */
@Composable
fun Avatar(name: String, color: PastelColor, modifier: Modifier = Modifier, size: Dp = 38.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color.solid), contentAlignment = Alignment.Center) {
        Text(initials(name), color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

/** Title and subtitle stacked, taking the free width of a row. */
@Composable
fun RowScope.RowText(title: String, subtitle: String = "") {
    val colors = StudyTheme.colors
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.foreground)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
        }
    }
}

/** The eight pastel swatches. The chosen one gets a dark ring. */
@Composable
fun ColorPicker(selected: PastelColor, onSelect: (PastelColor) -> Unit, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PastelColor.entries.forEach { color ->
            val isSelected = color == selected
            Box(
                Modifier
                    .size(36.dp)
                    .border(2.dp, if (isSelected) colors.foreground else Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color.solid)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(color) })
                    .semantics { contentDescription = color.label },
            )
        }
    }
}

// ------------------------------------------------------------------ charts

/** A ring that fills clockwise from the top. Whatever is passed as [content] sits in the middle. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    color: Color = StudyTheme.colors.primary,
    trackColor: Color = StudyTheme.colors.muted,
    animate: Boolean = true,
    brush: Brush? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val target = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = target, animationSpec = tween(600), label = "ring")
    // With reduced motion the ring jumps straight to its value.
    val shown = if (animate && !LocalReduceMotion.current) animated else target
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(trackColor, 0f, 360f, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            if (shown > 0f && brush != null) {
                drawArc(brush, -90f, 360f * shown, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            } else if (shown > 0f) {
                drawArc(color, -90f, 360f * shown, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

/** Seven bars, Monday to Sunday. Tap a bar to see that day's total. */
@Composable
fun WeekChart(bars: List<DayBar>, modifier: Modifier = Modifier) {
    BarChart(bars, modifier, description = "Minutes studied this week")
}

/**
 * A bar chart drawn to one shared scale. Tapping a bar highlights it and shows its value in the
 * line above the chart. [labelEvery] thins out the labels when there are many bars (a month).
 */
@Composable
fun BarChart(
    bars: List<DayBar>,
    modifier: Modifier = Modifier,
    description: String = "Minutes studied",
    labelEvery: Int = 1,
    showValues: Boolean = bars.size <= 7,
) {
    val colors = StudyTheme.colors
    val highest = max(1, bars.maxOfOrNull { it.minutes } ?: 1)
    val summary = bars.filter { it.minutes > 0 }.joinToString(", ") { "${it.label} ${formatMinutes(it.minutes)}" }.ifEmpty { "nothing yet" }
    var selected by remember(bars.size) { mutableIntStateOf(-1) }
    val wide = bars.size <= 7
    Column(modifier.fillMaxWidth().semantics { contentDescription = "$description. $summary" }) {
        Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.CenterStart) {
            val bar = bars.getOrNull(selected)
            if (bar != null) {
                Text(
                    "${bar.label} · Studied: ${formatMinutes(bar.minutes)}",
                    Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp)).background(colors.card).padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.foreground,
                )
            } else {
                Text("Tap a bar for details", style = MaterialTheme.typography.labelSmall, color = colors.mutedForeground.copy(alpha = 0.8f))
            }
        }
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 2.dp)) {
            bars.forEachIndexed { index, bar ->
                val isSelected = index == selected
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.muted else Color.Transparent)
                        .clickable(role = Role.Button, onClickLabel = "Show ${bar.label}") { selected = if (isSelected) -1 else index },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (showValues) {
                        Text(
                            text = if (bar.minutes > 0) formatMinutes(bar.minutes) else "",
                            modifier = Modifier.height(16.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.mutedForeground,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        if (bar.minutes > 0) {
                            Box(
                                Modifier
                                    .fillMaxHeight(max(0.06f, bar.minutes / highest.toFloat()))
                                    .widthIn(max = 34.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(if (wide) 8.dp else 3.dp))
                                    .background(if (bar.isToday || wide) colors.primary else colors.primary.copy(alpha = 0.7f)),
                            )
                        } else {
                            Box(Modifier.height(4.dp).widthIn(max = 34.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (isSelected) colors.border else colors.muted))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val showLabel = wide || index == 0 || (index + 1) % labelEvery == 0
                    Text(
                        text = if (showLabel) bar.label else "",
                        fontSize = if (wide) 11.sp else 9.sp,
                        fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (bar.isToday) colors.foreground else colors.mutedForeground,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/** A thin rounded progress bar. [percent] runs from 0 to 100. */
@Composable
fun ProgressBar(percent: Int, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    Box(modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(colors.muted)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(percent.coerceIn(0, 100) / 100f).clip(CircleShape).background(colors.primary))
    }
}

/** Page heading with a one-line description under it. */
@Composable
fun ScreenTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.foreground)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
    }
}
