package com.rikhotso.studytime.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Planning helpers: which sessions fall on a day, clash detection, free time, missed sessions,
 * spaced revision and the automatic timetable generator. All pure functions, tested in
 * app/src/test.
 */

/** A stretch of a day in minutes since midnight, for example 540 to 600 for 09:00 to 10:00. */
data class TimeBlock(val start: Int, val end: Int, val label: String = "")

private fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int) = aStart < bEnd && bStart < aEnd

/** How long an exam is assumed to occupy when checking for clashes. */
private const val EXAM_BLOCK_MINUTES = 120

fun PlannedSession.occursOn(day: LocalDate): Boolean {
    val first = parseDate(date) ?: return false
    if (day.isBefore(first) || day.toString() in skippedDates) return false
    return when (repeat) {
        Repeat.None -> day == first
        Repeat.Daily -> true
        Repeat.Weekly -> day.dayOfWeek == first.dayOfWeek
        Repeat.Days -> day == first || day.dayOfWeek.value in repeatDays
    }
}

fun PlannedSession.isCompletedOn(day: LocalDate): Boolean = day.toString() in completedDates

fun plannedOn(data: AppData, day: LocalDate): List<PlannedSession> =
    data.plannedSessions.filter { it.occursOn(day) }.sortedBy { minutesOfDay(it.startTime) }

fun classesOn(data: AppData, day: LocalDate): List<TimetableEntry> =
    data.entries.filter { it.dayOfWeek == day.dayOfWeek.value }.sortedBy { minutesOfDay(it.startTime) }

fun examsOn(data: AppData, day: LocalDate): List<Exam> =
    data.exams.filter { it.date == day.toString() }.sortedBy { minutesOfDay(it.time) }

/** Everything already booked on [day]: classes, planned sessions and exams. */
fun busyBlocks(data: AppData, day: LocalDate, ignorePlannedId: String = ""): List<TimeBlock> {
    val blocks = ArrayList<TimeBlock>()
    classesOn(data, day).forEach { blocks.add(TimeBlock(minutesOfDay(it.startTime), minutesOfDay(it.endTime), "${it.subjectName} class")) }
    plannedOn(data, day).filter { it.id != ignorePlannedId }.forEach {
        val start = minutesOfDay(it.startTime)
        blocks.add(TimeBlock(start, start + it.durationMinutes, it.title))
    }
    examsOn(data, day).forEach {
        val start = minutesOfDay(it.time)
        blocks.add(TimeBlock(start, start + EXAM_BLOCK_MINUTES, "${it.title} exam"))
    }
    return blocks.sortedBy { it.start }
}

/**
 * Checks a planned session against everything already booked. Returns a sentence describing the
 * first clash, or null when the time is free. Repeating sessions are checked for two weeks ahead.
 */
fun findSessionConflict(data: AppData, candidate: PlannedSession): String? {
    val first = parseDate(candidate.date) ?: return null
    val start = minutesOfDay(candidate.startTime)
    val end = start + candidate.durationMinutes
    val daysToCheck = if (candidate.repeat == Repeat.None) 0 else 13
    for (offset in 0..daysToCheck) {
        val day = first.plusDays(offset.toLong())
        if (!candidate.occursOn(day)) continue
        val clash = busyBlocks(data, day, ignorePlannedId = candidate.id).firstOrNull { overlaps(start, end, it.start, it.end) }
        if (clash != null) {
            val dayName = DAY_NAMES[day.dayOfWeek.value - 1]
            return "This overlaps with ${clash.label} on $dayName from ${formatTime(clash.start / 60, clash.start % 60)} to ${formatTime((clash.end / 60) % 24, clash.end % 60)}."
        }
    }
    return null
}

/** Checks a weekly class against the other classes on the same weekday. */
fun findClassConflict(data: AppData, dayOfWeek: Int, startTime: String, endTime: String, ignoreEntryId: String = ""): String? {
    val start = minutesOfDay(startTime)
    val end = minutesOfDay(endTime)
    val clash = data.entries.firstOrNull {
        it.id != ignoreEntryId && it.dayOfWeek == dayOfWeek && overlaps(start, end, minutesOfDay(it.startTime), minutesOfDay(it.endTime))
    } ?: return null
    return "This overlaps with ${clash.subjectName} from ${clash.startTime} to ${clash.endTime}."
}

/** The gaps inside the user's availability on [day] that are at least [minMinutes] long. */
fun freeSlots(data: AppData, day: LocalDate, minMinutes: Int, notBefore: Int = 0): List<TimeBlock> {
    val window = data.profile.availability.firstOrNull { it.dayOfWeek == day.dayOfWeek.value && it.enabled } ?: return emptyList()
    val windowEnd = minutesOfDay(window.end)
    var cursor = max(minutesOfDay(window.start), notBefore)
    val result = ArrayList<TimeBlock>()
    for (block in busyBlocks(data, day)) {
        if (block.end <= cursor) continue
        if (block.start >= windowEnd) break
        if (block.start - cursor >= minMinutes) result.add(TimeBlock(cursor, block.start))
        cursor = max(cursor, block.end)
    }
    if (windowEnd - cursor >= minMinutes) result.add(TimeBlock(cursor, windowEnd))
    return result
}

