package com.rikhotso.studytime.data

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Statistics. Every figure here is counted from saved focus sessions, tasks, planned sessions
 * and topics. Nothing is estimated or made up, so an empty app shows zeros.
 */

enum class StatPeriod(val label: String) { Today("Today"), Week("Week"), Month("Month"), All("All time") }

/** First and last day of [period], both included. All time starts at the first session. */
fun periodRange(period: StatPeriod, today: LocalDate, data: AppData, zone: ZoneId = ZoneId.systemDefault()): Pair<LocalDate, LocalDate> = when (period) {
    StatPeriod.Today -> today to today
    StatPeriod.Week -> mondayOf(today) to mondayOf(today).plusDays(6)
    StatPeriod.Month -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
    StatPeriod.All -> (data.sessions.minOfOrNull { dateOf(it.startedAt, zone) } ?: today) to today
}

data class PeriodSummary(
    val minutes: Int,
    val sessions: Int,
    val tasksDone: Int,
    val activeDays: Int,
    val goalMinutes: Int,
    val goalDaysMet: Int,
) {
    /** Progress toward the goal for the period, 0 to 100. */
    val goalPercent: Int get() = percentOf(minutes, goalMinutes)
}

fun periodSummary(data: AppData, period: StatPeriod, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): PeriodSummary {
    val (from, to) = periodRange(period, today, data, zone)
    val sessions = data.sessions.filter { dateOf(it.startedAt, zone).let { day -> !day.isBefore(from) && !day.isAfter(to) } }
    val byDay = minutesByDay(sessions, zone)
    val goal = when (period) {
        StatPeriod.Today -> data.profile.dailyGoalMinutes
        StatPeriod.Week -> data.profile.weeklyGoalMinutes
        StatPeriod.Month -> data.profile.monthlyGoalMinutes
        StatPeriod.All -> 0
    }
    return PeriodSummary(
        minutes = sessions.sumOf { it.durationMinutes },
        sessions = sessions.size,
        tasksDone = data.tasks.count { task ->
            task.done && task.completedAt > 0L && dateOf(task.completedAt, zone).let { day -> !day.isBefore(from) && !day.isAfter(to) }
        },
        activeDays = byDay.count { it.value > 0 },
        goalMinutes = goal,
        goalDaysMet = byDay.count { it.value >= data.profile.dailyGoalMinutes },
    )
}

data class SubjectShare(val subjectId: String, val name: String, val color: PastelColor, val minutes: Int, val percent: Int)

