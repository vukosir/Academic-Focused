package com.rikhotso.studytime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.DAY_NAMES
import com.rikhotso.studytime.data.DAY_NAMES_SHORT
import com.rikhotso.studytime.data.EducationLevel
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.ColorDot
import com.rikhotso.studytime.ui.components.ColorPicker
import com.rikhotso.studytime.ui.components.FieldLabel
import com.rikhotso.studytime.ui.components.Logo
import com.rikhotso.studytime.ui.components.MinutesSlider
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.SoftRow
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.components.StudyTextField
import com.rikhotso.studytime.ui.components.TileShape
import com.rikhotso.studytime.ui.components.TimeField
import com.rikhotso.studytime.ui.components.TrashButton
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft

private const val STEP_COUNT = 6

/**
 * The first-run setup: Welcome, Subjects, Timetable, Study hours, Goals, Ready. Everything is
 * saved as it is entered, so nothing is lost if the app is closed halfway. No account is needed.
 */
@Composable
fun OnboardingScreen(data: AppData, viewModel: StudyViewModel) {
    val colors = StudyTheme.colors
    var step by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = step > 0) { step -= 1 }

    Box(
        Modifier.fillMaxSize().background(colors.background).systemBarsPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Logo(Modifier.padding(top = 8.dp), markSize = 40.dp, fontSize = 24.sp)
            StepDots(current = step, count = STEP_COUNT)
            when (step) {
                0 -> WelcomeStep(data, viewModel, onContinue = { step = 1 })
                1 -> SubjectsStep(data, viewModel, onBack = { step = 0 }, onContinue = { step = 2 })
                2 -> TimetableStep(data, viewModel, onBack = { step = 1 }, onContinue = { step = 3 })
                3 -> HoursStep(data, viewModel, onBack = { step = 2 }, onContinue = { step = 4 })
                4 -> GoalStep(data, viewModel, onBack = { step = 3 }, onContinue = { step = 5 })
                else -> ReadyStep(data, onBack = { step = 4 }, onFinish = { viewModel.finishOnboarding() })
            }
        }
    }
}

@Composable
private fun StepDots(current: Int, count: Int) {
    val colors = StudyTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val color = when {
                index == current -> colors.primary
                index < current -> colors.primary.copy(alpha = 0.6f)
                else -> colors.muted
            }
            Box(Modifier.height(6.dp).width(if (index == current) 24.dp else 6.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun StepButtons(onBack: () -> Unit, onContinue: () -> Unit, continueLabel: String = "Continue") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        StudyButton("Back", onClick = onBack, kind = ButtonKind.Ghost, leadingIcon = R.drawable.ic_arrow_left)
        StudyButton(continueLabel, onClick = onContinue, modifier = Modifier.weight(1f), trailingIcon = R.drawable.ic_arrow_right)
    }
}

@Composable
private fun StepHeading(title: String, subtitle: String, centered: Boolean = false) {
    val colors = StudyTheme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.foreground)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mutedForeground,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
    }
}

@Composable
private fun WelcomeStep(data: AppData, viewModel: StudyViewModel, onContinue: () -> Unit) {
    val colors = StudyTheme.colors
    var name by rememberSaveable { mutableStateOf(data.profile.name) }
    var error by remember { mutableStateOf<String?>(null) }
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Welcome to studytime", style = MaterialTheme.typography.headlineMedium, color = colors.foreground, textAlign = TextAlign.Center)
            Text(
                "Your calm little companion for timetables, focus sessions, tasks, revision and challenges. Let's set things up. It only takes a minute.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
            StudyTextField(
                value = name,
                onValueChange = { name = it },
                label = "What should studytime call you?",
                placeholder = "Your first name",
                capitalization = KeyboardCapitalization.Words,
            )
            SelectField("Where are you studying?", EducationLevel.entries.map { it to it.label }, data.profile.educationLevel, { viewModel.setEducationLevel(it) })
            Column(
                Modifier.fillMaxWidth().clip(TileShape).background(PastelColor.Lavender.soft).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("What studytime does:", style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                listOf(
                    R.drawable.ic_timer to "A focus timer that keeps counting when you leave the app",
                    R.drawable.ic_calendar_days to "A planner that fits study sessions around your classes",
                    R.drawable.ic_list_checks to "Tasks with deadlines, priorities and checklists",
                    R.drawable.ic_graduation_cap to "Exam countdowns, revision topics and notes",
                    R.drawable.ic_trophy to "Streaks, challenges, badges and levels",
                    R.drawable.ic_chart_column to "Statistics built from what you really studied",
                ).forEach { (icon, text) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StudyIcon(icon, Modifier.padding(top = 2.dp).size(16.dp), tint = colors.primary)
                        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
                    }
                }
            }
            Text(
                "No account is needed. Everything stays on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
            error?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.destructive) }
            StudyButton(
                "Let's begin",
                onClick = {
                    error = viewModel.setName(name)
                    if (error == null) onContinue()
                },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = R.drawable.ic_arrow_right,
            )
        }
    }
}

