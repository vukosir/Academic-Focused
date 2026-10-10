package com.rikhotso.studytime

import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.Award
import com.rikhotso.studytime.data.Exam
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.PlannedSession
import com.rikhotso.studytime.data.Priority
import com.rikhotso.studytime.data.Profile
import com.rikhotso.studytime.data.ReminderKind
import com.rikhotso.studytime.data.SearchKind
import com.rikhotso.studytime.data.Settings
import com.rikhotso.studytime.data.StatPeriod
import com.rikhotso.studytime.data.StudyNote
import com.rikhotso.studytime.data.StudySession
import com.rikhotso.studytime.data.StudyTask
import com.rikhotso.studytime.data.Subject
import com.rikhotso.studytime.data.TaskDueFilter
import com.rikhotso.studytime.data.TaskFilter
import com.rikhotso.studytime.data.TaskStatusFilter
import com.rikhotso.studytime.data.Topic
import com.rikhotso.studytime.data.TopicStatus
import com.rikhotso.studytime.data.afterQuietHours
import com.rikhotso.studytime.data.badges
import com.rikhotso.studytime.data.dailyQuests
import com.rikhotso.studytime.data.examReadiness
import com.rikhotso.studytime.data.filterTasks
import com.rikhotso.studytime.data.inQuietHours
import com.rikhotso.studytime.data.isOverdue
import com.rikhotso.studytime.data.levelFor
import com.rikhotso.studytime.data.levelInfo
import com.rikhotso.studytime.data.minutesByDay
import com.rikhotso.studytime.data.periodSummary
import com.rikhotso.studytime.data.personalRecords
import com.rikhotso.studytime.data.planReminders
import com.rikhotso.studytime.data.plannedVersusActual
import com.rikhotso.studytime.data.searchAll
import com.rikhotso.studytime.data.sessionHistory
import com.rikhotso.studytime.data.sessionsCsv
import com.rikhotso.studytime.data.streakInfo
import com.rikhotso.studytime.data.subjectBreakdown
import com.rikhotso.studytime.data.tasksDueSoon
import com.rikhotso.studytime.data.totalXp
import com.rikhotso.studytime.data.unclaimedAwards
import com.rikhotso.studytime.data.weeklyQuests
import com.rikhotso.studytime.data.xpForLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Streaks, points, challenges, tasks, statistics, search and reminders. */
class ProgressLogicTest {
    private val zone = ZoneId.of("Africa/Johannesburg")

    // 10 October 2026 is a Saturday.
    private val saturday = LocalDate.of(2026, 10, 10)
    private val maths = Subject("maths", "Mathematics", PastelColor.Lavender)
    private val physics = Subject("physics", "Physics", PastelColor.Mint)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun session(date: LocalDate, hour: Int, minutes: Int, subject: Subject = maths, note: String = "") =
        StudySession("s$date-$hour", subject.id, subject.name, subject.color, minutes, at(date, hour), note = note)

    // ------------------------------------------------------------------ streaks

    @Test
    fun aDayOnlyCountsOnceTheMinimumIsReached() {
        val byDay = minutesByDay(listOf(session(saturday, 9, 10), session(saturday.minusDays(1), 9, 30)), zone)
        // Ten minutes today is not enough for a 15 minute minimum, but today is not over, so yesterday's streak stands.
        assertEquals(1, streakInfo(byDay, saturday, minimum = 15).current)
        assertEquals(2, streakInfo(byDay, saturday, minimum = 10).current)
    }

    @Test
    fun aFreezeEarnedOverSevenDaysSavesTheStreakOnce() {
        // Seven days in a row earn one freeze.
        val start = saturday.minusDays(12)
        val sevenDays = (0..6).map { session(start.plusDays(it.toLong()), 9, 30) }
        val earned = streakInfo(minutesByDay(sevenDays, zone), start.plusDays(6), 15)
        assertEquals(7, earned.current)
        assertEquals(1, earned.freezesLeft)

        // Day 8 is missed, day 9 is studied: the freeze covers the gap and the streak carries on.
        val afterGap = sevenDays + session(start.plusDays(8), 9, 30)
        val saved = streakInfo(minutesByDay(afterGap, zone), start.plusDays(8), 15)
        assertEquals(8, saved.current)
        assertEquals(0, saved.freezesLeft)
        assertEquals(listOf(start.plusDays(7)), saved.frozenDays)

        // A second missed day with no freeze left resets the streak, but the best is remembered.
        val broken = streakInfo(minutesByDay(afterGap, zone), start.plusDays(11), 15)
        assertEquals(0, broken.current)
        assertEquals(8, broken.best)
    }

