package com.rikhotso.studytime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rikhotso.studytime.data.Accent
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.AppDataJson
import com.rikhotso.studytime.data.Availability
import com.rikhotso.studytime.data.AvatarFrame
import com.rikhotso.studytime.data.Challenge
import com.rikhotso.studytime.data.ChallengeType
import com.rikhotso.studytime.data.DAY_NAMES
import com.rikhotso.studytime.data.DashboardCard
import com.rikhotso.studytime.data.EducationLevel
import com.rikhotso.studytime.data.Exam
import com.rikhotso.studytime.data.Friend
import com.rikhotso.studytime.data.Grade
import com.rikhotso.studytime.data.GroupSession
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.PlannedSession
import com.rikhotso.studytime.data.PracticeTest
import com.rikhotso.studytime.data.Priority
import com.rikhotso.studytime.data.Reminder
import com.rikhotso.studytime.data.Repeat
import com.rikhotso.studytime.data.Settings
import com.rikhotso.studytime.data.Storage
import com.rikhotso.studytime.data.StudyGroup
import com.rikhotso.studytime.data.StudyNote
import com.rikhotso.studytime.data.StudySession
import com.rikhotso.studytime.data.StudyTask
import com.rikhotso.studytime.data.Subject
import com.rikhotso.studytime.data.Subtask
import com.rikhotso.studytime.data.TaskCategory
import com.rikhotso.studytime.data.TimerFace
import com.rikhotso.studytime.data.TimerKind
import com.rikhotso.studytime.data.TimerLogic
import com.rikhotso.studytime.data.TimerMode
import com.rikhotso.studytime.data.TimerOutcome
import com.rikhotso.studytime.data.TimetableEntry
import com.rikhotso.studytime.data.Topic
import com.rikhotso.studytime.data.TopicStatus
import com.rikhotso.studytime.data.badges
import com.rikhotso.studytime.data.challengeProgress
import com.rikhotso.studytime.data.dateOf
import com.rikhotso.studytime.data.findClassConflict
import com.rikhotso.studytime.data.findSessionConflict
import com.rikhotso.studytime.data.formatMinuteOfDay
import com.rikhotso.studytime.data.generatePlan
import com.rikhotso.studytime.data.generateRevisionPlan
import com.rikhotso.studytime.data.levelFor
import com.rikhotso.studytime.data.minutesOfDay
import com.rikhotso.studytime.data.missedSessions
import com.rikhotso.studytime.data.newId
import com.rikhotso.studytime.data.nextFreeSlot
import com.rikhotso.studytime.data.parseDate
import com.rikhotso.studytime.data.planReminders
import com.rikhotso.studytime.data.reviewed
import com.rikhotso.studytime.data.sessionsCsv
import com.rikhotso.studytime.data.shortDate
import com.rikhotso.studytime.data.tasksCsv
import com.rikhotso.studytime.data.totalXp
import com.rikhotso.studytime.data.unclaimedAwards
import com.rikhotso.studytime.timer.AlarmScheduler
import com.rikhotso.studytime.timer.ReminderScheduler
import com.rikhotso.studytime.timer.bookTimerAlarms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The single source of truth for the app. Screens read [state] and call the functions below;
 * every change produces a new [AppData], which is shown straight away and saved in the background.
 *
 * Functions that validate input return null when the change was made, or a short message that
 * the dialog shows to the user.
 */
