@file:OptIn(ExperimentalLayoutApi::class)

package com.rikhotso.studytime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.DAY_NAMES
import com.rikhotso.studytime.data.DAY_NAMES_SHORT
import com.rikhotso.studytime.data.MONTH_NAMES_SHORT
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.PlannedSession
import com.rikhotso.studytime.data.Repeat
import com.rikhotso.studytime.data.Subject
import com.rikhotso.studytime.data.TimetableEntry
import com.rikhotso.studytime.data.classesOn
import com.rikhotso.studytime.data.examsOn
import com.rikhotso.studytime.data.formatMinuteOfDay
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.freeSlots
import com.rikhotso.studytime.data.isCompletedOn
import com.rikhotso.studytime.data.minutesOfDay
import com.rikhotso.studytime.data.missedSessions
import com.rikhotso.studytime.data.mondayOf
import com.rikhotso.studytime.data.plannedOn
import com.rikhotso.studytime.data.shortDate
import com.rikhotso.studytime.data.topicsDueForReview
import com.rikhotso.studytime.ui.AppNav
import com.rikhotso.studytime.ui.AppTab
import com.rikhotso.studytime.ui.PlannerTab
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.CheckCircle
import com.rikhotso.studytime.ui.components.ColorDot
import com.rikhotso.studytime.ui.components.ConfirmDialog
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.IconAction
import com.rikhotso.studytime.ui.components.MenuItem
import com.rikhotso.studytime.ui.components.MoreMenu
import com.rikhotso.studytime.ui.components.NoticeDialog
import com.rikhotso.studytime.ui.components.PillTabs
import com.rikhotso.studytime.ui.components.RowText
import com.rikhotso.studytime.ui.components.ScreenTitle
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.SoftRow
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.components.Tag
import com.rikhotso.studytime.ui.components.TextLink
import com.rikhotso.studytime.ui.components.TileShape
import com.rikhotso.studytime.ui.components.TrashButton
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft
import com.rikhotso.studytime.ui.theme.solid
import java.time.LocalDate
import java.time.LocalTime

/** The planner: the weekly timetable, the day view, the month calendar, tasks and exams. */
@Composable
fun PlannerScreen(data: AppData, viewModel: StudyViewModel, nav: AppNav, tab: PlannerTab, onTab: (PlannerTab) -> Unit) {
    ScreenColumn {
        ScreenTitle(
            "Planner",
            when (tab) {
                PlannerTab.Timetable -> "Your subjects and weekly timetable."
                PlannerTab.Today -> "Everything planned for one day."
                PlannerTab.Month -> "Sessions, deadlines and exams across the month."
                PlannerTab.Tasks -> "Homework, revision and everything else to do."
                PlannerTab.Exams -> "Countdowns and how revision is going."
            },
        )
        PillTabs(PlannerTab.entries.map { it.label }, selectedIndex = tab.ordinal, onSelect = { onTab(PlannerTab.entries[it]) })
        when (tab) {
            PlannerTab.Timetable -> TimetableTab(data, viewModel, nav)
            PlannerTab.Today -> DayTab(data, viewModel, nav)
            PlannerTab.Month -> MonthTab(data, viewModel, nav)
            PlannerTab.Tasks -> TasksTab(data, viewModel, nav)
            PlannerTab.Exams -> ExamsTab(data, viewModel, nav)
        }
    }
}

// ------------------------------------------------------------------ timetable

