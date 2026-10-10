package com.rikhotso.studytime.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.TimerLogic
import com.rikhotso.studytime.data.TimerMode
import com.rikhotso.studytime.data.TimerState
import com.rikhotso.studytime.data.formatClock
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.timer.canNotify
import com.rikhotso.studytime.ui.components.CardShape
import com.rikhotso.studytime.ui.components.FormDialog
import com.rikhotso.studytime.ui.components.IconAction
import com.rikhotso.studytime.ui.components.Logo
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.components.StudyTextField
import com.rikhotso.studytime.ui.components.SwitchRow
import com.rikhotso.studytime.ui.screens.FriendsScreen
import com.rikhotso.studytime.ui.screens.HomeScreen
import com.rikhotso.studytime.ui.screens.OnboardingScreen
import com.rikhotso.studytime.ui.screens.PlannerScreen
import com.rikhotso.studytime.ui.screens.ProfileScreen
import com.rikhotso.studytime.ui.screens.SearchScreen
import com.rikhotso.studytime.ui.screens.SubjectScreen
import com.rikhotso.studytime.ui.screens.TimerScreen
import com.rikhotso.studytime.ui.theme.StudyTheme
import kotlinx.coroutines.delay

/** The five destinations in the bottom bar. */
enum class AppTab(val label: String, @DrawableRes val icon: Int) {
    Home("Home", R.drawable.ic_house),
    Timer("Timer", R.drawable.ic_timer),
    Planner("Planner", R.drawable.ic_calendar_days),
    Friends("Friends", R.drawable.ic_users),
    Profile("Profile", R.drawable.ic_user),
}

/** The sections inside the Planner tab. */
enum class PlannerTab(val label: String) { Timetable("Timetable"), Today("Today"), Month("Month"), Tasks("Tasks"), Exams("Exams") }

/** The sections inside the Profile tab. */
enum class ProfileTab(val label: String) { Overview("Overview"), Stats("Stats"), Rewards("Rewards"), Settings("Settings") }

/** How a screen asks to go somewhere else. Screens never change the navigation state themselves. */
class AppNav(
    val goTab: (AppTab) -> Unit,
    val openPlanner: (PlannerTab) -> Unit,
    val openProfile: (ProfileTab) -> Unit,
    val openSubject: (String) -> Unit,
    val openSearch: () -> Unit,
)

/**
 * The root of the UI. Shows the welcome steps until setup is finished, then the main app with
 * its top bar, the current screen and the bottom navigation.
 */
@Composable
fun StudytimeApp(viewModel: StudyViewModel) {
    val data by viewModel.state.collectAsStateWithLifecycle()
    val summaryId by viewModel.summary.collectAsStateWithLifecycle()
    val colors = StudyTheme.colors
    val snackbar = remember { SnackbarHostState() }

    // Short confirmations from the ViewModel, shown one after another.
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    // Lets the timer move from focus to break to idle while the app is open.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.tick()
            delay(250)
        }
    }

    if (!data.onboarded) {
        OnboardingScreen(data, viewModel)
        return
    }

    var tab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var plannerTab by rememberSaveable { mutableStateOf(PlannerTab.Timetable) }
    var profileTab by rememberSaveable { mutableStateOf(ProfileTab.Overview) }
    var openSubjectId by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }

    val nav = remember {
        AppNav(
            goTab = {
                tab = it
                openSubjectId = ""
                searching = false
            },
            openPlanner = {
                plannerTab = it
                tab = AppTab.Planner
                openSubjectId = ""
                searching = false
            },
            openProfile = {
                profileTab = it
                tab = AppTab.Profile
                openSubjectId = ""
                searching = false
            },
            openSubject = {
                openSubjectId = it
                searching = false
            },
            openSearch = { searching = true },
        )
    }

    // Reminders need permission to notify on Android 13 and newer. Ask the first time there is
    // something to be reminded about. The system stops showing the question after two refusals.
    val context = LocalContext.current
    val askForNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var askedThisRun by rememberSaveable { mutableStateOf(false) }
    val hasThingsToRemind = data.plannedSessions.isNotEmpty() || data.exams.isNotEmpty() || data.tasks.any { !it.done && it.dueDate.isNotEmpty() }
    LaunchedEffect(hasThingsToRemind) {
        if (hasThingsToRemind && !askedThisRun && needsNotificationPermission() && !canNotify(context)) {
            askedThisRun = true
            askForNotifications.launch(NOTIFICATION_PERMISSION)
        }
    }

    val showingSubject = openSubjectId.isNotEmpty() && data.subject(openSubjectId) != null
    BackHandler(enabled = searching || showingSubject || tab != AppTab.Home) {
        when {
            searching -> searching = false
            showingSubject -> openSubjectId = ""
            else -> tab = AppTab.Home
        }
    }

    Scaffold(
        containerColor = colors.background,
        contentColor = colors.foreground,
        topBar = {
            TopBar(
                timer = data.timer,
                showTimerChip = tab != AppTab.Timer || searching || showingSubject,
                onTimerClick = { nav.goTab(AppTab.Timer) },
                onSearchClick = { searching = !searching },
            )
        },
        bottomBar = { BottomNav(current = tab, onSelect = { nav.goTab(it) }) },
        snackbarHost = {
            SnackbarHost(snackbar) { message ->
                Snackbar(
                    snackbarData = message,
                    shape = CardShape,
                    containerColor = colors.foreground,
                    contentColor = colors.background,
                )
            }
        },
    ) { padding ->
        // imePadding lifts the screen above the keyboard, so a text field near the bottom stays visible.
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            when {
                searching -> SearchScreen(data, nav, onClose = { searching = false })
                showingSubject -> SubjectScreen(data, viewModel, openSubjectId, nav, onBack = { openSubjectId = "" })
                else -> when (tab) {
                    AppTab.Home -> HomeScreen(data, viewModel, nav)
                    AppTab.Timer -> TimerScreen(data, viewModel)
                    AppTab.Planner -> PlannerScreen(data, viewModel, nav, plannerTab, onTab = { plannerTab = it })
                    AppTab.Friends -> FriendsScreen(data, viewModel)
                    AppTab.Profile -> ProfileScreen(data, viewModel, nav, profileTab, onTab = { profileTab = it })
                }
            }
        }
    }

    val summarySession = data.sessions.firstOrNull { it.id == summaryId }
    if (summarySession != null) {
        SessionSummaryDialog(data, summarySession.id, viewModel)
    }
}