/** The first free stretch of [durationMinutes] on or after [from], looking up to two weeks ahead. */
fun nextFreeSlot(data: AppData, from: LocalDate, fromMinute: Int, durationMinutes: Int, horizonDays: Int = 14): Pair<LocalDate, Int>? {
    for (offset in 0 until horizonDays) {
        val day = from.plusDays(offset.toLong())
        val slot = freeSlots(data, day, durationMinutes, notBefore = if (offset == 0) fromMinute else 0).firstOrNull()
        if (slot != null) return day to slot.start
    }
    return null
}

data class MissedSession(val session: PlannedSession, val day: LocalDate)

/** Planned sessions from the last week (and earlier today) that were not ticked off. */
fun missedSessions(data: AppData, today: LocalDate, nowMinute: Int, lookbackDays: Int = 7): List<MissedSession> {
    val result = ArrayList<MissedSession>()
    for (offset in lookbackDays downTo 0) {
        val day = today.minusDays(offset.toLong())
        plannedOn(data, day).forEach { session ->
            val ended = offset > 0 || minutesOfDay(session.startTime) + session.durationMinutes <= nowMinute
            if (ended && !session.isCompletedOn(day)) result.add(MissedSession(session, day))
        }
    }
    return result
}

// ------------------------------------------------------------------ spaced revision

/** Days to wait before the next review: tomorrow, then 3 days, a week, two weeks, a month. */
val REVIEW_INTERVALS = listOf(1, 3, 7, 14, 30)

/** Marks a topic as reviewed today and schedules its next review further out each time. */
fun reviewed(topic: Topic, today: LocalDate): Topic {
    val interval = REVIEW_INTERVALS[min(topic.reviewCount, REVIEW_INTERVALS.size - 1)]
    return topic.copy(
        reviewCount = topic.reviewCount + 1,
        lastReviewed = today.toString(),
        nextReview = today.plusDays(interval.toLong()).toString(),
        status = if (topic.status == TopicStatus.NotStarted) TopicStatus.InProgress else topic.status,
    )
}

fun topicsDueForReview(data: AppData, day: LocalDate): List<Topic> =
    data.topics.filter { it.nextReview.isNotEmpty() && it.nextReview <= day.toString() }

fun daysUntil(isoDate: String, today: LocalDate): Long? = parseDate(isoDate)?.let { ChronoUnit.DAYS.between(today, it) }

// ------------------------------------------------------------------ timetable generator

/**
 * Builds a study plan for [days] days starting at [start].
 *
 * It only uses time inside the user's availability, never overlaps a class, exam or session the
 * user planned by hand, and stops each day at the daily goal (the last session of a day is
 * shortened when a full one would go past the goal). Time is shared between subjects in
 * proportion to their daily goals, and a subject gets extra weight when it has an exam within two
 * weeks or a task that is due soon. Each session is linked to the most pressing task or revision
 * topic of its subject when there is one.
 */
fun generatePlan(data: AppData, start: LocalDate, days: Int, sessionMinutes: Int, nowMinute: Int = 0): List<PlannedSession> {
    if (data.subjects.isEmpty()) return emptyList()
    val length = sessionMinutes.coerceIn(15, 120)
    val shortest = min(length, 20)
    val gap = 10
    val manual = data.copy(plannedSessions = data.plannedSessions.filter { !it.generated })
    val allocated = HashMap<String, Int>()
    val taskMinutesLeft = HashMap<String, Int>()
    data.tasks.filter { !it.done }.forEach { taskMinutesLeft[it.id] = if (it.estimateMinutes > 0) it.estimateMinutes else length }
    val usedTopics = HashSet<String>()
    val plan = ArrayList<PlannedSession>()

    for (offset in 0 until days) {
        val day = start.plusDays(offset.toLong())
        var budget = data.profile.dailyGoalMinutes
        for (slot in freeSlots(manual, day, shortest, notBefore = if (offset == 0) nowMinute else 0)) {
            var cursor = slot.start
            while (budget >= shortest) {
                // The last session of a day or of a gap may be shorter, so the plan can reach the goal exactly.
                val size = minOf(length, budget, slot.end - cursor) / 5 * 5
                if (size < shortest) break
                val subject = data.subjects.maxByOrNull { subjectWeight(data, it, day) / (allocated.getOrDefault(it.id, 0) + size).toDouble() } ?: break
                val task = data.tasks
                    .filter { !it.done && it.subjectId == subject.id && (taskMinutesLeft[it.id] ?: 0) > 0 }
                    .filter { it.dueDate.isEmpty() || it.dueDate >= day.toString() }
                    .sortedWith(compareBy<StudyTask> { it.dueDate.ifEmpty { "9999" } }.thenByDescending { it.priority.rank })
                    .firstOrNull()
                val topic = if (task != null) null else data.topics
                    .filter { it.subjectId == subject.id && it.id !in usedTopics }
                    .filter { it.status != TopicStatus.Completed || (it.nextReview.isNotEmpty() && it.nextReview <= day.toString()) }
                    .minByOrNull { confidenceRank(it) }
                if (task != null) taskMinutesLeft[task.id] = (taskMinutesLeft[task.id] ?: 0) - size
                if (topic != null) usedTopics.add(topic.id)
                plan.add(
                    PlannedSession(
                        id = newId(),
                        title = when {
                            task != null -> task.title
                            topic != null -> "Revise ${topic.name}"
                            else -> "Study ${subject.name}"
                        },
                        subjectId = subject.id,
                        date = day.toString(),
                        startTime = formatTime(cursor / 60, cursor % 60),
                        durationMinutes = size,
                        taskId = task?.id.orEmpty(),
                        topicId = topic?.id.orEmpty(),
                        generated = true,
                    ),
                )
                allocated[subject.id] = allocated.getOrDefault(subject.id, 0) + size
                budget -= size
                cursor += size + gap
            }
        }
    }
    return plan
}