@Composable
private fun SubjectsStep(data: AppData, viewModel: StudyViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val colors = StudyTheme.colors
    var name by rememberSaveable { mutableStateOf("") }
    var teacher by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf(PastelColor.Lavender) }
    var error by remember { mutableStateOf<String?>(null) }
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StepHeading("Add your subjects", "Give each subject a colour so it shows up nicely across your planner and stats.")
            Column(
                Modifier.fillMaxWidth().clip(TileShape).border(1.dp, colors.border, TileShape).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StudyTextField(name, { name = it }, label = "Subject name", placeholder = "e.g. Mathematics")
                StudyTextField(teacher, { teacher = it }, label = "Teacher (optional)", placeholder = "e.g. Mrs. Pillay", capitalization = KeyboardCapitalization.Words)
                Column {
                    FieldLabel("Colour")
                    ColorPicker(selected = color, onSelect = { color = it })
                }
                error?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.destructive) }
                StudyButton(
                    "Add subject",
                    onClick = {
                        error = viewModel.addSubject(name, teacher, color)
                        if (error == null) {
                            name = ""
                            teacher = ""
                            color = PastelColor.entries[(data.subjects.size + 1) % PastelColor.entries.size]
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Secondary,
                    leadingIcon = R.drawable.ic_plus,
                )
            }
            if (data.subjects.isEmpty()) {
                Text("No subjects yet.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground, textAlign = TextAlign.Center)
            }
            data.subjects.forEach { subject ->
                SoftRow(subject.color) {
                    ColorDot(subject.color, size = 12.dp)
                    Text(subject.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                    if (subject.teacher.isNotEmpty()) {
                        Text(subject.teacher, style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                    }
                    TrashButton(onClick = { viewModel.removeSubject(subject.id) }, description = "Delete ${subject.name}")
                }
            }
            StepButtons(onBack, onContinue)
        }
    }
}

@Composable
private fun TimetableStep(data: AppData, viewModel: StudyViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val colors = StudyTheme.colors
    var subjectId by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableIntStateOf(1) }
    var start by rememberSaveable { mutableStateOf("09:00") }
    var end by rememberSaveable { mutableStateOf("10:00") }
    var room by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StepHeading("Build your weekly timetable", "Add your recurring classes. You can edit everything later in the planner.")
            if (data.subjects.isEmpty()) {
                Box(Modifier.fillMaxWidth().clip(TileShape).background(colors.muted.copy(alpha = 0.6f)).padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("Add a subject first to schedule classes.", style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground, textAlign = TextAlign.Center)
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().clip(TileShape).border(1.dp, colors.border, TileShape).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SelectField("Subject", data.subjects.map { it.id to it.name }, subjectId.ifEmpty { null }, { subjectId = it }, placeholder = "Pick subject")
                    SelectField("Day", DAY_NAMES.mapIndexed { index, label -> (index + 1) to label }, day, { day = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeField("Start", start, { start = it }, Modifier.weight(1f))
                        TimeField("End", end, { end = it }, Modifier.weight(1f))
                    }
                    StudyTextField(room, { room = it }, label = "Room (optional)", placeholder = "e.g. Room 12")
                    error?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.destructive) }
                    StudyButton(
                        "Add class",
                        onClick = {
                            error = viewModel.addEntry(subjectId, day, start, end, room)
                            if (error == null) room = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        kind = ButtonKind.Secondary,
                        leadingIcon = R.drawable.ic_plus,
                    )
                }
            }
            data.entries.forEach { entry ->
                SoftRow(entry.color) {
                    Text(DAY_NAMES_SHORT[entry.dayOfWeek - 1], Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
                    Text(entry.subjectName, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                    Text("${entry.startTime} - ${entry.endTime}", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                    TrashButton(onClick = { viewModel.removeEntry(entry.id) }, description = "Remove class")
                }
            }
            StepButtons(onBack, onContinue)
        }
    }
}

@Composable
private fun HoursStep(data: AppData, viewModel: StudyViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val colors = StudyTheme.colors
    var days by remember { mutableStateOf(data.profile.availability) }
    var error by remember { mutableStateOf<String?>(null) }
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StepHeading("When can you study?", "Pick the days and hours you are usually free. Study plans are only ever placed inside these hours.")
            AvailabilityEditor(days, onChange = { days = it })
            error?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.destructive) }
            StepButtons(onBack, onContinue = {
                error = viewModel.saveAvailability(days)
                if (error == null) onContinue()
            })
        }
    }
}

