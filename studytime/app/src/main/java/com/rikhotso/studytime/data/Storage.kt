package com.rikhotso.studytime.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Where the saved JSON lives. An interface so tests can swap in an in-memory version. */
interface Storage {
    fun read(): String?
    fun write(text: String)

    /** Sets aside a save that could not be read, so it is not lost when a fresh one is written. */
    fun keepUnreadable(text: String) {}
}

/**
 * Saves to a single file in the app's private storage. The text is written to a temporary file
 * first and then renamed, so a crash halfway through never leaves a half-written save.
 */
class FileStorage(private val file: File) : Storage {
    override fun read(): String? = try {
        if (file.exists()) file.readText() else null
    } catch (e: Exception) {
        null
    }

    override fun write(text: String) {
        try {
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(text)
            if (!temp.renameTo(file)) {
                file.writeText(text)
                temp.delete()
            }
        } catch (e: Exception) {
            // Saving is best effort. The data stays in memory and is written again on the next change.
        }
    }

    override fun keepUnreadable(text: String) {
        try {
            File(file.parentFile, file.name + ".unreadable").writeText(text)
        } catch (e: Exception) {
            // Nothing more can be done here. The app still starts with an empty save.
        }
    }
}

/**
 * Converts [AppData] to and from JSON using org.json, which is built into Android.
 *
 * Reading is forgiving: a field that is missing gets its default, so a save written by an older
 * version of studytime (version 1 had no tasks, exams or settings) opens without losing anything.
 */
object AppDataJson {
    private const val VERSION = 2

    // ------------------------------------------------------------------ writing