/** Minutes per subject inside the period, biggest first. */
fun subjectBreakdown(data: AppData, period: StatPeriod, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<SubjectShare> {
    val (from, to) = periodRange(period, today, data, zone)
    val sessions = data.sessions.filter { dateOf(it.startedAt, zone).let { day -> !day.isBefore(from) && !day.isAfter(to) } }
    val total = sessions.sumOf { it.durationMinutes }
    return sessions.groupBy { it.subjectId }.map { (id, group) ->
        // Use the subject's current name and colour when it still exists, otherwise what was saved with the session.
        val subject = data.subject(id)
        val minutes = group.sumOf { it.durationMinutes }
        SubjectShare(id, subject?.name ?: group.last().subjectName, subject?.color ?: group.last().color, minutes, percentOf(minutes, total))
    }.sortedByDescending { it.minutes }
}

/** How the plan compares with what happened, for sessions that should already be over. */
data class PlanComparison(
    val plannedSessions: Int,
    val completedSessions: Int,
    val plannedMinutes: Int,
    val studiedMinutes: Int,
)

fun plannedVersusActual(data: AppData, from: LocalDate, to: LocalDate, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): PlanComparison {
    var planned = 0
    var completed = 0
    var plannedMinutes = 0
    var day = from
    val last = if (to.isAfter(today)) today else to
    while (!day.isAfter(last)) {
        plannedOn(data, day).forEach { session ->
            planned++
            plannedMinutes += session.durationMinutes
            if (session.isCompletedOn(day)) completed++
        }
        day = day.plusDays(1)
    }
    val studied = data.sessions.filter { dateOf(it.startedAt, zone).let { d -> !d.isBefore(from) && !d.isAfter(last) } }.sumOf { it.durationMinutes }
    return PlanComparison(planned, completed, plannedMinutes, studied)
}

data class Record(val label: String, val value: String, val detail: String)

fun personalRecords(data: AppData, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Record> {
    if (data.sessions.isEmpty()) return emptyList()
    val byDay = minutesByDay(data.sessions, zone)
    val longest = data.sessions.maxBy { it.durationMinutes }
    val bestDay = byDay.maxBy { it.value }
    val sessionsPerDay = data.sessions.groupBy { dateOf(it.startedAt, zone) }.mapValues { it.value.size }
    val busiest = sessionsPerDay.maxBy { it.value }
    val bestWeek = byDay.entries.groupBy { mondayOf(it.key) }.mapValues { week -> week.value.sumOf { it.value } }.maxBy { it.value }
    val streak = streakInfo(byDay, today, data.profile.streakMinimumMinutes)
    return listOf(
        Record("Longest session", formatMinutes(longest.durationMinutes), "${longest.subjectName} on ${shortDate(dateOf(longest.startedAt, zone).toString())}"),
        Record("Most study in a day", formatMinutes(bestDay.value), shortDate(bestDay.key.toString())),
        Record("Most sessions in a day", busiest.value.toString(), shortDate(busiest.key.toString())),
        Record("Best week", formatMinutes(bestWeek.value), "Week of ${shortDate(bestWeek.key.toString())}"),
        Record("Longest streak", "${streak.best} day${if (streak.best == 1) "" else "s"}", "Current streak: ${streak.current}"),
    )
}

/**
 * Plain observations taken from the user's own records. Each one states the numbers it is based
 * on. With too little data it says so instead of guessing.
 */
fun insights(data: AppData, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<String> {
    if (data.sessions.size < 3) {
        return listOf("Finish a few focus sessions and studytime will point out patterns in your study here.")
    }
    val result = ArrayList<String>()
    val monday = mondayOf(today)
    val daysSoFar = today.dayOfWeek.value
    val weekSessions = data.sessions.filter { dateOf(it.startedAt, zone).let { d -> !d.isBefore(monday) && !d.isAfter(today) } }

    // Subjects that are behind their own daily goal this week.
    data.subjects.forEach { subject ->
        val expected = subject.goalMinutesPerDay * daysSoFar
        val actual = weekSessions.filter { it.subjectId == subject.id }.sumOf { it.durationMinutes }
        if (daysSoFar >= 2 && expected > 0 && actual * 2 < expected) {
            result.add("${subject.name} has had ${formatMinutes(actual)} this week against the ${formatMinutes(expected)} its daily goal adds up to so far.")
        }
    }

    // When the user tends to study.
    val blocks = listOf("in the morning" to 5..11, "in the afternoon" to 12..16, "in the evening" to 17..21)
    val minutesByBlock = blocks.associate { (name, hours) ->
        name to data.sessions.filter { java.time.Instant.ofEpochMilli(it.startedAt).atZone(zone).hour in hours }.sumOf { it.durationMinutes }
    }
    val total = data.sessions.sumOf { it.durationMinutes }
    val top = minutesByBlock.maxBy { it.value }
    if (total > 0 && top.value * 2 >= total) {
        result.add("${percentOf(top.value, total)}% of your focus time happens ${top.key}. Planning harder subjects for then may suit you.")
    }

    // Daily goal over the last seven days.
    val byDay = minutesByDay(data.sessions, zone)
    val goalDays = (0..6).count { (byDay[today.minusDays(it.toLong())] ?: 0) >= data.profile.dailyGoalMinutes }
    result.add("You reached your daily goal on $goalDays of the last 7 days.")

    // Planned sessions kept this week.
    val plan = plannedVersusActual(data, monday, today, today, zone)
    if (plan.plannedSessions >= 3 && plan.completedSessions * 2 < plan.plannedSessions) {
        result.add("You completed ${plan.completedSessions} of ${plan.plannedSessions} planned sessions this week. Shorter sessions may be easier to keep.")
    }

    val overdue = data.tasks.count { isOverdue(it, today, 24 * 60) }
    if (overdue > 0) result.add("$overdue task${if (overdue == 1) " is" else "s are"} past the deadline. The planner can fit them into free time.")

    val average = data.sessions.sumOf { it.durationMinutes } / data.sessions.size
    result.add("Your average focus session lasts ${formatMinutes(average)}.")
    return result
}

/** Past sessions, newest first, matching the search text and subject. */
fun sessionHistory(data: AppData, query: String = "", subjectId: String? = null): List<StudySession> {
    val text = query.trim().lowercase()
    return data.sessions
        .filter { subjectId == null || it.subjectId == subjectId }
        .filter { session ->
            text.isEmpty() || listOf(session.subjectName, session.taskTitle, session.topicName, session.note).any { it.lowercase().contains(text) }
        }
        .sortedByDescending { it.startedAt }
}

// ------------------------------------------------------------------ exam readiness

/**
 * Where revision stands for one exam. It reports what has been covered and what is left. It is
 * not a prediction of the mark.
 */
data class ExamReadiness(
    val daysLeft: Long,
    val topicsTotal: Int,
    val topicsCompleted: Int,
    val topicsInProgress: Int,
    val averageConfidence: Double?,
    val needAttention: List<Topic>,
    val openTasks: Int,
    val plannedSessionsLeft: Int,
) {
    val coveragePercent: Int get() = percentOf(topicsCompleted, topicsTotal)
}

fun examReadiness(data: AppData, exam: Exam, today: LocalDate): ExamReadiness {
    val topics = data.topics.filter { exam.subjectId.isNotEmpty() && it.subjectId == exam.subjectId }
    val rated = topics.filter { it.confidence > 0 }
    val examDay = parseDate(exam.date)
    var sessionsLeft = 0
    if (examDay != null) {
        var day = today
        while (day.isBefore(examDay) && sessionsLeft < 500) {
            sessionsLeft += plannedOn(data, day).count { it.subjectId == exam.subjectId && exam.subjectId.isNotEmpty() && !it.isCompletedOn(day) }
            day = day.plusDays(1)
        }
    }
    return ExamReadiness(
        daysLeft = daysUntil(exam.date, today) ?: 0L,
        topicsTotal = topics.size,
        topicsCompleted = topics.count { it.status == TopicStatus.Completed },
        topicsInProgress = topics.count { it.status == TopicStatus.InProgress },
        averageConfidence = if (rated.isEmpty()) null else ((rated.sumOf { it.confidence } * 10.0 / rated.size).roundToInt() / 10.0),
        needAttention = topics.filter { it.status != TopicStatus.Completed && (it.confidence in 1..2 || it.status == TopicStatus.NotStarted) },
        openTasks = data.tasks.count { !it.done && it.subjectId == exam.subjectId && exam.subjectId.isNotEmpty() },
        plannedSessionsLeft = sessionsLeft,
    )
}

/** Practice test results for a subject, oldest first, as percentages. */
fun practiceTrend(tests: List<PracticeTest>): List<Pair<PracticeTest, Int>> =
    tests.filter { it.maxScore > 0 }.sortedBy { it.date }.map { it to (it.score / it.maxScore * 100).roundToInt() }
