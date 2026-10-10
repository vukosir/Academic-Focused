package com.rikhotso.studytime

import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.Challenge
import com.rikhotso.studytime.data.ChallengeType
import com.rikhotso.studytime.data.Grade
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.Profile
import com.rikhotso.studytime.data.StudySession
import com.rikhotso.studytime.data.averagePercent
import com.rikhotso.studytime.data.challengePercent
import com.rikhotso.studytime.data.challengeProgress
import com.rikhotso.studytime.data.currentStreak
import com.rikhotso.studytime.data.firstName
import com.rikhotso.studytime.data.formatClock
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.formatNumber
import com.rikhotso.studytime.data.greeting
import com.rikhotso.studytime.data.minutesByDay
import com.rikhotso.studytime.data.minutesOfDay
import com.rikhotso.studytime.data.weekBars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Plain JVM tests for the helpers in data/StudyLogic.kt. Run them with the green arrow in Android Studio. */
class StudyLogicTest {
    private val zone = ZoneId.of("Africa/Johannesburg")
    private val saturday = LocalDate.of(2026, 10, 10)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun session(date: LocalDate, hour: Int, minutes: Int) =
        StudySession("s${date.dayOfMonth}-$hour", "", "General study", PastelColor.Lavender, minutes, at(date, hour))

    @Test
    fun formatsMinutesAndClock() {
        assertEquals("0m", formatMinutes(0))
        assertEquals("25m", formatMinutes(25))
        assertEquals("2h", formatMinutes(120))
        assertEquals("1h 30m", formatMinutes(90))
        assertEquals("25:00", formatClock(1500))
        assertEquals("00:59", formatClock(59))
        assertEquals("3", formatNumber(3.0))
        assertEquals("2.5", formatNumber(2.5))
    }

    @Test
    fun greetsByTimeOfDay() {
        assertEquals("Good morning", greeting(8))
        assertEquals("Good afternoon", greeting(13))
        assertEquals("Good evening", greeting(19))
        assertEquals("Good night", greeting(22))
        assertEquals("Vukosi", firstName("  Vukosi Rikhotso "))
        assertEquals("there", firstName(" "))
    }

    @Test
    fun readsTimesOfDay() {
        assertEquals(570, minutesOfDay("09:30"))
        assertEquals(0, minutesOfDay("nonsense"))
    }

    @Test
    fun streakCountsBackFromTodayOrYesterday() {
        val sessions = listOf(session(saturday, 9, 25), session(saturday.minusDays(1), 9, 25), session(saturday.minusDays(2), 20, 50))
        val byDay = minutesByDay(sessions, zone)
        assertEquals(3, currentStreak(byDay, saturday))
        // Nothing logged on Sunday yet: the streak still stands until a whole day is missed.
        assertEquals(3, currentStreak(byDay, saturday.plusDays(1)))
        assertEquals(0, currentStreak(byDay, saturday.plusDays(2)))
        assertEquals(0, currentStreak(emptyMap(), saturday))
    }

    @Test
    fun weekRunsMondayToSunday() {
        val bars = weekBars(minutesByDay(listOf(session(saturday, 9, 25), session(saturday, 14, 4)), zone), saturday)
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), bars.map { it.label })
        assertEquals(29, bars[5].minutes)
        assertEquals(true, bars[5].isToday)
        assertEquals(29, bars.sumOf { it.minutes })
    }

    @Test
    fun challengesOnlyCountWorkDoneAfterTheyWereCreated() {
        val created = at(saturday, 12)
        val data = AppData(
            profile = Profile(dailyGoalMinutes = 60),
            sessions = listOf(session(saturday, 8, 25), session(saturday, 13, 50), session(saturday, 15, 25)),
        )
        fun challenge(type: ChallengeType, target: Int) = Challenge("c", "test", type, target, createdAt = created)
        assertEquals(2.0, challengeProgress(challenge(ChallengeType.Sessions, 5), data, saturday, zone), 0.0)
        assertEquals(1.3, challengeProgress(challenge(ChallengeType.Hours, 5), data, saturday, zone), 0.0)
        assertEquals(1.0, challengeProgress(challenge(ChallengeType.DailyGoal, 3), data, saturday, zone), 0.0)
        assertEquals(1.0, challengeProgress(challenge(ChallengeType.Streak, 7), data, saturday, zone), 0.0)
        val milestone = Challenge("m", "chapters", ChallengeType.Milestone, 4, manualProgress = 3, createdAt = created)
        assertEquals(3.0, challengeProgress(milestone, data, saturday, zone), 0.0)
        assertEquals(75, challengePercent(3.0, 4))
        assertEquals(100, challengePercent(9.0, 4))
    }

    @Test
    fun averagesGradesAsPercentages() {
        fun grade(score: Double, max: Double) = Grade("g$score", "s", "Maths", PastelColor.Mint, "Test", score, max, "2026-10-10")
        assertEquals(75.0, averagePercent(listOf(grade(32.0, 40.0), grade(70.0, 100.0)))!!, 0.0001)
        assertNull(averagePercent(emptyList()))
    }
}