    // ------------------------------------------------------------------ points and challenges

    @Test
    fun pointsComeFromRecordsAndLevelsRiseWithThem() {
        assertEquals(0, xpForLevel(1))
        assertEquals(100, xpForLevel(2))
        assertEquals(250, xpForLevel(3))
        assertEquals(1, levelFor(99))
        assertEquals(2, levelFor(100))
        assertEquals(3, levelFor(260))

        val data = AppData(
            sessions = listOf(session(saturday, 9, 50), session(saturday, 11, 25)),
            rewardedTaskIds = listOf("a", "b"),
            awards = listOf(Award("daily:x", "Study for 30 minutes", 15, saturday.toString())),
        )
        // 75 minutes, two tasks at 10 each and one 15 point award.
        assertEquals(110, totalXp(data))
        val info = levelInfo(data)
        assertEquals(2, info.level)
        assertEquals(10, info.intoLevel)
        assertEquals(150, info.neededForNext)
    }

    @Test
    fun dailyChallengesAreStableForADayAndPaidOnlyOnce() {
        val empty = AppData(subjects = listOf(maths))
        val quests = dailyQuests(empty, saturday, zone)
        assertEquals(3, quests.size)
        assertEquals(3, quests.map { it.key }.distinct().size)
        assertEquals(quests.map { it.key }, dailyQuests(empty, saturday, zone).map { it.key })
        assertTrue(quests.all { it.key.startsWith("daily:2026-10-10:") && !it.done })
        // Another day brings a different set.
        assertFalse(quests.map { it.title } == dailyQuests(empty, saturday.plusDays(1), zone).map { it.title })
        assertEquals(2, weeklyQuests(empty, saturday, zone).size)

        // A long day of study completes at least one of them.
        val studied = empty.copy(sessions = listOf(session(saturday, 9, 60), session(saturday, 11, 60), session(saturday, 14, 45)))
        val awards = unclaimedAwards(studied, saturday, zone)
        assertTrue(awards.isNotEmpty())
        // Once recorded they are not offered again.
        val paid = studied.copy(awards = awards)
        assertTrue(unclaimedAwards(paid, saturday, zone).isEmpty())
        assertEquals(totalXp(studied) + awards.sumOf { it.xp }, totalXp(paid))
    }

    @Test
    fun challengesThatCannotBeDoneAreNotOffered() {
        // No topics, one subject and nothing planned: those three ideas must never appear.
        val data = AppData(subjects = listOf(maths))
        for (offset in 0L..20L) {
            val titles = dailyQuests(data, saturday.plusDays(offset), zone).map { it.title }
            assertFalse(titles.any { it.contains("revision topic") || it.contains("different subjects") || it.contains("planned session") })
            assertEquals(3, titles.distinct().size)
        }
    }

    @Test
    fun badgesAreEarnedFromRealProgress() {
        val none = badges(AppData(), saturday, zone)
        assertTrue(none.none { it.earned })
        val data = AppData(
            profile = Profile(dailyGoalMinutes = 60),
            sessions = listOf(session(saturday, 9, 60)),
            tasks = listOf(StudyTask("t", "Essay", done = true)),
        )
        val earned = badges(data, saturday, zone).filter { it.earned }.map { it.key }
        assertEquals(listOf("first_session", "hours_1", "daily_goal", "tasks_1"), earned)
    }

    // ------------------------------------------------------------------ tasks

