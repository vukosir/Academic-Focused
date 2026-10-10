package com.rikhotso.studytime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.DayBar
import com.rikhotso.studytime.data.MONTH_NAMES_SHORT
import com.rikhotso.studytime.data.StatPeriod
import com.rikhotso.studytime.data.StudySession
import com.rikhotso.studytime.data.dateOf
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.formatTime
import com.rikhotso.studytime.data.insights
import com.rikhotso.studytime.data.minutesByDay
import com.rikhotso.studytime.data.monthBars
import com.rikhotso.studytime.data.percentOf
import com.rikhotso.studytime.data.periodRange
import com.rikhotso.studytime.data.periodSummary
import com.rikhotso.studytime.data.personalRecords
import com.rikhotso.studytime.data.plannedVersusActual
import com.rikhotso.studytime.data.sessionHistory
import com.rikhotso.studytime.data.shortDate
import com.rikhotso.studytime.data.subjectBreakdown
import com.rikhotso.studytime.data.weekBars
import com.rikhotso.studytime.ui.components.BarChart
import com.rikhotso.studytime.ui.components.ColorDot
import com.rikhotso.studytime.ui.components.ConfirmDialog
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.MiniStat
import com.rikhotso.studytime.ui.components.PillTabs
import com.rikhotso.studytime.ui.components.ProgressBar
import com.rikhotso.studytime.ui.components.SearchField
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.ShareBars
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.components.TextLink
import com.rikhotso.studytime.ui.components.TileShape
import com.rikhotso.studytime.ui.components.TrashButton
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Statistics for today, this week, this month or all time, all counted from saved records. */
@Composable
fun StatsTab(data: AppData, viewModel: StudyViewModel) {
    val colors = StudyTheme.colors
    val today = LocalDate.now()
    var periodIndex by rememberSaveable { mutableIntStateOf(1) }
    val period = StatPeriod.entries[periodIndex]
    val byDay = remember(data.sessions) { minutesByDay(data.sessions) }
    val summary = periodSummary(data, period, today)
    val shares = subjectBreakdown(data, period, today)

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PillTabs(StatPeriod.entries.map { it.label }, selectedIndex = periodIndex, onSelect = { periodIndex = it })

        SectionCard(
            title = when (period) {
                StatPeriod.Today -> "Today"
                StatPeriod.Week -> "This week"
                StatPeriod.Month -> "${today.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${today.year}"
                StatPeriod.All -> "All time"
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("Study time", formatMinutes(summary.minutes), Modifier.weight(1f))
                    MiniStat("Sessions", summary.sessions.toString(), Modifier.weight(1f))
                    MiniStat("Tasks done", summary.tasksDone.toString(), Modifier.weight(1f))
                }
                if (summary.goalMinutes > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProgressBar(summary.goalPercent)
                        Text(
                            "${formatMinutes(summary.minutes)} of your ${formatMinutes(summary.goalMinutes)} goal · ${summary.goalPercent}%" +
                                if (period != StatPeriod.Today) " · daily goal met on ${summary.goalDaysMet} day${if (summary.goalDaysMet == 1) "" else "s"}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                        )
                    }
                } else {
                    Text(
                        "Studied on ${summary.activeDays} day${if (summary.activeDays == 1) "" else "s"} · daily goal met on ${summary.goalDaysMet}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                    )
                }
                when (period) {
                    StatPeriod.Week -> BarChart(weekBars(byDay, today), description = "Minutes studied each day this week")
                    StatPeriod.Month -> BarChart(monthBars(byDay, today), description = "Minutes studied each day this month", labelEvery = 5)
                    StatPeriod.All -> BarChart(lastMonths(byDay, today, 6), description = "Minutes studied in each of the last six months")
                    StatPeriod.Today -> Unit
                }
            }
        }

        SectionCard(title = "Time by subject") {
            if (shares.isEmpty()) {
                EmptyHint("No focus sessions in this period yet.")
            } else {
                ShareBars(shares)
            }
        }

        if (period != StatPeriod.All) {
            val (from, to) = periodRange(period, today, data)
            val plan = plannedVersusActual(data, from, to, today)
            SectionCard(title = "Planned and actual") {
                if (plan.plannedSessions == 0) {
                    EmptyHint("No planned sessions were due in this period. Plan some in the Planner to compare them with what you study.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompareBar("Planned", plan.plannedMinutes, maxOf(plan.plannedMinutes, plan.studiedMinutes), colors.secondary)
                        CompareBar("Studied", plan.studiedMinutes, maxOf(plan.plannedMinutes, plan.studiedMinutes), colors.primary)
                        Text(
                            "${plan.completedSessions} of ${plan.plannedSessions} planned sessions ticked off (${percentOf(plan.completedSessions, plan.plannedSessions)}%). " +
                                "Only sessions that should already be over are counted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                        )
                    }
                }
            }
        }

        SectionCard(title = "Personal records") {
            val records = personalRecords(data, today)
            if (records.isEmpty()) {
                EmptyHint("Your records appear after your first focus session.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    records.forEach { record ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StudyIcon(R.drawable.ic_trophy, Modifier.size(18.dp), tint = colors.primary)
                            Column(Modifier.weight(1f)) {
                                Text(record.label, style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                                Text(record.detail, style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                            }
                            Text(record.value, style = MaterialTheme.typography.titleMedium, color = colors.foreground)
                        }
                    }
                }
            }
        }

        SectionCard(title = "Insights") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                insights(data, today).forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StudyIcon(R.drawable.ic_lightbulb, Modifier.padding(top = 2.dp).size(16.dp), tint = colors.primary)
                        Text(line, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                    }
                }
                Text("Each insight is worked out from your own sessions, tasks and plans.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            }
        }

        HistoryCard(data, viewModel)
    }
}

