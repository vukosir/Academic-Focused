@file:OptIn(ExperimentalLayoutApi::class)

package com.rikhotso.studytime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.Exam
import com.rikhotso.studytime.data.daysUntil
import com.rikhotso.studytime.data.examReadiness
import com.rikhotso.studytime.data.formatNumber
import com.rikhotso.studytime.data.relativeDay
import com.rikhotso.studytime.data.shortDate
import com.rikhotso.studytime.ui.AppNav
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.ChoicePill
import com.rikhotso.studytime.ui.components.ConfirmDialog
import com.rikhotso.studytime.ui.components.DateField
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.FieldLabel
import com.rikhotso.studytime.ui.components.FormDialog
import com.rikhotso.studytime.ui.components.InfoNote
import com.rikhotso.studytime.ui.components.MenuItem
import com.rikhotso.studytime.ui.components.MoreMenu
import com.rikhotso.studytime.ui.components.ProgressBar
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.components.StudyTextField
import com.rikhotso.studytime.ui.components.Tag
import com.rikhotso.studytime.ui.components.TileShape
import com.rikhotso.studytime.ui.components.TimeField
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft
import java.time.LocalDate

/** The exam timetable with a countdown and a revision overview for each exam. */
@Composable
fun ExamsTab(data: AppData, viewModel: StudyViewModel, nav: AppNav) {
    val colors = StudyTheme.colors
    val today = LocalDate.now()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Exam?>(null) }
    var deleting by remember { mutableStateOf<Exam?>(null) }
    var planning by remember { mutableStateOf<Exam?>(null) }

    val upcoming = data.exams.filter { (daysUntil(it.date, today) ?: -1) >= 0 }.sortedBy { it.date + it.time }
    val past = data.exams.filter { (daysUntil(it.date, today) ?: -1) < 0 }.sortedByDescending { it.date }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudyButton("Exam", onClick = { adding = true }, small = true, leadingIcon = R.drawable.ic_plus)

        if (data.exams.isEmpty()) {
            EmptyHint("No exams yet. Add your exam dates to get a countdown and a revision overview for each one.")
        }

        upcoming.forEach { exam ->
            val readiness = examReadiness(data, exam, today)
            val color = data.subjectColor(exam.subjectId)
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        Modifier.size(60.dp).clip(RoundedCornerShape(20.dp)).background(color.soft),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(readiness.daysLeft.toString(), style = MaterialTheme.typography.headlineSmall, color = colors.foreground)
                        Text(if (readiness.daysLeft == 1L) "day" else "days", style = MaterialTheme.typography.labelSmall, color = colors.mutedForeground)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(exam.title, style = MaterialTheme.typography.titleMedium, color = colors.foreground)
                        Text(
                            "${shortDate(exam.date)} at ${exam.time}" + if (exam.location.isNotEmpty()) " · ${exam.location}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                        )
                        Text(
                            (if (exam.subjectId.isNotEmpty()) data.subjectName(exam.subjectId) + " · " else "") + "Exam is ${relativeDay(readiness.daysLeft)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                        )
                    }
                    MoreMenu(
                        "More actions for ${exam.title}",
                        listOf(MenuItem("Edit") { editing = exam }, MenuItem("Delete", danger = true) { deleting = exam }),
                    )
                }

                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Revision overview", style = MaterialTheme.typography.labelLarge, color = colors.foreground)
                    when {
                        exam.subjectId.isEmpty() -> Text("Link this exam to a subject to track its revision topics here.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                        readiness.topicsTotal == 0 -> Text("No revision topics yet. Add the topics of this subject to see what is covered and what is left.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                        else -> {
                            ProgressBar(readiness.coveragePercent)
                            Text(
                                "${readiness.topicsCompleted} of ${readiness.topicsTotal} topics completed (${readiness.coveragePercent}%) · ${readiness.topicsInProgress} in progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedForeground,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tag(if (readiness.averageConfidence != null) "Confidence ${formatNumber(readiness.averageConfidence)} of 5" else "Confidence not rated yet")
                                Tag("${readiness.openTasks} open task${if (readiness.openTasks == 1) "" else "s"}")
                                Tag("${readiness.plannedSessionsLeft} session${if (readiness.plannedSessionsLeft == 1) "" else "s"} planned")
                            }
                            if (readiness.needAttention.isNotEmpty()) {
                                Column(
                                    Modifier.fillMaxWidth().clip(TileShape).border(1.dp, colors.border, TileShape).padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StudyIcon(R.drawable.ic_circle_alert, Modifier.size(14.dp), tint = colors.mutedForeground)
                                        Text("Still needs attention", style = MaterialTheme.typography.labelMedium, color = colors.foreground)
                                    }
                                    Text(
                                        readiness.needAttention.take(5).joinToString(", ") { it.name } +
                                            if (readiness.needAttention.size > 5) " and ${readiness.needAttention.size - 5} more" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.mutedForeground,
                                    )
                                }
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (exam.subjectId.isNotEmpty()) {
                            StudyButton("Plan revision", onClick = { planning = exam }, small = true, leadingIcon = R.drawable.ic_wand_sparkles)
                            StudyButton("Topics", onClick = { nav.openSubject(exam.subjectId) }, kind = ButtonKind.Secondary, small = true, leadingIcon = R.drawable.ic_list_checks)
                        }
                    }
                }
            }
        }

        if (upcoming.isNotEmpty()) {
            InfoNote("The overview shows what you have covered and what is left. It does not predict your mark.")
        }

        if (past.isNotEmpty()) {
            SectionCard(title = "Past exams") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    past.forEach { exam ->
                        Row(
                            Modifier.fillMaxWidth().clip(TileShape).background(colors.muted.copy(alpha = 0.6f)).padding(start = 14.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(exam.title, style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                                Text("${shortDate(exam.date)} · ${relativeDay(daysUntil(exam.date, today) ?: 0)}", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                            }
                            MoreMenu("More actions for ${exam.title}", listOf(MenuItem("Edit") { editing = exam }, MenuItem("Delete", danger = true) { deleting = exam }))
                        }
                    }
                }
            }
        }
    }

    if (adding) ExamDialog(data, viewModel, null, onDone = { adding = false })
    editing?.let { exam -> ExamDialog(data, viewModel, exam, onDone = { editing = null }) }
    planning?.let { exam -> RevisionPlanDialog(viewModel, exam, onDone = { planning = null }) }
    deleting?.let { exam ->
        ConfirmDialog(
            title = "Delete this exam?",
            text = "\"${exam.title}\" will be removed from your exam timetable. Revision sessions already planned for it stay in the planner.",
            confirmLabel = "Delete exam",
            onConfirm = {
                viewModel.removeExam(exam.id)
                deleting = null
            },
            onDismiss = { deleting = null },
            danger = true,
        )
    }
}

