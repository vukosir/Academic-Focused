@file:OptIn(ExperimentalLayoutApi::class)

package com.rikhotso.studytime.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.AvatarFrame
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.badgesEarned
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.levelInfo
import com.rikhotso.studytime.data.minutesByDay
import com.rikhotso.studytime.data.streakInfo
import com.rikhotso.studytime.data.weekBars
import com.rikhotso.studytime.ui.AppNav
import com.rikhotso.studytime.ui.ProfileTab
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.ColorPicker
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.FieldLabel
import com.rikhotso.studytime.ui.components.FormDialog
import com.rikhotso.studytime.ui.components.MinutesSlider
import com.rikhotso.studytime.ui.components.NoticeDialog
import com.rikhotso.studytime.ui.components.PillTabs
import com.rikhotso.studytime.ui.components.ProfileAvatar
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.StatCard
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyTextField
import com.rikhotso.studytime.ui.components.Tag
import com.rikhotso.studytime.ui.components.WeekChart
import com.rikhotso.studytime.ui.rememberProfilePhotoPicker
import com.rikhotso.studytime.ui.theme.StudyTheme
import java.time.LocalDate

/** The profile: who you are, then Overview, Stats, Rewards and Settings. */
@Composable
fun ProfileScreen(data: AppData, viewModel: StudyViewModel, nav: AppNav, tab: ProfileTab, onTab: (ProfileTab) -> Unit) {
    val colors = StudyTheme.colors
    var editing by remember { mutableStateOf(false) }
    val level = levelInfo(data)
    val profile = data.profile

    ScreenColumn {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileAvatar(profile)
                Column(Modifier.weight(1f)) {
                    Text(profile.name.ifBlank { "Student" }, style = MaterialTheme.typography.headlineSmall, color = colors.foreground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(if (profile.username.isNotEmpty()) "@${profile.username}" else "", profile.about.ifBlank { profile.educationLevel.label })
                            .filter { it.isNotEmpty() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedForeground,
                    )
                }
                StudyButton("Edit", onClick = { editing = true }, kind = ButtonKind.Ghost, small = true, leadingIcon = R.drawable.ic_pencil)
            }
            if (profile.bio.isNotEmpty()) {
                Text(profile.bio, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
            }
            FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Tag("Level ${level.level}", background = colors.secondary, content = colors.onSecondary)
                Tag("${level.xp} points")
                Tag(profile.educationLevel.label)
            }
        }

        PillTabs(ProfileTab.entries.map { it.label }, selectedIndex = tab.ordinal, onSelect = { onTab(ProfileTab.entries[it]) })

        when (tab) {
            ProfileTab.Overview -> OverviewTab(data, viewModel, nav)
            ProfileTab.Stats -> StatsTab(data, viewModel)
            ProfileTab.Rewards -> RewardsTab(data, viewModel)
            ProfileTab.Settings -> SettingsTab(data, viewModel)
        }
    }

    if (editing) ProfileDialog(data, viewModel, onDone = { editing = false })
}

