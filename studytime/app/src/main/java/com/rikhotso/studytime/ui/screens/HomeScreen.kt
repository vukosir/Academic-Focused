package com.rikhotso.studytime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.DashboardCard
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.Quest
import com.rikhotso.studytime.data.challengePercent
import com.rikhotso.studytime.data.challengeProgress
import com.rikhotso.studytime.data.classesOn
import com.rikhotso.studytime.data.dailyQuests
import com.rikhotso.studytime.data.daysUntil
import com.rikhotso.studytime.data.examsOn
import com.rikhotso.studytime.data.firstName
import com.rikhotso.studytime.data.formatMinuteOfDay
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.formatNumber
import com.rikhotso.studytime.data.greeting
import com.rikhotso.studytime.data.isCompletedOn
import com.rikhotso.studytime.data.isOverdue
import com.rikhotso.studytime.data.levelInfo
import com.rikhotso.studytime.data.minutesByDay
import com.rikhotso.studytime.data.minutesOfDay
import com.rikhotso.studytime.data.plannedOn
import com.rikhotso.studytime.data.relativeDay
import com.rikhotso.studytime.data.shortDate
import com.rikhotso.studytime.data.streakInfo
import com.rikhotso.studytime.data.tasksDueSoon
import com.rikhotso.studytime.data.weekBars
import com.rikhotso.studytime.ui.AppNav
import com.rikhotso.studytime.ui.AppTab
import com.rikhotso.studytime.ui.PlannerTab
import com.rikhotso.studytime.ui.ProfileTab
import com.rikhotso.studytime.ui.components.CardShape
import com.rikhotso.studytime.ui.components.CheckCircle
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.PriorityTag
import com.rikhotso.studytime.ui.components.ProgressBar
import com.rikhotso.studytime.ui.components.ProgressRing
import com.rikhotso.studytime.ui.components.RowText
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.StatCard
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.components.Tag
import com.rikhotso.studytime.ui.components.TextLink
import com.rikhotso.studytime.ui.components.TileShape
import com.rikhotso.studytime.ui.components.WeekChart
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft
import com.rikhotso.studytime.ui.theme.solid
import java.time.LocalDate
import java.time.LocalTime

/**
 * The dashboard. Which cards appear, and in what order, comes from Settings. The cards for
 * tasks and exams only appear when there is something to show.
 */
