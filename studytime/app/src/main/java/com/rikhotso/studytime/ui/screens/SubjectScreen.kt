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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.Grade
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.StudyNote
import com.rikhotso.studytime.data.Subject
import com.rikhotso.studytime.data.Topic
import com.rikhotso.studytime.data.TopicStatus
import com.rikhotso.studytime.data.averagePercent
import com.rikhotso.studytime.data.dateOf
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.formatNumber
import com.rikhotso.studytime.data.mondayOf
import com.rikhotso.studytime.data.practiceTrend
import com.rikhotso.studytime.data.shortDate
import com.rikhotso.studytime.ui.AppNav
import com.rikhotso.studytime.ui.AppTab
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.ColorDot
import com.rikhotso.studytime.ui.components.ConfidencePicker
import com.rikhotso.studytime.ui.components.ConfirmDialog
import com.rikhotso.studytime.ui.components.DateField
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.FormDialog
import com.rikhotso.studytime.ui.components.IconAction
import com.rikhotso.studytime.ui.components.MenuItem
import com.rikhotso.studytime.ui.components.MiniStat
import com.rikhotso.studytime.ui.components.MoreMenu
import com.rikhotso.studytime.ui.components.NoticeDialog
import com.rikhotso.studytime.ui.components.PillTabs
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.components.StudyTextField
import com.rikhotso.studytime.ui.components.Tag
import com.rikhotso.studytime.ui.components.TileShape
import com.rikhotso.studytime.ui.components.TrashButton
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft
import com.rikhotso.studytime.ui.theme.solid
import java.time.LocalDate
import kotlin.math.roundToInt

/** Everything about one subject: its revision topics, notes, practice tests and grades. */
@Composable
fun SubjectScreen(data: AppData, viewModel: StudyViewModel, subjectId: String, nav: AppNav, onBack: () -> Unit) {
    val subject = data.subject(subjectId) ?: return
    val colors = StudyTheme.colors
    var tab by rememberSaveable(subjectId) { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val monday = mondayOf(today)
    val weekMinutes = data.sessions.filter { it.subjectId == subjectId && !dateOf(it.startedAt).isBefore(monday) }.sumOf { it.durationMinutes }
    val topics = data.topics.filter { it.subjectId == subjectId }
    val average = averagePercent(data.grades.filter { it.subjectId == subjectId })

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconAction(R.drawable.ic_arrow_left, "Back to the planner", onClick = onBack, tint = colors.foreground)
            ColorDot(subject.color, size = 16.dp)
            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Text(subject.name, style = MaterialTheme.typography.headlineSmall, color = colors.foreground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    (if (subject.teacher.isNotEmpty()) "${subject.teacher} · " else "") + "${formatMinutes(subject.goalMinutesPerDay)}/day goal",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
            }
            MoreMenu(
                "More actions for ${subject.name}",
                listOf(
                    MenuItem("Edit subject") { editing = true },
                    MenuItem("Move up in the list") { viewModel.moveSubject(subjectId, up = true) },
                    MenuItem("Move down in the list") { viewModel.moveSubject(subjectId, up = false) },
                    MenuItem("Delete subject", danger = true) { deleting = true },
                ),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat("This week", formatMinutes(weekMinutes), Modifier.weight(1f))
            MiniStat("Topics done", "${topics.count { it.status == TopicStatus.Completed }}/${topics.size}", Modifier.weight(1f))
            MiniStat("Avg grade", if (average == null) "none" else "${average.roundToInt()}%", Modifier.weight(1f))
        }

        PillTabs(listOf("Topics", "Notes", "Practice tests", "Grades"), selectedIndex = tab, onSelect = { tab = it })
        when (tab) {
            0 -> TopicsTab(data, viewModel, subject, topics, nav)
            1 -> NotesTab(data, viewModel, subject, topics)
            2 -> TestsTab(data, viewModel, subject)
            else -> GradesTab(data, viewModel, subject)
        }
    }

    if (editing) SubjectDialog(data, viewModel, subject, onDone = { editing = false })
    if (deleting) {
        ConfirmDialog(
            title = "Delete ${subject.name}?",
            text = "Its classes, revision topics and practice tests are deleted too. Your study history and grades are kept, and its tasks, notes and exams stay without a subject.",
            confirmLabel = "Delete subject",
            onConfirm = {
                deleting = false
                viewModel.removeSubject(subjectId)
                onBack()
            },
            onDismiss = { deleting = false },
            danger = true,
        )
    }
}

// ------------------------------------------------------------------ topics