@Composable
private fun TimetableTab(data: AppData, viewModel: StudyViewModel, nav: AppNav) {
    val colors = StudyTheme.colors
    var dialog by remember { mutableStateOf("") }
    var editingClass by remember { mutableStateOf<TimetableEntry?>(null) }
    var editingSession by remember { mutableStateOf<PlannedSession?>(null) }
    var deleting by remember { mutableStateOf<Subject?>(null) }
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val monday = mondayOf(today).plusWeeks(weekOffset.toLong())

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyButton(
                "Class",
                onClick = { dialog = if (data.subjects.isEmpty()) "subject" else "class" },
                kind = ButtonKind.Secondary,
                small = true,
                leadingIcon = R.drawable.ic_plus,
            )
            StudyButton("Subject", onClick = { dialog = "subject" }, small = true, leadingIcon = R.drawable.ic_plus)
            StudyButton("Session", onClick = { dialog = "session" }, kind = ButtonKind.Ghost, small = true, leadingIcon = R.drawable.ic_plus)
            StudyButton("Generate plan", onClick = { dialog = "generate" }, kind = ButtonKind.Ghost, small = true, leadingIcon = R.drawable.ic_wand_sparkles)
        }

        SectionCard(title = "Subjects") {
            if (data.subjects.isEmpty()) {
                EmptyHint("No subjects yet. Add your first subject to start colour-coding your week.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.subjects.forEach { subject ->
                        SoftRow(subject.color, Modifier.clip(TileShape).clickable(role = Role.Button, onClickLabel = "Open ${subject.name}") { nav.openSubject(subject.id) }) {
                            ColorDot(subject.color)
                            RowText(
                                title = subject.name,
                                subtitle = (if (subject.teacher.isNotEmpty()) "${subject.teacher} · " else "") + "${formatMinutes(subject.goalMinutesPerDay)}/day goal",
                            )
                            TrashButton(onClick = { deleting = subject }, description = "Delete ${subject.name}")
                        }
                    }
                    Text("Tap a subject for its topics, notes, practice tests and grades.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                }
            }
        }

        SectionCard(
            title = "Weekly timetable",
            action = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconAction(R.drawable.ic_chevron_left, "Previous week", onClick = { weekOffset -= 1 })
                    Text(
                        if (weekOffset == 0) "This week" else "${monday.dayOfMonth} ${MONTH_NAMES_SHORT[monday.monthValue - 1]}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.mutedForeground,
                    )
                    IconAction(R.drawable.ic_chevron_right, "Next week", onClick = { weekOffset += 1 })
                }
            },
        ) {
            if (data.subjects.isEmpty() && data.entries.isEmpty() && data.plannedSessions.isEmpty()) {
                EmptyHint("Add a subject first, then schedule your classes here.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DAY_NAMES_SHORT.forEachIndexed { index, dayName ->
                        val day = monday.plusDays(index.toLong())
                        val classes = classesOn(data, day)
                        val sessions = plannedOn(data, day)
                        val exams = examsOn(data, day)
                        val isToday = day == today
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(TileShape)
                                .border(1.dp, if (isToday) colors.primary.copy(alpha = 0.6f) else colors.border, TileShape)
                                .padding(10.dp)
                                .semantics(mergeDescendants = false) { contentDescription = DAY_NAMES[index] },
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (weekOffset == 0) dayName else "$dayName ${day.dayOfMonth}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isToday) colors.primary else colors.mutedForeground,
                            )
                            if (classes.isEmpty() && sessions.isEmpty() && exams.isEmpty()) {
                                // The quiet little line that marks an empty day.
                                Box(Modifier.padding(vertical = 14.dp).width(14.dp).height(1.5.dp).background(colors.mutedForeground.copy(alpha = 0.45f)))
                            }
                            classes.forEach { entry ->
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(entry.color.soft)
                                        .clickable(role = Role.Button, onClickLabel = "Edit ${entry.subjectName} class") { editingClass = entry }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(entry.subjectName, style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                                    Text(
                                        "${entry.startTime} - ${entry.endTime}" + if (entry.room.isNotEmpty()) " · ${entry.room}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.mutedForeground,
                                    )
                                }
                            }
                            sessions.forEach { session ->
                                val done = session.isCompletedOn(day)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, data.subjectColor(session.subjectId).solid, RoundedCornerShape(12.dp))
                                        .clickable(role = Role.Button, onClickLabel = "Edit ${session.title}") { editingSession = session }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    StudyIcon(if (done) R.drawable.ic_circle_check else R.drawable.ic_clock, Modifier.size(16.dp), tint = colors.mutedForeground)
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            session.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.foreground,
                                            textDecoration = if (done) TextDecoration.LineThrough else null,
                                        )
                                        Text(
                                            "Study · ${session.startTime} - ${formatMinuteOfDay(minutesOfDay(session.startTime) + session.durationMinutes)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.mutedForeground,
                                        )
                                    }
                                }
                            }
                            exams.forEach { exam ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.accent).padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    StudyIcon(R.drawable.ic_graduation_cap, Modifier.size(16.dp), tint = colors.onAccent)
                                    Column(Modifier.weight(1f)) {
                                        Text(exam.title, style = MaterialTheme.typography.titleSmall, color = colors.onAccent)
                                        Text("Exam · ${exam.time}" + if (exam.location.isNotEmpty()) " · ${exam.location}" else "", style = MaterialTheme.typography.bodySmall, color = colors.onAccent)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (dialog) {
        "subject" -> SubjectDialog(data, viewModel, null, onDone = { dialog = "" })
        "class" -> ClassDialog(data, viewModel, null, onDone = { dialog = "" })
        "session" -> PlannedDialog(data, viewModel, null, onDone = { dialog = "" })
        "generate" -> GeneratePlanDialog(data, viewModel, onDone = { dialog = "" })
    }
    editingClass?.let { entry -> ClassDialog(data, viewModel, entry, onDone = { editingClass = null }) }
    editingSession?.let { session -> PlannedDialog(data, viewModel, session, onDone = { editingSession = null }) }
    deleting?.let { subject ->
        ConfirmDialog(
            title = "Delete ${subject.name}?",
            text = "Its classes, revision topics and practice tests are deleted too. Your study history and grades are kept, and its tasks, notes and exams stay without a subject.",
            confirmLabel = "Delete subject",
            onConfirm = {
                viewModel.removeSubject(subject.id)
                deleting = null
            },
            onDismiss = { deleting = null },
            danger = true,
        )
    }
}