@Composable
private fun ExamDialog(data: AppData, viewModel: StudyViewModel, existing: Exam?, onDone: () -> Unit) {
    var subjectId by remember { mutableStateOf(existing?.subjectId.orEmpty()) }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var date by remember { mutableStateOf(existing?.date ?: LocalDate.now().plusWeeks(2).toString()) }
    var time by remember { mutableStateOf(existing?.time ?: "09:00") }
    var location by remember { mutableStateOf(existing?.location.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = if (existing == null) "Add an exam" else "Edit exam",
        confirmLabel = if (existing == null) "Add exam" else "Save",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.saveExam(existing?.id, subjectId, title, date, time, location)
            if (error == null) onDone()
        },
        error = error,
    ) {
        SelectField("Subject", listOf("" to "No subject") + data.subjects.map { it.id to it.name }, subjectId, { subjectId = it })
        StudyTextField(title, { title = it }, label = "Exam name", placeholder = "e.g. Paper 1, or leave blank to use the subject")
        DateField("Date", date, { date = it })
        TimeField("Start time", time, { time = it })
        StudyTextField(location, { location = it }, label = "Location (optional)", placeholder = "e.g. Hall B")
    }
}

/** Asks for the session length, then plans one revision session per unfinished topic before the exam. */
@Composable
private fun RevisionPlanDialog(viewModel: StudyViewModel, exam: Exam, onDone: () -> Unit) {
    val colors = StudyTheme.colors
    var minutes by remember { mutableIntStateOf(45) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = "Plan revision",
        confirmLabel = "Plan it",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.generateRevision(exam.id, minutes)
            if (error == null) onDone()
        },
        error = error,
    ) {
        Text(
            "For ${exam.title}: one session for each topic that is not completed yet, starting with the ones you rated lowest. " +
                "Sessions go into free time inside your study hours, at most two a day, and stop the day before the exam.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mutedForeground,
        )
        Column {
            FieldLabel("Session length")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 45, 60).forEach { value ->
                    ChoicePill("${value}m", selected = minutes == value, onClick = { minutes = value }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
