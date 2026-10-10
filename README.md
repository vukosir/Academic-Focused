# studytime

studytime is a calm, pastel study companion for Android: plan your timetable, focus with a
timer, keep track of tasks and exams, revise by topic, take on challenges and see your progress.

It is a native app written in Kotlin with Jetpack Compose. Everything is stored on the phone,
so it works without an account or an internet connection.

## Open and run

1. Open Android Studio, choose **File > Open** and pick this `studytime` folder.
2. Wait for the Gradle sync to finish.
3. Plug in your phone with USB debugging on (or start an emulator) and press **Run**.

The project uses Android Gradle Plugin 8.7.0, Kotlin 2.0.21, Gradle 8.9 and the Compose BOM
2024.11.00. It needs Android 8.0 (API 26) or newer on the phone. No libraries were added in
version 2, so the dependencies are the same as before.

If version 1 is already on the phone, version 2 installs over it and keeps your data.

## What is in the app

| Where | What it does |
| --- | --- |
| Welcome steps | Name, education level, subjects, weekly classes, study hours and goals |
| Home | Cards you can hide and reorder: stats, focus goal, today's schedule, tasks due, exam countdown, weekly chart, daily challenges, your challenges, shortcuts |
| Timer | Pomodoro, Custom and Stopwatch. Start, pause, resume, restart, finish and end. Each session is saved with its subject, task or topic and an optional note |
| Planner > Timetable | Subjects and the weekly timetable with classes, planned sessions and exams |
| Planner > Today | One day at a time: schedule, free time, missed sessions and topics due for revision |
| Planner > Month | A calendar of sessions, deadlines and exams |
| Planner > Tasks | Tasks with deadlines, priorities, categories, estimates, checklists, search and filters |
| Planner > Exams | Exam timetable, countdowns, a revision overview and a revision plan for each exam |
| A subject | Revision topics with status, confidence and spaced reviews, notes, practice tests and grades |
| Friends | Your own list of friends, groups and planned group sessions, kept on this phone |
| Profile > Overview | Profile, totals, the weekly chart, daily goal and grades |
| Profile > Stats | Today, week, month and all time: totals, charts, time by subject, planned and actual, records, insights and searchable history |
| Profile > Rewards | Level and points, streak and streak freezes, daily and weekly challenges, badges, your own challenges and unlocks |
| Profile > Settings | Light, dark or system theme, accent colour, high contrast, larger text, reduced motion, Home cards, timer sound and face, notifications, quiet hours, goals, study hours, export and restore |
| Search (top bar) | Finds subjects, tasks, notes, topics, exams, planned sessions and past sessions |

## How the main pieces work

- **Timer.** It stores timestamps, not a ticking counter, so it stays correct when the app is in
  the background or closed. An alarm plays the tone you picked and shows a notification when a
  round or a break ends.
- **Planning.** A planned session is refused when it overlaps a class, an exam or another
  session. The generator only uses your study hours, stops at your daily goal and gives more
  time to subjects with an exam coming up, an urgent task or topics you rated low.
- **Points and streaks.** Nothing is stored as a running total. Points, levels, streaks, badges
  and statistics are all counted from saved sessions and finished tasks, so they cannot drift.
- **Reminders.** Planned sessions, deadlines, exams and revision each have their own switch.
  Reminders are booked two weeks ahead and booked again after the phone restarts.
- **Saving.** Everything is one JSON file in the app's private storage. A save from version 1
  opens in version 2 with the new fields filled in with defaults.

## Not in this version

These need an online backend (accounts and a server), so they are not built yet:
registration and login, sync between devices, real friend requests, study rooms, live status,
shared sessions, group goals, invitations, reactions, friend challenges, leaderboards and
server-side permissions.

Also not included: dragging sessions to move them (use Move in the session's menu), an iPhone
or web version, and a database engine such as Room (data is saved as a JSON file).

## Where things live

```
app/src/main/java/com/rikhotso/studytime/
  MainActivity.kt          The only Activity. Creates the ViewModel and starts Compose
  StudyViewModel.kt        All app state and every action a screen can take
  data/Models.kt           The data classes (Subject, StudyTask, Exam, Topic and so on)
  data/Storage.kt          Saving and loading as JSON in the app's private storage
  data/StudyLogic.kt       Formatting, streaks, weekly and monthly totals
  data/TimerLogic.kt       The focus timer as a small state machine
  data/PlannerLogic.kt     Repeats, clash detection, free time, the plan generators, spaced revision
  data/TaskLogic.kt        Task filters and the search across the app
  data/Gamification.kt     Points, levels, daily and weekly challenges, badges
  data/Stats.kt            Summaries, subject breakdown, records, insights, exam readiness
  data/Reminders.kt        Which reminders to book, quiet hours, CSV export
  timer/TimerAlarms.kt     The end-of-round alarm, tone and notification
  timer/ReminderAlarms.kt  Reminder alarms and booking them again after a restart
  ui/StudytimeApp.kt       Top bar, bottom navigation and which screen is showing
  ui/Platform.kt           Photo picker, file export and import
  ui/Previews.kt           Design previews for Android Studio
  ui/theme/                Colours, fonts and the Compose theme
  ui/components/           Shared pieces: cards, buttons, charts, form fields, dialogs
  ui/screens/              The screens and their dialogs
app/src/main/res/
  drawable/                Icons and the launcher icon artwork
  font/                    Quicksand and Nunito
  values/strings.xml       The app name and notification channel names
app/src/test/              Unit tests for the logic in the data package
```

## Common changes

- **Colours:** `ui/theme/Color.kt`. `LightStudyColors` and `DarkStudyColors` hold the palette,
  `Accent.color` holds the accent colours and `PastelColor.solid` the eight subject colours.
- **Fonts and text sizes:** `ui/theme/Type.kt`.
- **Timer presets:** the lists `listOf(15, 25, 50)` and `listOf(0, 5, 10, 15)` in `ui/screens/TimerScreen.kt`.
- **Points and levels:** the constants at the top of `data/Gamification.kt`.
- **Daily and weekly challenges and badges:** the lists in `data/Gamification.kt`.
- **App name:** `res/values/strings.xml`. The name is always written in lowercase: studytime.
- **A new field on something you save:** add it to the data class in `data/Models.kt`, then to
  `encode` and `read` in `data/Storage.kt`. Give it a default so older saves still open.

## Tests

Right-click `app/src/test` and choose **Run tests**, or run `gradlew test`. There are 42 tests
covering the timer (including time away from the app and the stopwatch), repeats and clash
detection, the plan generators, spaced revision, streak freezes, points and challenges, task
filters, statistics, search and reminders with quiet hours.

## Credits

- Fonts: Quicksand and Nunito, SIL Open Font License 1.1.
- Icons: Lucide, ISC licence.

The licence texts are in the `licenses` folder.