    fun encode(data: AppData): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("onboarded", data.onboarded)
        val p = data.profile
        root.put(
            "profile",
            JSONObject().put("name", p.name).put("username", p.username).put("about", p.about).put("bio", p.bio)
                .put("educationLevel", p.educationLevel.key).put("avatarColor", p.avatarColor.key).put("avatarFrame", p.avatarFrame.key)
                .put("picturePath", p.picturePath).put("dailyGoalMinutes", p.dailyGoalMinutes)
                .put("weeklyGoalMinutes", p.weeklyGoalMinutes).put("monthlyGoalMinutes", p.monthlyGoalMinutes)
                .put("streakMinimumMinutes", p.streakMinimumMinutes)
                .put("availability", array(p.availability) { a ->
                    JSONObject().put("dayOfWeek", a.dayOfWeek).put("enabled", a.enabled).put("start", a.start).put("end", a.end)
                }),
        )
        val s = data.settings
        root.put(
            "settings",
            JSONObject().put("themeMode", s.themeMode.key).put("accent", s.accent.key).put("highContrast", s.highContrast)
                .put("largeText", s.largeText).put("reduceMotion", s.reduceMotion).put("timerSound", s.timerSound.key)
                .put("timerFace", s.timerFace.key).put("autoStartBreak", s.autoStartBreak).put("notifyTimer", s.notifyTimer)
                .put("notifySessions", s.notifySessions).put("notifyDeadlines", s.notifyDeadlines).put("notifyExams", s.notifyExams)
                .put("notifyRevision", s.notifyRevision).put("quietHoursEnabled", s.quietHoursEnabled)
                .put("quietStart", s.quietStart).put("quietEnd", s.quietEnd)
                .put("dashboardOrder", strings(s.dashboardOrder.map { it.key }))
                .put("hiddenCards", strings(s.hiddenCards.map { it.key })),
        )
        root.put("subjects", array(data.subjects) { x ->
            JSONObject().put("id", x.id).put("name", x.name).put("color", x.color.key)
                .put("teacher", x.teacher).put("goalMinutesPerDay", x.goalMinutesPerDay)
        })
        root.put("entries", array(data.entries) { e ->
            JSONObject().put("id", e.id).put("subjectId", e.subjectId).put("subjectName", e.subjectName)
                .put("color", e.color.key).put("dayOfWeek", e.dayOfWeek).put("startTime", e.startTime)
                .put("endTime", e.endTime).put("room", e.room)
        })
        root.put("plannedSessions", array(data.plannedSessions) { x ->
            JSONObject().put("id", x.id).put("title", x.title).put("subjectId", x.subjectId).put("date", x.date)
                .put("startTime", x.startTime).put("durationMinutes", x.durationMinutes).put("repeat", x.repeat.key)
                .put("repeatDays", JSONArray().also { days -> x.repeatDays.forEach { days.put(it) } })
                .put("taskId", x.taskId).put("topicId", x.topicId).put("generated", x.generated)
                .put("completedDates", strings(x.completedDates)).put("skippedDates", strings(x.skippedDates))
        })
        root.put("tasks", array(data.tasks) { x ->
            JSONObject().put("id", x.id).put("title", x.title).put("subjectId", x.subjectId).put("category", x.category.key)
                .put("priority", x.priority.key).put("dueDate", x.dueDate).put("dueTime", x.dueTime)
                .put("estimateMinutes", x.estimateMinutes).put("notes", x.notes).put("done", x.done)
                .put("completedAt", x.completedAt).put("createdAt", x.createdAt)
                .put("subtasks", array(x.subtasks) { step -> JSONObject().put("id", step.id).put("title", step.title).put("done", step.done) })
        })
        root.put("sessions", array(data.sessions) { x ->
            JSONObject().put("id", x.id).put("subjectId", x.subjectId).put("subjectName", x.subjectName)
                .put("color", x.color.key).put("durationMinutes", x.durationMinutes).put("startedAt", x.startedAt)
                .put("taskId", x.taskId).put("taskTitle", x.taskTitle).put("topicId", x.topicId)
                .put("topicName", x.topicName).put("note", x.note)
        })
        root.put("grades", array(data.grades) { g ->
            JSONObject().put("id", g.id).put("subjectId", g.subjectId).put("subjectName", g.subjectName)
                .put("color", g.color.key).put("assessmentName", g.assessmentName).put("score", g.score)
                .put("maxScore", g.maxScore).put("date", g.date).put("term", g.term)
        })
        root.put("exams", array(data.exams) { x ->
            JSONObject().put("id", x.id).put("subjectId", x.subjectId).put("title", x.title).put("date", x.date)
                .put("time", x.time).put("location", x.location)
        })
        root.put("topics", array(data.topics) { x ->
            JSONObject().put("id", x.id).put("subjectId", x.subjectId).put("name", x.name).put("status", x.status.key)
                .put("confidence", x.confidence).put("reviewCount", x.reviewCount).put("lastReviewed", x.lastReviewed)
                .put("nextReview", x.nextReview)
        })
        root.put("notes", array(data.notes) { x ->
            JSONObject().put("id", x.id).put("subjectId", x.subjectId).put("topicId", x.topicId).put("title", x.title)
                .put("body", x.body).put("updatedAt", x.updatedAt)
        })
        root.put("practiceTests", array(data.practiceTests) { x ->
            JSONObject().put("id", x.id).put("subjectId", x.subjectId).put("title", x.title).put("score", x.score)
                .put("maxScore", x.maxScore).put("date", x.date)
        })
        root.put("challenges", array(data.challenges) { c ->
            JSONObject().put("id", c.id).put("name", c.name).put("type", c.type.key).put("target", c.target)
                .put("reward", c.reward).put("manualProgress", c.manualProgress).put("completed", c.completed)
                .put("completedDate", c.completedDate).put("createdAt", c.createdAt)
        })
        root.put("awards", array(data.awards) { a ->
            JSONObject().put("key", a.key).put("title", a.title).put("xp", a.xp).put("date", a.date)
        })
        root.put("rewardedTaskIds", strings(data.rewardedTaskIds))
        root.put("seenBadges", strings(data.seenBadges))
        root.put("friends", array(data.friends) { f ->
            JSONObject().put("id", f.id).put("name", f.name).put("email", f.email)
                .put("color", f.color.key).put("notes", f.notes)
        })
        root.put("groups", array(data.groups) { g ->
            JSONObject().put("id", g.id).put("name", g.name).put("color", g.color.key).put("subject", g.subject)
                .put("memberNames", strings(g.memberNames)).put("description", g.description)
        })
        root.put("groupSessions", array(data.groupSessions) { x ->
            JSONObject().put("id", x.id).put("groupId", x.groupId).put("groupName", x.groupName)
                .put("color", x.color.key).put("topic", x.topic).put("scheduledDate", x.scheduledDate)
                .put("startTime", x.startTime).put("durationMinutes", x.durationMinutes)
        })
        val t = data.timer
        root.put(
            "timer",
            JSONObject().put("kind", t.kind.key).put("mode", t.mode.name).put("running", t.running).put("endsAt", t.endsAt)
                .put("remainingMs", t.remainingMs).put("totalMs", t.totalMs).put("focusMinutes", t.focusMinutes)
                .put("breakMinutes", t.breakMinutes).put("elapsedMs", t.elapsedMs).put("resumedAt", t.resumedAt)
                .put("subjectId", t.subjectId).put("taskId", t.taskId).put("topicId", t.topicId)
                .put("plannedId", t.plannedId).put("plannedDate", t.plannedDate).put("startedAt", t.startedAt)
                .put("completedRounds", t.completedRounds),
        )
        return root.toString()
    }

    // ------------------------------------------------------------------ reading

    /** Never throws: text that cannot be read gives a fresh, empty [AppData]. */
    fun decode(text: String?): AppData = decodeOrNull(text) ?: AppData()

    /** Returns null when [text] is not a studytime save, so the caller can tell "empty" from "broken". */
    fun decodeOrNull(text: String?): AppData? {
        if (text.isNullOrBlank()) return null
        return try {
            val root = JSONObject(text)
            if (!root.has("profile") && !root.has("onboarded")) return null
            read(root)
        } catch (e: Exception) {
            null
        }
    }

    private fun read(root: JSONObject): AppData {
        val profile = root.optJSONObject("profile") ?: JSONObject()
        val settings = root.optJSONObject("settings") ?: JSONObject()
        val timer = root.optJSONObject("timer") ?: JSONObject()
        val focusMinutes = timer.optInt("focusMinutes", 25).coerceIn(1, 180)
        val breakMinutes = timer.optInt("breakMinutes", 5).coerceIn(0, 60)
        val dailyGoal = profile.optInt("dailyGoalMinutes", 120).coerceIn(15, 480)

        val savedOrder = stringList(settings.optJSONArray("dashboardOrder")).mapNotNull { DashboardCard.fromKey(it) }.distinct()
        // Cards added in a newer version are appended, so they show up for someone with an older save.
        val order = savedOrder + Settings().dashboardOrder.filter { it !in savedOrder }

        val savedAvailability = list(profile, "availability") { o ->
            Availability(
                dayOfWeek = o.optInt("dayOfWeek", 1).coerceIn(1, 7),
                enabled = o.optBoolean("enabled", true),
                start = o.optString("start", "16:00"),
                end = o.optString("end", "20:00"),
            )
        }.distinctBy { it.dayOfWeek }
        val availability = defaultAvailability().map { fallback -> savedAvailability.firstOrNull { it.dayOfWeek == fallback.dayOfWeek } ?: fallback }

        return AppData(
            onboarded = root.optBoolean("onboarded", false),
            profile = Profile(
                name = profile.optString("name", ""),
                username = profile.optString("username", ""),
                about = profile.optString("about", ""),
                bio = profile.optString("bio", ""),
                educationLevel = EducationLevel.fromKey(profile.optString("educationLevel")),
                avatarColor = PastelColor.fromKey(profile.optString("avatarColor")),
                avatarFrame = AvatarFrame.fromKey(profile.optString("avatarFrame")),
                picturePath = profile.optString("picturePath", ""),
                dailyGoalMinutes = dailyGoal,
                weeklyGoalMinutes = profile.optInt("weeklyGoalMinutes", dailyGoal * 5).coerceIn(30, 3360),
                monthlyGoalMinutes = profile.optInt("monthlyGoalMinutes", dailyGoal * 20).coerceIn(60, 14400),
                streakMinimumMinutes = profile.optInt("streakMinimumMinutes", 15).coerceIn(1, 240),
                availability = availability,
            ),
            settings = Settings(
                themeMode = ThemeMode.fromKey(settings.optString("themeMode")),
                accent = Accent.fromKey(settings.optString("accent")),
                highContrast = settings.optBoolean("highContrast", false),
                largeText = settings.optBoolean("largeText", false),
                reduceMotion = settings.optBoolean("reduceMotion", false),
                timerSound = TimerSound.fromKey(settings.optString("timerSound")),
                timerFace = TimerFace.fromKey(settings.optString("timerFace")),
                autoStartBreak = settings.optBoolean("autoStartBreak", true),
                notifyTimer = settings.optBoolean("notifyTimer", true),
                notifySessions = settings.optBoolean("notifySessions", true),
                notifyDeadlines = settings.optBoolean("notifyDeadlines", true),
                notifyExams = settings.optBoolean("notifyExams", true),
                notifyRevision = settings.optBoolean("notifyRevision", true),
                quietHoursEnabled = settings.optBoolean("quietHoursEnabled", false),
                quietStart = settings.optString("quietStart", "22:00"),
                quietEnd = settings.optString("quietEnd", "07:00"),
                dashboardOrder = order,
                hiddenCards = stringList(settings.optJSONArray("hiddenCards")).mapNotNull { DashboardCard.fromKey(it) }.distinct(),
            ),
            subjects = list(root, "subjects") { o ->
                Subject(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    color = PastelColor.fromKey(o.optString("color")),
                    teacher = o.optString("teacher", ""),
                    goalMinutesPerDay = o.optInt("goalMinutesPerDay", 60),
                )
            },
            entries = list(root, "entries") { o ->
                TimetableEntry(
                    id = o.optString("id"),
                    subjectId = o.optString("subjectId", ""),
                    subjectName = o.optString("subjectName"),
                    color = PastelColor.fromKey(o.optString("color")),
                    dayOfWeek = o.optInt("dayOfWeek", 1).coerceIn(1, 7),
                    startTime = o.optString("startTime", "09:00"),
                    endTime = o.optString("endTime", "10:00"),
                    room = o.optString("room", ""),
                )
            },
            plannedSessions = list(root, "plannedSessions") { o ->
                val days = o.optJSONArray("repeatDays") ?: JSONArray()
                PlannedSession(
                    id = o.optString("id"),
                    title = o.optString("title", "Study"),
                    subjectId = o.optString("subjectId", ""),
                    date = o.optString("date", ""),
                    startTime = o.optString("startTime", "16:00"),
                    durationMinutes = o.optInt("durationMinutes", 45).coerceIn(5, 480),
                    repeat = Repeat.fromKey(o.optString("repeat")),
                    repeatDays = (0 until days.length()).map { days.optInt(it, 1) }.filter { it in 1..7 }.distinct(),
                    taskId = o.optString("taskId", ""),
                    topicId = o.optString("topicId", ""),
                    generated = o.optBoolean("generated", false),
                    completedDates = stringList(o.optJSONArray("completedDates")),
                    skippedDates = stringList(o.optJSONArray("skippedDates")),
                )
            },
            tasks = list(root, "tasks") { o ->
                StudyTask(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    subjectId = o.optString("subjectId", ""),
                    category = TaskCategory.fromKey(o.optString("category")),
                    priority = Priority.fromKey(o.optString("priority")),
                    dueDate = o.optString("dueDate", ""),
                    dueTime = o.optString("dueTime", ""),
                    estimateMinutes = o.optInt("estimateMinutes", 0).coerceAtLeast(0),
                    subtasks = list(o, "subtasks") { step ->
                        Subtask(step.optString("id"), step.optString("title"), step.optBoolean("done", false))
                    },
                    notes = o.optString("notes", ""),
                    done = o.optBoolean("done", false),
                    completedAt = o.optLong("completedAt", 0L),
                    createdAt = o.optLong("createdAt", 0L),
                )
            },
            sessions = list(root, "sessions") { o ->
                StudySession(
                    id = o.optString("id"),
                    subjectId = o.optString("subjectId", ""),
                    subjectName = o.optString("subjectName", "General study"),
                    color = PastelColor.fromKey(o.optString("color")),
                    durationMinutes = o.optInt("durationMinutes", 0),
                    startedAt = o.optLong("startedAt", 0L),
                    taskId = o.optString("taskId", ""),
                    taskTitle = o.optString("taskTitle", ""),
                    topicId = o.optString("topicId", ""),
                    topicName = o.optString("topicName", ""),
                    note = o.optString("note", ""),
                )
            },
            grades = list(root, "grades") { o ->
                Grade(
                    id = o.optString("id"),
                    subjectId = o.optString("subjectId", ""),
                    subjectName = o.optString("subjectName"),
                    color = PastelColor.fromKey(o.optString("color")),
                    assessmentName = o.optString("assessmentName"),
                    score = o.optDouble("score", 0.0),
                    maxScore = o.optDouble("maxScore", 100.0),
                    date = o.optString("date", ""),
                    term = o.optString("term", ""),
                )
            },
            exams = list(root, "exams") { o ->
                Exam(
                    id = o.optString("id"),
                    subjectId = o.optString("subjectId", ""),
                    title = o.optString("title"),
                    date = o.optString("date", ""),
                    time = o.optString("time", "09:00"),
                    location = o.optString("location", ""),
                )
            },
            topics = list(root, "topics") { o ->
                Topic(
                    id = o.optString("id"),
                    subjectId = o.optString("subjectId", ""),
                    name = o.optString("name"),
                    status = TopicStatus.fromKey(o.optString("status")),
                    confidence = o.optInt("confidence", 0).coerceIn(0, 5),
                    reviewCount = o.optInt("reviewCount", 0).coerceAtLeast(0),
                    lastReviewed = o.optString("lastReviewed", ""),
                    nextReview = o.optString("nextReview", ""),
                )
            },
            notes = list(root, "notes") { o ->
                StudyNote(
                    id = o.optString("id"),
                    subjectId = o.optString("subjectId", ""),
                    topicId = o.optString("topicId", ""),
                    title = o.optString("title"),
                    body = o.optString("body", ""),
                    updatedAt = o.optLong("updatedAt", 0L),
                )
            },
            practiceTests = list(root, "practiceTests") { o ->
                PracticeTest(
                    id = o.optString("id"),
                    subjectId = o.optString("subjectId", ""),
                    title = o.optString("title"),
                    score = o.optDouble("score", 0.0),
                    maxScore = o.optDouble("maxScore", 100.0),
                    date = o.optString("date", ""),
                )
            },
            challenges = list(root, "challenges") { o ->
                Challenge(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    type = ChallengeType.fromKey(o.optString("type")),
                    target = o.optInt("target", 7).coerceAtLeast(1),
                    reward = o.optString("reward", ""),
                    manualProgress = o.optInt("manualProgress", 0),
                    completed = o.optBoolean("completed", false),
                    completedDate = o.optString("completedDate", ""),
                    createdAt = o.optLong("createdAt", 0L),
                )
            },
            awards = list(root, "awards") { o ->
                Award(o.optString("key"), o.optString("title"), o.optInt("xp", 0).coerceIn(0, 500), o.optString("date", ""))
            }.distinctBy { it.key },
            rewardedTaskIds = stringList(root.optJSONArray("rewardedTaskIds")).distinct(),
            seenBadges = stringList(root.optJSONArray("seenBadges")).distinct(),
            friends = list(root, "friends") { o ->
                Friend(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    email = o.optString("email", ""),
                    color = PastelColor.fromKey(o.optString("color")),
                    notes = o.optString("notes", ""),
                )
            },
            groups = list(root, "groups") { o ->
                StudyGroup(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    color = PastelColor.fromKey(o.optString("color")),
                    subject = o.optString("subject", ""),
                    memberNames = stringList(o.optJSONArray("memberNames")),
                    description = o.optString("description", ""),
                )
            },
            groupSessions = list(root, "groupSessions") { o ->
                GroupSession(
                    id = o.optString("id"),
                    groupId = o.optString("groupId", ""),
                    groupName = o.optString("groupName"),
                    color = PastelColor.fromKey(o.optString("color")),
                    topic = o.optString("topic", "Study together"),
                    scheduledDate = o.optString("scheduledDate", ""),
                    startTime = o.optString("startTime", "18:00"),
                    durationMinutes = o.optInt("durationMinutes", 60),
                )
            },
            timer = TimerState(
                // Version 1 had no timer kind. Anything other than 25 and 5 minutes was a custom timer.
                kind = if (timer.has("kind")) TimerKind.fromKey(timer.optString("kind"))
                else if (focusMinutes == 25 && breakMinutes == 5) TimerKind.Pomodoro else TimerKind.Custom,
                mode = TimerMode.entries.firstOrNull { it.name == timer.optString("mode") } ?: TimerMode.Idle,
                running = timer.optBoolean("running", false),
                endsAt = timer.optLong("endsAt", 0L),
                remainingMs = timer.optLong("remainingMs", focusMinutes * 60_000L),
                totalMs = timer.optLong("totalMs", focusMinutes * 60_000L),
                focusMinutes = focusMinutes,
                breakMinutes = breakMinutes,
                elapsedMs = timer.optLong("elapsedMs", 0L).coerceAtLeast(0L),
                resumedAt = timer.optLong("resumedAt", 0L),
                subjectId = timer.optString("subjectId", ""),
                taskId = timer.optString("taskId", ""),
                topicId = timer.optString("topicId", ""),
                plannedId = timer.optString("plannedId", ""),
                plannedDate = timer.optString("plannedDate", ""),
                startedAt = timer.optLong("startedAt", 0L),
                completedRounds = timer.optInt("completedRounds", 0),
            ),
        )
    }

    // ------------------------------------------------------------------ helpers

    private fun <T> array(items: List<T>, toJson: (T) -> JSONObject): JSONArray {
        val result = JSONArray()
        items.forEach { result.put(toJson(it)) }
        return result
    }

    private fun strings(items: List<String>): JSONArray {
        val result = JSONArray()
        items.forEach { result.put(it) }
        return result
    }

    private fun stringList(source: JSONArray?): List<String> {
        if (source == null) return emptyList()
        return (0 until source.length()).map { source.optString(it) }.filter { it.isNotEmpty() }
    }

    private fun <T> list(root: JSONObject, name: String, fromJson: (JSONObject) -> T): List<T> {
        val source = root.optJSONArray(name) ?: return emptyList()
        val result = ArrayList<T>(source.length())
        for (index in 0 until source.length()) {
            val item = source.optJSONObject(index) ?: continue
            result.add(fromJson(item))
        }
        return result
    }
}
