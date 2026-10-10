package com.rikhotso.studytime.data

/**
 * Everything studytime remembers. These are plain immutable data classes: screens read them,
 * [com.rikhotso.studytime.StudyViewModel] replaces them with updated copies, and
 * [AppDataJson] turns the whole [AppData] tree into JSON for saving.
 *
 * Enums carry a [key] that is written to the save file and a [label] that is shown on screen,
 * so a label can be reworded without breaking old saves.
 */

// ------------------------------------------------------------------ colours and profile

/** The eight subject colours. */
enum class PastelColor(val key: String, val label: String) {
    Lavender("lavender", "Lavender"),
    BabyBlue("babyblue", "Baby blue"),
    Blush("blush", "Blush pink"),
    Butter("butter", "Butter"),
    Mint("mint", "Mint"),
    Lilac("lilac", "Lilac"),
    Peach("peach", "Peach"),
    Sky("sky", "Sky");

    companion object {
        fun fromKey(key: String?): PastelColor = entries.firstOrNull { it.key == key } ?: Lavender
    }
}

enum class EducationLevel(val key: String, val label: String) {
    HighSchool("high_school", "High school"),
    Matric("matric", "Matric"),
    University("university", "University"),
    College("college", "College"),
    Independent("independent", "Independent learning");

    companion object {
        fun fromKey(key: String?): EducationLevel = entries.firstOrNull { it.key == key } ?: University
    }
}

/** When the user can study on one weekday. [dayOfWeek] is 1 for Monday to 7 for Sunday. */
data class Availability(
    val dayOfWeek: Int,
    val enabled: Boolean,
    val start: String,
    val end: String,
)

fun defaultAvailability(): List<Availability> = (1..7).map { day ->
    when (day) {
        6 -> Availability(day, true, "09:00", "13:00")
        7 -> Availability(day, false, "09:00", "13:00")
        else -> Availability(day, true, "16:00", "20:00")
    }
}

/** A decoration around the profile avatar. The fancier ones are unlocked by reaching a level. */
enum class AvatarFrame(val key: String, val label: String, val requiredLevel: Int) {
    None("none", "No frame", 1),
    Ring("ring", "Lavender ring", 2),
    Double("double", "Double ring", 4),
    Rainbow("rainbow", "Pastel rainbow", 6);

    companion object {
        fun fromKey(key: String?): AvatarFrame = entries.firstOrNull { it.key == key } ?: None
    }
}

data class Profile(
    val name: String = "",
    val username: String = "",
    val about: String = "",
    val bio: String = "",
    val educationLevel: EducationLevel = EducationLevel.University,
    val avatarColor: PastelColor = PastelColor.Lavender,
    val avatarFrame: AvatarFrame = AvatarFrame.None,
    /** Path of a copied profile photo inside the app's private storage, or blank for initials. */
    val picturePath: String = "",
    val dailyGoalMinutes: Int = 120,
    val weeklyGoalMinutes: Int = 600,
    val monthlyGoalMinutes: Int = 2400,
    /** A day only counts toward the streak once this many minutes are studied. */
    val streakMinimumMinutes: Int = 15,
    val availability: List<Availability> = defaultAvailability(),
)

// ------------------------------------------------------------------ subjects and timetable

data class Subject(
    val id: String,
    val name: String,
    val color: PastelColor = PastelColor.Lavender,
    val teacher: String = "",
    val goalMinutesPerDay: Int = 60,
)

/** One recurring class. [dayOfWeek] follows java.time: 1 is Monday and 7 is Sunday. Times are "HH:mm". */
data class TimetableEntry(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val color: PastelColor,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val room: String = "",
)

enum class Repeat(val key: String, val label: String) {
    None("none", "Does not repeat"),
    Daily("daily", "Every day"),
    Weekly("weekly", "Every week"),
    Days("days", "On selected days");

    companion object {
        fun fromKey(key: String?): Repeat = entries.firstOrNull { it.key == key } ?: None
    }
}

