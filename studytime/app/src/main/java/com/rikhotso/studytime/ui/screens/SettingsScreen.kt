@file:OptIn(ExperimentalLayoutApi::class)

package com.rikhotso.studytime.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.Accent
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.DAY_NAMES
import com.rikhotso.studytime.data.ThemeMode
import com.rikhotso.studytime.data.TimerFace
import com.rikhotso.studytime.data.TimerSound
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.levelInfo
import com.rikhotso.studytime.timer.TimerTones
import com.rikhotso.studytime.timer.canNotify
import com.rikhotso.studytime.ui.NOTIFICATION_PERMISSION
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.ChoicePill
import com.rikhotso.studytime.ui.components.ConfirmDialog
import com.rikhotso.studytime.ui.components.FieldLabel
import com.rikhotso.studytime.ui.components.IconAction
import com.rikhotso.studytime.ui.components.InfoNote
import com.rikhotso.studytime.ui.components.MinutesSlider
import com.rikhotso.studytime.ui.components.NoticeDialog
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.components.SwitchRow
import com.rikhotso.studytime.ui.components.TileShape
import com.rikhotso.studytime.ui.components.TimeField
import com.rikhotso.studytime.ui.needsNotificationPermission
import com.rikhotso.studytime.ui.rememberTextExporter
import com.rikhotso.studytime.ui.rememberTextImporter
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.color
import com.rikhotso.studytime.ui.theme.deepColor
import java.time.LocalDate