class StudyViewModel(
    private val storage: Storage,
    private val alarms: AlarmScheduler,
    private val reminders: ReminderScheduler? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val messageChannel = Channel<String>(capacity = 32)

    /** Short confirmations for the snackbar, for example "Task added". They wait here until the UI collects them. */
    val messages: Flow<String> = messageChannel.receiveAsFlow()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppData> = _state.asStateFlow()

    private val _summary = MutableStateFlow<String?>(null)

    /** The id of a focus session that has just been saved, while its summary is waiting to be shown. */
    val summary: StateFlow<String?> = _summary.asStateFlow()

    private val saveLock = Mutex()
    private var bookedReminders: List<Reminder>? = null
    private var knownLevel = levelFor(totalXp(_state.value))

    init {
        // Catch up on anything that finished while the app was closed.
        tick()
        settle(silent = true)
        persist()
    }

    private fun load(): AppData {
        val text = storage.read()
        val loaded = AppDataJson.decodeOrNull(text)
        if (loaded == null && !text.isNullOrBlank()) {
            // Never overwrite a save that could not be read without keeping a copy of it.
            storage.keepUnreadable(text)
            say("Your saved data could not be read, so studytime started fresh. A copy of the old file was kept.")
        }
        return loaded ?: AppData()
    }

    private fun update(transform: (AppData) -> AppData) {
        _state.value = transform(_state.value)
        persist()
    }

    /** Writes the save file and books reminders again when they changed. Runs off the main thread. */
    private fun persist() {
        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock {
                val snapshot = _state.value
                storage.write(AppDataJson.encode(snapshot))
                if (reminders != null) {
                    val plan = planReminders(snapshot, clock())
                    if (plan != bookedReminders) {
                        bookedReminders = plan
                        reminders.sync(plan)
                    }
                }
            }
        }
    }

    private fun say(message: String) {
        messageChannel.trySend(message)
    }

    private fun today(): LocalDate = dateOf(clock())

    private fun nowMinute(): Int = Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }

    private fun number(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    // ---------------------------------------------------------------- rewards

    /**
     * Hands out anything that has just been earned: daily and weekly challenges, personal
     * challenges, badges and level ups. Each is recorded once, so nothing is ever paid twice.
     */
    private fun settle(silent: Boolean = false) {
        val today = today()
        val before = _state.value
        val awards = unclaimedAwards(before, today)
        val finished = before.challenges.filter { !it.completed && challengeProgress(it, before, today) >= it.target }
        if (awards.isNotEmpty() || finished.isNotEmpty()) {
            val ids = finished.map { it.id }.toSet()
            update { data ->
                data.copy(
                    awards = data.awards + awards.filter { award -> data.awards.none { it.key == award.key } },
                    challenges = data.challenges.map { if (it.id in ids) it.copy(completed = true, completedDate = today.toString()) else it },
                )
            }
            if (!silent) {
                awards.forEach { say("Challenge complete: ${it.title}. +${it.xp} points") }
                finished.forEach { say("Challenge complete: ${it.name}") }
            }
        }
        val current = _state.value
        val fresh = badges(current, today).filter { it.earned && it.key !in current.seenBadges }
        if (fresh.isNotEmpty()) {
            update { it.copy(seenBadges = (it.seenBadges + fresh.map { badge -> badge.key }).distinct()) }
            if (!silent) fresh.forEach { say("Badge earned: ${it.title}") }
        }
        val level = levelFor(totalXp(_state.value))
        if (level > knownLevel && !silent) say("Level $level reached. Check Rewards for anything new to unlock.")
        knownLevel = level
    }

    // ---------------------------------------------------------------- onboarding and profile

    fun setName(name: String): String? {
        val clean = name.trim().take(40)
        if (clean.isEmpty()) return "Type a name so studytime knows what to call you."
        update { it.copy(profile = it.profile.copy(name = clean)) }
        return null
    }

    fun setEducationLevel(level: EducationLevel) = update { it.copy(profile = it.profile.copy(educationLevel = level)) }

    fun finishOnboarding() {
        update { it.copy(onboarded = true) }
        say("Welcome to studytime, ${_state.value.profile.name}")
    }

    fun updateProfile(
        name: String,
        username: String,
        about: String,
        bio: String,
        level: EducationLevel,
        avatarColor: PastelColor,
        frame: AvatarFrame,
    ): String? {
        val clean = name.trim().take(40)
        if (clean.isEmpty()) return "Type the name studytime should use."
        val handle = username.trim().removePrefix("@").lowercase()
        if (handle.isNotEmpty() && !Regex("[a-z0-9_]{3,20}").matches(handle)) {
            return "A username needs 3 to 20 letters, numbers or underscores."
        }
        val currentLevel = levelFor(totalXp(_state.value))
        if (frame.requiredLevel > currentLevel) return "${frame.label} unlocks at level ${frame.requiredLevel}."
        update {
            it.copy(
                profile = it.profile.copy(
                    name = clean,
                    username = handle,
                    about = about.trim().take(60),
                    bio = bio.trim().take(200),
                    educationLevel = level,
                    avatarColor = avatarColor,
                    avatarFrame = frame,
                ),
            )
        }
        return null
    }

    /** Remembers the copied photo at [path], or goes back to initials when it is blank. */
    fun setProfilePicture(path: String) {
        val old = _state.value.profile.picturePath
        update { it.copy(profile = it.profile.copy(picturePath = path)) }
        if (old.isNotEmpty() && old != path) {
            try {
                File(old).delete()
            } catch (e: Exception) {
                // The old photo stays behind, which only costs a little storage.
            }
        }
    }

    fun saveGoals(dailyMinutes: Int, weeklyMinutes: Int, monthlyMinutes: Int, streakMinimumMinutes: Int): String? {
        val daily = dailyMinutes.coerceIn(15, 480)
        if (weeklyMinutes < daily) return "The weekly goal should be at least one day's goal."
        if (monthlyMinutes < weeklyMinutes) return "The monthly goal should be at least one week's goal."
        update {
            it.copy(
                profile = it.profile.copy(
                    dailyGoalMinutes = daily,
                    weeklyGoalMinutes = weeklyMinutes.coerceIn(30, 3360),
                    monthlyGoalMinutes = monthlyMinutes.coerceIn(60, 14400),
                    streakMinimumMinutes = streakMinimumMinutes.coerceIn(5, 240),
                ),
            )
        }
        settle()
        return null
    }

    /** Kept for the quick slider on the profile. Weekly and monthly goals grow with it if they would fall behind. */
    fun setDailyGoal(minutes: Int) {
        val daily = minutes.coerceIn(15, 480)
        update {
            it.copy(
                profile = it.profile.copy(
                    dailyGoalMinutes = daily,
                    weeklyGoalMinutes = it.profile.weeklyGoalMinutes.coerceAtLeast(daily),
                    monthlyGoalMinutes = it.profile.monthlyGoalMinutes.coerceAtLeast(daily),
                ),
            )
        }
        settle()
        say("Daily goal updated")
    }

    fun saveAvailability(days: List<Availability>): String? {
        val bad = days.firstOrNull { it.enabled && minutesOfDay(it.end) <= minutesOfDay(it.start) }
        if (bad != null) return "On ${DAY_NAMES[bad.dayOfWeek - 1]} the end time needs to be after the start time."
        if (days.none { it.enabled }) return "Keep at least one day switched on so sessions can be planned."
        update { it.copy(profile = it.profile.copy(availability = days.sortedBy { day -> day.dayOfWeek })) }
        return null
    }

    // ---------------------------------------------------------------- settings

    fun updateSettings(change: (Settings) -> Settings) {
        val before = _state.value
        update { it.copy(settings = change(it.settings)) }
        val after = _state.value
        // The timer alarm carries the sound and notification choice, so book it again when those change.
        if (before.settings.timerSound != after.settings.timerSound || before.settings.notifyTimer != after.settings.notifyTimer ||
            before.settings.autoStartBreak != after.settings.autoStartBreak
        ) {
            bookTimerAlarms(alarms, after.timer, after.settings)
        }
    }

    fun setAccent(accent: Accent): String? {
        val level = levelFor(totalXp(_state.value))
        if (accent.requiredLevel > level) return "${accent.label} unlocks at level ${accent.requiredLevel}."
        updateSettings { it.copy(accent = accent) }
        return null
    }

    fun setTimerFace(face: TimerFace): String? {
        val level = levelFor(totalXp(_state.value))
        if (face.requiredLevel > level) return "${face.label} unlocks at level ${face.requiredLevel}."
        updateSettings { it.copy(timerFace = face) }
        return null
    }

    /** Moves a Home card one place up or down. */
    fun moveCard(card: DashboardCard, up: Boolean) = updateSettings { settings ->
        val order = settings.dashboardOrder.toMutableList()
        val index = order.indexOf(card)
        val target = if (up) index - 1 else index + 1
        if (index < 0 || target !in order.indices) return@updateSettings settings
        order[index] = order[target]
        order[target] = card
        settings.copy(dashboardOrder = order)
    }

    fun setCardHidden(card: DashboardCard, hidden: Boolean) = updateSettings { settings ->
        settings.copy(hiddenCards = if (hidden) (settings.hiddenCards + card).distinct() else settings.hiddenCards - card)
    }

    // ---------------------------------------------------------------- data: export, import, reset

    fun backupJson(): String = AppDataJson.encode(_state.value)

    fun sessionsCsvText(): String = sessionsCsv(_state.value)

    fun tasksCsvText(): String = tasksCsv(_state.value)

    fun exported(what: String) = say("$what saved")

    fun exportFailed() = say("That file could not be written. Try another folder.")

    /** Replaces everything with the contents of a backup made by "Export backup". */
    fun importBackup(text: String?): String? {
        val loaded = AppDataJson.decodeOrNull(text) ?: return "That file is not a studytime backup."
        alarms.cancelAll()
        _summary.value = null
        // A running timer or a photo path from another phone would not make sense here.
        val pictureExists = loaded.profile.picturePath.isNotEmpty() && try {
            File(loaded.profile.picturePath).exists()
        } catch (e: Exception) {
            false
        }
        update {
            loaded.copy(
                onboarded = true,
                timer = TimerLogic.stop(loaded.timer),
                profile = if (pictureExists) loaded.profile else loaded.profile.copy(picturePath = ""),
            )
        }
        knownLevel = levelFor(totalXp(_state.value))
        settle(silent = true)
        say("Backup restored")
        return null
    }

    /** Erases everything and goes back to the welcome steps. */
    fun resetAll() {
        alarms.cancelAll()
        _summary.value = null
        val picture = _state.value.profile.picturePath
        update { AppData() }
        knownLevel = 1
        if (picture.isNotEmpty()) {
            try {
                File(picture).delete()
            } catch (e: Exception) {
                // Nothing else to clean up.
            }
        }
    }

    // ---------------------------------------------------------------- subjects and classes

    fun addSubject(name: String, teacher: String, color: PastelColor, goalMinutesPerDay: Int = 60): String? =
        saveSubject(null, name, teacher, color, goalMinutesPerDay)

    /** Adds a subject, or changes the one with [id]. A rename or new colour is carried into its classes and history. */
    fun saveSubject(id: String?, name: String, teacher: String, color: PastelColor, goalMinutesPerDay: Int): String? {
        val clean = name.trim().take(40)
        if (clean.isEmpty()) return "Give the subject a name."
        if (_state.value.subjects.any { it.id != id && it.name.equals(clean, ignoreCase = true) }) {
            return "You already have a subject with that name."
        }
        val goal = goalMinutesPerDay.coerceIn(15, 240)
        if (id == null) {
            val subject = Subject(newId(), clean, color, teacher.trim().take(40), goal)
            update { it.copy(subjects = it.subjects + subject) }
        } else {
            update { data ->
                data.copy(
                    subjects = data.subjects.map { if (it.id == id) it.copy(name = clean, teacher = teacher.trim().take(40), color = color, goalMinutesPerDay = goal) else it },
                    entries = data.entries.map { if (it.subjectId == id) it.copy(subjectName = clean, color = color) else it },
                    sessions = data.sessions.map { if (it.subjectId == id) it.copy(subjectName = clean, color = color) else it },
                    grades = data.grades.map { if (it.subjectId == id) it.copy(subjectName = clean, color = color) else it },
                )
            }
        }
        return null
    }

    /** Moves a subject one place up or down in the list. */
    fun moveSubject(id: String, up: Boolean) = update { data ->
        val list = data.subjects.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        val target = if (up) index - 1 else index + 1
        if (index < 0 || target !in list.indices) return@update data
        val moved = list[index]
        list[index] = list[target]
        list[target] = moved
        data.copy(subjects = list)
    }

    /**
     * Deletes a subject with its classes, topics and practice tests. Study history and grades are
     * kept, and tasks, notes, exams and planned sessions stay but lose the subject.
     */
    fun removeSubject(id: String) {
        update { data ->
            data.copy(
                subjects = data.subjects.filterNot { it.id == id },
                entries = data.entries.filterNot { it.subjectId == id },
                topics = data.topics.filterNot { it.subjectId == id },
                practiceTests = data.practiceTests.filterNot { it.subjectId == id },
                tasks = data.tasks.map { if (it.subjectId == id) it.copy(subjectId = "") else it },
                notes = data.notes.map { if (it.subjectId == id) it.copy(subjectId = "", topicId = "") else it },
                exams = data.exams.map { if (it.subjectId == id) it.copy(subjectId = "") else it },
                plannedSessions = data.plannedSessions.map { if (it.subjectId == id) it.copy(subjectId = "", topicId = "") else it },
                timer = if (data.timer.subjectId == id) data.timer.copy(subjectId = "", topicId = "") else data.timer,
            )
        }
    }

    fun addEntry(subjectId: String, dayOfWeek: Int, startTime: String, endTime: String, room: String): String? =
        saveEntry(null, subjectId, dayOfWeek, startTime, endTime, room)

    /** Adds a weekly class, or changes the one with [id]. Classes on the same day may not overlap. */
    fun saveEntry(id: String?, subjectId: String, dayOfWeek: Int, startTime: String, endTime: String, room: String): String? {
        val data = _state.value
        val subject = data.subject(subjectId) ?: return "Pick a subject for this class."
        if (minutesOfDay(endTime) <= minutesOfDay(startTime)) return "The end time needs to be after the start time."
        val day = dayOfWeek.coerceIn(1, 7)
        findClassConflict(data, day, startTime, endTime, ignoreEntryId = id.orEmpty())?.let { return it }
        val entry = TimetableEntry(
            id = id ?: newId(),
            subjectId = subject.id,
            subjectName = subject.name,
            color = subject.color,
            dayOfWeek = day,
            startTime = startTime,
            endTime = endTime,
            room = room.trim().take(30),
        )
        update { current ->
            current.copy(entries = if (id == null) current.entries + entry else current.entries.map { if (it.id == id) entry else it })
        }
        return null
    }

    fun removeEntry(id: String) = update { data -> data.copy(entries = data.entries.filterNot { it.id == id }) }

    // ---------------------------------------------------------------- planned sessions

    /** Adds a planned study session, or changes the one with [id]. Refuses a time that clashes with something else. */
    fun savePlanned(
        id: String?,
        title: String,
        subjectId: String,
        date: String,
        startTime: String,
        durationMinutes: Int,
        repeat: Repeat,
        repeatDays: List<Int>,
        taskId: String = "",
        topicId: String = "",
    ): String? {
        val data = _state.value
        if (parseDate(date) == null) return "Pick a date for this session."
        val duration = durationMinutes.coerceIn(10, 240)
        if (minutesOfDay(startTime) + duration > 24 * 60) return "This session would run past midnight. Start it earlier or make it shorter."
        if (repeat == Repeat.Days && repeatDays.isEmpty()) return "Pick at least one day for it to repeat on."
        val existing = data.plannedSessions.firstOrNull { it.id == id }
        val session = PlannedSession(
            id = id ?: newId(),
            title = title.trim().take(60).ifBlank { if (subjectId.isNotEmpty()) "Study ${data.subjectName(subjectId)}" else "Study session" },
            subjectId = subjectId,
            date = date,
            startTime = startTime,
            durationMinutes = duration,
            repeat = repeat,
            repeatDays = if (repeat == Repeat.Days) repeatDays.distinct().sorted() else emptyList(),
            taskId = taskId,
            topicId = topicId,
            generated = false,
            completedDates = existing?.completedDates.orEmpty(),
            skippedDates = existing?.skippedDates.orEmpty(),
        )
        findSessionConflict(data, session)?.let { return it }
        update { current ->
            current.copy(
                plannedSessions = if (existing == null) current.plannedSessions + session else current.plannedSessions.map { if (it.id == session.id) session else it },
            )
        }
        return null
    }

    /** Deletes a planned session and, when it repeats, every repeat of it. */
    fun removePlanned(id: String) = update { data -> data.copy(plannedSessions = data.plannedSessions.filterNot { it.id == id }) }

    /** Removes one day of a repeating session, or the whole session when it does not repeat. */
    fun skipPlanned(id: String, day: String) = update { data ->
        data.copy(
            plannedSessions = data.plannedSessions.mapNotNull { session ->
                when {
                    session.id != id -> session
                    session.repeat == Repeat.None -> null
                    else -> session.copy(skippedDates = (session.skippedDates + day).distinct())
                }
            },
        )
    }

    fun togglePlannedDone(id: String, day: String) {
        update { data ->
            data.copy(
                plannedSessions = data.plannedSessions.map { session ->
                    if (session.id != id) session
                    else session.copy(completedDates = if (day in session.completedDates) session.completedDates - day else session.completedDates + day)
                },
            )
        }
        settle()
    }

    /**
     * Moves one occurrence to a new day and time. A session that repeats keeps its other days:
     * only [fromDay] is taken out and a single session is added at the new time.
     */
    fun movePlanned(id: String, fromDay: String, toDay: String, startTime: String): String? {
        val data = _state.value
        val session = data.plannedSessions.firstOrNull { it.id == id } ?: return "That session no longer exists."
        if (parseDate(toDay) == null) return "Pick a date for this session."
        if (minutesOfDay(startTime) + session.durationMinutes > 24 * 60) return "This session would run past midnight."
        if (session.repeat == Repeat.None) {
            val moved = session.copy(date = toDay, startTime = startTime)
            findSessionConflict(data, moved)?.let { return it }
            update { current -> current.copy(plannedSessions = current.plannedSessions.map { if (it.id == id) moved else it }) }
        } else {
            val single = session.copy(
                id = newId(), date = toDay, startTime = startTime, repeat = Repeat.None, repeatDays = emptyList(),
                completedDates = emptyList(), skippedDates = emptyList(),
            )
            val withoutOld = data.copy(
                plannedSessions = data.plannedSessions.map { if (it.id == id) it.copy(skippedDates = (it.skippedDates + fromDay).distinct()) else it },
            )
            findSessionConflict(withoutOld, single)?.let { return it }
            update { withoutOld.copy(plannedSessions = withoutOld.plannedSessions + single) }
        }
        return null
    }

    /** Makes a one-off copy of a session in the next free time inside the user's availability. */
    fun duplicatePlanned(id: String): String? {
        val data = _state.value
        val session = data.plannedSessions.firstOrNull { it.id == id } ?: return "That session no longer exists."
        val slot = nextFreeSlot(data, today(), nowMinute(), session.durationMinutes) ?: return "There is no free time in the next two weeks. Widen your study hours in Settings."
        val copy = session.copy(
            id = newId(), date = slot.first.toString(), startTime = formatMinuteOfDay(slot.second), repeat = Repeat.None,
            repeatDays = emptyList(), completedDates = emptyList(), skippedDates = emptyList(), generated = false,
        )
        update { it.copy(plannedSessions = it.plannedSessions + copy) }
        say("Copied to ${shortDate(copy.date)} at ${copy.startTime}")
        return null
    }

    /** Moves a missed session into the next free time. Returns the moved session, or null when there is no room. */
    private fun moveToFreeTime(id: String, missedDay: String): PlannedSession? {
        val data = _state.value
        val session = data.plannedSessions.firstOrNull { it.id == id } ?: return null
        // Look for room as if the missed one was already gone, so it can take its own slot later today.
        val without = data.copy(
            plannedSessions = data.plannedSessions.mapNotNull {
                when {
                    it.id != id -> it
                    it.repeat == Repeat.None -> null
                    else -> it.copy(skippedDates = (it.skippedDates + missedDay).distinct())
                }
            },
        )
        val slot = nextFreeSlot(without, today(), nowMinute(), session.durationMinutes) ?: return null
        val moved = session.copy(
            id = if (session.repeat == Repeat.None) session.id else newId(),
            date = slot.first.toString(), startTime = formatMinuteOfDay(slot.second), repeat = Repeat.None, repeatDays = emptyList(),
            completedDates = emptyList(), skippedDates = emptyList(),
        )
        update { without.copy(plannedSessions = without.plannedSessions + moved) }
        return moved
    }

    /** Moves one missed session into the next free time. Returns a message when there is no room. */
    fun rescheduleMissed(id: String, missedDay: String): String? {
        val moved = moveToFreeTime(id, missedDay)
            ?: return "There is no free time in the next two weeks. Widen your study hours in Settings."
        say("${moved.title} moved to ${shortDate(moved.date)} at ${moved.startTime}")
        return null
    }

    fun rescheduleAllMissed() {
        val missed = missedSessions(_state.value, today(), nowMinute())
        val moved = missed.count { moveToFreeTime(it.session.id, it.day.toString()) != null }
        val left = missed.size - moved
        say(
            when {
                missed.isEmpty() -> "Nothing to reschedule"
                left == 0 -> "$moved session${if (moved == 1) "" else "s"} moved into free time"
                else -> "$moved moved. $left did not fit in the next two weeks."
            },
        )
    }

    /**
     * Replaces the upcoming sessions made by the generator with a fresh plan for the next [days]
     * days. Sessions the user added by hand are never touched.
     */
    fun generateStudyPlan(days: Int, sessionMinutes: Int): String? {
        val data = _state.value
        if (data.subjects.isEmpty()) return "Add at least one subject first, so there is something to plan."
        val today = today()
        val kept = data.plannedSessions.filterNot { it.generated && it.completedDates.isEmpty() && it.date >= today.toString() }
        val base = data.copy(plannedSessions = kept)
        val plan = generatePlan(base, today, days.coerceIn(1, 28), sessionMinutes, nowMinute())
        if (plan.isEmpty()) return "No free time was found. Check your study hours and daily goal in Settings."
        update { base.copy(plannedSessions = kept + plan) }
        say("${plan.size} session${if (plan.size == 1) "" else "s"} planned across your free time")
        return null
    }

    /** Plans revision sessions for one exam, weakest topics first, in free time before the exam. */
    fun generateRevision(examId: String, sessionMinutes: Int): String? {
        val data = _state.value
        val exam = data.exams.firstOrNull { it.id == examId } ?: return "That exam no longer exists."
        if (exam.subjectId.isEmpty()) return "Link this exam to a subject first, so its topics can be planned."
        val result = generateRevisionPlan(data, exam, today(), sessionMinutes, nowMinute())
        if (result.topicsTotal == 0) return "Add revision topics to ${data.subjectName(exam.subjectId)} first. Each topic gets its own session."
        if (result.sessions.isEmpty()) return "No free time was found before the exam. Check your study hours in Settings."
        update { it.copy(plannedSessions = it.plannedSessions + result.sessions) }
        say(
            if (result.topicsCovered < result.topicsTotal) "Planned ${result.topicsCovered} of ${result.topicsTotal} topics. The rest did not fit before the exam."
            else "Planned a session for each of the ${result.topicsTotal} topics",
        )
        return null
    }

    // ---------------------------------------------------------------- tasks

    fun saveTask(
        id: String?,
        title: String,
        subjectId: String,
        category: TaskCategory,
        priority: Priority,
        dueDate: String,
        dueTime: String,
        estimateText: String,
        notes: String,
    ): String? {
        val clean = title.trim().take(80)
        if (clean.isEmpty()) return "Give the task a title."
        if (dueDate.isNotEmpty() && parseDate(dueDate) == null) return "Pick a valid due date."
        val estimate = if (estimateText.isBlank()) 0 else estimateText.trim().toIntOrNull()
        if (estimate == null || estimate < 0 || estimate > 600) return "The estimate needs to be a number of minutes from 0 to 600."
        val existing = _state.value.tasks.firstOrNull { it.id == id }
        val task = (existing ?: StudyTask(id = newId(), title = clean, createdAt = clock())).copy(
            title = clean,
            subjectId = subjectId,
            category = category,
            priority = priority,
            dueDate = dueDate,
            dueTime = if (dueDate.isEmpty()) "" else dueTime,
            estimateMinutes = estimate,
            notes = notes.trim().take(500),
        )
        update { data -> data.copy(tasks = if (existing == null) listOf(task) + data.tasks else data.tasks.map { if (it.id == task.id) task else it }) }
        return null
    }

    /** Ticks a task off or back on. Points are given the first time a task is finished, and only once. */
    fun toggleTask(id: String) {
        val task = _state.value.tasks.firstOrNull { it.id == id } ?: return
        val nowDone = !task.done
        update { data ->
            data.copy(
                tasks = data.tasks.map { if (it.id == id) it.copy(done = nowDone, completedAt = if (nowDone) clock() else 0L) else it },
                rewardedTaskIds = if (nowDone && id !in data.rewardedTaskIds) data.rewardedTaskIds + id else data.rewardedTaskIds,
                timer = if (nowDone && data.timer.taskId == id && data.timer.mode == TimerMode.Idle) data.timer.copy(taskId = "") else data.timer,
            )
        }
        settle()
    }

    fun removeTask(id: String) = update { data ->
        data.copy(
            tasks = data.tasks.filterNot { it.id == id },
            timer = if (data.timer.taskId == id) data.timer.copy(taskId = "") else data.timer,
            plannedSessions = data.plannedSessions.map { if (it.taskId == id) it.copy(taskId = "") else it },
        )
    }

    fun addSubtask(taskId: String, title: String): String? {
        val clean = title.trim().take(80)
        if (clean.isEmpty()) return "Type the step first."
        update { data -> data.copy(tasks = data.tasks.map { if (it.id == taskId) it.copy(subtasks = it.subtasks + Subtask(newId(), clean)) else it }) }
        return null
    }

    fun toggleSubtask(taskId: String, subtaskId: String) = update { data ->
        data.copy(
            tasks = data.tasks.map { task ->
                if (task.id != taskId) task else task.copy(subtasks = task.subtasks.map { if (it.id == subtaskId) it.copy(done = !it.done) else it })
            },
        )
    }

    fun removeSubtask(taskId: String, subtaskId: String) = update { data ->
        data.copy(tasks = data.tasks.map { if (it.id == taskId) it.copy(subtasks = it.subtasks.filterNot { step -> step.id == subtaskId }) else it })
    }

    // ---------------------------------------------------------------- exams, topics, notes, tests

    fun saveExam(id: String?, subjectId: String, title: String, date: String, time: String, location: String): String? {
        if (parseDate(date) == null) return "Pick the date of the exam."
        val clean = title.trim().take(60).ifBlank { if (subjectId.isNotEmpty()) "${_state.value.subjectName(subjectId)} exam" else "" }
        if (clean.isEmpty()) return "Name the exam, or pick its subject."
        val exam = Exam(id ?: newId(), subjectId, clean, date, time, location.trim().take(60))
        update { data -> data.copy(exams = if (id == null) data.exams + exam else data.exams.map { if (it.id == id) exam else it }) }
        return null
    }

    fun removeExam(id: String) = update { data -> data.copy(exams = data.exams.filterNot { it.id == id }) }

    /** Adds one topic per line, so a whole syllabus can be pasted in at once. */
    fun addTopics(subjectId: String, text: String): String? {
        if (_state.value.subject(subjectId) == null) return "That subject no longer exists."
        val existing = _state.value.topics.filter { it.subjectId == subjectId }.map { it.name.lowercase() }
        val names = text.split('\n', ';').map { it.trim().take(80) }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        if (names.isEmpty()) return "Type at least one topic."
        val fresh = names.filter { it.lowercase() !in existing }
        if (fresh.isEmpty()) return "Those topics are already on the list."
        update { it.copy(topics = it.topics + fresh.map { name -> Topic(newId(), subjectId, name) }) }
        return null
    }

    fun renameTopic(id: String, name: String): String? {
        val clean = name.trim().take(80)
        if (clean.isEmpty()) return "Give the topic a name."
        update { data -> data.copy(topics = data.topics.map { if (it.id == id) it.copy(name = clean) else it }) }
        return null
    }

    fun setTopicStatus(id: String, status: TopicStatus) {
        update { data -> data.copy(topics = data.topics.map { if (it.id == id) it.copy(status = status) else it }) }
        settle()
    }

    fun setTopicConfidence(id: String, confidence: Int) = update { data ->
        data.copy(topics = data.topics.map { if (it.id == id) it.copy(confidence = confidence.coerceIn(0, 5)) else it })
    }

    /** Marks a topic as reviewed today and books its next spaced review. */
    fun reviewTopic(id: String) {
        val topic = _state.value.topics.firstOrNull { it.id == id } ?: return
        val next = reviewed(topic, today())
        update { data -> data.copy(topics = data.topics.map { if (it.id == id) next else it }) }
        say("${topic.name} reviewed. Next review on ${shortDate(next.nextReview)}.")
        settle()
    }

    /** Stops the spaced reviews for a topic. */
    fun clearTopicReviews(id: String) = update { data ->
        data.copy(topics = data.topics.map { if (it.id == id) it.copy(nextReview = "") else it })
    }

    fun removeTopic(id: String) = update { data ->
        data.copy(
            topics = data.topics.filterNot { it.id == id },
            notes = data.notes.map { if (it.topicId == id) it.copy(topicId = "") else it },
            plannedSessions = data.plannedSessions.map { if (it.topicId == id) it.copy(topicId = "") else it },
            timer = if (data.timer.topicId == id) data.timer.copy(topicId = "") else data.timer,
        )
    }

    fun saveNote(id: String?, subjectId: String, topicId: String, title: String, body: String): String? {
        val cleanBody = body.trim().take(20_000)
        val clean = title.trim().take(80).ifBlank { cleanBody.lineSequence().firstOrNull().orEmpty().take(40) }
        if (clean.isEmpty()) return "Write a title or some text first."
        val note = StudyNote(id ?: newId(), subjectId, topicId, clean, cleanBody, clock())
        update { data -> data.copy(notes = if (id == null) listOf(note) + data.notes else data.notes.map { if (it.id == id) note else it }) }
        return null
    }

    fun removeNote(id: String) = update { data -> data.copy(notes = data.notes.filterNot { it.id == id }) }

    fun addPracticeTest(subjectId: String, title: String, scoreText: String, maxText: String, date: String): String? {
        if (_state.value.subject(subjectId) == null) return "Pick a subject for this practice test."
        val clean = title.trim().take(60)
        if (clean.isEmpty()) return "Name the practice test, for example 2024 Paper 1."
        val max = number(maxText)
        if (max == null || max <= 0.0) return "The maximum score needs to be more than zero."
        val score = number(scoreText)
        if (score == null || score < 0.0 || score > max) return "The score needs to be between 0 and ${maxText.trim()}."
        val test = PracticeTest(newId(), subjectId, clean, score, max, if (parseDate(date) != null) date else today().toString())
        update { it.copy(practiceTests = it.practiceTests + test) }
        return null
    }

    fun removePracticeTest(id: String) = update { data -> data.copy(practiceTests = data.practiceTests.filterNot { it.id == id }) }

    // ---------------------------------------------------------------- friends (kept on this phone)

    fun addFriend(name: String, email: String, color: PastelColor, notes: String): String? {
        val clean = name.trim().take(40)
        if (clean.isEmpty()) return "Type your friend's name."
        val friend = Friend(newId(), clean, email.trim().take(80), color, notes.trim().take(120))
        update { it.copy(friends = it.friends + friend) }
        return null
    }

    fun removeFriend(id: String) = update { data -> data.copy(friends = data.friends.filterNot { it.id == id }) }

    fun addGroup(name: String, subject: String, color: PastelColor, members: List<String>, description: String): String? {
        val clean = name.trim().take(40)
        if (clean.isEmpty()) return "Give the group a name."
        val group = StudyGroup(newId(), clean, color, subject.trim().take(40), members, description.trim().take(160))
        update { it.copy(groups = listOf(group) + it.groups) }
        return null
    }

    fun removeGroup(id: String) = update { data -> data.copy(groups = data.groups.filterNot { it.id == id }) }

    fun addGroupSession(groupId: String, topic: String, date: String, time: String, durationMinutes: Int): String? {
        val group = _state.value.groups.firstOrNull { it.id == groupId } ?: return "Pick a group for this session."
        if (parseDate(date) == null) return "Pick a date for this session."
        val session = GroupSession(
            id = newId(),
            groupId = group.id,
            groupName = group.name,
            color = group.color,
            topic = topic.trim().take(60).ifBlank { "Study together" },
            scheduledDate = date,
            startTime = time,
            durationMinutes = durationMinutes.coerceIn(30, 180),
        )
        update { it.copy(groupSessions = it.groupSessions + session) }
        return null
    }

    fun removeGroupSession(id: String) =
        update { data -> data.copy(groupSessions = data.groupSessions.filterNot { it.id == id }) }

    // ---------------------------------------------------------------- grades and personal challenges

    fun addGrade(subjectId: String, assessment: String, scoreText: String, maxText: String, date: String, term: String): String? {
        val subject = _state.value.subject(subjectId) ?: return "Pick a subject for this grade."
        val name = assessment.trim().take(60)
        if (name.isEmpty()) return "Name the assessment, for example Term 1 Test."
        val max = number(maxText)
        if (max == null || max <= 0.0) return "The maximum score needs to be more than zero."
        val score = number(scoreText)
        if (score == null || score < 0.0 || score > max) return "The score needs to be between 0 and ${maxText.trim()}."
        val grade = Grade(
            id = newId(),
            subjectId = subject.id,
            subjectName = subject.name,
            color = subject.color,
            assessmentName = name,
            score = score,
            maxScore = max,
            date = if (parseDate(date) != null) date else today().toString(),
            term = term.trim().take(20),
        )
        update { it.copy(grades = listOf(grade) + it.grades) }
        return null
    }

    fun removeGrade(id: String) = update { data -> data.copy(grades = data.grades.filterNot { it.id == id }) }

    fun addChallenge(name: String, type: ChallengeType, targetText: String, reward: String): String? {
        val clean = name.trim().take(50)
        if (clean.isEmpty()) return "Give the challenge a name."
        val target = targetText.trim().toIntOrNull()
        if (target == null || target < 1 || target > 1000) return "The target needs to be a whole number from 1 to 1000."
        val challenge = Challenge(newId(), clean, type, target, reward.trim().take(40), createdAt = clock())
        update { it.copy(challenges = listOf(challenge) + it.challenges) }
        return null
    }

    fun removeChallenge(id: String) =
        update { data -> data.copy(challenges = data.challenges.filterNot { it.id == id }) }

    /** Adds one step to a milestone challenge. */
    fun stepMilestone(id: String) {
        update { data ->
            data.copy(
                challenges = data.challenges.map {
                    if (it.id == id && !it.completed) it.copy(manualProgress = (it.manualProgress + 1).coerceAtMost(it.target)) else it
                },
            )
        }
        settle()
    }

    // ---------------------------------------------------------------- focus timer

    private fun rebook() {
        val data = _state.value
        bookTimerAlarms(alarms, data.timer, data.settings)
    }

    /** Pomodoro, Custom or Stopwatch. Only changes while no round is in progress. */
    fun setTimerKind(kind: TimerKind) = update { it.copy(timer = TimerLogic.withKind(it.timer, kind)) }

    fun startTimer() {
        update { it.copy(timer = TimerLogic.startOrResume(it.timer, clock())) }
        rebook()
    }

    fun pauseTimer() {
        update { it.copy(timer = TimerLogic.pause(it.timer, clock())) }
        rebook()
    }

    /** Starts the current focus round or break again from the beginning. Nothing is saved. */
    fun restartTimer() {
        update { it.copy(timer = TimerLogic.restart(it.timer, clock())) }
        rebook()
    }

    /** Ends the focus round now, saves the time that was really studied and offers the break. */
    fun finishTimer() {
        val data = _state.value
        val outcome = TimerLogic.finish(data.timer, clock(), data.settings.autoStartBreak) ?: return
        applyOutcome(outcome)
        rebook()
    }

    /** Throws the current round away without saving it. */
    fun stopTimer() {
        update { it.copy(timer = TimerLogic.stop(it.timer)) }
        rebook()
    }

    fun skipBreak() {
        update { it.copy(timer = TimerLogic.skipBreak(it.timer)) }
        rebook()
    }

    fun setFocusMinutes(minutes: Int) = update { data ->
        val timer = if (data.timer.kind == TimerKind.Pomodoro && data.timer.mode == TimerMode.Idle) data.timer.copy(kind = TimerKind.Custom) else data.timer
        data.copy(timer = TimerLogic.withFocusMinutes(timer, minutes))
    }

    fun setBreakMinutes(minutes: Int) {
        update { data ->
            val timer = if (data.timer.kind == TimerKind.Pomodoro && data.timer.mode == TimerMode.Idle) data.timer.copy(kind = TimerKind.Custom) else data.timer
            data.copy(timer = timer.copy(breakMinutes = minutes.coerceIn(0, 60)))
        }
        rebook()
    }

    /** Sets what the next session is for. A task or topic from another subject is cleared. */
    fun setTimerSubject(subjectId: String) = update { data ->
        val timer = data.timer
        val taskFits = data.tasks.any { it.id == timer.taskId && (it.subjectId == subjectId || it.subjectId.isEmpty()) }
        val topicFits = data.topics.any { it.id == timer.topicId && it.subjectId == subjectId }
        data.copy(timer = timer.copy(subjectId = subjectId, taskId = if (taskFits) timer.taskId else "", topicId = if (topicFits) timer.topicId else ""))
    }

    /** Links the next session to a task or to a revision topic. Blank ids clear the link. */
    fun setTimerLink(taskId: String, topicId: String) = update { it.copy(timer = it.timer.copy(taskId = taskId, topicId = topicId)) }

    /** Gets the timer ready for a task. Returns a message when a round is already in progress. */
    fun prepareTimerForTask(taskId: String): String? {
        val data = _state.value
        val task = data.tasks.firstOrNull { it.id == taskId } ?: return "That task no longer exists."
        if (data.timer.mode != TimerMode.Idle) return "Finish or end the current round first."
        // A task without a subject keeps whatever subject is already picked.
        update { it.copy(timer = it.timer.copy(subjectId = task.subjectId.ifEmpty { it.timer.subjectId }, taskId = task.id, topicId = "", plannedId = "", plannedDate = "")) }
        return null
    }

    fun prepareTimerForTopic(topicId: String): String? {
        val data = _state.value
        val topic = data.topics.firstOrNull { it.id == topicId } ?: return "That topic no longer exists."
        if (data.timer.mode != TimerMode.Idle) return "Finish or end the current round first."
        update { it.copy(timer = it.timer.copy(subjectId = topic.subjectId, taskId = "", topicId = topic.id, plannedId = "", plannedDate = "")) }
        return null
    }

    /** Starts a focus round for a planned session. Finishing the round ticks the session off for [day]. */
    fun startPlanned(plannedId: String, day: String): String? {
        val data = _state.value
        val session = data.plannedSessions.firstOrNull { it.id == plannedId } ?: return "That session no longer exists."
        if (data.timer.mode != TimerMode.Idle) return "Finish or end the current round first."
        val prepared = TimerLogic.withFocusMinutes(data.timer.copy(kind = TimerKind.Custom), session.durationMinutes).copy(
            subjectId = session.subjectId,
            taskId = if (data.tasks.any { it.id == session.taskId && !it.done }) session.taskId else "",
            topicId = if (data.topics.any { it.id == session.topicId }) session.topicId else "",
            plannedId = session.id,
            plannedDate = day,
        )
        update { it.copy(timer = TimerLogic.startOrResume(prepared, clock())) }
        rebook()
        return null
    }

    /**
     * Called a few times a second while the app is open, and once at launch. When a countdown has
     * reached its end this saves the session and moves the timer on.
     */
    fun tick() {
        // A long absence can cover both the focus round and its break, so loop until settled.
        repeat(3) {
            val data = _state.value
            val outcome = TimerLogic.advance(data.timer, clock(), data.settings.autoStartBreak) ?: return
            applyOutcome(outcome)
            rebook()
        }
    }

    /** Saves the finished focus round as one session. The same round can never be saved twice. */
    private fun applyOutcome(outcome: TimerOutcome) {
        val before = _state.value
        val old = before.timer
        val alreadySaved = before.sessions.any { it.startedAt == outcome.sessionStartedAt }
        if (outcome.savedMinutes < 1 || alreadySaved) {
            update { it.copy(timer = outcome.timer) }
            if (outcome.savedMinutes < 1 && outcome.message.isNotEmpty()) say(outcome.message)
            return
        }
        val subject = before.subject(old.subjectId)
        val task = before.tasks.firstOrNull { it.id == old.taskId }
        val topic = before.topics.firstOrNull { it.id == old.topicId }
        val session = StudySession(
            id = newId(),
            subjectId = subject?.id.orEmpty(),
            subjectName = subject?.name ?: "General study",
            color = subject?.color ?: PastelColor.Lavender,
            durationMinutes = outcome.savedMinutes,
            startedAt = outcome.sessionStartedAt,
            taskId = task?.id.orEmpty(),
            taskTitle = task?.title.orEmpty(),
            topicId = topic?.id.orEmpty(),
            topicName = topic?.name.orEmpty(),
        )
        update { data ->
            data.copy(
                sessions = data.sessions + session,
                timer = outcome.timer,
                plannedSessions = data.plannedSessions.map { planned ->
                    if (planned.id == old.plannedId && old.plannedDate.isNotEmpty() && old.plannedDate !in planned.completedDates) {
                        planned.copy(completedDates = planned.completedDates + old.plannedDate)
                    } else {
                        planned
                    }
                },
            )
        }
        _summary.value = session.id
        settle()
    }

    /** Adds or changes the note on a saved session. The session itself is never duplicated. */
    fun saveSessionNote(sessionId: String, note: String) = update { data ->
        data.copy(sessions = data.sessions.map { if (it.id == sessionId) it.copy(note = note.trim().take(300)) else it })
    }

    fun dismissSummary() {
        _summary.value = null
    }

    /** Deletes a session from the history. Points and statistics follow, because they are counted from the records. */
    fun removeSession(id: String) {
        update { data -> data.copy(sessions = data.sessions.filterNot { it.id == id }) }
        if (_summary.value == id) _summary.value = null
        knownLevel = levelFor(totalXp(_state.value))
    }

    /** Builds the ViewModel with its dependencies. Used from MainActivity. */
    class Factory(
        private val storage: Storage,
        private val alarms: AlarmScheduler,
        private val reminders: ReminderScheduler?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyViewModel(storage, alarms, reminders) as T
    }
}
