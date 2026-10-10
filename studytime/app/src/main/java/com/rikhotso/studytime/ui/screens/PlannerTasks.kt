@file:OptIn(ExperimentalLayoutApi::class)

package com.rikhotso.studytime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.Priority
import com.rikhotso.studytime.data.StudyTask
import com.rikhotso.studytime.data.TaskCategory
import com.rikhotso.studytime.data.TaskDueFilter
import com.rikhotso.studytime.data.TaskFilter
import com.rikhotso.studytime.data.TaskStatusFilter
import com.rikhotso.studytime.data.daysUntil
import com.rikhotso.studytime.data.filterTasks
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.isOverdue
import com.rikhotso.studytime.data.relativeDay
import com.rikhotso.studytime.data.shortDate
import com.rikhotso.studytime.data.subtaskProgress
import com.rikhotso.studytime.ui.AppNav
import com.rikhotso.studytime.ui.AppTab
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.CheckCircle
import com.rikhotso.studytime.ui.components.ChoicePill
import com.rikhotso.studytime.ui.components.ConfirmDialog
import com.rikhotso.studytime.ui.components.DateField
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.FieldLabel
import com.rikhotso.studytime.ui.components.FormDialog
import com.rikhotso.studytime.ui.components.IconAction
import com.rikhotso.studytime.ui.components.MenuItem
import com.rikhotso.studytime.ui.components.MoreMenu
import com.rikhotso.studytime.ui.components.NoticeDialog
import com.rikhotso.studytime.ui.components.PillTabs
import com.rikhotso.studytime.ui.components.PriorityTag
import com.rikhotso.studytime.ui.components.SearchField
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyTextField
import com.rikhotso.studytime.ui.components.SwitchRow
import com.rikhotso.studytime.ui.components.Tag
import com.rikhotso.studytime.ui.components.TimeField
import com.rikhotso.studytime.ui.components.TileShape
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft
import java.time.LocalDate
import java.time.LocalTime

