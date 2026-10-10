@file:OptIn(ExperimentalLayoutApi::class)

package com.rikhotso.studytime.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.DAY_NAMES
import com.rikhotso.studytime.data.DAY_NAMES_SHORT
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.PlannedSession
import com.rikhotso.studytime.data.Repeat
import com.rikhotso.studytime.data.Subject
import com.rikhotso.studytime.data.TimetableEntry
import com.rikhotso.studytime.data.formatMinuteOfDay
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.minutesOfDay
import com.rikhotso.studytime.data.nextFreeSlot
import com.rikhotso.studytime.data.parseDate
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.ChoicePill
import com.rikhotso.studytime.ui.components.ColorPicker
import com.rikhotso.studytime.ui.components.DateField
import com.rikhotso.studytime.ui.components.FieldLabel
import com.rikhotso.studytime.ui.components.FormDialog
import com.rikhotso.studytime.ui.components.MinutesSlider
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyTextField
import com.rikhotso.studytime.ui.components.TimeField
import com.rikhotso.studytime.ui.components.ToggleChip
import com.rikhotso.studytime.ui.theme.StudyTheme
import java.time.LocalDate
import java.time.LocalTime

/** The forms used across the planner: subjects, classes, planned sessions and the plan generator. */

/** Adds a subject, or edits [existing]. */
@Composable
fun SubjectDialog(data: AppData, viewModel: StudyViewModel, existing: Subject?, onDone: () -> Unit) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var teacher by remember { mutableStateOf(existing?.teacher.orEmpty()) }
    var color by remember { mutableStateOf(existing?.color ?: PastelColor.entries[data.subjects.size % PastelColor.entries.size]) }
    var goal by remember { mutableIntStateOf(existing?.goalMinutesPerDay ?: 60) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = if (existing == null) "New subject" else "Edit subject",
        confirmLabel = if (existing == null) "Add subject" else "Save",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.saveSubject(existing?.id, name, teacher, color, goal)
            if (error == null) onDone()
        },
        error = error,
    ) {
        StudyTextField(name, { name = it }, label = "Name", placeholder = "e.g. Physics")
        StudyTextField(teacher, { teacher = it }, label = "Teacher (optional)", capitalization = KeyboardCapitalization.Words)
        Column {
            FieldLabel("Colour")
            ColorPicker(selected = color, onSelect = { color = it })
        }
        Column {
            FieldLabel("Daily study goal: ${formatMinutes(goal)}")
            MinutesSlider(goal, { goal = it }, min = 15, max = 240, step = 15)
        }
    }
}

/** Adds a weekly class, or edits [existing]. A class may not overlap another class on the same day. */
@Composable
fun ClassDialog(data: AppData, viewModel: StudyViewModel, existing: TimetableEntry?, onDone: () -> Unit, presetDay: Int = LocalDate.now().dayOfWeek.value) {
    var subjectId by remember { mutableStateOf(existing?.subjectId ?: data.subjects.firstOrNull()?.id.orEmpty()) }
    var day by remember { mutableIntStateOf(existing?.dayOfWeek ?: presetDay) }
    var start by remember { mutableStateOf(existing?.startTime ?: "09:00") }
    var end by remember { mutableStateOf(existing?.endTime ?: "10:00") }
    var room by remember { mutableStateOf(existing?.room.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = if (existing == null) "Add a class" else "Edit class",
        confirmLabel = if (existing == null) "Add class" else "Save",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.saveEntry(existing?.id, subjectId, day, start, end, room)
            if (error == null) onDone()
        },
        error = error,
    ) {
        SelectField("Subject", data.subjects.map { it.id to it.name }, subjectId.ifEmpty { null }, { subjectId = it }, placeholder = "Pick subject")
        SelectField("Day", DAY_NAMES.mapIndexed { index, label -> (index + 1) to label }, day, { day = it })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            TimeField("Start", start, { start = it }, Modifier.weight(1f))
            TimeField("End", end, { end = it }, Modifier.weight(1f))
        }
        StudyTextField(room, { room = it }, label = "Room", placeholder = "optional")
        if (existing != null) {
            StudyButton(
                "Delete this class",
                onClick = {
                    viewModel.removeEntry(existing.id)
                    onDone()
                },
                kind = ButtonKind.Danger,
                small = true,
                leadingIcon = R.drawable.ic_trash_2,
            )
        }
    }
}