@Composable
private fun TopicsTab(data: AppData, viewModel: StudyViewModel, subject: Subject, topics: List<Topic>, nav: AppNav) {
    val colors = StudyTheme.colors
    var draft by rememberSaveable(subject.id) { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<Topic?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now().toString()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard(title = "Add topics") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StudyTextField(
                    draft,
                    { draft = it },
                    label = "One topic per line",
                    placeholder = "Algebra\nTrigonometry\nCalculus",
                    singleLine = false,
                )
                error?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.destructive) }
                StudyButton("Add to the list", onClick = {
                    error = viewModel.addTopics(subject.id, draft)
                    if (error == null) draft = ""
                }, kind = ButtonKind.Secondary, small = true, leadingIcon = R.drawable.ic_plus)
            }
        }

        if (topics.isEmpty()) {
            EmptyHint("No topics yet. Break the syllabus into topics to track what is done and what still needs work.")
        } else {
            val done = topics.count { it.status == TopicStatus.Completed }
            Text(
                "$done of ${topics.size} completed · tap a status to change it · rate each topic from 1 (lost) to 5 (solid)",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
        }

        topics.forEach { topic ->
            Column(
                Modifier.fillMaxWidth().clip(TileShape).background(colors.card).border(1.dp, colors.border, TileShape).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(topic.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                    MoreMenu(
                        "More actions for ${topic.name}",
                        listOf(
                            MenuItem("Study this now") {
                                val problem = viewModel.prepareTimerForTopic(topic.id)
                                if (problem == null) nav.goTab(AppTab.Timer) else notice = problem
                            },
                            MenuItem("Rename") { renaming = topic },
                        ) + (if (topic.nextReview.isNotEmpty()) listOf(MenuItem("Stop review reminders") { viewModel.clearTopicReviews(topic.id) }) else emptyList()) +
                            listOf(MenuItem("Delete", danger = true) { viewModel.removeTopic(topic.id) }),
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val next = TopicStatus.entries[(topic.status.ordinal + 1) % TopicStatus.entries.size]
                    val (background, content) = when (topic.status) {
                        TopicStatus.Completed -> PastelColor.Mint.soft to colors.foreground
                        TopicStatus.InProgress -> colors.secondary to colors.onSecondary
                        TopicStatus.NotStarted -> colors.muted to colors.mutedForeground
                    }
                    Box(
                        Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(role = Role.Button, onClickLabel = "Change to ${next.label}") { viewModel.setTopicStatus(topic.id, next) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Tag(topic.status.label, background = background, content = content)
                    }
                    ConfidencePicker(topic.confidence, { viewModel.setTopicConfidence(topic.id, it) })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when {
                            topic.reviewCount == 0 -> "Not reviewed yet"
                            topic.nextReview.isEmpty() -> "Reviewed ${topic.reviewCount} time${if (topic.reviewCount == 1) "" else "s"}"
                            topic.nextReview <= today -> "Reviewed ${topic.reviewCount} time${if (topic.reviewCount == 1) "" else "s"} · review due now"
                            else -> "Reviewed ${topic.reviewCount} time${if (topic.reviewCount == 1) "" else "s"} · next review ${shortDate(topic.nextReview)}"
                        },
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (topic.nextReview.isNotEmpty() && topic.nextReview <= today) colors.primary else colors.mutedForeground,
                    )
                    StudyButton("Reviewed", onClick = { viewModel.reviewTopic(topic.id) }, kind = ButtonKind.Secondary, small = true, modifier = Modifier.padding(end = 8.dp))
                }
            }
        }
        if (topics.isNotEmpty()) {
            Text(
                "Each time you tap Reviewed, the next review moves further away: 1 day, 3 days, 1 week, 2 weeks, then monthly.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
            )
        }
    }

    renaming?.let { topic ->
        var name by remember(topic.id) { mutableStateOf(topic.name) }
        var renameError by remember(topic.id) { mutableStateOf<String?>(null) }
        FormDialog(
            title = "Rename topic",
            confirmLabel = "Save",
            onDismiss = { renaming = null },
            onConfirm = {
                renameError = viewModel.renameTopic(topic.id, name)
                if (renameError == null) renaming = null
            },
            error = renameError,
        ) {
            StudyTextField(name, { name = it }, label = "Topic name")
        }
    }
    notice?.let { text -> NoticeDialog("Not possible right now", text, onDone = { notice = null }) }
}

// ------------------------------------------------------------------ notes

