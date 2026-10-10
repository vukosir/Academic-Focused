package com.rikhotso.studytime.data

import java.time.LocalDate

/** Filtering, sorting and searching for tasks, plus the search that covers the whole app. */

enum class TaskStatusFilter(val label: String) { Open("To do"), Overdue("Overdue"), Done("Done"), All("All") }

enum class TaskDueFilter(val label: String) { Any("Any date"), Today("Due today"), Week("Next 7 days"), NoDate("No date") }

data class TaskFilter(
    val query: String = "",
    val status: TaskStatusFilter = TaskStatusFilter.Open,
    /** Null means every subject. Blank means tasks without a subject. */
    val subjectId: String? = null,
    val priority: Priority? = null,
    val due: TaskDueFilter = TaskDueFilter.Any,
) {
    val isDefault: Boolean get() = this == TaskFilter()
}

/** True when an unfinished task is past its date (and time, when it has one). */
fun isOverdue(task: StudyTask, today: LocalDate, nowMinute: Int): Boolean {
    if (task.done || task.dueDate.isEmpty()) return false
    val due = parseDate(task.dueDate) ?: return false
    return when {
        due.isBefore(today) -> true
        due == today && task.dueTime.isNotEmpty() -> minutesOfDay(task.dueTime) < nowMinute
        else -> false
    }
}

/** Tasks that match [filter]: unfinished first, then by deadline, then the most urgent first. */
fun filterTasks(data: AppData, filter: TaskFilter, today: LocalDate, nowMinute: Int): List<StudyTask> {
    val text = filter.query.trim().lowercase()
    return data.tasks
        .filter { task ->
            when (filter.status) {
                TaskStatusFilter.Open -> !task.done
                TaskStatusFilter.Overdue -> isOverdue(task, today, nowMinute)
                TaskStatusFilter.Done -> task.done
                TaskStatusFilter.All -> true
            }
        }
        .filter { filter.subjectId == null || it.subjectId == filter.subjectId }
        .filter { filter.priority == null || it.priority == filter.priority }
        .filter { task ->
            val days = daysUntil(task.dueDate, today)
            when (filter.due) {
                TaskDueFilter.Any -> true
                TaskDueFilter.Today -> days == 0L
                TaskDueFilter.Week -> days != null && days in 0..7
                TaskDueFilter.NoDate -> task.dueDate.isEmpty()
            }
        }
        .filter { task ->
            text.isEmpty() || task.title.lowercase().contains(text) || task.notes.lowercase().contains(text) ||
                data.subjectName(task.subjectId).lowercase().contains(text) || task.subtasks.any { it.title.lowercase().contains(text) }
        }
        .sortedWith(
            compareBy<StudyTask> { it.done }
                .thenBy { it.dueDate.ifEmpty { "9999-12-31" } }
                .thenBy { it.dueTime.ifEmpty { "99:99" } }
                .thenByDescending { it.priority.rank }
                .thenBy { it.createdAt },
        )
}

/** Unfinished tasks due within [days] days or already overdue, most pressing first. */
fun tasksDueSoon(data: AppData, today: LocalDate, days: Int = 3): List<StudyTask> =
    data.tasks
        .filter { !it.done && (daysUntil(it.dueDate, today)?.let { left -> left <= days } ?: false) }
        .sortedWith(compareBy<StudyTask> { it.dueDate }.thenByDescending { it.priority.rank })

fun subtaskProgress(task: StudyTask): String = "${task.subtasks.count { it.done }}/${task.subtasks.size}"

// ------------------------------------------------------------------ search across the app

enum class SearchKind(val label: String) {
    Subject("Subjects"), Task("Tasks"), Note("Notes"), Topic("Topics"), Exam("Exams"), Planned("Planned sessions"), Session("Study history")
}

/** One hit. [targetId] is the id of the item, and [subjectId] is the subject it belongs to, if any. */
data class SearchResult(
    val kind: SearchKind,
    val targetId: String,
    val title: String,
    val subtitle: String,
    val subjectId: String = "",
)

/** Looks for [query] in subjects, tasks, notes, topics, exams, planned sessions and past sessions. */
fun searchAll(data: AppData, query: String, limitPerKind: Int = 8): List<SearchResult> {
    val text = query.trim().lowercase()
    if (text.length < 2) return emptyList()
    fun String.hit() = lowercase().contains(text)
    val results = ArrayList<SearchResult>()
    data.subjects.filter { it.name.hit() || it.teacher.hit() }.take(limitPerKind).forEach {
        results.add(SearchResult(SearchKind.Subject, it.id, it.name, it.teacher.ifEmpty { "${formatMinutes(it.goalMinutesPerDay)} a day goal" }, it.id))
    }
    data.tasks.filter { task -> task.title.hit() || task.notes.hit() || task.subtasks.any { it.title.hit() } }.take(limitPerKind).forEach {
        val due = if (it.dueDate.isEmpty()) "No deadline" else "Due ${shortDate(it.dueDate)}"
        results.add(SearchResult(SearchKind.Task, it.id, it.title, "${data.subjectName(it.subjectId)} · $due" + if (it.done) " · done" else "", it.subjectId))
    }
    data.notes.filter { it.title.hit() || it.body.hit() }.take(limitPerKind).forEach {
        results.add(SearchResult(SearchKind.Note, it.id, it.title, data.subjectName(it.subjectId) + " · " + it.body.replace('\n', ' ').take(60), it.subjectId))
    }
    data.topics.filter { it.name.hit() }.take(limitPerKind).forEach {
        results.add(SearchResult(SearchKind.Topic, it.id, it.name, "${data.subjectName(it.subjectId)} · ${it.status.label}", it.subjectId))
    }
    data.exams.filter { it.title.hit() || it.location.hit() || data.subjectName(it.subjectId).hit() }.take(limitPerKind).forEach {
        results.add(SearchResult(SearchKind.Exam, it.id, it.title, "${shortDate(it.date)} at ${it.time}" + if (it.location.isNotEmpty()) " · ${it.location}" else "", it.subjectId))
    }
    data.plannedSessions.filter { it.title.hit() }.take(limitPerKind).forEach {
        results.add(SearchResult(SearchKind.Planned, it.id, it.title, "${shortDate(it.date)} at ${it.startTime} · ${formatMinutes(it.durationMinutes)}", it.subjectId))
    }
    sessionHistory(data, text).take(limitPerKind).forEach {
        val what = listOf(it.taskTitle, it.topicName, it.note).firstOrNull { part -> part.isNotEmpty() }
        results.add(
            SearchResult(SearchKind.Session, it.id, "${it.subjectName} · ${formatMinutes(it.durationMinutes)}", shortDate(dateOf(it.startedAt).toString()) + if (what != null) " · $what" else "", it.subjectId),
        )
    }
    return results
}