/** One total per month for the last [count] months, oldest first. */
private fun lastMonths(byDay: Map<LocalDate, Int>, today: LocalDate, count: Int): List<DayBar> =
    (count - 1 downTo 0).map { back ->
        val month = today.withDayOfMonth(1).minusMonths(back.toLong())
        val total = byDay.filterKeys { it.year == month.year && it.monthValue == month.monthValue }.values.sum()
        DayBar(MONTH_NAMES_SHORT[month.monthValue - 1], total, back == 0)
    }

@Composable
private fun CompareBar(label: String, minutes: Int, highest: Int, color: Color) {
    val colors = StudyTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.foreground)
            Text(formatMinutes(minutes), style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(colors.muted)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(if (highest <= 0) 0f else (minutes / highest.toFloat()).coerceIn(0f, 1f)).clip(CircleShape).background(color))
        }
    }
}

/** Every saved session, newest first, with a search box and a subject filter. */
@Composable
private fun HistoryCard(data: AppData, viewModel: StudyViewModel) {
    val colors = StudyTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var subjectFilter by rememberSaveable { mutableStateOf("*") }
    var shown by rememberSaveable { mutableIntStateOf(15) }
    var deleting by remember { mutableStateOf<StudySession?>(null) }
    val sessions = sessionHistory(data, query, if (subjectFilter == "*") null else subjectFilter)

    SectionCard(title = "Study history") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (data.sessions.isEmpty()) {
                EmptyHint("Finished focus sessions are listed here.")
            } else {
                SearchField(query, { query = it }, placeholder = "Search subject, task, topic or note")
                SelectField(
                    "Subject",
                    listOf("*" to "Every subject", "" to "General study") + data.subjects.map { it.id to it.name },
                    subjectFilter,
                    { subjectFilter = it },
                )
                Text(
                    "${sessions.size} session${if (sessions.size == 1) "" else "s"} · ${formatMinutes(sessions.sumOf { it.durationMinutes })}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedForeground,
                )
                sessions.take(shown).forEach { session ->
                    val started = Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault())
                    Row(
                        Modifier.fillMaxWidth().clip(TileShape).background(session.color.soft).padding(start = 14.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ColorDot(session.color, size = 10.dp)
                        Column(Modifier.weight(1f)) {
                            Text("${session.subjectName} · ${formatMinutes(session.durationMinutes)}", style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                            Text(
                                "${shortDate(dateOf(session.startedAt).toString())} at ${formatTime(started.hour, started.minute)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedForeground,
                            )
                            val details = listOf(
                                if (session.taskTitle.isNotEmpty()) "Task: ${session.taskTitle}" else "",
                                if (session.topicName.isNotEmpty()) "Topic: ${session.topicName}" else "",
                                session.note,
                            ).filter { it.isNotEmpty() }
                            if (details.isNotEmpty()) {
                                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                            }
                        }
                        TrashButton(onClick = { deleting = session }, description = "Delete this session")
                    }
                }
                if (sessions.size > shown) {
                    TextLink("Show more", onClick = { shown += 30 })
                }
                if (sessions.isEmpty()) {
                    EmptyHint("No sessions match this search.")
                }
            }
        }
    }

    deleting?.let { session ->
        ConfirmDialog(
            title = "Delete this session?",
            text = "${formatMinutes(session.durationMinutes)} of ${session.subjectName} will be removed from your history, statistics and points.",
            confirmLabel = "Delete session",
            onConfirm = {
                viewModel.removeSession(session.id)
                deleting = null
            },
            onDismiss = { deleting = null },
            danger = true,
        )
    }
}