/**
 * A study session the user plans to do. It first happens on [date] and may repeat after that.
 * Finishing it on a given day adds that day to [completedDates].
 */
data class PlannedSession(
    val id: String,
    val title: String,
    val subjectId: String = "",
    val date: String,
    val startTime: String,
    val durationMinutes: Int = 45,
    val repeat: Repeat = Repeat.None,
    val repeatDays: List<Int> = emptyList(),
    val taskId: String = "",
    val topicId: String = "",
    /** True when the timetable generator created it, so "regenerate" knows what it may replace. */
    val generated: Boolean = false,
    val completedDates: List<String> = emptyList(),
    /** Days on which a repeating session was skipped or moved, so it no longer shows there. */
    val skippedDates: List<String> = emptyList(),
)

// ------------------------------------------------------------------ tasks

enum class Priority(val key: String, val label: String, val rank: Int) {
    Low("low", "Low", 0),
    Medium("medium", "Medium", 1),
    High("high", "High", 2),
    Urgent("urgent", "Urgent", 3);

    companion object {
        fun fromKey(key: String?): Priority = entries.firstOrNull { it.key == key } ?: Medium
    }
}

enum class TaskCategory(val key: String, val label: String) {
    Homework("homework", "Homework"),
    Revision("revision", "Revision"),
    Project("project", "Project"),
    Test("test", "Test"),
    Reading("reading", "Reading"),
    Other("other", "Other");

    companion object {
        fun fromKey(key: String?): TaskCategory = entries.firstOrNull { it.key == key } ?: Homework
    }
}

data class Subtask(val id: String, val title: String, val done: Boolean = false)

data class StudyTask(
    val id: String,
    val title: String,
    val subjectId: String = "",
    val category: TaskCategory = TaskCategory.Homework,
    val priority: Priority = Priority.Medium,
    val dueDate: String = "",
    val dueTime: String = "",
    val estimateMinutes: Int = 0,
    val subtasks: List<Subtask> = emptyList(),
    val notes: String = "",
    val done: Boolean = false,
    val completedAt: Long = 0L,
    val createdAt: Long = 0L,
)

// ------------------------------------------------------------------ sessions, grades, exams, revision

/** One finished focus session. [startedAt] is epoch milliseconds. */
data class StudySession(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val color: PastelColor,
    val durationMinutes: Int,
    val startedAt: Long,
    val taskId: String = "",
    val taskTitle: String = "",
    val topicId: String = "",
    val topicName: String = "",
    val note: String = "",
)

/** One logged assessment. [date] is an ISO date such as 2026-10-10. */
data class Grade(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val color: PastelColor,
    val assessmentName: String,
    val score: Double,
    val maxScore: Double,
    val date: String,
    val term: String = "",
)

data class Exam(
    val id: String,
    val subjectId: String = "",
    val title: String,
    val date: String,
    val time: String = "09:00",
    val location: String = "",
)

enum class TopicStatus(val key: String, val label: String) {
    NotStarted("not_started", "Not started"),
    InProgress("in_progress", "In progress"),
    Completed("completed", "Completed");

    companion object {
        fun fromKey(key: String?): TopicStatus = entries.firstOrNull { it.key == key } ?: NotStarted
    }
}

/**
 * One piece of a subject's syllabus. [confidence] runs from 1 (lost) to 5 (solid), or 0 when it
 * has not been rated. Each review pushes [nextReview] further out (spaced revision).
 */
data class Topic(
    val id: String,
    val subjectId: String,
    val name: String,
    val status: TopicStatus = TopicStatus.NotStarted,
    val confidence: Int = 0,
    val reviewCount: Int = 0,
    val lastReviewed: String = "",
    val nextReview: String = "",
)

data class StudyNote(
    val id: String,
    val subjectId: String = "",
    val topicId: String = "",
    val title: String,
    val body: String = "",
    val updatedAt: Long = 0L,
)

data class PracticeTest(
    val id: String,
    val subjectId: String,
    val title: String,
    val score: Double,
    val maxScore: Double,
    val date: String,
)

