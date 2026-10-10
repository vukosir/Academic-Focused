package com.rikhotso.studytime

import com.rikhotso.studytime.data.TimerKind
import com.rikhotso.studytime.data.TimerLogic
import com.rikhotso.studytime.data.TimerMode
import com.rikhotso.studytime.data.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Walks the focus timer through its phases using made-up clock times. */
class TimerLogicTest {
    private val minute = 60_000L
    private val t0 = 1_000_000_000_000L

    @Test
    fun aFullRoundSavesTheSessionAndStartsTheBreak() {
        val focusing = TimerLogic.startOrResume(TimerState(), t0)
        assertEquals(TimerMode.Focus, focusing.mode)
        assertTrue(focusing.running)
        assertEquals(t0 + 25 * minute, focusing.endsAt)
        assertEquals(0.4f, TimerLogic.progress(focusing, t0 + 10 * minute), 0.001f)
        assertNull(TimerLogic.advance(focusing, t0 + 24 * minute))

        val afterFocus = TimerLogic.advance(focusing, t0 + 25 * minute + 200)
        assertNotNull(afterFocus)
        assertEquals(25, afterFocus!!.savedMinutes)
        assertEquals(t0, afterFocus.sessionStartedAt)
        assertEquals(TimerMode.Break, afterFocus.timer.mode)
        assertEquals(t0 + 30 * minute, afterFocus.timer.endsAt)
        assertEquals(1, afterFocus.timer.completedRounds)

        val afterBreak = TimerLogic.advance(afterFocus.timer, t0 + 30 * minute)
        assertEquals(TimerMode.Idle, afterBreak!!.timer.mode)
        assertEquals(0, afterBreak.savedMinutes)
        assertEquals(1, afterBreak.timer.completedRounds)
    }

    @Test
    fun pausingKeepsWhatIsLeft() {
        val focusing = TimerLogic.startOrResume(TimerState(), t0)
        val paused = TimerLogic.pause(focusing, t0 + 10 * minute)
        assertFalse(paused.running)
        assertEquals(15 * minute, TimerLogic.remainingMs(paused, t0 + 40 * minute))
        val resumed = TimerLogic.startOrResume(paused, t0 + 40 * minute)
        assertEquals(t0 + 55 * minute, resumed.endsAt)
    }

    @Test
    fun comingBackLaterStillSavesTheRound() {
        val focusing = TimerLogic.startOrResume(TimerState(), t0)
        // Two hours later the focus round and its break are both long over.
        val outcome = TimerLogic.advance(focusing, t0 + 120 * minute)!!
        assertEquals(25, outcome.savedMinutes)
        assertEquals(TimerMode.Idle, outcome.timer.mode)
        // Two minutes into the break it is still running, with three minutes left.
        val duringBreak = TimerLogic.advance(focusing, t0 + 27 * minute)!!
        assertEquals(TimerMode.Break, duringBreak.timer.mode)
        assertEquals(3 * minute, TimerLogic.remainingMs(duringBreak.timer, t0 + 27 * minute))
    }

    @Test
    fun finishingEarlySavesOnlyTheMinutesStudied() {
        val focusing = TimerLogic.startOrResume(TimerState(), t0)
        val finished = TimerLogic.finish(focusing, t0 + 4 * minute + 10_000)!!
        assertEquals(4, finished.savedMinutes)
        assertEquals(TimerMode.Break, finished.timer.mode)
        val tooShort = TimerLogic.finish(focusing, t0 + 20_000)!!
        assertEquals(0, tooShort.savedMinutes)
        assertEquals(0, tooShort.timer.completedRounds)
        // There is nothing to finish while idle or during a break.
        assertNull(TimerLogic.finish(TimerState(), t0))
        assertNull(TimerLogic.finish(finished.timer, t0 + 5 * minute))
    }

    @Test
    fun timeAwayFromTheAppIsStillCounted() {
        // The timer only stores timestamps, so being closed for 20 minutes changes nothing.
        val focusing = TimerLogic.startOrResume(TimerState(), t0)
        assertEquals(5 * minute, TimerLogic.remainingMs(focusing, t0 + 20 * minute))
        assertEquals(20 * minute, TimerLogic.elapsedMs(focusing, t0 + 20 * minute))
        // A pause in the middle is not counted as study time.
        val paused = TimerLogic.pause(focusing, t0 + 10 * minute)
        val resumed = TimerLogic.startOrResume(paused, t0 + 60 * minute)
        assertEquals(12 * minute, TimerLogic.elapsedMs(resumed, t0 + 62 * minute))
        assertEquals(12, TimerLogic.finish(resumed, t0 + 62 * minute)!!.savedMinutes)
    }