/**
 * Adds a planned study session, or edits [existing]. A time that clashes with a class, an exam
 * or another session is refused, and "Suggest a free time" fills in the next gap in your study hours.
 */
@Composable
fun PlannedDialog(
    data: AppData,
    viewModel: StudyViewModel,
    existing: PlannedSession?,
    onDone: () -> Unit,
    presetDate: String = LocalDate.now().toString(),
    presetTaskId: String = "",
) {
    val colors = StudyTheme.colors
    val presetTask = data.tasks.firstOrNull { it.id == presetTaskId }
    var title by remember { mutableStateOf(existing?.title ?: presetTask?.title.orEmpty()) }
    var subjectId by remember { mutableStateOf(existing?.subjectId ?: presetTask?.subjectId.orEmpty()) }
    var date by remember { mutableStateOf(existing?.date ?: presetDate) }
    var start by remember { mutableStateOf(existing?.startTime ?: "16:00") }
    var duration by remember { mutableIntStateOf(existing?.durationMinutes ?: presetTask?.estimateMinutes?.takeIf { it in 15..180 } ?: 45) }
    var repeat by remember { mutableStateOf(existing?.repeat ?: Repeat.None) }
    var repeatDays by remember { mutableStateOf(existing?.repeatDays ?: emptyList()) }
    var link by remember {
        mutableStateOf(
            when {
                existing != null && existing.taskId.isNotEmpty() -> "task:${existing.taskId}"
                existing != null && existing.topicId.isNotEmpty() -> "topic:${existing.topicId}"
                presetTask != null -> "task:${presetTask.id}"
                else -> ""
            },
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = if (existing == null) "Plan a study session" else "Edit session",
        confirmLabel = if (existing == null) "Add session" else "Save",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.savePlanned(
                id = existing?.id,
                title = title,
                subjectId = subjectId,
                date = date,
                startTime = start,
                durationMinutes = duration,
                repeat = repeat,
                repeatDays = repeatDays,
                taskId = if (link.startsWith("task:")) link.removePrefix("task:") else "",
                topicId = if (link.startsWith("topic:")) link.removePrefix("topic:") else "",
            )
            if (error == null) onDone()
        },
        error = error,
    ) {
        StudyTextField(title, { title = it }, label = "Title", placeholder = "e.g. Revise chapter 4")
        SelectField("Subject", listOf("" to "No subject") + data.subjects.map { it.id to it.name }, subjectId, { subjectId = it })
        DateField("Date", date, { date = it })
        TimeField("Start", start, { start = it })
        Column {
            FieldLabel("Length: ${formatMinutes(duration)} (ends ${formatMinuteOfDay(minutesOfDay(start) + duration)})")
            MinutesSlider(duration, { duration = it }, min = 15, max = 180, step = 5)
        }
        StudyButton(
            "Suggest a free time",
            onClick = {
                val from = parseDate(date)?.takeIf { !it.isBefore(LocalDate.now()) } ?: LocalDate.now()
                val nowMinute = LocalTime.now().let { it.hour * 60 + it.minute }
                // When editing, look as if this session was not booked yet so it can keep or shift its own slot.
                val others = data.copy(plannedSessions = data.plannedSessions.filterNot { it.id == existing?.id })
                val slot = nextFreeSlot(others, from, if (from == LocalDate.now()) nowMinute else 0, duration)
                if (slot == null) {
                    error = "No free time was found in the next two weeks. Widen your study hours in Settings."
                } else {
                    date = slot.first.toString()
                    start = formatMinuteOfDay(slot.second)
                    error = null
                }
            },
            kind = ButtonKind.Secondary,
            small = true,
            leadingIcon = R.drawable.ic_wand_sparkles,
        )
        SelectField("Repeat", Repeat.entries.map { it to it.label }, repeat, { repeat = it })
        if (repeat == Repeat.Days) {
            Column {
                FieldLabel("Repeat on")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_NAMES_SHORT.forEachIndexed { index, name ->
                        val day = index + 1
                        ToggleChip(name, day in repeatDays, onClick = { repeatDays = if (day in repeatDays) repeatDays - day else repeatDays + day })
                    }
                }
            }
        }
        val tasks = data.tasks.filter { !it.done && (subjectId.isEmpty() || it.subjectId == subjectId || it.subjectId.isEmpty()) }
        val topics = data.topics.filter { subjectId.isNotEmpty() && it.subjectId == subjectId }
        if (tasks.isNotEmpty() || topics.isNotEmpty() || link.isNotEmpty()) {
            SelectField(
                "For (optional)",
                listOf("" to "Nothing in particular") + tasks.map { "task:${it.id}" to "Task: ${it.title}" } + topics.map { "topic:${it.id}" to "Topic: ${it.name}" },
                link,
                { link = it },
            )
        }
        if (existing != null && existing.repeat != Repeat.None) {
            Text("Changes apply to every repeat of this session.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
        }
    }
}