// ------------------------------------------------------------------ one day

/** A planned session in a list, with a tick box and its actions. Shared by the day and month views. */
@Composable
fun PlannedSessionRow(
    data: AppData,
    viewModel: StudyViewModel,
    session: PlannedSession,
    day: LocalDate,
    onStartTimer: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val colors = StudyTheme.colors
    val done = session.isCompletedOn(day)
    val color = data.subjectColor(session.subjectId)
    Row(
        Modifier.fillMaxWidth().clip(TileShape).background(color.soft).padding(start = 4.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CheckCircle(done, onToggle = { viewModel.togglePlannedDone(session.id, day.toString()) }, description = if (done) "Mark ${session.title} as not done" else "Mark ${session.title} as done")
        Column(Modifier.weight(1f)) {
            Text(
                session.title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.foreground,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            )
            Text(
                "${session.startTime} - ${formatMinuteOfDay(minutesOfDay(session.startTime) + session.durationMinutes)} · ${data.subjectName(session.subjectId)}" +
                    (if (session.repeat != Repeat.None) " · repeats" else "") + (if (session.generated) " · generated" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
        }
        if (!done) {
            IconAction(R.drawable.ic_play, "Start a focus round for ${session.title}", onClick = {
                val problem = viewModel.startPlanned(session.id, day.toString())
                if (problem == null) onStartTimer() else onMessage(problem)
            }, tint = colors.foreground)
        }
        MoreMenu(
            "More actions for ${session.title}",
            listOf(
                MenuItem("Edit") { onEdit() },
                MenuItem("Move") { onMove() },
                MenuItem("Copy to next free time") { viewModel.duplicatePlanned(session.id)?.let(onMessage) },
                MenuItem(if (session.repeat == Repeat.None) "Delete" else "Skip this day", danger = true) { viewModel.skipPlanned(session.id, day.toString()) },
            ) + if (session.repeat == Repeat.None) emptyList() else listOf(MenuItem("Delete every repeat", danger = true) { viewModel.removePlanned(session.id) }),
        )
    }
}

@Composable
private fun DayTab(data: AppData, viewModel: StudyViewModel, nav: AppNav) {
    val colors = StudyTheme.colors
    val today = LocalDate.now()
    val nowMinute = LocalTime.now().let { it.hour * 60 + it.minute }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val day = today.plusDays(offset.toLong())
    var dialog by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<PlannedSession?>(null) }
    var moving by remember { mutableStateOf<PlannedSession?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    val classes = classesOn(data, day)
    val sessions = plannedOn(data, day)
    val exams = examsOn(data, day)
    val missed = missedSessions(data, today, nowMinute)
    val dueTopics = topicsDueForReview(data, today)

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(R.drawable.ic_chevron_left, "Previous day", onClick = { offset -= 1 }, tint = colors.foreground)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (offset) {
                        0 -> "Today"
                        1 -> "Tomorrow"
                        -1 -> "Yesterday"
                        else -> DAY_NAMES[day.dayOfWeek.value - 1]
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.foreground,
                )
                Text(shortDate(day.toString()) + " ${day.year}", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            }
            IconAction(R.drawable.ic_chevron_right, "Next day", onClick = { offset += 1 }, tint = colors.foreground)
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyButton("Session", onClick = { dialog = "session" }, small = true, leadingIcon = R.drawable.ic_plus)
            StudyButton("Generate plan", onClick = { dialog = "generate" }, kind = ButtonKind.Secondary, small = true, leadingIcon = R.drawable.ic_wand_sparkles)
            if (offset != 0) StudyButton("Back to today", onClick = { offset = 0 }, kind = ButtonKind.Ghost, small = true)
        }

        SectionCard(title = "Schedule") {
            if (classes.isEmpty() && sessions.isEmpty() && exams.isEmpty()) {
                EmptyHint("Nothing planned for this day. Add a session, or let studytime generate a plan from your free time.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exams.forEach { exam ->
                        Row(
                            Modifier.fillMaxWidth().clip(TileShape).background(colors.accent).padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StudyIcon(R.drawable.ic_graduation_cap, Modifier.size(18.dp), tint = colors.onAccent)
                            Column(Modifier.weight(1f)) {
                                Text(exam.title, style = MaterialTheme.typography.titleSmall, color = colors.onAccent)
                                Text("Exam at ${exam.time}" + if (exam.location.isNotEmpty()) " · ${exam.location}" else "", style = MaterialTheme.typography.bodySmall, color = colors.onAccent)
                            }
                        }
                    }
                    classes.forEach { entry ->
                        SoftRow(entry.color, Modifier.padding(vertical = 2.dp)) {
                            StudyIcon(R.drawable.ic_book_open, Modifier.size(18.dp), tint = colors.mutedForeground)
                            RowText(entry.subjectName, "Class · ${entry.startTime} - ${entry.endTime}" + if (entry.room.isNotEmpty()) " · ${entry.room}" else "")
                            Box(Modifier.height(40.dp))
                        }
                    }
                    sessions.forEach { session ->
                        PlannedSessionRow(
                            data, viewModel, session, day,
                            onStartTimer = { nav.goTab(AppTab.Timer) },
                            onEdit = { editing = session },
                            onMove = { moving = session },
                            onMessage = { notice = it },
                        )
                    }
                }
            }
            val free = freeSlots(data, day, minMinutes = 25, notBefore = if (offset == 0) nowMinute else 0)
            Text(
                when {
                    offset < 0 -> ""
                    free.isEmpty() -> "No free study time left inside your study hours for this day."
                    else -> "Free in your study hours: " + free.joinToString(", ") { "${formatMinuteOfDay(it.start)} - ${formatMinuteOfDay(it.end)}" }
                },
                Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
        }

        if (missed.isNotEmpty()) {
            SectionCard(title = "Missed sessions", action = { TextLink("Reschedule all", onClick = { viewModel.rescheduleAllMissed() }) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    missed.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().clip(TileShape).border(1.dp, colors.border, TileShape).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RowText(item.session.title, "${shortDate(item.day.toString())} at ${item.session.startTime} · ${formatMinutes(item.session.durationMinutes)}")
                            StudyButton(
                                "Reschedule",
                                onClick = { viewModel.rescheduleMissed(item.session.id, item.day.toString())?.let { notice = it } },
                                kind = ButtonKind.Secondary,
                                small = true,
                            )
                            MoreMenu(
                                "More actions for ${item.session.title}",
                                listOf(
                                    MenuItem("I did this one") { viewModel.togglePlannedDone(item.session.id, item.day.toString()) },
                                    MenuItem("Skip it", danger = true) { viewModel.skipPlanned(item.session.id, item.day.toString()) },
                                ),
                            )
                        }
                    }
                    Text("Reschedule moves a session into the next free time inside your study hours.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                }
            }
        }

        if (dueTopics.isNotEmpty() && offset == 0) {
            SectionCard(title = "Due for revision") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dueTopics.take(6).forEach { topic ->
                        SoftRow(data.subjectColor(topic.subjectId)) {
                            RowText(topic.name, "${data.subjectName(topic.subjectId)} · reviewed ${topic.reviewCount} time${if (topic.reviewCount == 1) "" else "s"}")
                            StudyButton("Reviewed", onClick = { viewModel.reviewTopic(topic.id) }, kind = ButtonKind.Secondary, small = true)
                            IconAction(R.drawable.ic_play, "Start a focus round for ${topic.name}", onClick = {
                                val problem = viewModel.prepareTimerForTopic(topic.id)
                                if (problem == null) nav.goTab(AppTab.Timer) else notice = problem
                            }, tint = colors.foreground)
                        }
                    }
                    if (dueTopics.size > 6) Text("and ${dueTopics.size - 6} more", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                }
            }
        }
    }

    when (dialog) {
        "session" -> PlannedDialog(data, viewModel, null, onDone = { dialog = "" }, presetDate = (if (day.isBefore(today)) today else day).toString())
        "generate" -> GeneratePlanDialog(data, viewModel, onDone = { dialog = "" })
    }
    editing?.let { session -> PlannedDialog(data, viewModel, session, onDone = { editing = null }) }
    moving?.let { session -> MovePlannedDialog(viewModel, session, day.toString(), onDone = { moving = null }) }
    notice?.let { text -> NoticeDialog("Not possible right now", text, onDone = { notice = null }) }
}

