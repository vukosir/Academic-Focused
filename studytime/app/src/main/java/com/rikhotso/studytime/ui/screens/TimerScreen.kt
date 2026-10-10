@file:OptIn(ExperimentalLayoutApi::class)

package com.rikhotso.studytime.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.TimerFace
import com.rikhotso.studytime.data.TimerKind
import com.rikhotso.studytime.data.TimerLogic
import com.rikhotso.studytime.data.TimerMode
import com.rikhotso.studytime.data.dateOf
import com.rikhotso.studytime.data.formatClock
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.ui.KeepScreenOn
import com.rikhotso.studytime.ui.NOTIFICATION_PERMISSION
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.ChoicePill
import com.rikhotso.studytime.ui.components.ConfirmDialog
import com.rikhotso.studytime.ui.components.FieldLabel
import com.rikhotso.studytime.ui.components.MinutesSlider
import com.rikhotso.studytime.ui.components.PillTabs
import com.rikhotso.studytime.ui.components.ProgressRing
import com.rikhotso.studytime.ui.components.ScreenTitle
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.needsNotificationPermission
import com.rikhotso.studytime.ui.rememberNow
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.solid
import java.time.LocalDate

/**
 * The focus timer: the ring and its controls, then what you are studying and for how long.
 * Pomodoro and Custom count down, Stopwatch counts up until Finish is tapped.
 */