    @Test
    fun stopwatchCountsUpUntilFinished() {
        val idle = TimerLogic.withKind(TimerState(), TimerKind.Stopwatch)
        val running = TimerLogic.startOrResume(idle, t0)
        assertEquals(TimerMode.Focus, running.mode)
        assertEquals(37 * minute, TimerLogic.displayMs(running, t0 + 37 * minute))
        // It has no end, so it never moves on by itself.
        assertNull(TimerLogic.advance(running, t0 + 300 * minute))
        val paused = TimerLogic.pause(running, t0 + 37 * minute)
        assertEquals(37 * minute, TimerLogic.displayMs(paused, t0 + 90 * minute))
        val resumed = TimerLogic.startOrResume(paused, t0 + 90 * minute)
        val finished = TimerLogic.finish(resumed, t0 + 98 * minute)!!
        assertEquals(45, finished.savedMinutes)
        assertEquals(t0, finished.sessionStartedAt)
        assertEquals(TimerMode.Break, finished.timer.mode)
        // A stopwatch left running for three days is saved as twelve hours at most.
        assertEquals(720, TimerLogic.finish(running, t0 + 3 * 24 * 60 * minute)!!.savedMinutes)
    }

    @Test
    fun restartBeginsThePhaseAgain() {
        val focusing = TimerLogic.startOrResume(TimerState(), t0)
        val restarted = TimerLogic.restart(focusing, t0 + 10 * minute)
        assertEquals(t0 + 35 * minute, restarted.endsAt)
        assertEquals(t0 + 10 * minute, restarted.startedAt)
        val paused = TimerLogic.restart(TimerLogic.pause(focusing, t0 + 10 * minute), t0 + 12 * minute)
        assertFalse(paused.running)
        assertEquals(25 * minute, TimerLogic.remainingMs(paused, t0 + 50 * minute))
    }

    @Test
    fun aBreakCanWaitToBeStarted() {
        val focusing = TimerLogic.startOrResume(TimerState(), t0)
        val outcome = TimerLogic.advance(focusing, t0 + 25 * minute, autoStartBreak = false)!!
        assertEquals(25, outcome.savedMinutes)
        assertEquals(TimerMode.Break, outcome.timer.mode)
        assertFalse(outcome.timer.running)
        // Nothing happens until the user starts the break.
        assertNull(TimerLogic.advance(outcome.timer, t0 + 200 * minute, autoStartBreak = false))
        val started = TimerLogic.startOrResume(outcome.timer, t0 + 40 * minute)
        assertEquals(t0 + 45 * minute, started.endsAt)
        assertEquals(TimerMode.Idle, TimerLogic.skipBreak(started).mode)
    }

    @Test
    fun switchingKindOnlyWorksWhileIdle() {
        val custom = TimerLogic.withFocusMinutes(TimerLogic.withKind(TimerState(), TimerKind.Custom), 40)
        assertEquals(40 * minute, TimerLogic.remainingMs(custom, t0))
        val pomodoro = TimerLogic.withKind(custom, TimerKind.Pomodoro)
        assertEquals(25, pomodoro.focusMinutes)
        assertEquals(5, pomodoro.breakMinutes)
        val focusing = TimerLogic.startOrResume(pomodoro, t0)
        assertEquals(TimerKind.Pomodoro, TimerLogic.withKind(focusing, TimerKind.Stopwatch).kind)
    }

    @Test
    fun noBreakGoesStraightBackToIdle() {
        val focusing = TimerLogic.startOrResume(TimerState(breakMinutes = 0, focusMinutes = 50), t0)
        val outcome = TimerLogic.advance(focusing, t0 + 50 * minute)!!
        assertEquals(50, outcome.savedMinutes)
        assertEquals(TimerMode.Idle, outcome.timer.mode)
        assertEquals(50 * minute, TimerLogic.remainingMs(outcome.timer, t0 + 51 * minute))
    }

    @Test
    fun stoppingClearsTheRoundCount() {
        val stopped = TimerLogic.stop(TimerState(mode = TimerMode.Focus, running = true, completedRounds = 3))
        assertEquals(TimerMode.Idle, stopped.mode)
        assertEquals(0, stopped.completedRounds)
        assertEquals(15 * minute, TimerLogic.remainingMs(TimerLogic.withFocusMinutes(stopped, 15), t0))
    }
}
