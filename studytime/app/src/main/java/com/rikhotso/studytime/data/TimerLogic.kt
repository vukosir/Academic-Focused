package com.rikhotso.studytime.data

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The focus timer as a small state machine of pure functions:
 *
 *   Idle  --start-->  Focus  --time up or finish-->  Break  --time up-->  Idle
 *
 * Each function takes the current [TimerState] and the current time and returns the next state.
 * The ViewModel saves the result and schedules the end-of-phase notification.
 *
 * Pomodoro and Custom count down to an end time. Stopwatch counts up until the user finishes.
 */

/** What changed when a phase ended. [savedMinutes] is above zero when a focus session should be logged. */
data class TimerOutcome(
    val timer: TimerState,
    val savedMinutes: Int = 0,
    val sessionStartedAt: Long = 0L,
    val message: String = "",
)

/** The longest single session that is saved: twelve hours. */
const val MAX_SESSION_MINUTES = 720

object TimerLogic {

    private fun isStopwatchFocus(timer: TimerState) = timer.kind == TimerKind.Stopwatch && timer.mode == TimerMode.Focus

    /** Time left in a countdown phase. A stopwatch has no end, so this is 0 for it. */
    fun remainingMs(timer: TimerState, now: Long): Long = when {
        timer.mode == TimerMode.Idle -> if (timer.kind == TimerKind.Stopwatch) 0L else timer.focusMinutes * 60_000L
        isStopwatchFocus(timer) -> 0L
        timer.running -> max(0L, timer.endsAt - now)
        else -> timer.remainingMs
    }

    /** Time spent in the current phase so far. */
    fun elapsedMs(timer: TimerState, now: Long): Long = when {
        timer.mode == TimerMode.Idle -> 0L
        isStopwatchFocus(timer) -> timer.elapsedMs + if (timer.running) max(0L, now - timer.resumedAt) else 0L
        else -> max(0L, timer.totalMs - remainingMs(timer, now))
    }

    /** False while the stopwatch is counting up, true for every countdown (and for a break). */
    fun countsDown(timer: TimerState): Boolean = !(timer.kind == TimerKind.Stopwatch && timer.mode != TimerMode.Break)

    /** What the clock face shows: time counted for the stopwatch, time left otherwise. */
    fun displayMs(timer: TimerState, now: Long): Long = if (countsDown(timer)) remainingMs(timer, now) else elapsedMs(timer, now)

    /** 0 at the start of a phase, 1 at the end. A stopwatch fills the ring once an hour. */
    fun progress(timer: TimerState, now: Long): Float {
        if (timer.mode == TimerMode.Idle) return 0f
        if (isStopwatchFocus(timer)) return (elapsedMs(timer, now) % 3_600_000L) / 3_600_000f
        if (timer.totalMs <= 0L) return 0f
        return (1f - remainingMs(timer, now).toFloat() / timer.totalMs.toFloat()).coerceIn(0f, 1f)
    }

    /** Starts a focus round from idle, or resumes a paused phase. */
    fun startOrResume(timer: TimerState, now: Long): TimerState = when {
        timer.mode == TimerMode.Idle && timer.kind == TimerKind.Stopwatch ->
            timer.copy(mode = TimerMode.Focus, running = true, elapsedMs = 0L, resumedAt = now, startedAt = now)
        timer.mode == TimerMode.Idle -> {
            val total = timer.focusMinutes * 60_000L
            timer.copy(mode = TimerMode.Focus, running = true, totalMs = total, remainingMs = total, endsAt = now + total, startedAt = now)
        }
        timer.running -> timer
        isStopwatchFocus(timer) -> timer.copy(running = true, resumedAt = now)
        else -> timer.copy(running = true, endsAt = now + timer.remainingMs)
    }

    fun pause(timer: TimerState, now: Long): TimerState = when {
        timer.mode == TimerMode.Idle || !timer.running -> timer
        isStopwatchFocus(timer) -> timer.copy(running = false, elapsedMs = elapsedMs(timer, now))
        else -> timer.copy(running = false, remainingMs = max(0L, timer.endsAt - now))
    }

    /** Starts the current phase again from the beginning, keeping it running or paused as it was. */
    fun restart(timer: TimerState, now: Long): TimerState = when {
        timer.mode == TimerMode.Idle -> timer
        isStopwatchFocus(timer) -> timer.copy(elapsedMs = 0L, resumedAt = now, startedAt = now)
        else -> timer.copy(
            remainingMs = timer.totalMs,
            endsAt = if (timer.running) now + timer.totalMs else 0L,
            startedAt = if (timer.mode == TimerMode.Focus) now else timer.startedAt,
        )
    }