@Composable
private fun NotesTab(data: AppData, viewModel: StudyViewModel, subject: Subject, topics: List<Topic>) {
    val colors = StudyTheme.colors
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StudyNote?>(null) }
    var topicFilter by rememberSaveable(subject.id) { mutableStateOf("*") }
    val all = data.notes.filter { it.subjectId == subject.id }
    val notes = all.filter { topicFilter == "*" || it.topicId == topicFilter }.sortedByDescending { it.updatedAt }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudyButton("Note", onClick = { adding = true }, small = true, leadingIcon = R.drawable.ic_plus)
        if (all.isNotEmpty() && topics.isNotEmpty()) {
            SelectField("Show", listOf("*" to "Every note", "" to "Notes without a topic") + topics.map { it.id to it.name }, topicFilter, { topicFilter = it })
        }
        if (notes.isEmpty()) {
            EmptyHint(if (all.isEmpty()) "No notes yet. Write up what you revise and file it under a topic." else "No notes under this topic.")
        }
        notes.forEach { note ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(TileShape)
                    .background(subject.color.soft)
                    .clickable(role = Role.Button, onClickLabel = "Open ${note.title}") { editing = note }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(note.title, style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                if (note.body.isNotEmpty()) {
                    Text(note.body, style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    topics.firstOrNull { it.id == note.topicId }?.let { Tag(it.name, background = colors.background.copy(alpha = 0.7f), content = colors.foreground) }
                    Text("Edited ${shortDate(dateOf(note.updatedAt).toString())}", style = MaterialTheme.typography.labelSmall, color = colors.mutedForeground)
                }
            }
        }
    }

    if (adding) NoteDialog(viewModel, subject, topics, null, onDone = { adding = false })
    editing?.let { note -> NoteDialog(viewModel, subject, topics, note, onDone = { editing = null }) }
}

@Composable
private fun NoteDialog(viewModel: StudyViewModel, subject: Subject, topics: List<Topic>, existing: StudyNote?, onDone: () -> Unit) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var topicId by remember { mutableStateOf(existing?.topicId.orEmpty()) }
    var body by remember { mutableStateOf(existing?.body.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = if (existing == null) "New note" else "Edit note",
        confirmLabel = "Save",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.saveNote(existing?.id, subject.id, topicId, title, body)
            if (error == null) onDone()
        },
        error = error,
    ) {
        StudyTextField(title, { title = it }, label = "Title", placeholder = "e.g. Chain rule summary")
        if (topics.isNotEmpty()) {
            SelectField("Topic", listOf("" to "No topic") + topics.map { it.id to it.name }, topicId, { topicId = it })
        }
        StudyTextField(body, { body = it }, label = "Note", placeholder = "Write your notes here", singleLine = false, minLines = 6)
        if (existing != null) {
            StudyButton("Delete this note", onClick = {
                viewModel.removeNote(existing.id)
                onDone()
            }, kind = ButtonKind.Danger, small = true, leadingIcon = R.drawable.ic_trash_2)
        }
    }
}

// ------------------------------------------------------------------ practice tests

@Composable
private fun TestsTab(data: AppData, viewModel: StudyViewModel, subject: Subject) {
    val colors = StudyTheme.colors
    var adding by remember { mutableStateOf(false) }
    val trend = practiceTrend(data.practiceTests.filter { it.subjectId == subject.id })

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudyButton("Practice test", onClick = { adding = true }, small = true, leadingIcon = R.drawable.ic_plus)
        if (trend.isEmpty()) {
            EmptyHint("No practice tests yet. Log each past paper or quiz to see how your scores move over time.")
        } else {
            SectionCard(title = "Scores over time") {
                val summary = trend.joinToString(", ") { "${it.first.title} ${it.second}%" }
                Row(
                    Modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = "Practice test scores, oldest first. $summary" },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    trend.takeLast(10).forEach { (test, percent) ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$percent%", fontSize = 10.sp, color = colors.mutedForeground, maxLines = 1, softWrap = false)
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                Box(
                                    Modifier.fillMaxHeight((percent / 100f).coerceIn(0.03f, 1f)).widthIn(max = 34.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(subject.color.solid),
                                )
                            }
                            Text(shortDate(test.date).substringAfter(' '), fontSize = 9.sp, color = colors.mutedForeground, maxLines = 1, softWrap = false)
                        }
                    }
                }
                if (trend.size >= 2) {
                    val change = trend.last().second - trend.first().second
                    Text(
                        when {
                            change > 0 -> "Up $change points from your first test to your latest."
                            change < 0 -> "Down ${-change} points from your first test to your latest."
                            else -> "Level with your first test."
                        } + " Average: ${trend.sumOf { it.second } / trend.size}%.",
                        Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                    )
                }
            }
            trend.reversed().forEachIndexed { index, (test, percent) ->
                val previous = trend.reversed().getOrNull(index + 1)?.second
                Row(
                    Modifier.fillMaxWidth().clip(TileShape).background(subject.color.soft).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(test.title, style = MaterialTheme.typography.titleSmall, color = colors.foreground)
                        Text(
                            "${shortDate(test.date)} · ${formatNumber(test.score)}/${formatNumber(test.maxScore)}" +
                                if (previous != null) " · ${if (percent >= previous) "+" else ""}${percent - previous} since the one before" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground,
                        )
                    }
                    Text("$percent%", style = MaterialTheme.typography.titleMedium, color = colors.foreground)
                    TrashButton(onClick = { viewModel.removePracticeTest(test.id) }, description = "Delete ${test.title}")
                }
            }
        }
    }

    if (adding) {
        var title by remember { mutableStateOf("") }
        var score by remember { mutableStateOf("") }
        var max by remember { mutableStateOf("100") }
        var date by remember { mutableStateOf(LocalDate.now().toString()) }
        var error by remember { mutableStateOf<String?>(null) }
        FormDialog(
            title = "Log a practice test",
            confirmLabel = "Save",
            onDismiss = { adding = false },
            onConfirm = {
                error = viewModel.addPracticeTest(subject.id, title, score, max, date)
                if (error == null) adding = false
            },
            error = error,
        ) {
            StudyTextField(title, { title = it }, label = "Test", placeholder = "e.g. 2024 Paper 1")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudyTextField(score, { score = it }, label = "Score", modifier = Modifier.weight(1f), placeholder = "62", keyboardType = KeyboardType.Decimal)
                StudyTextField(max, { max = it }, label = "Out of", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            }
            DateField("Date", date, { date = it })
        }
    }
}