/** The task manager: search, filters, and a card per task with its checklist. */
@Composable
fun TasksTab(data: AppData, viewModel: StudyViewModel, nav: AppNav) {
    val colors = StudyTheme.colors
    val today = LocalDate.now()
    val nowMinute = LocalTime.now().let { it.hour * 60 + it.minute }
    var query by rememberSaveable { mutableStateOf("") }
    var statusIndex by rememberSaveable { mutableStateOf(0) }
    var subjectFilter by rememberSaveable { mutableStateOf("*") }
    var priorityKey by rememberSaveable { mutableStateOf("") }
    var dueIndex by rememberSaveable { mutableStateOf(0) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StudyTask?>(null) }
    var planning by remember { mutableStateOf<StudyTask?>(null) }
    var deleting by remember { mutableStateOf<StudyTask?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var expandedId by rememberSaveable { mutableStateOf("") }

    val filter = TaskFilter(
        query = query,
        status = TaskStatusFilter.entries[statusIndex],
        subjectId = if (subjectFilter == "*") null else subjectFilter,
        priority = Priority.entries.firstOrNull { it.key == priorityKey },
        due = TaskDueFilter.entries[dueIndex],
    )
    val tasks = filterTasks(data, filter, today, nowMinute)
    val overdueCount = data.tasks.count { isOverdue(it, today, nowMinute) }
    val extraFilters = listOf(subjectFilter != "*", priorityKey.isNotEmpty(), dueIndex != 0).count { it }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SearchField(query, { query = it }, placeholder = "Search tasks")
        PillTabs(
            TaskStatusFilter.entries.map { if (it == TaskStatusFilter.Overdue && overdueCount > 0) "${it.label} ($overdueCount)" else it.label },
            selectedIndex = statusIndex,
            onSelect = { statusIndex = it },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyButton("Task", onClick = { adding = true }, small = true, leadingIcon = R.drawable.ic_plus)
            StudyButton(
                if (extraFilters > 0) "Filters ($extraFilters)" else "Filters",
                onClick = { showFilters = !showFilters },
                kind = ButtonKind.Secondary,
                small = true,
                leadingIcon = R.drawable.ic_sliders_horizontal,
            )
            if (extraFilters > 0 || query.isNotEmpty()) {
                StudyButton("Clear", onClick = {
                    query = ""
                    subjectFilter = "*"
                    priorityKey = ""
                    dueIndex = 0
                }, kind = ButtonKind.Ghost, small = true)
            }
        }
        if (showFilters) {
            Column(
                Modifier.fillMaxWidth().clip(TileShape).border(1.dp, colors.border, TileShape).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SelectField(
                    "Subject",
                    listOf("*" to "Every subject", "" to "No subject") + data.subjects.map { it.id to it.name },
                    subjectFilter,
                    { subjectFilter = it },
                )
                SelectField("Priority", listOf("" to "Any priority") + Priority.entries.reversed().map { it.key to it.label }, priorityKey, { priorityKey = it })
                SelectField("Due", TaskDueFilter.entries.mapIndexed { index, due -> index to due.label }, dueIndex, { dueIndex = it })
            }
        }

        if (tasks.isEmpty()) {
            EmptyHint(
                when {
                    data.tasks.isEmpty() -> "No tasks yet. Add homework, revision or a project to keep track of what is due."
                    !filter.isDefault -> "No tasks match this search and these filters."
                    else -> "Nothing left to do. Enjoy it."
                },
            )
        }
        tasks.forEach { task ->
            TaskCard(
                data = data,
                viewModel = viewModel,
                task = task,
                overdue = isOverdue(task, today, nowMinute),
                today = today,
                expanded = expandedId == task.id,
                onToggleExpand = { expandedId = if (expandedId == task.id) "" else task.id },
                onStart = {
                    val problem = viewModel.prepareTimerForTask(task.id)
                    if (problem == null) nav.goTab(AppTab.Timer) else notice = problem
                },
                onEdit = { editing = task },
                onPlan = { planning = task },
                onDelete = { deleting = task },
            )
        }
    }

    if (adding) TaskDialog(data, viewModel, null, onDone = { adding = false })
    editing?.let { task -> TaskDialog(data, viewModel, task, onDone = { editing = null }) }
    planning?.let { task -> PlannedDialog(data, viewModel, null, onDone = { planning = null }, presetTaskId = task.id) }
    deleting?.let { task ->
        ConfirmDialog(
            title = "Delete this task?",
            text = "\"${task.title}\" and its checklist will be removed. Focus sessions already saved for it stay in your history.",
            confirmLabel = "Delete task",
            onConfirm = {
                viewModel.removeTask(task.id)
                deleting = null
            },
            onDismiss = { deleting = null },
            danger = true,
        )
    }
    notice?.let { text -> NoticeDialog("Not possible right now", text, onDone = { notice = null }) }
}

