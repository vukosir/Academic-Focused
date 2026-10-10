@file:OptIn(ExperimentalLayoutApi::class)

package com.rikhotso.studytime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.DAY_NAMES_SHORT
import com.rikhotso.studytime.data.MONTH_NAMES_SHORT
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.parseDate
import com.rikhotso.studytime.ui.components.Avatar
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.CardShape
import com.rikhotso.studytime.ui.components.ColorPicker
import com.rikhotso.studytime.ui.components.DateField
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.FieldLabel
import com.rikhotso.studytime.ui.components.FormDialog
import com.rikhotso.studytime.ui.components.InfoNote
import com.rikhotso.studytime.ui.components.MinutesSlider
import com.rikhotso.studytime.ui.components.PillTabs
import com.rikhotso.studytime.ui.components.RowText
import com.rikhotso.studytime.ui.components.ScreenTitle
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.SoftRow
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyTextField
import com.rikhotso.studytime.ui.components.TimeField
import com.rikhotso.studytime.ui.components.ToggleChip
import com.rikhotso.studytime.ui.components.TrashButton
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft
import java.time.LocalDate

/** Friends, study groups and planned group sessions, on three tabs. */
@Composable
fun FriendsScreen(data: AppData, viewModel: StudyViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf("") }

    ScreenColumn {
        ScreenTitle("Study with friends", "Add friends, form groups and plan sessions together.")
        PillTabs(listOf("Friends", "Groups", "Sessions"), selectedIndex = tab, onSelect = { tab = it })
        InfoNote(
            "For now this is your own list, kept on this phone. Friend requests, shared study rooms, live status and leaderboards need the online version of studytime.",
            icon = R.drawable.ic_users,
        )
        when (tab) {
            0 -> FriendsTab(data, viewModel, onAdd = { dialog = "friend" })
            1 -> GroupsTab(data, viewModel, onAdd = { dialog = "group" })
            else -> SessionsTab(data, viewModel, onAdd = { dialog = if (data.groups.isEmpty()) "group" else "session" })
        }
    }

    when (dialog) {
        "friend" -> FriendDialog(viewModel, onDone = { dialog = "" })
        "group" -> GroupDialog(data, viewModel, onDone = { dialog = "" })
        "session" -> SessionDialog(data, viewModel, onDone = { dialog = "" })
    }
}

@Composable
private fun AddRow(label: String, icon: Int, kind: ButtonKind, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        StudyButton(label, onClick = onClick, kind = kind, small = true, leadingIcon = icon)
    }
}

@Composable
private fun FriendsTab(data: AppData, viewModel: StudyViewModel, onAdd: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AddRow("Add friend", R.drawable.ic_user_plus, ButtonKind.Primary, onAdd)
        if (data.friends.isEmpty()) {
            EmptyHint("No friends added yet. Invite a study buddy to get started.")
        }
        data.friends.sortedBy { it.name.lowercase() }.forEach { friend ->
            SoftRow(friend.color) {
                Avatar(friend.name, friend.color)
                RowText(title = friend.name, subtitle = friend.email.ifEmpty { friend.notes.ifEmpty { "study buddy" } })
                TrashButton(onClick = { viewModel.removeFriend(friend.id) }, description = "Remove ${friend.name}")
            }
        }
    }
}

@Composable
private fun GroupsTab(data: AppData, viewModel: StudyViewModel, onAdd: () -> Unit) {
    val colors = StudyTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AddRow("New group", R.drawable.ic_plus, ButtonKind.Secondary, onAdd)
        if (data.groups.isEmpty()) {
            EmptyHint("No groups yet. Create one to plan sessions together.")
        }
        data.groups.forEach { group ->
            Column(
                Modifier.fillMaxWidth().clip(CardShape).background(group.color.soft).padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowText(title = group.name, subtitle = group.subject)
                    TrashButton(onClick = { viewModel.removeGroup(group.id) }, description = "Delete ${group.name}")
                }
                if (group.memberNames.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        group.memberNames.forEach { member ->
                            Text(
                                member,
                                Modifier.clip(CircleShape).background(colors.background.copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.foreground,
                            )
                        }
                    }
                }
                if (group.description.isNotEmpty()) {
                    Text(group.description, Modifier.padding(end = 10.dp), style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                }
            }
        }
    }
}