// ------------------------------------------------------------------ grades

@Composable
private fun GradesTab(data: AppData, viewModel: StudyViewModel, subject: Subject) {
    var adding by remember { mutableStateOf(false) }
    val grades = data.grades.filter { it.subjectId == subject.id }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudyButton("Grade", onClick = { adding = true }, small = true, leadingIcon = R.drawable.ic_plus)
        if (grades.isEmpty()) {
            EmptyHint("No grades logged for this subject yet.")
        } else {
            GradeGroup(subject.name, grades, onDelete = { viewModel.removeGrade(it) })
        }
    }
    if (adding) GradeDialog(data, viewModel, onDone = { adding = false }, presetSubjectId = subject.id)
}

/** One subject's grades with the average in the corner. Used here and on the profile. */
@Composable
fun GradeGroup(subjectName: String, grades: List<Grade>, onDelete: (String) -> Unit) {
    val colors = StudyTheme.colors
    val average = averagePercent(grades)
    Column(
        Modifier.fillMaxWidth().clip(TileShape).background(grades.first().color.soft).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(subjectName, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.foreground)
            if (average != null) {
                StudyIcon(R.drawable.ic_trending_up, Modifier.size(14.dp), tint = colors.foreground)
                Text("${formatNumber((average * 10).roundToInt() / 10.0)}% avg", style = MaterialTheme.typography.labelMedium, color = colors.foreground)
            }
        }
        grades.forEach { grade ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.background.copy(alpha = 0.6f)).padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    grade.assessmentName + if (grade.term.isNotEmpty()) " · ${grade.term}" else "",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.foreground,
                )
                val percent = if (grade.maxScore > 0) (grade.score / grade.maxScore * 100).roundToInt() else 0
                Text(
                    "${formatNumber(grade.score)}/${formatNumber(grade.maxScore)} ($percent%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.foreground,
                )
                TrashButton(onClick = { onDelete(grade.id) }, description = "Delete ${grade.assessmentName}")
            }
        }
    }
}

@Composable
fun GradeDialog(data: AppData, viewModel: StudyViewModel, onDone: () -> Unit, presetSubjectId: String = "") {
    var subjectId by remember { mutableStateOf(presetSubjectId.ifEmpty { if (data.subjects.size == 1) data.subjects.first().id else "" }) }
    var assessment by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var max by remember { mutableStateOf("100") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var term by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = "Log a grade",
        confirmLabel = "Save grade",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.addGrade(subjectId, assessment, score, max, date, term)
            if (error == null) onDone()
        },
        error = error,
    ) {
        SelectField("Subject", data.subjects.map { it.id to it.name }, subjectId.ifEmpty { null }, { subjectId = it }, placeholder = "Pick subject")
        StudyTextField(assessment, { assessment = it }, label = "Assessment", placeholder = "e.g. Term 1 Test")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StudyTextField(score, { score = it }, label = "Score", modifier = Modifier.weight(1f), placeholder = "78", keyboardType = KeyboardType.Decimal)
            StudyTextField(max, { max = it }, label = "Out of", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
        }
        DateField("Date", date, { date = it })
        StudyTextField(term, { term = it }, label = "Term (optional)", placeholder = "Term 1")
    }
}
