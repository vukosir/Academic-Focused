package com.rikhotso.studytime.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure helpers shared by every screen: formatting, streaks, weekly totals and challenge
 * progress. Nothing here touches Android, so it is covered by plain unit tests in
 * app/src/test.
 */

val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
val DAY_NAMES_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val MONTH_NAMES_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

/** 25 becomes "25m", 120 becomes "2h" and 90 becomes "1h 30m". */
fun formatMinutes(total: Int): String {
    val minutes = max(0, total)
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

/** Seconds as "MM:SS" for the timer face, or "H:MM:SS" once it passes an hour. */
fun formatClock(totalSeconds: Long): String {
    val seconds = max(0L, totalSeconds)
    return if (seconds >= 3600) {
        "%d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    } else {
        "%02d:%02d".format(seconds / 60, seconds % 60)
    }
}

/** Shows 3.0 as "3" and 2.5 as "2.5". */
fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else ((value * 10).roundToInt() / 10.0).toString()

fun greeting(hour: Int): String = when {
    hour < 12 -> "Good morning"
    hour < 17 -> "Good afternoon"
    hour < 21 -> "Good evening"
    else -> "Good night"
}

fun firstName(fullName: String): String =
    fullName.trim().split(Regex("\\s+")).firstOrNull().orEmpty().ifBlank { "there" }

fun initials(name: String): String = name.trim().take(2).uppercase().ifBlank { "?" }

/** "09:30" becomes 570. Anything unreadable becomes 0. */
fun minutesOfDay(time: String): Int {
    val parts = time.split(":")
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hours * 60 + minutes
}

fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

/** 570 becomes "09:30". Values past midnight wrap round. */
fun formatMinuteOfDay(minuteOfDay: Int): String = formatTime((minuteOfDay / 60) % 24, minuteOfDay % 60)

/** "2026-10-13" becomes "Tue 13 Oct". */
fun shortDate(iso: String): String {
    val date = parseDate(iso) ?: return iso
    return "${DAY_NAMES_SHORT[date.dayOfWeek.value - 1]} ${date.dayOfMonth} ${MONTH_NAMES_SHORT[date.monthValue - 1]}"
}

/** "today", "tomorrow", "in 12 days" or "3 days ago". */
fun relativeDay(days: Long): String = when {
    days == 0L -> "today"
    days == 1L -> "tomorrow"
    days == -1L -> "yesterday"
    days > 1L -> "in $days days"
    else -> "${-days} days ago"
}

/** "2026-10-13" becomes a date, or null when the text is not an ISO date. */
fun parseDate(iso: String): LocalDate? = try {
    LocalDate.parse(iso)
} catch (e: Exception) {
    null
}

fun dateOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

/** Focus minutes for each calendar day, counted on the day a session started. */
fun minutesByDay(sessions: List<StudySession>, zone: ZoneId = ZoneId.systemDefault()): Map<LocalDate, Int> {
    val totals = HashMap<LocalDate, Int>()
    for (session in sessions) {
        val day = dateOf(session.startedAt, zone)
        totals[day] = (totals[day] ?: 0) + session.durationMinutes
    }
    return totals
}

/**
 * Consecutive days with at least one session, ending today. If nothing is logged today yet the
 * streak still counts from yesterday, so it only breaks after a full missed day.
 */
fun currentStreak(byDay: Map<LocalDate, Int>, today: LocalDate): Int {
    var cursor = today
    if ((byDay[cursor] ?: 0) <= 0) {
        cursor = cursor.minusDays(1)
        if ((byDay[cursor] ?: 0) <= 0) return 0
    }
    var streak = 0
    while ((byDay[cursor] ?: 0) > 0) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

/**
 * The streak with a safety net. A day counts once [minimum] minutes are studied. Every seventh
 * streak day earns a streak freeze (two can be held), and a missed day uses a freeze instead of
 * resetting the streak. Everything is worked out from the session records, so it can never drift.
 */
data class StreakInfo(
    val current: Int = 0,
    val best: Int = 0,
    val freezesLeft: Int = 0,
    val frozenDays: List<LocalDate> = emptyList(),
)

const val MAX_STREAK_FREEZES = 2
const val STREAK_DAYS_PER_FREEZE = 7

fun streakInfo(byDay: Map<LocalDate, Int>, today: LocalDate, minimum: Int): StreakInfo {
    val need = max(1, minimum)
    val first = byDay.filterValues { it >= need }.keys.minOrNull() ?: return StreakInfo()
    if (first.isAfter(today)) return StreakInfo()
    var streak = 0
    var best = 0
    var freezes = 0
    val frozen = ArrayList<LocalDate>()
    var day = first
    while (!day.isAfter(today)) {
        val counted = (byDay[day] ?: 0) >= need
        when {
            counted -> {
                streak++
                if (streak % STREAK_DAYS_PER_FREEZE == 0 && freezes < MAX_STREAK_FREEZES) freezes++
            }
            // Today is not over yet, so an empty today never breaks the streak.
            day == today -> Unit
            streak > 0 && freezes > 0 -> {
                freezes--
                frozen.add(day)
            }
            else -> streak = 0
        }
        best = max(best, streak)
        day = day.plusDays(1)
    }
    return StreakInfo(streak, best, freezes, frozen)
}

data class DayBar(val label: String, val minutes: Int, val isToday: Boolean)

fun mondayOf(day: LocalDate): LocalDate = day.minusDays((day.dayOfWeek.value - 1).toLong())

/** One bar for every day of the month that contains [today]. */
fun monthBars(byDay: Map<LocalDate, Int>, today: LocalDate): List<DayBar> {
    val first = today.withDayOfMonth(1)
    return (0 until today.lengthOfMonth()).map { offset ->
        val day = first.plusDays(offset.toLong())
        DayBar(day.dayOfMonth.toString(), byDay[day] ?: 0, day == today)
    }
}

fun monthTotal(byDay: Map<LocalDate, Int>, today: LocalDate): Int = monthBars(byDay, today).sumOf { it.minutes }

/** Monday to Sunday of the week that contains [today]. */
fun weekBars(byDay: Map<LocalDate, Int>, today: LocalDate): List<DayBar> {
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    return (0..6).map { offset ->
        val day = monday.plusDays(offset.toLong())
        DayBar(DAY_NAMES_SHORT[offset], byDay[day] ?: 0, day == today)
    }
}

fun weekTotal(byDay: Map<LocalDate, Int>, today: LocalDate): Int = weekBars(byDay, today).sumOf { it.minutes }

/**
 * How far a challenge has got. Sessions that ended before the challenge was created do not
 * count, so a new challenge always starts from zero.
 */
fun challengeProgress(
    challenge: Challenge,
    data: AppData,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): Double {
    if (challenge.completed) return challenge.target.toDouble()
    if (challenge.type == ChallengeType.Milestone) {
        return challenge.manualProgress.coerceIn(0, challenge.target).toDouble()
    }
    val counted = data.sessions.filter { it.startedAt + it.durationMinutes * 60_000L >= challenge.createdAt }
    return when (challenge.type) {
        ChallengeType.Sessions -> counted.size.toDouble()
        ChallengeType.Hours -> (counted.sumOf { it.durationMinutes } / 6.0).roundToInt() / 10.0
        ChallengeType.DailyGoal ->
            minutesByDay(counted, zone).values.count { it >= data.profile.dailyGoalMinutes }.toDouble()
        else -> streakInfo(minutesByDay(counted, zone), today, data.profile.streakMinimumMinutes).current.toDouble()
    }
}

/** 0 to 100, for progress bars. */
fun challengePercent(progress: Double, target: Int): Int =
    if (target <= 0) 0 else ((progress / target) * 100).roundToInt().coerceIn(0, 100)

fun percentOf(part: Int, whole: Int): Int = if (whole <= 0) 0 else ((part * 100.0) / whole).roundToInt().coerceIn(0, 100)

/** Average percentage across a list of grades, or null when there are none. */
fun averagePercent(grades: List<Grade>): Double? {
    val valid = grades.filter { it.maxScore > 0 }
    if (valid.isEmpty()) return null
    return valid.sumOf { it.score / it.maxScore * 100.0 } / valid.size
}