@Composable
private fun GoalStep(data: AppData, viewModel: StudyViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val colors = StudyTheme.colors
    var daily by rememberSaveable { mutableIntStateOf(data.profile.dailyGoalMinutes) }
    var weekly by rememberSaveable { mutableIntStateOf(data.profile.weeklyGoalMinutes) }
    var monthly by rememberSaveable { mutableIntStateOf(data.profile.monthlyGoalMinutes) }
    // Weekly and monthly follow the daily goal until the user moves them by hand.
    var linked by rememberSaveable { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            StepHeading("Set your study goals", "Gentle targets to aim for. You can change them anytime in Settings.", centered = true)
            Text(formatMinutes(daily), style = MaterialTheme.typography.displaySmall, color = colors.primary)
            Text("a day", style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
            MinutesSlider(daily, {
                daily = it
                if (linked) {
                    weekly = (it * 5).coerceIn(60, 3360)
                    monthly = (it * 20).coerceIn(120, 14400)
                }
            }, min = 15, max = 360, step = 15)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("15m", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                Text("6h", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            }
            Column(Modifier.fillMaxWidth()) {
                FieldLabel("Weekly goal: ${formatMinutes(weekly)}")
                MinutesSlider(weekly, {
                    weekly = it
                    linked = false
                }, min = 60, max = 3360, step = 30)
                FieldLabel("Monthly goal: ${formatMinutes(monthly)}")
                MinutesSlider(monthly, {
                    monthly = it
                    linked = false
                }, min = 120, max = 14400, step = 60)
            }
            error?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.destructive) }
            StepButtons(onBack, onContinue = {
                error = viewModel.saveGoals(daily, weekly, monthly, data.profile.streakMinimumMinutes)
                if (error == null) onContinue()
            })
        }
    }
}

@Composable
private fun ReadyStep(data: AppData, onBack: () -> Unit, onFinish: () -> Unit) {
    val colors = StudyTheme.colors
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(20.dp)).background(PastelColor.Mint.soft), contentAlignment = Alignment.Center) {
                StudyIcon(R.drawable.ic_check, Modifier.size(28.dp), tint = colors.primary)
            }
            Text("You're all set", style = MaterialTheme.typography.headlineMedium, color = colors.foreground)
            Text(
                "Your studytime is ready. Head to your dashboard to see today's plan and start your first focus session.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
            Column(
                Modifier.fillMaxWidth().clip(TileShape).background(colors.muted.copy(alpha = 0.5f)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val studyDays = data.profile.availability.count { it.enabled }
                Text("Studying: ${data.profile.educationLevel.label}", style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                Text("Subjects: ${data.subjects.size}", style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                Text("Classes scheduled: ${data.entries.size}", style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                Text("Study days a week: $studyDays", style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                Text(
                    "Goals: ${formatMinutes(data.profile.dailyGoalMinutes)} a day, ${formatMinutes(data.profile.weeklyGoalMinutes)} a week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.foreground,
                )
            }
            Text(
                "Tip: the Planner can generate a study plan from your free hours, and the search icon at the top finds anything you add.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
            StepButtons(onBack, onFinish, continueLabel = "Go to dashboard")
        }
    }
}
