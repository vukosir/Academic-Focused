package com.rikhotso.studytime

import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.Availability
import com.rikhotso.studytime.data.Exam
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.PlannedSession
import com.rikhotso.studytime.data.Priority
import com.rikhotso.studytime.data.Profile
import com.rikhotso.studytime.data.Repeat
import com.rikhotso.studytime.data.StudyTask
import com.rikhotso.studytime.data.Subject
import com.rikhotso.studytime.data.TimetableEntry
import com.rikhotso.studytime.data.Topic
import com.rikhotso.studytime.data.TopicStatus
import com.rikhotso.studytime.data.busyBlocks
import com.rikhotso.studytime.data.findClassConflict
import com.rikhotso.studytime.data.findSessionConflict
import com.rikhotso.studytime.data.freeSlots
import com.rikhotso.studytime.data.generatePlan
import com.rikhotso.studytime.data.generateRevisionPlan
import com.rikhotso.studytime.data.minutesOfDay
import com.rikhotso.studytime.data.missedSessions
import com.rikhotso.studytime.data.nextFreeSlot
import com.rikhotso.studytime.data.occursOn
import com.rikhotso.studytime.data.plannedOn
import com.rikhotso.studytime.data.reviewed
import com.rikhotso.studytime.data.topicsDueForReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Scheduling: repeats, clash detection, free time, the timetable generator and spaced revision. */
class PlannerLogicTest {
    // 12 October 2026 is a Monday.
    private val monday = LocalDate.of(2026, 10, 12)
    private val maths = Subject("maths", "Mathematics", PastelColor.Lavender, goalMinutesPerDay = 60)
    private val physics = Subject("physics", "Physics", PastelColor.Mint, goalMinutesPerDay = 60)

    /** Free to study from 16:00 to 20:00 every day. */
    private val everyEvening = (1..7).map { Availability(it, true, "16:00", "20:00") }

    private fun planned(id: String, date: LocalDate, start: String, minutes: Int = 45, repeat: Repeat = Repeat.None, days: List<Int> = emptyList()) =
        PlannedSession(id = id, title = "Session $id", subjectId = "maths", date = date.toString(), startTime = start, durationMinutes = minutes, repeat = repeat, repeatDays = days)

    private fun mathsClass(day: Int, start: String, end: String, id: String = "class$day") =
        TimetableEntry(id, "maths", "Mathematics", PastelColor.Lavender, day, start, end)

    @Test
    fun repeatsFallOnTheRightDays() {
        val once = planned("a", monday, "16:00")
        assertTrue(once.occursOn(monday))
        assertFalse(once.occursOn(monday.plusDays(7)))

        val daily = planned("b", monday, "16:00", repeat = Repeat.Daily)
        assertFalse(daily.occursOn(monday.minusDays(1)))
        assertTrue(daily.occursOn(monday.plusDays(30)))

        val weekly = planned("c", monday, "16:00", repeat = Repeat.Weekly)
        assertTrue(weekly.occursOn(monday.plusDays(14)))
        assertFalse(weekly.occursOn(monday.plusDays(1)))

        val tuesdaysAndThursdays = planned("d", monday, "16:00", repeat = Repeat.Days, days = listOf(2, 4))
        assertTrue(tuesdaysAndThursdays.occursOn(monday))
        assertTrue(tuesdaysAndThursdays.occursOn(monday.plusDays(1)))
        assertFalse(tuesdaysAndThursdays.occursOn(monday.plusDays(2)))
        assertTrue(tuesdaysAndThursdays.occursOn(monday.plusDays(3)))

        // A skipped day drops out without touching the others.
        val skipped = daily.copy(skippedDates = listOf(monday.plusDays(2).toString()))
        assertFalse(skipped.occursOn(monday.plusDays(2)))
        assertTrue(skipped.occursOn(monday.plusDays(3)))
    }