    @Test
    fun tasksAreFilteredSortedAndFlaggedOverdue() {
        val tasks = listOf(
            StudyTask("a", "Read chapter 3", subjectId = "maths", priority = Priority.Low, dueDate = saturday.plusDays(5).toString()),
            StudyTask("b", "Lab report", subjectId = "physics", priority = Priority.Urgent, dueDate = saturday.minusDays(1).toString()),
            StudyTask("c", "Past paper", subjectId = "maths", priority = Priority.High, dueDate = saturday.toString(), dueTime = "14:00"),
            StudyTask("d", "Tidy notes", priority = Priority.Medium),
            StudyTask("e", "Old homework", subjectId = "maths", dueDate = saturday.minusDays(3).toString(), done = true),
        )
        val data = AppData(subjects = listOf(maths, physics), tasks = tasks)
        val noon = 12 * 60

        assertTrue(isOverdue(tasks[1], saturday, noon))
        assertFalse(isOverdue(tasks[2], saturday, noon))
        assertTrue(isOverdue(tasks[2], saturday, 15 * 60))
        assertFalse(isOverdue(tasks[3], saturday, noon))
        assertFalse(isOverdue(tasks[4], saturday, noon))

        // Open tasks: earliest deadline first, undated last.
        assertEquals(listOf("b", "c", "a", "d"), filterTasks(data, TaskFilter(), saturday, noon).map { it.id })
        assertEquals(listOf("b"), filterTasks(data, TaskFilter(status = TaskStatusFilter.Overdue), saturday, noon).map { it.id })
        assertEquals(listOf("e"), filterTasks(data, TaskFilter(status = TaskStatusFilter.Done), saturday, noon).map { it.id })
        assertEquals(listOf("c", "a"), filterTasks(data, TaskFilter(subjectId = "maths"), saturday, noon).map { it.id })
        assertEquals(listOf("b"), filterTasks(data, TaskFilter(priority = Priority.Urgent), saturday, noon).map { it.id })
        assertEquals(listOf("c"), filterTasks(data, TaskFilter(due = TaskDueFilter.Today), saturday, noon).map { it.id })
        assertEquals(listOf("d"), filterTasks(data, TaskFilter(due = TaskDueFilter.NoDate), saturday, noon).map { it.id })
        assertEquals(listOf("b"), filterTasks(data, TaskFilter(query = "  LAB "), saturday, noon).map { it.id })
        // Searching by subject name works too.
        assertEquals(listOf("b"), filterTasks(data, TaskFilter(query = "physics"), saturday, noon).map { it.id })
        assertEquals(listOf("b", "c"), tasksDueSoon(data, saturday, days = 3).map { it.id })
    }

    // ------------------------------------------------------------------ statistics

    @Test
    fun summariesCountOnlyWhatIsInThePeriod() {
        val data = AppData(
            profile = Profile(dailyGoalMinutes = 60, weeklyGoalMinutes = 300, monthlyGoalMinutes = 1200),
            subjects = listOf(maths, physics),
            sessions = listOf(
                session(saturday, 9, 45),
                session(saturday, 11, 30, physics),
                session(saturday.minusDays(2), 18, 60),
                session(saturday.minusDays(9), 18, 90, physics),
            ),
            tasks = listOf(StudyTask("t", "Essay", done = true, completedAt = at(saturday, 10))),
        )
        val today = periodSummary(data, StatPeriod.Today, saturday, zone)
        assertEquals(75, today.minutes)
        assertEquals(2, today.sessions)
        assertEquals(1, today.tasksDone)
        assertEquals(100, today.goalPercent)

        val week = periodSummary(data, StatPeriod.Week, saturday, zone)
        assertEquals(135, week.minutes)
        assertEquals(2, week.activeDays)
        assertEquals(2, week.goalDaysMet)
        assertEquals(45, week.goalPercent)

        assertEquals(225, periodSummary(data, StatPeriod.Month, saturday, zone).minutes)
        assertEquals(4, periodSummary(data, StatPeriod.All, saturday, zone).sessions)

        val shares = subjectBreakdown(data, StatPeriod.Week, saturday, zone)
        assertEquals(listOf("Mathematics" to 105, "Physics" to 30), shares.map { it.name to it.minutes })
        assertEquals(100, shares.sumOf { it.percent })

        val records = personalRecords(data, saturday, zone)
        assertEquals("1h 30m", records.first { it.label == "Longest session" }.value)
        assertEquals("1h 30m", records.first { it.label == "Most study in a day" }.value)
        assertEquals("2", records.first { it.label == "Most sessions in a day" }.value)
        assertTrue(personalRecords(AppData(), saturday, zone).isEmpty())
    }