@Composable
private fun TaskCard(
    data: AppData,
    viewModel: StudyViewModel,
    task: StudyTask,
    overdue: Boolean,
    today: LocalDate,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onPlan: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = StudyTheme.colors
    val subjectColor = data.subjectColor(task.subjectId)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .background(colors.card)
            .border(1.dp, if (overdue) colors.destructive.copy(alpha = 0.6f) else colors.border, TileShape)
            .padding(start = 4.dp, end = 2.dp, top = 6.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            CheckCircle(task.done, onToggle = { viewModel.toggleTask(task.id) }, description = if (task.done) "Mark ${task.title} as not done" else "Mark ${task.title} as done")
            Column(
                Modifier.weight(1f).clip(TileShape).clickable(role = Role.Button, onClickLabel = if (expanded) "Hide details" else "Show details", onClick = onToggleExpand).padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (task.done) colors.mutedForeground else colors.foreground,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PriorityTag(task.priority)
                    Tag(task.category.label)
                    if (task.subjectId.isNotEmpty()) Tag(data.subjectName(task.subjectId), background = subjectColor.soft, content = colors.foreground)
                    if (task.dueDate.isNotEmpty()) {
                        val days = daysUntil(task.dueDate, today) ?: 0
                        val label = (if (overdue) "Overdue: " else "Due ") + (if (days in -6..6) relativeDay(days) else shortDate(task.dueDate)) +
                            if (task.dueTime.isNotEmpty()) " ${task.dueTime}" else ""
                        if (overdue) Tag(label, background = colors.destructive.copy(alpha = 0.16f), content = colors.destructive) else Tag(label)
                    }
                    if (task.estimateMinutes > 0) Tag("About ${formatMinutes(task.estimateMinutes)}")
                    if (task.subtasks.isNotEmpty()) Tag("${subtaskProgress(task)} steps")
                }
            }
            if (!task.done) {
                IconAction(R.drawable.ic_play, "Start a focus session for ${task.title}", onClick = onStart, tint = colors.foreground)
            }
            MoreMenu(
                "More actions for ${task.title}",
                listOf(
                    MenuItem("Edit") { onEdit() },
                    MenuItem("Plan a session for it") { onPlan() },
                    MenuItem("Delete", danger = true) { onDelete() },
                ),
            )
        }
        if (expanded) {
            Column(Modifier.padding(start = 12.dp, end = 10.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (task.notes.isNotEmpty()) {
                    Text(task.notes, style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
                }
                Text("Checklist", style = MaterialTheme.typography.labelLarge, color = colors.foreground)
                task.subtasks.forEach { step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CheckCircle(step.done, onToggle = { viewModel.toggleSubtask(task.id, step.id) }, description = "Toggle ${step.title}")
                        Text(
                            step.title,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (step.done) colors.mutedForeground else colors.foreground,
                            textDecoration = if (step.done) TextDecoration.LineThrough else null,
                        )
                        IconAction(R.drawable.ic_x, "Remove ${step.title}", onClick = { viewModel.removeSubtask(task.id, step.id) })
                    }
                }
                var step by remember(task.id) { mutableStateOf("") }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudyTextField(step, { step = it }, label = "Add a step", modifier = Modifier.weight(1f), placeholder = "e.g. Write the introduction")
                    StudyButton("Add", onClick = { if (viewModel.addSubtask(task.id, step) == null) step = "" }, kind = ButtonKind.Secondary, small = true, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
}

/** Adds a task, or edits [existing]. */
@Composable
fun TaskDialog(data: AppData, viewModel: StudyViewModel, existing: StudyTask?, onDone: () -> Unit, presetSubjectId: String = "") {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var subjectId by remember { mutableStateOf(existing?.subjectId ?: presetSubjectId) }
    var category by remember { mutableStateOf(existing?.category ?: TaskCategory.Homework) }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.Medium) }
    var dueDate by remember { mutableStateOf(existing?.dueDate.orEmpty()) }
    var hasTime by remember { mutableStateOf(existing?.dueTime?.isNotEmpty() == true) }
    var dueTime by remember { mutableStateOf(existing?.dueTime?.ifEmpty { "17:00" } ?: "17:00") }
    var estimate by remember { mutableStateOf(existing?.estimateMinutes?.takeIf { it > 0 }?.toString().orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = if (existing == null) "New task" else "Edit task",
        confirmLabel = if (existing == null) "Add task" else "Save",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.saveTask(existing?.id, title, subjectId, category, priority, dueDate, if (hasTime) dueTime else "", estimate, notes)
            if (error == null) onDone()
        },
        error = error,
    ) {
        StudyTextField(title, { title = it }, label = "Title", placeholder = "e.g. Finish the lab report")
        SelectField("Subject", listOf("" to "No subject") + data.subjects.map { it.id to it.name }, subjectId, { subjectId = it })
        SelectField("Category", TaskCategory.entries.map { it to it.label }, category, { category = it })
        Column {
            FieldLabel("Priority")
            // A wrapping row, so the four labels never get squeezed on a narrow phone.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Priority.entries.forEach { level ->
                    ChoicePill(level.label, selected = priority == level, onClick = { priority = level })
                }
            }
        }
        DateField("Due date (optional)", dueDate, { dueDate = it })
        if (dueDate.isNotEmpty()) {
            StudyButton("Remove the due date", onClick = {
                dueDate = ""
                hasTime = false
            }, kind = ButtonKind.Ghost, small = true)
            SwitchRow("Add a due time", hasTime, { hasTime = it })
            if (hasTime) TimeField("Due time", dueTime, { dueTime = it })
        }
        StudyTextField(estimate, { estimate = it.filter { c -> c.isDigit() }.take(3) }, label = "Estimated minutes (optional)", placeholder = "e.g. 90", keyboardType = KeyboardType.Number)
        StudyTextField(notes, { notes = it }, label = "Notes (optional)", singleLine = false)
    }
}