@Composable
fun TimerScreen(data: AppData, viewModel: StudyViewModel) {
    val colors = StudyTheme.colors
    val timer = data.timer
    val now = rememberNow(timer.running)
    val countsDown = TimerLogic.countsDown(timer)
    val seconds = (TimerLogic.displayMs(timer, now) + if (countsDown) 999 else 0) / 1000
    val idle = timer.mode == TimerMode.Idle
    val isBreak = timer.mode == TimerMode.Break
    var confirmEnd by remember { mutableStateOf(false) }

    // Asks once for permission to notify when a round ends. The timer works either way.
    val askForNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val start = {
        if (idle && needsNotificationPermission()) {
            askForNotifications.launch(NOTIFICATION_PERMISSION)
        }
        viewModel.startTimer()
    }

    KeepScreenOn(timer.running)

    ScreenColumn {
        ScreenTitle("Focus timer", "Pomodoro-style focus blocks. Finished sessions feed straight into your stats.")

        PillTabs(
            tabs = TimerKind.entries.map { it.label },
            selectedIndex = TimerKind.entries.indexOf(timer.kind),
            onSelect = { if (idle) viewModel.setTimerKind(TimerKind.entries[it]) },
        )
        if (!idle) {
            Text("The timer type can be changed once this round is over.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
        }

        SectionCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val face = data.settings.timerFace
                ProgressRing(
                    progress = TimerLogic.progress(timer, now),
                    modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f),
                    strokeWidth = if (face == TimerFace.Bold) 22.dp else 14.dp,
                    color = if (isBreak) PastelColor.Mint.solid else colors.primary,
                    animate = false,
                    brush = if (face == TimerFace.Sunrise && !isBreak) {
                        Brush.sweepGradient(listOf(PastelColor.Peach.solid, PastelColor.Butter.solid, PastelColor.Blush.solid, PastelColor.Lavender.solid, PastelColor.Peach.solid))
                    } else {
                        null
                    },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ClockDigits(formatClock(seconds), if (countsDown) "remaining" else "studied so far")
                        Row(
                            Modifier.clip(CircleShape).background(colors.muted).padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            StudyIcon(if (isBreak) R.drawable.ic_coffee else R.drawable.ic_brain, Modifier.size(14.dp), tint = colors.mutedForeground)
                            Text(
                                when {
                                    isBreak && !timer.running -> "Break ready"
                                    isBreak -> "Break"
                                    !idle && !timer.running -> "Paused"
                                    else -> "Focus"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.mutedForeground,
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when {
                        idle -> StudyButton("Start", onClick = start, leadingIcon = R.drawable.ic_play)
                        isBreak -> {
                            if (timer.running) {
                                StudyButton("Pause", onClick = { viewModel.pauseTimer() }, kind = ButtonKind.Secondary, leadingIcon = R.drawable.ic_pause)
                            } else {
                                StudyButton("Start break", onClick = { viewModel.startTimer() }, leadingIcon = R.drawable.ic_coffee)
                            }
                            StudyButton("Skip break", onClick = { viewModel.skipBreak() }, kind = ButtonKind.Ghost, leadingIcon = R.drawable.ic_skip_forward)
                        }
                        else -> {
                            if (timer.running) {
                                StudyButton("Pause", onClick = { viewModel.pauseTimer() }, kind = ButtonKind.Secondary, leadingIcon = R.drawable.ic_pause)
                            } else {
                                StudyButton("Resume", onClick = start, leadingIcon = R.drawable.ic_play)
                            }
                            StudyButton("Finish", onClick = { viewModel.finishTimer() }, kind = if (timer.running) ButtonKind.Primary else ButtonKind.Secondary, leadingIcon = R.drawable.ic_check)
                            StudyButton("Restart", onClick = { viewModel.restartTimer() }, kind = ButtonKind.Ghost, leadingIcon = R.drawable.ic_rotate_ccw)
                            StudyButton(
                                "End",
                                onClick = { if (TimerLogic.elapsedMs(timer, System.currentTimeMillis()) >= 60_000L) confirmEnd = true else viewModel.stopTimer() },
                                kind = ButtonKind.Danger,
                                leadingIcon = R.drawable.ic_square,
                            )
                        }
                    }
                }

                if (!idle && !isBreak) {
                    Text(
                        "Finish saves the time you studied. End throws this round away.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                        textAlign = TextAlign.Center,
                    )
                }
                if (timer.completedRounds > 0) {
                    val count = timer.completedRounds
                    Text(
                        "$count session${if (count == 1) "" else "s"} completed this round · resets on End",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                        textAlign = TextAlign.Center,
                    )
                }
                val todays = data.sessions.filter { dateOf(it.startedAt) == LocalDate.now() }
                if (todays.isNotEmpty()) {
                    Text(
                        "Today: ${todays.size} session${if (todays.size == 1) "" else "s"} · ${formatMinutes(todays.sumOf { it.durationMinutes })}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.mutedForeground,
                    )
                }
            }
        }

        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val subjectOptions = listOf("" to "General study (no subject)") + data.subjects.map { it.id to it.name }
                SelectField("Studying", subjectOptions, timer.subjectId, onSelect = { viewModel.setTimerSubject(it) })
                if (data.subjects.isEmpty()) {
                    Text("Add subjects in the planner to track them here.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                }

                // A task or a revision topic this session is for. It is saved with the session.
                val tasks = data.tasks.filter { !it.done && (timer.subjectId.isEmpty() || it.subjectId == timer.subjectId || it.subjectId.isEmpty()) }
                val topics = data.topics.filter { timer.subjectId.isNotEmpty() && it.subjectId == timer.subjectId }
                val linkOptions = listOf("" to "Nothing in particular") +
                    tasks.map { "task:${it.id}" to "Task: ${it.title}" } +
                    topics.map { "topic:${it.id}" to "Topic: ${it.name}" }
                val currentLink = when {
                    timer.taskId.isNotEmpty() -> "task:${timer.taskId}"
                    timer.topicId.isNotEmpty() -> "topic:${timer.topicId}"
                    else -> ""
                }
                SelectField("Working on", linkOptions, currentLink, onSelect = { choice ->
                    when {
                        choice.startsWith("task:") -> {
                            val id = choice.removePrefix("task:")
                            // Picking a task also picks its subject, when no round is running.
                            if (!idle || viewModel.prepareTimerForTask(id) != null) viewModel.setTimerLink(id, "")
                        }
                        choice.startsWith("topic:") -> viewModel.setTimerLink("", choice.removePrefix("topic:"))
                        else -> viewModel.setTimerLink("", "")
                    }
                })
                if (tasks.isEmpty() && topics.isEmpty()) {
                    Text("Add tasks in the planner, or topics inside a subject, to link sessions to them.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                }

                if (timer.kind == TimerKind.Stopwatch) {
                    Text("The stopwatch counts up until you tap Finish, then saves the time.", style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
                } else {
                    Column {
                        FieldLabel("Focus duration")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 50).forEach { minutes ->
                                ChoicePill("${minutes}m", selected = timer.focusMinutes == minutes, onClick = { viewModel.setFocusMinutes(minutes) }, modifier = Modifier.weight(1f))
                            }
                        }
                        if (timer.kind == TimerKind.Custom) {
                            Text("Any length: ${formatMinutes(timer.focusMinutes)}", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                            MinutesSlider(timer.focusMinutes, { viewModel.setFocusMinutes(it) }, min = 5, max = 180, step = 5)
                        }
                    }
                }

                Column {
                    FieldLabel("Break duration")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 5, 10, 15).forEach { minutes ->
                            ChoicePill(
                                if (minutes == 0) "None" else "${minutes}m",
                                selected = timer.breakMinutes == minutes,
                                onClick = { viewModel.setBreakMinutes(minutes) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Text(
                    when {
                        timer.kind == TimerKind.Pomodoro -> "Pomodoro is 25 minutes of focus and a 5 minute break. Picking another length switches to Custom."
                        data.settings.autoStartBreak -> "The break starts by itself when a focus round ends. You can change that in Settings."
                        else -> "When a focus round ends the break waits for you to start it."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
            }
        }
    }

    if (confirmEnd) {
        ConfirmDialog(
            title = "End without saving?",
            text = "The time studied in this round will not be saved. Tap Finish instead if you want to keep it.",
            confirmLabel = "End round",
            onConfirm = {
                confirmEnd = false
                viewModel.stopTimer()
            },
            onDismiss = { confirmEnd = false },
            danger = true,
        )
    }
}

/** The big clock. Each digit sits in its own fixed-width box so the time does not wobble. */
@Composable
private fun ClockDigits(clock: String, what: String) {
    val colors = StudyTheme.colors
    // "1:05:00" is longer than "25:00", so it is drawn a little smaller to stay inside the ring.
    val size = if (clock.length > 5) 40 else 52
    val style = MaterialTheme.typography.displaySmall.copy(fontSize = size.sp, lineHeight = (size + 8).sp)
    val digitWidth = with(LocalDensity.current) { (size.sp * 0.64f).toDp() }
    Row(Modifier.semantics(mergeDescendants = true) { contentDescription = "$clock $what" }) {
        clock.forEach { character ->
            Box(Modifier.width(if (character == ':') digitWidth / 2 else digitWidth), contentAlignment = Alignment.Center) {
                Text(character.toString(), style = style, color = colors.foreground, maxLines = 1, softWrap = false)
            }
        }
    }
}
