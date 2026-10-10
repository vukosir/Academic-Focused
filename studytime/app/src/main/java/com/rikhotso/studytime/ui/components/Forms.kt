@file:OptIn(ExperimentalMaterial3Api::class)

package com.rikhotso.studytime.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.data.DAY_NAMES_SHORT
import com.rikhotso.studytime.data.MONTH_NAMES_SHORT
import com.rikhotso.studytime.data.formatTime
import com.rikhotso.studytime.data.minutesOfDay
import com.rikhotso.studytime.data.parseDate
import com.rikhotso.studytime.ui.theme.StudyTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

/** Inputs and dialogs. Every form in the app is built from these. */

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = StudyTheme.colors.foreground,
    )
}

@Composable
fun StudyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    singleLine: Boolean = true,
    minLines: Int = if (singleLine) 1 else 2,
) {
    val colors = StudyTheme.colors
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, color = colors.mutedForeground) },
            singleLine = singleLine,
            minLines = minLines,
            shape = FieldShape,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.foreground,
                unfocusedTextColor = colors.foreground,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.border,
                cursorColor = colors.primary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
        )
    }
}

/** The outlined box that the select, time and date fields share. Tapping it opens a picker. */
@Composable
private fun PickerBox(
    text: String,
    isPlaceholder: Boolean,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StudyTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .border(1.dp, colors.border, FieldShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isPlaceholder) colors.mutedForeground else colors.foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        StudyIcon(icon, Modifier.size(18.dp), tint = colors.mutedForeground)
    }
}

/** A drop-down. [options] pairs each stored value with the text shown for it. */
@Composable
fun <T> SelectField(
    label: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pick one",
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        Box {
            PickerBox(
                text = current?.second ?: placeholder,
                isPlaceholder = current == null,
                icon = R.drawable.ic_chevron_down,
                onClick = { open = true },
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            onSelect(option.first)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

/** A time of day as "HH:mm". Opens a small dialog with hour and minute inputs. */
@Composable
fun TimeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val colors = StudyTheme.colors
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        PickerBox(text = value, isPlaceholder = false, icon = R.drawable.ic_clock, onClick = { open = true })
    }
    if (open) {
        val minutes = minutesOfDay(value)
        val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                StudyButton("Set time", onClick = {
                    onChange(formatTime(state.hour, state.minute))
                    open = false
                })
            },
            dismissButton = { StudyButton("Cancel", onClick = { open = false }, kind = ButtonKind.Ghost) },
            title = { Text(label, style = MaterialTheme.typography.headlineSmall) },
            text = { TimeInput(state = state) },
            containerColor = colors.background,
            titleContentColor = colors.foreground,
            textContentColor = colors.foreground,
            shape = CardShape,
        )
    }
}

/** "2026-10-13" becomes "Tue 13 Oct 2026" for display. */
fun friendlyDate(iso: String): String {
    val date = parseDate(iso) ?: return "Pick a date"
    return "${DAY_NAMES_SHORT[date.dayOfWeek.value - 1]} ${date.dayOfMonth} ${MONTH_NAMES_SHORT[date.monthValue - 1]} ${date.year}"
}

/** A calendar date stored as an ISO string. Opens the Material date picker. */
@Composable
fun DateField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        PickerBox(
            text = friendlyDate(value),
            isPlaceholder = parseDate(value) == null,
            icon = R.drawable.ic_calendar_days,
            onClick = { open = true },
        )
    }
    if (open) {
        val start = (parseDate(value) ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = start)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                StudyButton("Set date", onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        onChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    open = false
                })
            },
            dismissButton = { StudyButton("Cancel", onClick = { open = false }, kind = ButtonKind.Ghost) },
        ) {
            DatePicker(state = state)
        }
    }
}

/** A slider that snaps to [step] minutes. */
@Composable
fun MinutesSlider(value: Int, onChange: (Int) -> Unit, min: Int, max: Int, step: Int, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    Slider(
        value = value.toFloat(),
        onValueChange = { raw -> onChange(((raw / step).roundToInt() * step).coerceIn(min, max)) },
        modifier = modifier.fillMaxWidth(),
        valueRange = min.toFloat()..max.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = colors.primary,
            activeTrackColor = colors.primary,
            inactiveTrackColor = colors.primary.copy(alpha = 0.2f),
        ),
    )
}

/** The dialog every "add" form lives in. [error] shows in red above the buttons. */
@Composable
fun FormDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = StudyTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { StudyButton(confirmLabel, onClick = onConfirm) },
        dismissButton = { StudyButton("Cancel", onClick = onDismiss, kind = ButtonKind.Ghost) },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                content()
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.labelLarge, color = colors.destructive)
                }
            }
        },
        containerColor = colors.background,
        titleContentColor = colors.foreground,
        textContentColor = colors.foreground,
        shape = CardShape,
    )
}

/** A row of rounded tabs. The selected one lifts onto the page colour. */
@Composable
fun PillTabs(tabs: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    val scroll = rememberScrollState()
    // On a narrow phone the row can be wider than the screen, so bring the chosen tab into view.
    LaunchedEffect(selectedIndex, scroll.maxValue) {
        if (scroll.maxValue > 0) {
            scroll.animateScrollTo(if (selectedIndex * 2 >= tabs.size) scroll.maxValue else 0)
        }
    }
    Row(
        modifier.horizontalScroll(scroll).clip(CircleShape).background(colors.muted).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) colors.background else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) colors.foreground else colors.mutedForeground,
                )
            }
        }
    }
}

/** A choice chip used for the timer presets. Fills the width it is given. */
@Composable
fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    Box(
        modifier
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.muted)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.mutedForeground,
            maxLines = 1,
        )
    }
}

/** A small toggle chip, used to pick group members. */
@Composable
fun ToggleChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.primary else colors.muted)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onPrimary else colors.mutedForeground,
        )
    }
}