    @Test
    fun plannedIsComparedWithWhatHappened() {
        val monday = saturday.minusDays(5)
        val kept = PlannedSession("a", "Maths", "maths", monday.toString(), "16:00", 60, completedDates = listOf(monday.toString()))
        val missed = PlannedSession("b", "Physics", "physics", monday.plusDays(1).toString(), "16:00", 45)
        val future = PlannedSession("c", "Later", "maths", saturday.plusDays(1).toString(), "16:00", 45)
        val data = AppData(plannedSessions = listOf(kept, missed, future), sessions = listOf(session(monday, 16, 60)))
        val result = plannedVersusActual(data, monday, monday.plusDays(6), saturday, zone)
        // Tomorrow's session has not happened yet, so it is not counted against the plan.
        assertEquals(2, result.plannedSessions)
        assertEquals(1, result.completedSessions)
        assertEquals(105, result.plannedMinutes)
        assertEquals(60, result.studiedMinutes)
    }

    @Test
    fun historyAndSearchFindThingsByText() {
        val data = AppData(
            subjects = listOf(maths, physics),
            sessions = listOf(session(saturday, 9, 45, note = "Chain rule practice"), session(saturday.minusDays(1), 9, 30, physics)),
            tasks = listOf(StudyTask("t", "Chain rule worksheet", subjectId = "maths")),
            notes = listOf(StudyNote("n", "maths", "", "Derivatives", "The chain rule is used when...")),
            topics = listOf(Topic("tp", "physics", "Kinematics")),
            exams = listOf(Exam("e", "physics", "Physics final", saturday.plusDays(20).toString(), location = "Hall B")),
        )
        assertEquals(2, sessionHistory(data).size)
        assertEquals(saturday, com.rikhotso.studytime.data.dateOf(sessionHistory(data).first().startedAt, zone))
        assertEquals(1, sessionHistory(data, "chain").size)
        assertEquals(1, sessionHistory(data, subjectId = "physics").size)

        val hits = searchAll(data, "chain")
        assertEquals(setOf(SearchKind.Task, SearchKind.Note, SearchKind.Session), hits.map { it.kind }.toSet())
        assertEquals(setOf(SearchKind.Subject, SearchKind.Exam, SearchKind.Session), searchAll(data, "physics").map { it.kind }.toSet())
        assertEquals(listOf(SearchKind.Exam), searchAll(data, "hall b").map { it.kind })
        assertEquals(listOf(SearchKind.Topic), searchAll(data, "kinem").map { it.kind })
        // One letter is too little to search on.
        assertTrue(searchAll(data, "c").isEmpty())
    }

    @Test
    fun examReadinessReportsCoverageNotAPrediction() {
        val exam = Exam("e", "maths", "Maths paper 1", saturday.plusDays(12).toString())
        val data = AppData(
            subjects = listOf(maths),
            exams = listOf(exam),
            topics = listOf(
                Topic("a", "maths", "Algebra", TopicStatus.Completed, confidence = 5),
                Topic("b", "maths", "Calculus", TopicStatus.InProgress, confidence = 2),
                Topic("c", "maths", "Statistics"),
                Topic("d", "maths", "Geometry", TopicStatus.InProgress, confidence = 4),
            ),
            tasks = listOf(StudyTask("t", "Past paper", subjectId = "maths")),
        )
        val readiness = examReadiness(data, exam, saturday)
        assertEquals(12L, readiness.daysLeft)
        assertEquals(25, readiness.coveragePercent)
        assertEquals(2, readiness.topicsInProgress)
        assertEquals(listOf("b", "c"), readiness.needAttention.map { it.id })
        assertEquals(1, readiness.openTasks)
        assertEquals(3.7, readiness.averageConfidence!!, 0.0001)
    }

    @Test
    fun csvExportEscapesCommasAndQuotes() {
        val data = AppData(sessions = listOf(session(saturday, 9, 45, note = "Limits, then \"hard\" ones")))
        val lines = sessionsCsv(data, zone).trim().lines()
        assertEquals("date,start,subject,minutes,task,topic,note", lines[0])
        assertEquals("2026-10-10,09:00,Mathematics,45,,,\"Limits, then \"\"hard\"\" ones\"", lines[1])
    }

    // ------------------------------------------------------------------ reminders