    /** Ends the round without saving and clears the round counter and any planned session link. */
    fun stop(timer: TimerState): TimerState = idle(timer).copy(completedRounds = 0)

    /** Changes the focus length. While idle the clock face follows the new length straight away. */
    fun withFocusMinutes(timer: TimerState, minutes: Int): TimerState {
        val changed = timer.copy(focusMinutes = minutes.coerceIn(1, 180))
        return if (changed.mode == TimerMode.Idle) idle(changed) else changed
    }

    /** Switches between Pomodoro, Custom and Stopwatch. Only allowed while idle. */
    fun withKind(timer: TimerState, kind: TimerKind): TimerState {
        if (timer.mode != TimerMode.Idle) return timer
        val changed = if (kind == TimerKind.Pomodoro) timer.copy(kind = kind, focusMinutes = 25, breakMinutes = 5) else timer.copy(kind = kind)
        return idle(changed)
    }

    private fun idle(timer: TimerState): TimerState {
        val total = timer.focusMinutes * 60_000L
        return timer.copy(
            mode = TimerMode.Idle, running = false, endsAt = 0L, totalMs = total, remainingMs = total,
            elapsedMs = 0L, resumedAt = 0L, plannedId = "", plannedDate = "",
        )
    }

    /** Moves into the break. With [autoStart] off the break waits, paused, for the user to start it. */
    private fun breakFrom(timer: TimerState, breakStartedAt: Long, autoStart: Boolean): TimerState {
        val total = timer.breakMinutes * 60_000L
        return timer.copy(
            mode = TimerMode.Break,
            running = autoStart,
            totalMs = total,
            remainingMs = total,
            endsAt = if (autoStart) breakStartedAt + total else 0L,
            elapsedMs = 0L,
            resumedAt = 0L,
            startedAt = breakStartedAt,
            plannedId = "",
            plannedDate = "",
        )
    }

    /**
     * Ends a focus round now. Only the minutes that were really studied are saved, and anything
     * under a minute is not saved at all. Returns null when there is no focus round to finish.
     */
    fun finish(timer: TimerState, now: Long, autoStartBreak: Boolean = true): TimerOutcome? {
        if (timer.mode != TimerMode.Focus) return null
        // A stopwatch that was forgotten for days should not turn into one enormous session.
        val minutes = (elapsedMs(timer, now) / 60_000.0).roundToInt().coerceAtMost(MAX_SESSION_MINUTES)
        val counted = if (minutes >= 1) timer.copy(completedRounds = timer.completedRounds + 1) else timer
        val next = if (timer.breakMinutes > 0) breakFrom(counted, now, autoStartBreak) else idle(counted)
        return TimerOutcome(
            timer = next,
            savedMinutes = minutes,
            sessionStartedAt = timer.startedAt,
            message = if (minutes >= 1) "Focus session saved" else "Less than a minute of focus, so nothing was saved",
        )
    }

    /** Skips the rest of a break and goes back to idle. */
    fun skipBreak(timer: TimerState): TimerState = if (timer.mode == TimerMode.Break) idle(timer) else timer

    /**
     * Moves the timer on when a running countdown has reached its end, or returns null when
     * there is nothing to do. It also catches up after the app was closed: a focus round that
     * finished while the app was away is still saved, and its break only starts if that break
     * is not over yet.
     */
    fun advance(timer: TimerState, now: Long, autoStartBreak: Boolean = true): TimerOutcome? {
        if (timer.mode == TimerMode.Idle || !timer.running || isStopwatchFocus(timer) || now < timer.endsAt) return null
        if (timer.mode == TimerMode.Break) {
            return TimerOutcome(timer = idle(timer), message = "Break's over. Ready for another round?")
        }
        val minutes = max(1, (timer.totalMs / 60_000.0).roundToInt())
        val counted = timer.copy(completedRounds = timer.completedRounds + 1)
        val breakMs = timer.breakMinutes * 60_000L
        val next = when {
            timer.breakMinutes <= 0 -> idle(counted)
            autoStartBreak -> if (now < timer.endsAt + breakMs) breakFrom(counted, timer.endsAt, true) else idle(counted)
            // The break waits to be started, but not for ever: after half an hour the offer lapses.
            else -> if (now < timer.endsAt + 30 * 60_000L) breakFrom(counted, timer.endsAt, false) else idle(counted)
        }
        return TimerOutcome(timer = next, savedMinutes = minutes, sessionStartedAt = timer.startedAt, message = "Focus session saved")
    }
}