/** Appearance, the Home screen, the timer, notifications, goals, study hours and your data. */
@Composable
fun SettingsTab(data: AppData, viewModel: StudyViewModel) {
    val colors = StudyTheme.colors
    val settings = data.settings
    val level = levelInfo(data).level
    var notice by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf("") }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // ------------------------------------------------------------ appearance
        SectionCard(title = "Appearance") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    FieldLabel("Theme")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ChoicePill(mode.label, selected = settings.themeMode == mode, onClick = { viewModel.updateSettings { it.copy(themeMode = mode) } }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Column {
                    FieldLabel("Accent colour")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Accent.entries.forEach { accent ->
                            val locked = accent.requiredLevel > level
                            val selected = settings.accent == accent
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .border(2.dp, if (selected) colors.foreground else Color.Transparent, CircleShape)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(if (settings.highContrast && !colors.isDark) accent.deepColor else accent.color)
                                    .selectable(selected = selected, role = Role.RadioButton, onClick = { viewModel.setAccent(accent)?.let { notice = it } })
                                    .semantics { contentDescription = accent.label + if (locked) ", unlocks at level ${accent.requiredLevel}" else "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (locked) StudyIcon(R.drawable.ic_lock, Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }
                    Text("Locked colours open up as you level up.", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                }
                SwitchRow("High contrast", settings.highContrast, { on -> viewModel.updateSettings { it.copy(highContrast = on) } }, subtitle = "Darker text, clearer outlines and a deeper accent")
                SwitchRow("Larger text", settings.largeText, { on -> viewModel.updateSettings { it.copy(largeText = on) } }, subtitle = "On top of your phone's own font size")
                SwitchRow("Reduce motion", settings.reduceMotion, { on -> viewModel.updateSettings { it.copy(reduceMotion = on) } }, subtitle = "Progress rings jump instead of animating")
            }
        }

        // ------------------------------------------------------------ home
        SectionCard(title = "Home screen cards") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Choose which cards show on Home and in what order.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                settings.dashboardOrder.forEachIndexed { index, card ->
                    val hidden = card in settings.hiddenCards
                    Row(
                        Modifier.fillMaxWidth().clip(TileShape).border(1.dp, colors.border, TileShape).padding(start = 12.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            card.label,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (hidden) colors.mutedForeground.copy(alpha = 0.6f) else colors.foreground,
                        )
                        IconAction(R.drawable.ic_arrow_up, "Move ${card.label} up", onClick = { viewModel.moveCard(card, up = true) }, enabled = index > 0)
                        IconAction(R.drawable.ic_arrow_down, "Move ${card.label} down", onClick = { viewModel.moveCard(card, up = false) }, enabled = index < settings.dashboardOrder.size - 1)
                        IconAction(
                            if (hidden) R.drawable.ic_eye_off else R.drawable.ic_eye,
                            if (hidden) "Show ${card.label}" else "Hide ${card.label}",
                            onClick = { viewModel.setCardHidden(card, !hidden) },
                            tint = if (hidden) colors.mutedForeground else colors.primary,
                        )
                    }
                }
                Text("Tasks due soon and Exam countdown only appear when there is something to show.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            }
        }

        // ------------------------------------------------------------ timer
        SectionCard(title = "Timer") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectField("Sound when a round ends", TimerSound.entries.map { it to it.label }, settings.timerSound, { sound -> viewModel.updateSettings { it.copy(timerSound = sound) } })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudyButton(
                        "Preview",
                        onClick = { TimerTones.play(settings.timerSound) },
                        kind = ButtonKind.Secondary,
                        small = true,
                        leadingIcon = R.drawable.ic_volume_2,
                        enabled = settings.timerSound != TimerSound.Off,
                    )
                    Text("Plays at your notification volume.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                }
                SelectField(
                    "Timer face",
                    TimerFace.entries.map { it to if (it.requiredLevel > level) "${it.label} (unlocks at level ${it.requiredLevel})" else it.label },
                    settings.timerFace,
                    { face -> viewModel.setTimerFace(face)?.let { notice = it } },
                )
                SwitchRow(
                    "Start breaks automatically",
                    settings.autoStartBreak,
                    { on -> viewModel.updateSettings { it.copy(autoStartBreak = on) } },
                    subtitle = "When off, the break waits for you to start it",
                )
            }
        }

        // ------------------------------------------------------------ notifications
        NotificationSettings(data, viewModel)

        // ------------------------------------------------------------ goals and hours
        GoalSettings(data, viewModel)
        AvailabilitySettings(data, viewModel)

        // ------------------------------------------------------------ data
        val stamp = LocalDate.now().toString()
        val exportBackup = rememberTextExporter("application/json", { viewModel.backupJson() }) { ok -> if (ok) viewModel.exported("Backup") else viewModel.exportFailed() }
        val exportSessions = rememberTextExporter("text/csv", { viewModel.sessionsCsvText() }) { ok -> if (ok) viewModel.exported("Study history") else viewModel.exportFailed() }
        val exportTasks = rememberTextExporter("text/csv", { viewModel.tasksCsvText() }) { ok -> if (ok) viewModel.exported("Task list") else viewModel.exportFailed() }
        val importBackup = rememberTextImporter { text ->
            if (text == null) notice = "That file could not be read." else pendingImport = text
        }
        SectionCard(title = "Your data") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoNote("Everything is saved on this phone only. There is no account yet, so nothing is uploaded and nothing syncs to other devices. A backup file is how you move your data.", icon = R.drawable.ic_shield_check)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudyButton("Export backup", onClick = { exportBackup("studytime-backup-$stamp.json") }, small = true, leadingIcon = R.drawable.ic_download)
                    StudyButton("Study history (CSV)", onClick = { exportSessions("studytime-sessions-$stamp.csv") }, kind = ButtonKind.Secondary, small = true, leadingIcon = R.drawable.ic_download)
                    StudyButton("Tasks (CSV)", onClick = { exportTasks("studytime-tasks-$stamp.csv") }, kind = ButtonKind.Secondary, small = true, leadingIcon = R.drawable.ic_download)
                }
                Text("CSV files open in Excel or Google Sheets. The backup holds everything and can be restored below.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudyButton("Restore a backup", onClick = importBackup, kind = ButtonKind.Ghost, small = true, leadingIcon = R.drawable.ic_history)
                    StudyButton("Delete all my data", onClick = { confirm = "reset" }, kind = ButtonKind.Danger, small = true, leadingIcon = R.drawable.ic_trash_2)
                }
            }
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("studytime · made with care", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
        }
    }

    notice?.let { text -> NoticeDialog("Not yet", text, onDone = { notice = null }) }
    pendingImport?.let { text ->
        ConfirmDialog(
            title = "Restore this backup?",
            text = "Everything in studytime on this phone is replaced by what is in the backup file. This cannot be undone, so export a backup of the current data first if you want to keep it.",
            confirmLabel = "Replace my data",
            onConfirm = {
                pendingImport = null
                viewModel.importBackup(text)?.let { notice = it }
            },
            onDismiss = { pendingImport = null },
            danger = true,
        )
    }
    if (confirm == "reset") {
        ConfirmDialog(
            title = "Delete all your data?",
            text = "This erases your profile, subjects, timetable, tasks, sessions, exams, notes, grades, challenges and friends from this phone, then starts the welcome steps again. It cannot be undone.",
            confirmLabel = "Erase everything",
            onConfirm = {
                confirm = ""
                viewModel.resetAll()
            },
            onDismiss = { confirm = "" },
            danger = true,
        )
    }
}