@Composable
fun HomeScreen(data: AppData, viewModel: StudyViewModel, nav: AppNav) {
    val colors = StudyTheme.colors
    val today = LocalDate.now()
    val now = LocalTime.now()
    val nowMinute = now.hour * 60 + now.minute
    val byDay = remember(data.sessions) { minutesByDay(data.sessions) }
    val todayMinutes = byDay[today] ?: 0
    val goal = data.profile.dailyGoalMinutes
    val bars = weekBars(byDay, today)
    val streak = streakInfo(byDay, today, data.profile.streakMinimumMinutes)

    ScreenColumn {
        Column {
            Text("${greeting(now.hour)},", style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
            Text(firstName(data.profile.name), style = MaterialTheme.typography.headlineMedium, color = colors.foreground)
            Text("Here's your study day at a glance.", style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
        }

        data.settings.dashboardOrder.filter { it !in data.settings.hiddenCards }.forEach { card ->
            when (card) {
                DashboardCard.Stats -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(R.drawable.ic_flame, "Day streak", streak.current.toString(), PastelColor.Blush, Modifier.weight(1f))
                        StatCard(R.drawable.ic_clock, "Today", formatMinutes(todayMinutes), PastelColor.Lavender, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(R.drawable.ic_target, "Weekly", formatMinutes(bars.sumOf { it.minutes }), PastelColor.BabyBlue, Modifier.weight(1f))
                        StatCard(R.drawable.ic_book_open, "Subjects", data.subjects.size.toString(), PastelColor.Mint, Modifier.weight(1f))
                    }
                }

                DashboardCard.FocusGoal -> SectionCard(
                    title = "Today's focus goal",
                    action = { StudyButton("Start session", onClick = { nav.goTab(AppTab.Timer) }, small = true, leadingIcon = R.drawable.ic_timer) },
                ) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProgressRing(progress = todayMinutes / goal.coerceAtLeast(1).toFloat(), modifier = Modifier.size(128.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(formatMinutes(todayMinutes), style = MaterialTheme.typography.headlineMedium, color = colors.foreground)
                                Text("of ${formatMinutes(goal)}", style = MaterialTheme.typography.labelSmall, color = colors.mutedForeground)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (todayMinutes >= goal) "Lovely. You've reached your daily goal." else "Just ${formatMinutes(goal - todayMinutes)} to go to hit your daily goal.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.mutedForeground,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                when {
                                    todayMinutes >= data.profile.streakMinimumMinutes -> "Today counts toward your streak."
                                    streak.freezesLeft > 0 -> "Study ${formatMinutes(data.profile.streakMinimumMinutes)} today to grow your streak. You have ${streak.freezesLeft} streak freeze${if (streak.freezesLeft == 1) "" else "s"} in reserve."
                                    else -> "Keep your streak alive by studying ${formatMinutes(data.profile.streakMinimumMinutes)} today."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedForeground,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                DashboardCard.Today -> TodayCard(data, today, onEdit = { nav.openPlanner(PlannerTab.Today) })

                DashboardCard.TasksDue -> {
                    val due = tasksDueSoon(data, today)
                    if (due.isNotEmpty()) {
                        SectionCard(title = "Tasks due soon", action = { TextLink("All tasks", onClick = { nav.openPlanner(PlannerTab.Tasks) }) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                due.take(4).forEach { task ->
                                    val overdue = isOverdue(task, today, nowMinute)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        CheckCircle(false, onToggle = { viewModel.toggleTask(task.id) }, description = "Mark ${task.title} as done")
                                        Column(Modifier.weight(1f)) {
                                            Text(task.title, style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                                            Text(
                                                (if (overdue) "Overdue · " else "") + "Due ${relativeDay(daysUntil(task.dueDate, today) ?: 0)}" +
                                                    (if (task.dueTime.isNotEmpty()) " at ${task.dueTime}" else "") + " · ${data.subjectName(task.subjectId)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (overdue) colors.destructive else colors.mutedForeground,
                                            )
                                        }
                                        PriorityTag(task.priority)
                                    }
                                }
                                if (due.size > 4) {
                                    Text("and ${due.size - 4} more", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                                }
                            }
                        }
                    }
                }

                DashboardCard.Exams -> {
                    val upcoming = data.exams.filter { (daysUntil(it.date, today) ?: -1) >= 0 }.sortedBy { it.date + it.time }
                    if (upcoming.isNotEmpty()) {
                        SectionCard(title = "Exam countdown", action = { TextLink("All exams", onClick = { nav.openPlanner(PlannerTab.Exams) }) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                upcoming.take(3).forEach { exam ->
                                    val days = daysUntil(exam.date, today) ?: 0
                                    Row(
                                        Modifier.fillMaxWidth().clip(TileShape).background(data.subjectColor(exam.subjectId).soft).padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Column(
                                            Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.background.copy(alpha = 0.7f)),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text(days.toString(), style = MaterialTheme.typography.titleMedium, color = colors.foreground)
                                            Text(if (days == 1L) "day" else "days", style = MaterialTheme.typography.labelSmall, color = colors.mutedForeground)
                                        }
                                        RowText(exam.title, "${shortDate(exam.date)} at ${exam.time}" + if (exam.location.isNotEmpty()) " · ${exam.location}" else "")
                                    }
                                }
                            }
                        }
                    }
                }

                DashboardCard.Week -> SectionCard(title = "This week's study") {
                    WeekChart(bars)
                }

                DashboardCard.DailyChallenges -> {
                    val level = levelInfo(data)
                    SectionCard(
                        title = "Daily challenges",
                        action = { TextLink("Level ${level.level} · ${level.xp} pts", onClick = { nav.openProfile(ProfileTab.Rewards) }) },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            dailyQuests(data, today).forEach { QuestRow(it) }
                        }
                    }
                }

                DashboardCard.Challenges -> {
                    val active = data.challenges.filter { !it.completed }
                    SectionCard(title = "Active challenges", action = { TextLink("View all", onClick = { nav.openProfile(ProfileTab.Rewards) }) }) {
                        if (active.isEmpty()) {
                            EmptyHint("No active challenges yet. Create one on your profile to stay motivated.")
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                active.take(4).forEach { challenge ->
                                    val progress = challengeProgress(challenge, data, today)
                                    val percent = challengePercent(progress, challenge.target)
                                    Column(
                                        Modifier.fillMaxWidth().clip(TileShape).border(1.dp, colors.border, TileShape).padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            StudyIcon(R.drawable.ic_sparkles, Modifier.size(16.dp), tint = colors.primary)
                                            Text(challenge.name, style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                                        }
                                        ProgressBar(percent)
                                        Text(
                                            "${formatNumber(progress)}/${challenge.target} ${challenge.type.unit} · $percent%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.mutedForeground,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DashboardCard.QuickLinks -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickLink(R.drawable.ic_timer, PastelColor.Lavender, "Start focusing", "Pomodoro timer", Modifier.weight(1f)) { nav.goTab(AppTab.Timer) }
                    QuickLink(R.drawable.ic_plus, PastelColor.BabyBlue, "Plan your week", "Timetable and subjects", Modifier.weight(1f)) { nav.openPlanner(PlannerTab.Timetable) }
                }
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextLink("Customise this screen", onClick = { nav.openProfile(ProfileTab.Settings) })
        }
    }
}

private class TodayItem(val start: Int, val color: PastelColor, val title: String, val subtitle: String, val done: Boolean = false)

/** Everything booked for today in time order: classes, planned study sessions and exams. */
@Composable
private fun TodayCard(data: AppData, today: LocalDate, onEdit: () -> Unit) {
    val colors = StudyTheme.colors
    val items = ArrayList<TodayItem>()
    classesOn(data, today).forEach {
        items.add(TodayItem(minutesOfDay(it.startTime), it.color, it.subjectName, "Class · ${it.startTime} - ${it.endTime}" + if (it.room.isNotEmpty()) " · ${it.room}" else ""))
    }
    plannedOn(data, today).forEach {
        val start = minutesOfDay(it.startTime)
        items.add(
            TodayItem(start, data.subjectColor(it.subjectId), it.title, "Study · ${it.startTime} - ${formatMinuteOfDay(start + it.durationMinutes)}", it.isCompletedOn(today)),
        )
    }
    examsOn(data, today).forEach {
        items.add(TodayItem(minutesOfDay(it.time), data.subjectColor(it.subjectId), it.title, "Exam · ${it.time}" + if (it.location.isNotEmpty()) " · ${it.location}" else ""))
    }

    SectionCard(title = "Today's schedule", action = { TextLink("Edit planner", onClick = onEdit) }) {
        if (items.isEmpty()) {
            EmptyHint("No classes scheduled today. Enjoy the breathing room, or add one in the planner.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.sortedBy { it.start }.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clip(TileShape).background(item.color.soft).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.width(6.dp).height(36.dp).clip(CircleShape).background(item.color.solid))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.foreground,
                                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                            )
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                        }
                        if (item.done) Tag("Done", background = colors.background.copy(alpha = 0.7f), content = colors.foreground)
                    }
                }
            }
        }
    }
}

/** One daily or weekly challenge with its progress and the points it pays. */
@Composable
fun QuestRow(quest: Quest) {
    val colors = StudyTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .then(if (quest.done) Modifier.background(PastelColor.Mint.soft) else Modifier.border(1.dp, colors.border, TileShape))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyIcon(if (quest.done) R.drawable.ic_circle_check else R.drawable.ic_target, Modifier.size(16.dp), tint = colors.primary)
            Text(quest.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.foreground)
            Text(if (quest.done) "+${quest.xp} earned" else "+${quest.xp} pts", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
        }
        if (!quest.done) {
            ProgressBar(quest.percent)
            Text("${quest.progress} of ${quest.target}", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
        }
    }
}

@Composable
private fun QuickLink(icon: Int, accent: PastelColor, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = StudyTheme.colors
    Row(
        modifier
            .shadow(4.dp, CardShape, ambientColor = Color(0x33896BC2), spotColor = Color(0x33896BC2))
            .clip(CardShape)
            .background(colors.card)
            .border(1.dp, colors.border, CardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(accent.soft), contentAlignment = Alignment.Center) {
            StudyIcon(icon, Modifier.size(20.dp), tint = colors.primary)
        }
        RowText(title, subtitle)
    }
}