/** Moves one occurrence of a session to another day and time. */
@Composable
fun MovePlannedDialog(viewModel: StudyViewModel, session: PlannedSession, day: String, onDone: () -> Unit) {
    val colors = StudyTheme.colors
    var date by remember { mutableStateOf(day) }
    var start by remember { mutableStateOf(session.startTime) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = "Move session",
        confirmLabel = "Move",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.movePlanned(session.id, day, date, start)
            if (error == null) onDone()
        },
        error = error,
    ) {
        Text(session.title, style = MaterialTheme.typography.titleMedium, color = colors.foreground)
        DateField("New date", date, { date = it })
        TimeField("New start time", start, { start = it })
        if (session.repeat != Repeat.None) {
            Text("Only this day moves. The other repeats stay where they are.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
        }
    }
}

/** Asks how far ahead and how long each session should be, then builds a study plan. */
@Composable
fun GeneratePlanDialog(data: AppData, viewModel: StudyViewModel, onDone: () -> Unit) {
    val colors = StudyTheme.colors
    var days by remember { mutableIntStateOf(7) }
    var minutes by remember { mutableIntStateOf(45) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = "Generate a study plan",
        confirmLabel = "Generate",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.generateStudyPlan(days, minutes)
            if (error == null) onDone()
        },
        error = error,
    ) {
        Text(
            "studytime fills your free study hours with sessions, up to your daily goal of ${formatMinutes(data.profile.dailyGoalMinutes)}. " +
                "Subjects with an exam coming up, an urgent task or shaky topics get more time. It never overlaps your classes, exams or sessions you added yourself.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mutedForeground,
        )
        Column {
            FieldLabel("Plan ahead for")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3 to "3 days", 7 to "1 week", 14 to "2 weeks").forEach { (value, label) ->
                    ChoicePill(label, selected = days == value, onClick = { days = value }, modifier = Modifier.weight(1f))
                }
            }
        }
        Column {
            FieldLabel("Session length")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 45, 60).forEach { value ->
                    ChoicePill("${value}m", selected = minutes == value, onClick = { minutes = value }, modifier = Modifier.weight(1f))
                }
            }
        }
        if (data.plannedSessions.any { it.generated && it.completedDates.isEmpty() && it.date >= LocalDate.now().toString() }) {
            Text(
                "Upcoming sessions from the last generated plan will be replaced.",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
        }
    }
}