@Composable
private fun SessionsTab(data: AppData, viewModel: StudyViewModel, onAdd: () -> Unit) {
    val colors = StudyTheme.colors
    val today = LocalDate.now().toString()
    val upcoming = data.groupSessions.filter { it.scheduledDate >= today }.sortedBy { it.scheduledDate + it.startTime }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AddRow("Schedule session", R.drawable.ic_calendar_clock, ButtonKind.Secondary, onAdd)
        if (upcoming.isEmpty()) {
            EmptyHint(
                if (data.groups.isEmpty()) "No upcoming group sessions. Create a group first, then schedule a session so everyone commits in advance."
                else "No upcoming group sessions. Schedule one so your group commits in advance.",
            )
        }
        upcoming.forEach { session ->
            val date = parseDate(session.scheduledDate) ?: LocalDate.now()
            SoftRow(session.color) {
                Column(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.background.copy(alpha = 0.7f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(DAY_NAMES_SHORT[date.dayOfWeek.value - 1], style = MaterialTheme.typography.labelSmall, color = colors.mutedForeground)
                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = colors.foreground)
                }
                RowText(
                    title = session.groupName,
                    subtitle = "${session.topic} · ${date.dayOfMonth} ${MONTH_NAMES_SHORT[date.monthValue - 1]} · ${session.startTime} · ${formatMinutes(session.durationMinutes)}",
                )
                TrashButton(onClick = { viewModel.removeGroupSession(session.id) }, description = "Cancel session")
            }
        }
    }
}

@Composable
private fun FriendDialog(viewModel: StudyViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(PastelColor.Blush) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = "Add a friend",
        confirmLabel = "Add",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.addFriend(name, email, color, notes)
            if (error == null) onDone()
        },
        error = error,
    ) {
        StudyTextField(name, { name = it }, label = "Name", placeholder = "e.g. Aisha", capitalization = KeyboardCapitalization.Words)
        StudyTextField(email, { email = it }, label = "Email (optional)", placeholder = "aisha@example.com", keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None)
        Column {
            FieldLabel("Colour")
            ColorPicker(selected = color, onSelect = { color = it })
        }
        StudyTextField(notes, { notes = it }, label = "Notes", placeholder = "study buddy for Maths", singleLine = false)
    }
}

@Composable
private fun GroupDialog(data: AppData, viewModel: StudyViewModel, onDone: () -> Unit) {
    val colors = StudyTheme.colors
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(PastelColor.BabyBlue) }
    var members by remember { mutableStateOf(emptyList<String>()) }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = "Create a study group",
        confirmLabel = "Create group",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.addGroup(name, subject, color, members, description)
            if (error == null) onDone()
        },
        error = error,
    ) {
        StudyTextField(name, { name = it }, label = "Group name", placeholder = "e.g. Chem Crew", capitalization = KeyboardCapitalization.Words)
        StudyTextField(subject, { subject = it }, label = "Shared subject (optional)", placeholder = "e.g. Chemistry")
        Column {
            FieldLabel("Colour")
            ColorPicker(selected = color, onSelect = { color = it })
        }
        Column {
            FieldLabel("Members")
            if (data.friends.isEmpty()) {
                Text("Add friends first to include them.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.friends.forEach { friend ->
                        val selected = friend.name in members
                        ToggleChip(friend.name, selected, onClick = {
                            members = if (selected) members - friend.name else members + friend.name
                        })
                    }
                }
            }
        }
        StudyTextField(description, { description = it }, label = "Description", singleLine = false)
    }
}

@Composable
private fun SessionDialog(data: AppData, viewModel: StudyViewModel, onDone: () -> Unit) {
    var groupId by remember { mutableStateOf(if (data.groups.size == 1) data.groups.first().id else "") }
    var topic by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var time by remember { mutableStateOf("18:00") }
    var duration by remember { mutableIntStateOf(60) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = "Schedule a group session",
        confirmLabel = "Schedule",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.addGroupSession(groupId, topic, date, time, duration)
            if (error == null) onDone()
        },
        error = error,
    ) {
        SelectField("Group", data.groups.map { it.id to it.name }, groupId.ifEmpty { null }, { groupId = it }, placeholder = "Pick group")
        StudyTextField(topic, { topic = it }, label = "Topic", placeholder = "e.g. Past paper review")
        DateField("Date", date, { date = it })
        TimeField("Time", time, { time = it })
        Column {
            FieldLabel("Duration: ${formatMinutes(duration)}")
            MinutesSlider(duration, { duration = it }, min = 30, max = 180, step = 15)
        }
    }
}