@Composable
private fun OverviewTab(data: AppData, viewModel: StudyViewModel, nav: AppNav) {
    val colors = StudyTheme.colors
    val today = LocalDate.now()
    val byDay = remember(data.sessions) { minutesByDay(data.sessions) }
    val streak = streakInfo(byDay, today, data.profile.streakMinimumMinutes)
    var dialog by remember { mutableStateOf("") }
    // The slider moves freely; the goal is only saved when "Save goal" is pressed.
    var goalDraft by remember(data.profile.dailyGoalMinutes) { mutableIntStateOf(data.profile.dailyGoalMinutes) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(R.drawable.ic_clock, "Total focus", formatMinutes(data.sessions.sumOf { it.durationMinutes }), PastelColor.Lavender, Modifier.weight(1f))
                StatCard(R.drawable.ic_flame, "Day streak", streak.current.toString(), PastelColor.Blush, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(R.drawable.ic_award, "Badges earned", badgesEarned(data, today).toString(), PastelColor.Butter, Modifier.weight(1f))
                StatCard(R.drawable.ic_book_open, "Subjects", data.subjects.size.toString(), PastelColor.Mint, Modifier.weight(1f))
            }
        }

        SectionCard(title = "This week's study") {
            WeekChart(weekBars(byDay, today))
        }

        SectionCard(
            title = "Daily study goal",
            action = { Text(formatMinutes(goalDraft), style = MaterialTheme.typography.titleLarge, color = colors.primary) },
        ) {
            MinutesSlider(goalDraft, { goalDraft = it }, min = 15, max = 360, step = 15)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("15m", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                Text("6h", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StudyButton("Save goal", onClick = { viewModel.setDailyGoal(goalDraft) }, small = true, leadingIcon = R.drawable.ic_target)
                StudyButton("Weekly and monthly goals", onClick = { nav.openProfile(ProfileTab.Settings) }, kind = ButtonKind.Ghost, small = true)
            }
        }

        SectionCard(
            title = "Grades and progress",
            action = {
                StudyButton("Add grade", onClick = { dialog = if (data.subjects.isEmpty()) "needSubject" else "grade" }, small = true, leadingIcon = R.drawable.ic_plus)
            },
        ) {
            if (data.grades.isEmpty()) {
                EmptyHint("No grades logged yet. Add one to track your academic progress alongside your study time.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.grades.groupBy { it.subjectName }.forEach { (subjectName, grades) ->
                        GradeGroup(subjectName, grades, onDelete = { viewModel.removeGrade(it) })
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Everything is saved on this phone.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            Text("studytime · made with care", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
        }
    }

    when (dialog) {
        "grade" -> GradeDialog(data, viewModel, onDone = { dialog = "" })
        "needSubject" -> NoticeDialog(
            title = "Add a subject first",
            text = "Grades are logged against a subject. Add one in the Planner, then come back here.",
            onDone = { dialog = "" },
        )
    }
}

@Composable
private fun ProfileDialog(data: AppData, viewModel: StudyViewModel, onDone: () -> Unit) {
    val colors = StudyTheme.colors
    val profile = data.profile
    val level = levelInfo(data).level
    var name by remember { mutableStateOf(profile.name) }
    var username by remember { mutableStateOf(profile.username) }
    var about by remember { mutableStateOf(profile.about) }
    var bio by remember { mutableStateOf(profile.bio) }
    var education by remember { mutableStateOf(profile.educationLevel) }
    var color by remember { mutableStateOf(profile.avatarColor) }
    var frame by remember { mutableStateOf(profile.avatarFrame) }
    var error by remember { mutableStateOf<String?>(null) }
    val pickPhoto = rememberProfilePhotoPicker(
        onSaved = { viewModel.setProfilePicture(it) },
        onFailed = { error = "That picture could not be opened. Try another one." },
    )
    FormDialog(
        title = "Edit profile",
        confirmLabel = "Save",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.updateProfile(name, username, about, bio, education, color, frame)
            if (error == null) onDone()
        },
        error = error,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ProfileAvatar(profile.copy(name = name, avatarColor = color, avatarFrame = frame), size = 56.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StudyButton("Choose photo", onClick = pickPhoto, kind = ButtonKind.Secondary, small = true, leadingIcon = R.drawable.ic_image)
                if (profile.picturePath.isNotEmpty()) {
                    StudyButton("Remove photo", onClick = { viewModel.setProfilePicture("") }, kind = ButtonKind.Ghost, small = true)
                }
            }
        }
        StudyTextField(name, { name = it }, label = "Display name", capitalization = KeyboardCapitalization.Words)
        StudyTextField(username, { username = it }, label = "Username (optional)", placeholder = "e.g. vukosi_r", capitalization = KeyboardCapitalization.None)
        StudyTextField(about, { about = it }, label = "What you study (optional)", placeholder = "e.g. Diploma in IT")
        StudyTextField(bio, { bio = it.take(200) }, label = "Short bio (optional)", singleLine = false)
        SelectField("Education level", com.rikhotso.studytime.data.EducationLevel.entries.map { it to it.label }, education, { education = it })
        Column {
            FieldLabel("Avatar colour")
            ColorPicker(selected = color, onSelect = { color = it })
        }
        SelectField(
            "Avatar frame",
            AvatarFrame.entries.map { it to if (it.requiredLevel > level) "${it.label} (unlocks at level ${it.requiredLevel})" else it.label },
            frame,
            { frame = it },
        )
        Text("The photo stays on this phone. Frames are unlocked by levelling up.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
    }
}
