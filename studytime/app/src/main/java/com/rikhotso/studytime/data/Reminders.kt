package com.rikhotso.studytime.data

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Works out which reminders should be booked with the system: planned sessions, task deadlines,
 * exams and revision that is due. It is a pure function of the saved data, so the list can be
 * rebuilt at any time (after a change, or when the phone restarts) and always comes out the same.
 */

enum class ReminderKind { Session, Deadline, Exam, Revision }

data class Reminder(
    val key: String,
    val atMillis: Long,
    val title: String,
    val text: String,
    val kind: ReminderKind,
)

const val MAX_REMINDERS = 48
const val REMINDER_DAYS_AHEAD = 14
const val SESSION_REMINDER_LEAD_MINUTES = 10

/** True when [minuteOfDay] falls inside quiet hours. Handles a range that crosses midnight. */
fun inQuietHours(settings: Settings, minuteOfDay: Int): Boolean {
    if (!settings.quietHoursEnabled) return false
    val start = minutesOfDay(settings.quietStart)
    val end = minutesOfDay(settings.quietEnd)
    return when {
        start == end -> false
        start < end -> minuteOfDay in start until end
        else -> minuteOfDay >= start || minuteOfDay < end
    }
}

/** A time inside quiet hours is moved to the moment quiet hours end. Other times are returned unchanged. */
fun afterQuietHours(settings: Settings, time: LocalDateTime): LocalDateTime {
    val minute = time.hour * 60 + time.minute
    if (!inQuietHours(settings, minute)) return time
    val end = minutesOfDay(settings.quietEnd)
    val endTime = LocalTime.of((end / 60) % 24, end % 60)
    // Quiet hours that started the evening before end today, otherwise they end tomorrow morning.
    return if (minute < end) time.toLocalDate().atTime(endTime) else time.toLocalDate().plusDays(1).atTime(endTime)
}

fun planReminders(data: AppData, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Reminder> {
    if (!data.onboarded) return emptyList()
    val settings = data.settings
    val today = dateOf(now, zone)
    val result = ArrayList<Reminder>()

    fun add(key: String, time: LocalDateTime, title: String, text: String, kind: ReminderKind, latest: LocalDateTime? = null) {
        val shifted = afterQuietHours(settings, time)
        // A reminder that quiet hours would push past the thing it is about is dropped instead.
        if (latest != null && shifted.isAfter(latest)) return
        val at = shifted.atZone(zone).toInstant().toEpochMilli()
        if (at > now) result.add(Reminder(key, at, title, text, kind))
    }

    for (offset in 0..REMINDER_DAYS_AHEAD) {
        val day = today.plusDays(offset.toLong())

        if (settings.notifySessions) {
            plannedOn(data, day).filter { !it.isCompletedOn(day) }.forEach { session ->
                val start = day.atTime(LocalTime.MIDNIGHT).plusMinutes(minutesOfDay(session.startTime).toLong())
                add(
                    "session:${session.id}:$day",
                    start.minusMinutes(SESSION_REMINDER_LEAD_MINUTES.toLong()),
                    "Study session at ${session.startTime}",
                    "${session.title} · ${formatMinutes(session.durationMinutes)}",
                    ReminderKind.Session,
                    latest = start,
                )
            }
        }

        if (settings.notifyRevision) {
            // Topics that fall due on this day. Anything already overdue is gathered into today's reminder.
            val due = data.topics.filter { topic ->
                topic.nextReview.isNotEmpty() && (topic.nextReview == day.toString() || (offset == 0 && topic.nextReview < day.toString()))
            }
            if (due.isNotEmpty()) {
                val names = due.take(3).joinToString(", ") { it.name } + if (due.size > 3) " and ${due.size - 3} more" else ""
                add("revision:$day", day.atTime(16, 0), "${due.size} topic${if (due.size == 1) "" else "s"} due for revision", names, ReminderKind.Revision)
            }
        }
    }
    if (settings.notifyDeadlines) {
        data.tasks.filter { !it.done }.forEach { task ->
            val day = parseDate(task.dueDate) ?: return@forEach
            if (day.isBefore(today) || day.isAfter(today.plusDays(REMINDER_DAYS_AHEAD + 1L))) return@forEach
            val subject = data.subjectName(task.subjectId)
            add("task-eve:${task.id}", day.minusDays(1).atTime(17, 0), "Due tomorrow: ${task.title}", subject, ReminderKind.Deadline)
            if (task.dueTime.isNotEmpty()) {
                val due = day.atTime(LocalTime.MIDNIGHT).plusMinutes(minutesOfDay(task.dueTime).toLong())
                add("task-day:${task.id}", due.minusHours(1), "Due at ${task.dueTime}: ${task.title}", subject, ReminderKind.Deadline, latest = due)
            } else {
                add("task-day:${task.id}", day.atTime(8, 0), "Due today: ${task.title}", subject, ReminderKind.Deadline)
            }
        }
    }

    if (settings.notifyExams) {
        data.exams.forEach { exam ->
            val day = parseDate(exam.date) ?: return@forEach
            if (day.isBefore(today) || day.isAfter(today.plusDays(REMINDER_DAYS_AHEAD + 7L))) return@forEach
            val where = if (exam.location.isNotEmpty()) " · ${exam.location}" else ""
            add("exam-week:${exam.id}", day.minusDays(7).atTime(9, 0), "${exam.title} is in one week", "${shortDate(exam.date)} at ${exam.time}$where", ReminderKind.Exam)
            add("exam-eve:${exam.id}", day.minusDays(1).atTime(18, 0), "${exam.title} is tomorrow", "Starts at ${exam.time}$where", ReminderKind.Exam)
        }
    }


    return result.sortedBy { it.atMillis }.distinctBy { it.key }.take(MAX_REMINDERS)
}

// ------------------------------------------------------------------ export

private fun csvCell(text: String): String {
    val clean = text.replace("\r", " ").replace("\n", " ")
    return if (clean.any { it == ',' || it == '"' }) "\"" + clean.replace("\"", "\"\"") + "\"" else clean
}

/** Every focus session as a spreadsheet-friendly CSV file, oldest first. */
fun sessionsCsv(data: AppData, zone: ZoneId = ZoneId.systemDefault()): String {
    val lines = ArrayList<String>()
    lines.add("date,start,subject,minutes,task,topic,note")
    data.sessions.sortedBy { it.startedAt }.forEach { session ->
        val started = java.time.Instant.ofEpochMilli(session.startedAt).atZone(zone)
        lines.add(
            listOf(
                started.toLocalDate().toString(),
                formatTime(started.hour, started.minute),
                session.subjectName,
                session.durationMinutes.toString(),
                session.taskTitle,
                session.topicName,
                session.note,
            ).joinToString(",") { csvCell(it) },
        )
    }
    return lines.joinToString("\n") + "\n"
}

/** Every task as a CSV file. */
fun tasksCsv(data: AppData): String {
    val lines = ArrayList<String>()
    lines.add("title,subject,category,priority,due date,due time,estimate minutes,done,steps done,steps total,notes")
    data.tasks.forEach { task ->
        lines.add(
            listOf(
                task.title,
                data.subjectName(task.subjectId),
                task.category.label,
                task.priority.label,
                task.dueDate,
                task.dueTime,
                task.estimateMinutes.toString(),
                if (task.done) "yes" else "no",
                task.subtasks.count { it.done }.toString(),
                task.subtasks.size.toString(),
                task.notes,
            ).joinToString(",") { csvCell(it) },
        )
    }
    return lines.joinToString("\n") + "\n"
}