/** How much attention a subject deserves on [day]. Bigger means more sessions. */
private fun subjectWeight(data: AppData, subject: Subject, day: LocalDate): Double {
    var weight = max(15, subject.goalMinutesPerDay).toDouble()
    // An exam within two weeks: up to three times the weight as it gets closer.
    val nextExam = data.exams.filter { it.subjectId == subject.id }.mapNotNull { daysUntil(it.date, day) }.filter { it >= 0 }.minOrNull()
    if (nextExam != null && nextExam <= 14) weight *= 1.0 + (14 - nextExam) / 7.0
    // An open task due within three days, or marked urgent.
    val pressing = data.tasks.any { task ->
        !task.done && task.subjectId == subject.id &&
            (task.priority == Priority.Urgent || (daysUntil(task.dueDate, day)?.let { it in 0..3 } ?: false))
    }
    if (pressing) weight *= 1.5
    // Topics the user is unsure about.
    val shaky = data.topics.count { it.subjectId == subject.id && it.status != TopicStatus.Completed && it.confidence in 1..2 }
    weight *= 1.0 + min(shaky, 5) * 0.1
    return weight
}

/** Topics the user rated low come first. An unrated topic sits in the middle. */
private fun confidenceRank(topic: Topic): Int = if (topic.confidence == 0) 3 else topic.confidence

/** What [generateRevisionPlan] managed to fit in. */
data class RevisionPlan(val sessions: List<PlannedSession>, val topicsCovered: Int, val topicsTotal: Int)

/**
 * Builds a revision plan for one exam: one session per unfinished topic of the exam's subject,
 * weakest topics first, in free time between [start] and the day before the exam. At most
 * [perDay] revision sessions are placed on one day. Topics that do not fit are left out and
 * reported through [RevisionPlan.topicsCovered].
 */
fun generateRevisionPlan(
    data: AppData,
    exam: Exam,
    start: LocalDate,
    sessionMinutes: Int,
    nowMinute: Int = 0,
    perDay: Int = 2,
): RevisionPlan {
    val examDay = parseDate(exam.date) ?: return RevisionPlan(emptyList(), 0, 0)
    val length = sessionMinutes.coerceIn(15, 120)
    val topics = data.topics
        .filter { it.subjectId == exam.subjectId && it.status != TopicStatus.Completed }
        .sortedBy { confidenceRank(it) }
    if (exam.subjectId.isEmpty() || topics.isEmpty()) return RevisionPlan(emptyList(), 0, topics.size)

    val sessions = ArrayList<PlannedSession>()
    var working = data
    var index = 0
    var day = start
    while (day.isBefore(examDay) && index < topics.size) {
        var placedToday = 0
        while (placedToday < perDay && index < topics.size) {
            val slot = freeSlots(working, day, length, notBefore = if (day == start) nowMinute else 0).firstOrNull() ?: break
            val topic = topics[index]
            val session = PlannedSession(
                id = newId(),
                title = "Revise ${topic.name}",
                subjectId = exam.subjectId,
                date = day.toString(),
                startTime = formatTime(slot.start / 60, slot.start % 60),
                durationMinutes = length,
                topicId = topic.id,
                generated = true,
            )
            sessions.add(session)
            // Book it into a working copy so the next search sees this slot as taken (plus a short gap).
            working = working.copy(plannedSessions = working.plannedSessions + session.copy(durationMinutes = length + 10))
            index++
            placedToday++
        }
        day = day.plusDays(1)
    }
    return RevisionPlan(sessions, index, topics.size)
}