// ------------------------------------------------------------------ challenges and rewards

enum class ChallengeType(val key: String, val label: String, val unit: String) {
    Streak("streak", "Day streak", "days"),
    DailyGoal("daily_goal", "Daily goal days", "goal days"),
    Sessions("sessions", "Focus sessions", "sessions"),
    Hours("hours", "Study hours", "hours"),
    Milestone("milestone", "Milestone", "steps");

    companion object {
        fun fromKey(key: String?): ChallengeType = entries.firstOrNull { it.key == key } ?: Streak
    }
}

/**
 * A personal challenge. Every type except [ChallengeType.Milestone] is measured from real focus
 * sessions that happened after [createdAt]; a milestone is ticked up by hand in [manualProgress].
 */
data class Challenge(
    val id: String,
    val name: String,
    val type: ChallengeType,
    val target: Int,
    val reward: String = "",
    val manualProgress: Int = 0,
    val completed: Boolean = false,
    val completedDate: String = "",
    val createdAt: Long = 0L,
)

/** Bonus experience points that were handed out once, for example for a daily challenge. */
data class Award(val key: String, val title: String, val xp: Int, val date: String)

// ------------------------------------------------------------------ friends (kept on this phone)

data class Friend(
    val id: String,
    val name: String,
    val email: String = "",
    val color: PastelColor = PastelColor.Blush,
    val notes: String = "",
)

data class StudyGroup(
    val id: String,
    val name: String,
    val color: PastelColor = PastelColor.BabyBlue,
    val subject: String = "",
    val memberNames: List<String> = emptyList(),
    val description: String = "",
)

/** A planned group study session. [scheduledDate] is an ISO date and [startTime] is "HH:mm". */
data class GroupSession(
    val id: String,
    val groupId: String,
    val groupName: String,
    val color: PastelColor,
    val topic: String,
    val scheduledDate: String,
    val startTime: String,
    val durationMinutes: Int = 60,
)

// ------------------------------------------------------------------ timer

enum class TimerMode { Idle, Focus, Break }

enum class TimerKind(val key: String, val label: String) {
    Pomodoro("pomodoro", "Pomodoro"),
    Custom("custom", "Custom"),
    Stopwatch("stopwatch", "Stopwatch");

    companion object {
        fun fromKey(key: String?): TimerKind = entries.firstOrNull { it.key == key } ?: Pomodoro
    }
}

/**
 * The focus timer. It is driven by timestamps instead of a ticking counter, so it stays correct
 * when the app is in the background or has been closed.
 *
 * Countdown ([TimerKind.Pomodoro] and [TimerKind.Custom]): while [running], the phase ends at
 * [endsAt] (epoch milliseconds); while paused, [remainingMs] holds what is left.
 *
 * Stopwatch: [elapsedMs] holds the time counted before the last pause and [resumedAt] is when it
 * was last started, so the time shown is elapsedMs plus the time since resumedAt.
 */
data class TimerState(
    val kind: TimerKind = TimerKind.Pomodoro,
    val mode: TimerMode = TimerMode.Idle,
    val running: Boolean = false,
    val endsAt: Long = 0L,
    val remainingMs: Long = 25 * 60_000L,
    val totalMs: Long = 25 * 60_000L,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val elapsedMs: Long = 0L,
    val resumedAt: Long = 0L,
    val subjectId: String = "",
    val taskId: String = "",
    val topicId: String = "",
    /** Set when the round was started from a planned session, so finishing it ticks that session off. */
    val plannedId: String = "",
    val plannedDate: String = "",
    val startedAt: Long = 0L,
    val completedRounds: Int = 0,
)

// ------------------------------------------------------------------ settings

enum class ThemeMode(val key: String, val label: String) {
    Light("light", "Light"),
    Dark("dark", "Dark"),
    System("system", "System");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: Light
    }
}