@Composable
private fun NotificationSettings(data: AppData, viewModel: StudyViewModel) {
    val colors = StudyTheme.colors
    val settings = data.settings
    val context = LocalContext.current
    // Checked again after the permission dialog closes.
    var allowed by remember { mutableStateOf(canNotify(context)) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> allowed = granted || canNotify(context) }

    SectionCard(title = "Notifications") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!allowed && needsNotificationPermission()) {
                InfoNote("Notifications are switched off for studytime on this phone, so none of these can appear yet.", icon = R.drawable.ic_bell)
                StudyButton("Allow notifications", onClick = { askPermission.launch(NOTIFICATION_PERMISSION) }, small = true, leadingIcon = R.drawable.ic_bell)
            }
            SwitchRow("Timer", settings.notifyTimer, { on -> viewModel.updateSettings { it.copy(notifyTimer = on) } }, subtitle = "When a focus round or a break ends")
            SwitchRow("Planned sessions", settings.notifySessions, { on -> viewModel.updateSettings { it.copy(notifySessions = on) } }, subtitle = "10 minutes before a session starts")
            SwitchRow("Deadlines", settings.notifyDeadlines, { on -> viewModel.updateSettings { it.copy(notifyDeadlines = on) } }, subtitle = "The evening before a task is due, and on the day")
            SwitchRow("Exams", settings.notifyExams, { on -> viewModel.updateSettings { it.copy(notifyExams = on) } }, subtitle = "One week before, and the evening before")
            SwitchRow("Revision", settings.notifyRevision, { on -> viewModel.updateSettings { it.copy(notifyRevision = on) } }, subtitle = "When topics are due for a spaced review")
            SwitchRow(
                "Quiet hours",
                settings.quietHoursEnabled,
                { on -> viewModel.updateSettings { it.copy(quietHoursEnabled = on) } },
                subtitle = "Reminders wait until quiet hours are over",
            )
            if (settings.quietHoursEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeField("From", settings.quietStart, { time -> viewModel.updateSettings { it.copy(quietStart = time) } }, Modifier.weight(1f))
                    TimeField("Until", settings.quietEnd, { time -> viewModel.updateSettings { it.copy(quietEnd = time) } }, Modifier.weight(1f))
                }
                Text(
                    "The end of a timer round you started yourself still sounds during quiet hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
            }
            Text("Friend and group notifications arrive with the online version.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
        }
    }
}

@Composable
private fun GoalSettings(data: AppData, viewModel: StudyViewModel) {
    val colors = StudyTheme.colors
    val profile = data.profile
    var daily by remember(profile.dailyGoalMinutes) { mutableIntStateOf(profile.dailyGoalMinutes) }
    var weekly by remember(profile.weeklyGoalMinutes) { mutableIntStateOf(profile.weeklyGoalMinutes) }
    var monthly by remember(profile.monthlyGoalMinutes) { mutableIntStateOf(profile.monthlyGoalMinutes) }
    var minimum by remember(profile.streakMinimumMinutes) { mutableIntStateOf(profile.streakMinimumMinutes) }
    var error by remember { mutableStateOf<String?>(null) }

    SectionCard(title = "Study goals") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Daily: ${formatMinutes(daily)}")
            MinutesSlider(daily, { daily = it }, min = 15, max = 480, step = 15)
            FieldLabel("Weekly: ${formatMinutes(weekly)}")
            MinutesSlider(weekly, { weekly = it }, min = 60, max = 3360, step = 30)
            FieldLabel("Monthly: ${formatMinutes(monthly)}")
            MinutesSlider(monthly, { monthly = it }, min = 120, max = 14400, step = 60)
            FieldLabel("A day counts for your streak after ${formatMinutes(minimum)}")
            MinutesSlider(minimum, { minimum = it }, min = 5, max = 120, step = 5)
            error?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.destructive) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyButton("Save goals", onClick = {
                    error = viewModel.saveGoals(daily, weekly, monthly, minimum)
                    if (error == null) viewModel.exported("Goals")
                }, small = true, leadingIcon = R.drawable.ic_target)
                StudyButton("Fill from daily", onClick = {
                    weekly = (daily * 5).coerceIn(60, 3360)
                    monthly = (daily * 20).coerceIn(120, 14400)
                }, kind = ButtonKind.Ghost, small = true)
            }
            Text("Fill from daily sets the weekly goal to five days and the monthly goal to twenty days of your daily goal.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
        }
    }
}

@Composable
private fun AvailabilitySettings(data: AppData, viewModel: StudyViewModel) {
    val colors = StudyTheme.colors
    var days by remember(data.profile.availability) { mutableStateOf(data.profile.availability) }
    var error by remember { mutableStateOf<String?>(null) }
    val changed = days != data.profile.availability

    SectionCard(title = "Study hours") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "The days and times you are free to study. Generated plans, rescheduled sessions and \"suggest a free time\" only use these hours.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
            AvailabilityEditor(days, onChange = { days = it })
            error?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.destructive) }
            StudyButton("Save study hours", onClick = {
                error = viewModel.saveAvailability(days)
                if (error == null) viewModel.exported("Study hours")
            }, small = true, enabled = changed, leadingIcon = R.drawable.ic_clock)
        }
    }
}

/** One row per weekday with a switch and a from and until time. Shared with the welcome steps. */
@Composable
fun AvailabilityEditor(days: List<com.rikhotso.studytime.data.Availability>, onChange: (List<com.rikhotso.studytime.data.Availability>) -> Unit) {
    val colors = StudyTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
            fun replace(next: com.rikhotso.studytime.data.Availability) = onChange(days.map { if (it.dayOfWeek == day.dayOfWeek) next else it })
            Column(
                Modifier.fillMaxWidth().clip(TileShape).border(1.dp, colors.border, TileShape).padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                SwitchRow(DAY_NAMES[day.dayOfWeek - 1], day.enabled, { replace(day.copy(enabled = it)) }, subtitle = if (day.enabled) "${day.start} - ${day.end}" else "Not available")
                if (day.enabled) {
                    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeField("From", day.start, { replace(day.copy(start = it)) }, Modifier.weight(1f))
                        TimeField("Until", day.end, { replace(day.copy(end = it)) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