    @Test
    fun overlappingSessionsAreRefused() {
        val data = AppData(
            subjects = listOf(maths),
            entries = listOf(mathsClass(1, "09:00", "10:00")),
            plannedSessions = listOf(planned("a", monday, "16:00", 60)),
            exams = listOf(Exam("e", "maths", "Maths paper 1", monday.plusDays(2).toString(), "09:00")),
        )
        // Overlaps the planned session by 15 minutes.
        assertNotNull(findSessionConflict(data, planned("new", monday, "16:45")))
        // Starts exactly when it ends, which is allowed.
        assertNull(findSessionConflict(data, planned("new", monday, "17:00")))
        // Overlaps the Monday class.
        assertTrue(findSessionConflict(data, planned("new", monday, "09:30"))!!.contains("Mathematics class"))
        // Overlaps the exam, which is given two hours.
        assertTrue(findSessionConflict(data, planned("new", monday.plusDays(2), "10:30"))!!.contains("exam"))
        // Editing a session never clashes with itself.
        assertNull(findSessionConflict(data, planned("a", monday, "16:15", 60)))
        // A weekly repeat is checked on its later days too: it would hit next Monday's class.
        val classNextWeekOnly = data.copy(plannedSessions = emptyList())
        assertNotNull(findSessionConflict(classNextWeekOnly, planned("new", monday.minusDays(7), "09:15", repeat = Repeat.Weekly)))
    }

    @Test
    fun classesOnTheSameDayMayNotOverlap() {
        val data = AppData(entries = listOf(mathsClass(1, "09:00", "10:00")))
        assertNotNull(findClassConflict(data, 1, "09:30", "10:30"))
        assertNull(findClassConflict(data, 1, "10:00", "11:00"))
        assertNull(findClassConflict(data, 2, "09:30", "10:30"))
        assertNull(findClassConflict(data, 1, "09:15", "09:45", ignoreEntryId = "class1"))
    }

    @Test
    fun freeTimeStaysInsideAvailabilityAndAroundBookings() {
        val data = AppData(
            profile = Profile(availability = everyEvening),
            plannedSessions = listOf(planned("a", monday, "17:00", 60)),
        )
        val slots = freeSlots(data, monday, minMinutes = 30)
        assertEquals(listOf(960 to 1020, 1080 to 1200), slots.map { it.start to it.end })
        // Nothing fits when the gap is too short.
        assertEquals(listOf(1080 to 1200), freeSlots(data, monday, minMinutes = 90).map { it.start to it.end })
        // Later in the day only the time that is left counts.
        assertEquals(1140, freeSlots(data, monday, 30, notBefore = 1140).first().start)

        // A day that is switched off has no free time, so the search moves to the next day.
        val noMondays = data.copy(profile = Profile(availability = everyEvening.map { if (it.dayOfWeek == 1) it.copy(enabled = false) else it }))
        assertTrue(freeSlots(noMondays, monday, 30).isEmpty())
        assertEquals(monday.plusDays(1) to 960, nextFreeSlot(noMondays, monday, 0, 60))
    }

    @Test
    fun missedSessionsAreFoundAndCompletedOnesAreNot() {
        val done = planned("done", monday, "16:00").copy(completedDates = listOf(monday.toString()))
        val missed = planned("missed", monday, "17:00")
        val laterToday = planned("later", monday.plusDays(1), "18:00")
        val data = AppData(plannedSessions = listOf(done, missed, laterToday))
        val found = missedSessions(data, today = monday.plusDays(1), nowMinute = minutesOfDay("12:00"))
        assertEquals(listOf("missed"), found.map { it.session.id })
        // Once today's session has ended without being ticked off, it counts as missed too.
        val evening = missedSessions(data, today = monday.plusDays(1), nowMinute = minutesOfDay("19:00"))
        assertEquals(listOf("missed", "later"), evening.map { it.session.id })
    }

    @Test
    fun generatedPlanRespectsAvailabilityGoalsAndExistingBookings() {
        val data = AppData(
            profile = Profile(dailyGoalMinutes = 120, availability = everyEvening),
            subjects = listOf(maths, physics),
            entries = listOf(mathsClass(1, "16:00", "17:00")),
            plannedSessions = listOf(planned("manual", monday.plusDays(1), "16:00", 60)),
        )
        val plan = generatePlan(data, monday, days = 3, sessionMinutes = 45)
        assertTrue(plan.isNotEmpty())
        assertTrue(plan.all { it.generated })

        val withPlan = data.copy(plannedSessions = data.plannedSessions + plan)
        for (offset in 0..2) {
            val day = monday.plusDays(offset.toLong())
            val todays = plan.filter { it.date == day.toString() }
            // Never more than the daily goal.
            assertTrue(todays.sumOf { it.durationMinutes } <= 120)
            // Always inside 16:00 to 20:00.
            assertTrue(todays.all { minutesOfDay(it.startTime) >= 960 && minutesOfDay(it.startTime) + it.durationMinutes <= 1200 })
            // No two bookings on a day overlap, including the class and the session added by hand.
            val blocks = busyBlocks(withPlan, day).sortedBy { it.start }
            assertTrue(blocks.zipWithNext().all { (first, second) -> first.end <= second.start })
        }
        // Monday's class takes 16:00 to 17:00, so the first session starts after it.
        assertEquals("17:00", plan.first { it.date == monday.toString() }.startTime)
        // The day's goal of two hours is met exactly: two full sessions and a shorter one.
        assertEquals(listOf(45, 45, 30), plan.filter { it.date == monday.toString() }.map { it.durationMinutes })
        // Both subjects get time.
        assertEquals(setOf("maths", "physics"), plan.map { it.subjectId }.toSet())
        // Without subjects there is nothing to plan.
        assertTrue(generatePlan(data.copy(subjects = emptyList()), monday, 3, 45).isEmpty())
    }

