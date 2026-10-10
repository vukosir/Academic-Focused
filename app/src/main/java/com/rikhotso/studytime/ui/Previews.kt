package com.rikhotso.studytime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.DayBar
import com.rikhotso.studytime.data.Exam
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.Profile
import com.rikhotso.studytime.data.Settings
import com.rikhotso.studytime.data.Storage
import com.rikhotso.studytime.data.StudyTask
import com.rikhotso.studytime.data.Subject
import com.rikhotso.studytime.data.ThemeMode
import com.rikhotso.studytime.data.TimerSound
import com.rikhotso.studytime.data.TimetableEntry
import com.rikhotso.studytime.timer.AlarmScheduler
import com.rikhotso.studytime.ui.components.ColorPicker
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.Logo
import com.rikhotso.studytime.ui.components.ProgressRing
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.StatCard
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.WeekChart
import com.rikhotso.studytime.ui.screens.HomeScreen
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.StudytimeTheme
import java.time.LocalDate

/**
 * Previews for the Android Studio design pane. Open this file and choose Split or Design to
 * see them without running the app. Edit a component and the preview refreshes.
 */

@Preview(name = "Brand and pieces", showBackground = true, backgroundColor = 0xFFFAF9F4, widthDp = 360)
@Composable
private fun PiecesPreview() {
    StudytimeTheme(Settings(themeMode = ThemeMode.Light)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Logo()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(R.drawable.ic_flame, "Day streak", "3", PastelColor.Blush, Modifier.weight(1f))
                StatCard(R.drawable.ic_clock, "Today", "1h 15m", PastelColor.Lavender, Modifier.weight(1f))
            }
            SectionCard(title = "This week's study") {
                WeekChart(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").mapIndexed { index, label ->
                    DayBar(label, listOf(50, 25, 0, 90, 75, 29, 0)[index], isToday = index == 5)
                })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProgressRing(progress = 0.6f, modifier = Modifier.size(96.dp)) {
                    Text("54m", style = MaterialTheme.typography.titleLarge, color = StudyTheme.colors.foreground)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudyButton("Start session", onClick = {}, leadingIcon = R.drawable.ic_timer)
                    ColorPicker(selected = PastelColor.Mint, onSelect = {}, modifier = Modifier.width(220.dp))
                }
            }
            EmptyHint("No classes scheduled today.")
        }
    }
}

@Preview(name = "Home, light", showBackground = true, backgroundColor = 0xFFFAF9F4, widthDp = 360, heightDp = 1800)
@Composable
private fun HomeLightPreview() {
    StudytimeTheme(Settings(themeMode = ThemeMode.Light)) { HomeScreen(previewData(), previewViewModel(), previewNav()) }
}

@Preview(name = "Home, dark", showBackground = true, backgroundColor = 0xFF221B2D, widthDp = 360, heightDp = 1800)
@Composable
private fun HomeDarkPreview() {
    StudytimeTheme(Settings(themeMode = ThemeMode.Dark)) {
        Column(Modifier.background(StudyTheme.colors.background)) { HomeScreen(previewData(), previewViewModel(), previewNav()) }
    }
}

/** Made-up data for the previews only. The real app starts empty. */
private fun previewData(): AppData {
    val maths = Subject("p1", "Mathematics", PastelColor.Lavender, "Mrs. Pillay")
    val today = LocalDate.now()
    return AppData(
        onboarded = true,
        profile = Profile(name = "Preview Student", dailyGoalMinutes = 90),
        subjects = listOf(maths, Subject("p2", "Physics", PastelColor.Mint)),
        entries = listOf(TimetableEntry("e1", "p1", "Mathematics", PastelColor.Lavender, today.dayOfWeek.value, "09:00", "10:00", "Room 12")),
        tasks = listOf(StudyTask("t1", "Problem set 4", subjectId = "p1", dueDate = today.plusDays(1).toString())),
        exams = listOf(Exam("x1", "p2", "Physics final", today.plusDays(12).toString(), "09:00", "Hall B")),
    )
}

/** A ViewModel that saves nowhere and rings nothing, so previews never touch real data. */
private fun previewViewModel() = StudyViewModel(
    storage = object : Storage {
        override fun read(): String? = null
        override fun write(text: String) = Unit
    },
    alarms = object : AlarmScheduler {
        override fun schedule(focusEndsAt: Long?, breakEndsAt: Long?, breakMinutes: Int, sound: TimerSound, notify: Boolean) = Unit
        override fun cancelAll() = Unit
    },
)

private fun previewNav() = AppNav(goTab = {}, openPlanner = {}, openProfile = {}, openSubject = {}, openSearch = {})