// ------------------------------------------------------------------ month

@Composable
private fun MonthTab(data: AppData, viewModel: StudyViewModel, nav: AppNav) {
    val colors = StudyTheme.colors
    val today = LocalDate.now()
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedIso by rememberSaveable { mutableStateOf(today.toString()) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PlannedSession?>(null) }
    var moving by remember { mutableStateOf<PlannedSession?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    val first = today.withDayOfMonth(1).plusMonths(monthOffset.toLong())
    val selected = LocalDate.parse(selectedIso)
    val leading = first.dayOfWeek.value - 1
    val cells = leading + first.lengthOfMonth()
    val rows = (cells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconAction(R.drawable.ic_chevron_left, "Previous month", onClick = { monthOffset -= 1 }, tint = colors.foreground)
                Text(
                    "${first.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${first.year}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.foreground,
                    textAlign = TextAlign.Center,
                )
                IconAction(R.drawable.ic_chevron_right, "Next month", onClick = { monthOffset += 1 }, tint = colors.foreground)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                DAY_NAMES_SHORT.forEach { name ->
                    Text(name.take(1), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = colors.mutedForeground, textAlign = TextAlign.Center)
                }
            }
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (column in 0..6) {
                        val dayNumber = row * 7 + column - leading + 1
                        if (dayNumber < 1 || dayNumber > first.lengthOfMonth()) {
                            Box(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val day = first.withDayOfMonth(dayNumber)
                            val iso = day.toString()
                            val hasSession = plannedOn(data, day).isNotEmpty()
                            val hasTask = data.tasks.any { !it.done && it.dueDate == iso }
                            val hasExam = data.exams.any { it.date == iso }
                            val isSelected = day == selected
                            val what = listOfNotNull("sessions".takeIf { hasSession }, "tasks due".takeIf { hasTask }, "exam".takeIf { hasExam }).joinToString(", ")
                            Column(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colors.primary else Color.Transparent)
                                    .border(1.dp, if (day == today && !isSelected) colors.primary else Color.Transparent, RoundedCornerShape(12.dp))
                                    .selectable(selected = isSelected, role = Role.Button, onClick = { selectedIso = iso })
                                    .semantics { contentDescription = shortDate(iso) + if (what.isNotEmpty()) ": $what" else "" },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    dayNumber.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) colors.onPrimary else colors.foreground,
                                )
                                Row(Modifier.height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (hasSession) Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSelected) colors.onPrimary else PastelColor.Lavender.solid))
                                    if (hasTask) Box(Modifier.size(5.dp).clip(CircleShape).background(PastelColor.BabyBlue.solid))
                                    if (hasExam) Box(Modifier.size(5.dp).clip(CircleShape).background(colors.destructive))
                                }
                            }
                        }
                    }
                }
            }
            FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Legend(PastelColor.Lavender.solid, "Study session")
                Legend(PastelColor.BabyBlue.solid, "Task due")
                Legend(colors.destructive, "Exam")
            }
        }

        SectionCard(
            title = shortDate(selectedIso),
            action = {
                if (!selected.isBefore(today)) StudyButton("Session", onClick = { adding = true }, small = true, leadingIcon = R.drawable.ic_plus)
            },
        ) {
            val classes = classesOn(data, selected)
            val sessions = plannedOn(data, selected)
            val tasks = data.tasks.filter { it.dueDate == selectedIso }
            val exams = examsOn(data, selected)
            if (classes.isEmpty() && sessions.isEmpty() && tasks.isEmpty() && exams.isEmpty()) {
                EmptyHint("Nothing on this day.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exams.forEach { exam ->
                        Row(
                            Modifier.fillMaxWidth().clip(TileShape).background(colors.accent).padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StudyIcon(R.drawable.ic_graduation_cap, Modifier.size(18.dp), tint = colors.onAccent)
                            Column(Modifier.weight(1f)) {
                                Text(exam.title, style = MaterialTheme.typography.titleSmall, color = colors.onAccent)
                                Text("Exam at ${exam.time}" + if (exam.location.isNotEmpty()) " · ${exam.location}" else "", style = MaterialTheme.typography.bodySmall, color = colors.onAccent)
                            }
                        }
                    }
                    classes.forEach { entry ->
                        SoftRow(entry.color) {
                            StudyIcon(R.drawable.ic_book_open, Modifier.size(18.dp), tint = colors.mutedForeground)
                            RowText(entry.subjectName, "Class · ${entry.startTime} - ${entry.endTime}")
                            Box(Modifier.height(40.dp))
                        }
                    }
                    sessions.forEach { session ->
                        PlannedSessionRow(
                            data, viewModel, session, selected,
                            onStartTimer = { nav.goTab(AppTab.Timer) },
                            onEdit = { editing = session },
                            onMove = { moving = session },
                            onMessage = { notice = it },
                        )
                    }
                    tasks.forEach { task ->
                        Row(
                            Modifier.fillMaxWidth().clip(TileShape).border(1.dp, colors.border, TileShape).padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            CheckCircle(task.done, onToggle = { viewModel.toggleTask(task.id) }, description = "Toggle ${task.title}")
                            Column(Modifier.weight(1f)) {
                                Text(task.title, style = MaterialTheme.typography.titleSmall, color = colors.foreground, textDecoration = if (task.done) TextDecoration.LineThrough else null)
                                Text("Task due" + (if (task.dueTime.isNotEmpty()) " at ${task.dueTime}" else "") + " · ${data.subjectName(task.subjectId)}", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                            }
                            Tag(task.priority.label)
                        }
                    }
                }
            }
        }
    }

    if (adding) PlannedDialog(data, viewModel, null, onDone = { adding = false }, presetDate = selectedIso)
    editing?.let { session -> PlannedDialog(data, viewModel, session, onDone = { editing = null }) }
    moving?.let { session -> MovePlannedDialog(viewModel, session, selectedIso, onDone = { moving = null }) }
    notice?.let { text -> NoticeDialog("Not possible right now", text, onDone = { notice = null }) }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = StudyTheme.colors.mutedForeground)
    }
}