/** The colour of buttons and highlights. Some are unlocked by reaching a level. */
enum class Accent(val key: String, val label: String, val requiredLevel: Int) {
    Lavender("lavender", "Lavender", 1),
    Sky("sky", "Baby blue", 1),
    Blush("blush", "Blush", 2),
    Mint("mint", "Mint", 3),
    Peach("peach", "Peach", 4),
    Lilac("lilac", "Lilac", 5);

    companion object {
        fun fromKey(key: String?): Accent = entries.firstOrNull { it.key == key } ?: Lavender
    }
}

enum class TimerSound(val key: String, val label: String) {
    Chime("chime", "Soft tone"),
    Bell("bell", "Bright tone"),
    Beep("beep", "Beep"),
    DoubleBeep("double_beep", "Double beep"),
    Off("off", "No sound");

    companion object {
        fun fromKey(key: String?): TimerSound = entries.firstOrNull { it.key == key } ?: Chime
    }
}

/** How the timer ring is drawn. The fancier ones are unlocked by reaching a level. */
enum class TimerFace(val key: String, val label: String, val requiredLevel: Int) {
    Ring("ring", "Classic ring", 1),
    Bold("bold", "Bold ring", 3),
    Sunrise("sunrise", "Sunrise", 5);

    companion object {
        fun fromKey(key: String?): TimerFace = entries.firstOrNull { it.key == key } ?: Ring
    }
}

/** The cards that can appear on the Home screen. */
enum class DashboardCard(val key: String, val label: String) {
    Stats("stats", "Quick stats"),
    FocusGoal("focus_goal", "Today's focus goal"),
    Today("today", "Today's schedule"),
    TasksDue("tasks_due", "Tasks due soon"),
    Exams("exams", "Exam countdown"),
    Week("week", "This week's study"),
    DailyChallenges("daily_challenges", "Daily challenges"),
    Challenges("challenges", "Active challenges"),
    QuickLinks("quick_links", "Shortcuts");

    companion object {
        fun fromKey(key: String?): DashboardCard? = entries.firstOrNull { it.key == key }
    }
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.Light,
    val accent: Accent = Accent.Lavender,
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val reduceMotion: Boolean = false,
    val timerSound: TimerSound = TimerSound.Chime,
    val timerFace: TimerFace = TimerFace.Ring,
    val autoStartBreak: Boolean = true,
    val notifyTimer: Boolean = true,
    val notifySessions: Boolean = true,
    val notifyDeadlines: Boolean = true,
    val notifyExams: Boolean = true,
    val notifyRevision: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStart: String = "22:00",
    val quietEnd: String = "07:00",
    /** Home cards in the order they are shown. Cards in [hiddenCards] are skipped. */
    val dashboardOrder: List<DashboardCard> = DashboardCard.entries,
    val hiddenCards: List<DashboardCard> = emptyList(),
)

// ------------------------------------------------------------------ the whole save

data class AppData(
    val onboarded: Boolean = false,
    val profile: Profile = Profile(),
    val settings: Settings = Settings(),
    val subjects: List<Subject> = emptyList(),
    val entries: List<TimetableEntry> = emptyList(),
    val plannedSessions: List<PlannedSession> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val grades: List<Grade> = emptyList(),
    val exams: List<Exam> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val notes: List<StudyNote> = emptyList(),
    val practiceTests: List<PracticeTest> = emptyList(),
    val challenges: List<Challenge> = emptyList(),
    val awards: List<Award> = emptyList(),
    /** Ids of tasks that already earned their points, so re-ticking a task never pays twice. */
    val rewardedTaskIds: List<String> = emptyList(),
    /** Badges the user has already been told about. */
    val seenBadges: List<String> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val groups: List<StudyGroup> = emptyList(),
    val groupSessions: List<GroupSession> = emptyList(),
    val timer: TimerState = TimerState(),
) {
    fun subject(id: String): Subject? = subjects.firstOrNull { it.id == id }
    fun subjectName(id: String): String = subject(id)?.name ?: "General study"
    fun subjectColor(id: String): PastelColor = subject(id)?.color ?: PastelColor.Lavender
}