    @Test
    fun remindersAreBuiltFromPlansDeadlinesExamsAndRevision() {
        val now = at(saturday, 12)
        val data = AppData(
            onboarded = true,
            subjects = listOf(maths),
            plannedSessions = listOf(
                PlannedSession("p", "Maths revision", "maths", saturday.toString(), "16:00", 45),
                PlannedSession("gone", "Earlier", "maths", saturday.toString(), "09:00", 45),
                PlannedSession("done", "Finished", "maths", saturday.toString(), "18:00", 45, completedDates = listOf(saturday.toString())),
            ),
            tasks = listOf(StudyTask("t", "Lab report", subjectId = "maths", dueDate = saturday.plusDays(2).toString(), dueTime = "14:00")),
            exams = listOf(Exam("e", "maths", "Maths paper 1", saturday.plusDays(8).toString(), "09:00", "Hall B")),
            topics = listOf(Topic("tp", "maths", "Calculus", nextReview = saturday.plusDays(1).toString())),
        )
        val plan = planReminders(data, now, zone)
        fun time(key: String) = plan.first { it.key == key }.atMillis

        // Ten minutes before the session. The one that already started and the finished one are left out.
        assertEquals(at(saturday, 15, 50), time("session:p:$saturday"))
        assertTrue(plan.none { it.key.startsWith("session:gone") || it.key.startsWith("session:done") })
        // The evening before the deadline, and an hour before the due time.
        assertEquals(at(saturday.plusDays(1), 17), time("task-eve:t"))
        assertEquals(at(saturday.plusDays(2), 13), time("task-day:t"))
        // A week before the exam and the evening before.
        assertEquals(at(saturday.plusDays(1), 9), time("exam-week:e"))
        assertEquals(at(saturday.plusDays(7), 18), time("exam-eve:e"))
        assertEquals(ReminderKind.Revision, plan.first { it.key == "revision:${saturday.plusDays(1)}" }.kind)
        // Soonest first, and nothing in the past.
        assertEquals(plan.sortedBy { it.atMillis }, plan)
        assertTrue(plan.all { it.atMillis > now })

        // Switching a kind off removes just that kind.
        val noExams = planReminders(data.copy(settings = Settings(notifyExams = false)), now, zone)
        assertTrue(noExams.none { it.kind == ReminderKind.Exam })
        assertTrue(noExams.any { it.kind == ReminderKind.Session })
        // Nothing is booked before setup is finished.
        assertTrue(planReminders(data.copy(onboarded = false), now, zone).isEmpty())
    }

    @Test
    fun quietHoursMoveOrDropReminders() {
        val quiet = Settings(quietHoursEnabled = true, quietStart = "22:00", quietEnd = "07:00")
        assertTrue(inQuietHours(quiet, 23 * 60))
        assertTrue(inQuietHours(quiet, 6 * 60 + 59))
        assertFalse(inQuietHours(quiet, 7 * 60))
        assertFalse(inQuietHours(Settings(), 23 * 60))
        // Late evening moves to the next morning, early morning to later the same morning.
        assertEquals(LocalDateTime.of(2026, 10, 11, 7, 0), afterQuietHours(quiet, LocalDateTime.of(2026, 10, 10, 23, 30)))
        assertEquals(LocalDateTime.of(2026, 10, 10, 7, 0), afterQuietHours(quiet, LocalDateTime.of(2026, 10, 10, 5, 0)))
        assertEquals(LocalDateTime.of(2026, 10, 10, 12, 0), afterQuietHours(quiet, LocalDateTime.of(2026, 10, 10, 12, 0)))

        val now = at(saturday, 12)
        val allDayQuiet = Settings(quietHoursEnabled = true, quietStart = "08:00", quietEnd = "20:00")
        val data = AppData(
            onboarded = true,
            settings = allDayQuiet,
            plannedSessions = listOf(PlannedSession("p", "Maths revision", "maths", saturday.toString(), "16:00", 45)),
            tasks = listOf(StudyTask("t", "Lab report", dueDate = saturday.plusDays(2).toString())),
        )
        val plan = planReminders(data, now, zone)
        // A session reminder that could only arrive after the session started is dropped.
        assertTrue(plan.none { it.kind == ReminderKind.Session })
        // The deadline reminders wait until quiet hours end at 20:00.
        assertEquals(at(saturday.plusDays(1), 20), plan.first { it.key == "task-eve:t" }.atMillis)
        assertEquals(at(saturday.plusDays(2), 20), plan.first { it.key == "task-day:t" }.atMillis)
    }
}
