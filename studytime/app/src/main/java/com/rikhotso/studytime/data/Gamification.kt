package com.rikhotso.studytime.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields

/**
 * Points, levels, the daily and weekly challenges, and badges. Nothing here is stored as a
 * running total: every number is worked out from the real records (sessions, finished tasks and
 * the awards that were handed out), so points cannot be earned twice or drift out of step.
 */

const val XP_PER_FOCUS_MINUTE = 1
const val XP_PER_TASK = 10
const val XP_PER_PERSONAL_CHALLENGE = 25
const val XP_DAILY_QUEST = 15
const val XP_WEEKLY_QUEST = 50

fun totalXp(data: AppData): Int =
    data.sessions.sumOf { it.durationMinutes } * XP_PER_FOCUS_MINUTE +
        data.rewardedTaskIds.size * XP_PER_TASK +
        data.challenges.count { it.completed } * XP_PER_PERSONAL_CHALLENGE +
        data.awards.sumOf { it.xp }

/** Points needed to reach [level]. Level 1 starts at 0, level 2 at 100, level 3 at 250, level 4 at 450. */
fun xpForLevel(level: Int): Int = if (level <= 1) 0 else 25 * (level - 1) * (level + 2)

fun levelFor(xp: Int): Int {
    var level = 1
    while (level < 99 && xpForLevel(level + 1) <= xp) level++
    return level
}

data class LevelInfo(val level: Int, val xp: Int, val intoLevel: Int, val neededForNext: Int) {
    val percent: Int get() = percentOf(intoLevel, neededForNext)
}

fun levelInfo(data: AppData): LevelInfo {
    val xp = totalXp(data)
    val level = levelFor(xp)
    val floor = xpForLevel(level)
    return LevelInfo(level, xp, xp - floor, xpForLevel(level + 1) - floor)
}

// ------------------------------------------------------------------ daily and weekly challenges

/** One optional challenge for today or for this week. [key] is unique per day or week. */
data class Quest(
    val key: String,
    val title: String,
    val progress: Int,
    val target: Int,
    val xp: Int,
    val claimed: Boolean,
) {
    val done: Boolean get() = progress >= target
    val percent: Int get() = percentOf(progress, target)
}

private fun sessionsOn(data: AppData, day: LocalDate, zone: ZoneId) = data.sessions.filter { dateOf(it.startedAt, zone) == day }

private fun tasksDoneOn(data: AppData, day: LocalDate, zone: ZoneId) =
    data.tasks.count { it.done && it.completedAt > 0L && dateOf(it.completedAt, zone) == day }

/** A challenge that can be offered. [possible] is false when the user has nothing to do it with yet. */
private data class QuestIdea(val id: String, val title: String, val progress: Int, val target: Int, val possible: Boolean = true)

/** Picks [count] ideas starting at [start], swapping any that cannot be done for one that always can. */
private fun pickQuests(pool: List<QuestIdea>, start: Int, count: Int): List<QuestIdea> {
    val slots = (0 until count).map { (start + it * 2) % pool.size }
    val picked = ArrayList<QuestIdea>()
    for (slot in slots) {
        val idea = pool[slot]
        val spare = pool.filterIndexed { index, other -> other.possible && index !in slots && other !in picked }
        picked.add(if (idea.possible || spare.isEmpty()) idea else spare[(start + picked.size) % spare.size])
    }
    return picked
}