    @Test
    fun examsAndDeadlinesPullTimeTowardsTheirSubject() {
        val base = AppData(profile = Profile(dailyGoalMinutes = 180, availability = everyEvening), subjects = listOf(maths, physics))
        val even = generatePlan(base, monday, days = 5, sessionMinutes = 45)
        val withExam = generatePlan(base.copy(exams = listOf(Exam("e", "physics", "Physics final", monday.plusDays(6).toString()))), monday, 5, 45)
        assertTrue(withExam.count { it.subjectId == "physics" } > even.count { it.subjectId == "physics" })

        // A task with a deadline is given a session and linked to it.
        val task = StudyTask("t", "Problem set 4", subjectId = "maths", priority = Priority.Urgent, dueDate = monday.plusDays(2).toString(), estimateMinutes = 90)
        val withTask = generatePlan(base.copy(tasks = listOf(task)), monday, 5, 45)
        assertEquals(2, withTask.count { it.taskId == "t" })
        assertTrue(withTask.filter { it.taskId == "t" }.all { it.date <= task.dueDate })
    }

    @Test
    fun revisionPlanCoversWeakTopicsFirstAndStopsBeforeTheExam() {
        val topics = listOf(
            Topic("t1", "maths", "Calculus", confidence = 4),
            Topic("t2", "maths", "Trigonometry", confidence = 1),
            Topic("t3", "maths", "Algebra", status = TopicStatus.Completed, confidence = 5),
            Topic("t4", "maths", "Statistics"),
        )
        val exam = Exam("e", "maths", "Maths paper 1", monday.plusDays(2).toString(), "09:00")
        val data = AppData(profile = Profile(availability = everyEvening), subjects = listOf(maths), topics = topics, exams = listOf(exam))
        val plan = generateRevisionPlan(data, exam, monday, sessionMinutes = 45)
        // The finished topic is left out and the weakest one comes first.
        assertEquals(listOf("t2", "t4", "t1"), plan.sessions.map { it.topicId })
        assertEquals(3, plan.topicsCovered)
        assertTrue(plan.sessions.all { it.date < exam.date })
        // Two on Monday and one on Tuesday, without overlapping.
        assertEquals(listOf("16:00", "16:55", "16:00"), plan.sessions.map { it.startTime })

        // With the exam tomorrow only two fit, and the plan says so.
        val tomorrow = exam.copy(date = monday.plusDays(1).toString())
        val short = generateRevisionPlan(data.copy(exams = listOf(tomorrow)), tomorrow, monday, 45)
        assertEquals(2, short.topicsCovered)
        assertEquals(3, short.topicsTotal)
    }

    @Test
    fun reviewsAreSpacedFurtherApartEachTime() {
        var topic = Topic("t", "maths", "Calculus")
        val expectedGaps = listOf(1, 3, 7, 14, 30, 30)
        var day = monday
        for (gap in expectedGaps) {
            topic = reviewed(topic, day)
            assertEquals(day.plusDays(gap.toLong()).toString(), topic.nextReview)
            day = day.plusDays(gap.toLong())
        }
        assertEquals(TopicStatus.InProgress, topic.status)
        assertEquals(6, topic.reviewCount)
        val data = AppData(topics = listOf(topic, Topic("other", "maths", "Never reviewed")))
        assertEquals(listOf("t"), topicsDueForReview(data, day).map { it.id })
        assertTrue(topicsDueForReview(data, day.minusDays(1)).isEmpty())
    }

    @Test
    fun plannedSessionsForADayComeBackInTimeOrder() {
        val data = AppData(plannedSessions = listOf(planned("late", monday, "18:00"), planned("early", monday, "16:00"), planned("other", monday.plusDays(1), "16:00")))
        assertEquals(listOf("early", "late"), plannedOn(data, monday).map { it.id })
    }
}