/**
 * Shown once after a focus round is saved. The session is already stored at this point, so
 * closing the dialog loses nothing. The note is added to that same session.
 */
@Composable
private fun SessionSummaryDialog(data: AppData, sessionId: String, viewModel: StudyViewModel) {
    val session = data.sessions.firstOrNull { it.id == sessionId } ?: return
    val task = data.tasks.firstOrNull { it.id == session.taskId && !it.done }
    val topic = data.topics.firstOrNull { it.id == session.topicId }
    var note by remember(sessionId) { mutableStateOf(session.note) }
    var finishTask by remember(sessionId) { mutableStateOf(false) }
    var reviewTopic by remember(sessionId) { mutableStateOf(topic != null) }
    val colors = StudyTheme.colors
    FormDialog(
        title = "Session saved",
        confirmLabel = "Done",
        onDismiss = { viewModel.dismissSummary() },
        onConfirm = {
            if (note.trim() != session.note) viewModel.saveSessionNote(sessionId, note)
            if (finishTask && task != null) viewModel.toggleTask(task.id)
            if (reviewTopic && topic != null) viewModel.reviewTopic(topic.id)
            viewModel.dismissSummary()
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${formatMinutes(session.durationMinutes)} of ${session.subjectName}", style = MaterialTheme.typography.titleMedium, color = colors.foreground)
            if (session.taskTitle.isNotEmpty()) Text("Task: ${session.taskTitle}", style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
            if (session.topicName.isNotEmpty()) Text("Topic: ${session.topicName}", style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
            Text("+${session.durationMinutes} points", style = MaterialTheme.typography.labelMedium, color = colors.primary)
        }
        StudyTextField(note, { note = it.take(300) }, label = "Note (optional)", placeholder = "What did you get done?", singleLine = false)
        if (task != null) {
            SwitchRow("Mark the task as done", finishTask, { finishTask = it }, subtitle = task.title)
        }
        if (topic != null) {
            SwitchRow("Count as a review of this topic", reviewTopic, { reviewTopic = it }, subtitle = "Books the next spaced review")
        }
    }
}

/**
 * The current time, refreshed a few times a second while [active] is true. Anything that reads
 * it redraws on each refresh, so only the timer face and the top bar chip use it.
 */
@Composable
fun rememberNow(active: Boolean): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        now = System.currentTimeMillis()
        while (active) {
            delay(200)
            now = System.currentTimeMillis()
        }
    }
    return now
}

@Composable
private fun TopBar(timer: TimerState, showTimerChip: Boolean, onTimerClick: () -> Unit, onSearchClick: () -> Unit) {
    val colors = StudyTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Logo(Modifier.weight(1f), markSize = 28.dp, fontSize = 20.sp)
            if (showTimerChip && timer.mode != TimerMode.Idle) {
                val now = rememberNow(timer.running)
                val seconds = (TimerLogic.displayMs(timer, now) + if (TimerLogic.countsDown(timer)) 999 else 0) / 1000
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable(role = Role.Button, onClickLabel = "Open the timer", onClick = onTimerClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StudyIcon(
                        if (timer.mode == TimerMode.Break) R.drawable.ic_coffee else R.drawable.ic_brain,
                        Modifier.size(14.dp),
                        tint = colors.onPrimary,
                    )
                    Text(formatClock(seconds), style = MaterialTheme.typography.labelMedium, color = colors.onPrimary)
                }
            }
            IconAction(R.drawable.ic_search, "Search studytime", onClick = onSearchClick, tint = colors.foreground)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

@Composable
private fun BottomNav(current: AppTab, onSelect: (AppTab) -> Unit) {
    val colors = StudyTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.card)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
            AppTab.entries.forEach { tab ->
                val selected = tab == current
                val tint = if (selected) colors.primary else colors.mutedForeground
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) })
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StudyIcon(tab.icon, Modifier.size(22.dp), tint = tint)
                    Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
                }
            }
        }
    }
}