/** Three challenges for [today]. Which three is decided by the date, so they change every day. */
fun dailyQuests(data: AppData, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Quest> {
    val sessions = sessionsOn(data, today, zone)
    val minutes = sessions.sumOf { it.durationMinutes }
    val goal = data.profile.dailyGoalMinutes
    val planned = plannedOn(data, today)
    val pool = listOf(
        QuestIdea("minutes", "Study for 30 minutes", minutes, 30),
        QuestIdea("sessions", "Finish 2 focus sessions", sessions.size, 2),
        QuestIdea("subjects", "Study 2 different subjects", sessions.map { it.subjectId }.filter { it.isNotEmpty() }.distinct().size, 2, data.subjects.size >= 2),
        QuestIdea("goal", "Reach your daily goal of ${formatMinutes(goal)}", minutes, goal),
        QuestIdea("review", "Review 1 revision topic", data.topics.count { it.lastReviewed == today.toString() }, 1, data.topics.isNotEmpty()),
        QuestIdea("task", "Complete 1 task", tasksDoneOn(data, today, zone), 1),
        QuestIdea("planned", "Complete a planned session", planned.count { it.isCompletedOn(today) }, 1, planned.isNotEmpty()),
        QuestIdea("long", "Finish one session of 45 minutes or more", sessions.count { it.durationMinutes >= 45 }, 1),
        QuestIdea("early", "Study for 15 minutes", minutes, 15),
    )
    val start = Math.floorMod(today.toEpochDay(), pool.size.toLong()).toInt()
    return pickQuests(pool, start, 3).map { idea ->
        val key = "daily:$today:${idea.id}"
        Quest(key, idea.title, idea.progress.coerceAtMost(idea.target), idea.target, XP_DAILY_QUEST, data.awards.any { it.key == key })
    }
}

/** "2026-W41", the ISO week that contains [day]. */
fun weekKey(day: LocalDate): String =
    "${day.get(IsoFields.WEEK_BASED_YEAR)}-W${"%02d".format(day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))}"

/** Two challenges for the week (Monday to Sunday) that contains [today]. */
fun weeklyQuests(data: AppData, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Quest> {
    val monday = mondayOf(today)
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val sessions = data.sessions.filter { dateOf(it.startedAt, zone) in days }
    val minutes = sessions.sumOf { it.durationMinutes }
    val weeklyGoal = data.profile.weeklyGoalMinutes
    val subjectTarget = data.subjects.size.coerceIn(2, 3)
    val pool = listOf(
        QuestIdea("minutes", "Reach your weekly goal of ${formatMinutes(weeklyGoal)}", minutes, weeklyGoal),
        QuestIdea("subjects", "Study $subjectTarget different subjects this week", sessions.map { it.subjectId }.filter { it.isNotEmpty() }.distinct().size, subjectTarget, data.subjects.size >= 2),
        QuestIdea("sessions", "Finish 8 focus sessions this week", sessions.size, 8),
        QuestIdea("tasks", "Complete 5 tasks this week", days.sumOf { tasksDoneOn(data, it, zone) }, 5),
        QuestIdea("days", "Study on 5 different days this week", sessions.map { dateOf(it.startedAt, zone) }.distinct().size, 5),
    )
    val start = Math.floorMod(today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), pool.size)
    return pickQuests(pool, start, 2).map { idea ->
        val key = "weekly:${weekKey(today)}:${idea.id}"
        Quest(key, idea.title, idea.progress.coerceAtMost(idea.target), idea.target, XP_WEEKLY_QUEST, data.awards.any { it.key == key })
    }
}

/** Finished challenges that have not been paid out yet, as the awards to add. */
fun unclaimedAwards(data: AppData, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Award> =
    (dailyQuests(data, today, zone) + weeklyQuests(data, today, zone))
        .filter { it.done && !it.claimed }
        .map { Award(it.key, it.title, it.xp, today.toString()) }

// ------------------------------------------------------------------ badges

data class Badge(val key: String, val title: String, val description: String, val earned: Boolean)

fun badges(data: AppData, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Badge> {
    val byDay = minutesByDay(data.sessions, zone)
    val total = data.sessions.sumOf { it.durationMinutes }
    val streak = streakInfo(byDay, today, data.profile.streakMinimumMinutes)
    val tasksDone = data.tasks.count { it.done }
    val plannedDone = data.plannedSessions.sumOf { it.completedDates.size }
    val bestWeek = byDay.entries.groupBy { mondayOf(it.key) }.values.maxOfOrNull { week -> week.sumOf { it.value } } ?: 0
    return listOf(
        Badge("first_session", "First focus", "Finish your first focus session", data.sessions.isNotEmpty()),
        Badge("hours_1", "One hour in", "Study for 1 hour in total", total >= 60),
        Badge("hours_10", "Ten hour club", "Study for 10 hours in total", total >= 600),
        Badge("hours_50", "Fifty hours", "Study for 50 hours in total", total >= 3000),
        Badge("daily_goal", "Goal getter", "Reach your daily goal", byDay.values.any { it >= data.profile.dailyGoalMinutes }),
        Badge("weekly_goal", "Strong week", "Reach your weekly goal", bestWeek >= data.profile.weeklyGoalMinutes),
        Badge("streak_3", "Three in a row", "Keep a 3 day streak", streak.best >= 3),
        Badge("streak_7", "Full week", "Keep a 7 day streak", streak.best >= 7),
        Badge("streak_30", "Unstoppable", "Keep a 30 day streak", streak.best >= 30),
        Badge("tasks_1", "Ticked off", "Complete your first task", tasksDone >= 1),
        Badge("tasks_10", "Task tamer", "Complete 10 tasks", tasksDone >= 10),
        Badge("planner_5", "On schedule", "Complete 5 planned sessions", plannedDone >= 5),
        Badge("topics_10", "Syllabus climber", "Complete 10 revision topics", data.topics.count { it.status == TopicStatus.Completed } >= 10),
        Badge("level_5", "Level five", "Reach level 5", levelFor(totalXp(data)) >= 5),
    )
}

/** Built-in badges earned plus personal challenges completed. */
fun badgesEarned(data: AppData, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int =
    badges(data, today, zone).count { it.earned } + data.challenges.count { it.completed }
