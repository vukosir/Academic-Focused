package com.rikhotso.studytime.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.SearchKind
import com.rikhotso.studytime.data.SearchResult
import com.rikhotso.studytime.data.searchAll
import com.rikhotso.studytime.ui.AppNav
import com.rikhotso.studytime.ui.PlannerTab
import com.rikhotso.studytime.ui.ProfileTab
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.IconAction
import com.rikhotso.studytime.ui.components.SearchField
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.components.TileShape
import com.rikhotso.studytime.ui.theme.StudyTheme

/** One search box for subjects, tasks, notes, topics, exams, planned sessions and study history. */
@Composable
fun SearchScreen(data: AppData, nav: AppNav, onClose: () -> Unit) {
    val colors = StudyTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, data) { searchAll(data, query) }
    val focus = remember { FocusRequester() }
    // Puts the cursor in the box as soon as the screen opens.
    LaunchedEffect(Unit) {
        try {
            focus.requestFocus()
        } catch (e: IllegalStateException) {
            // The field was not on screen yet. The user can still tap it.
        }
    }

    fun open(result: SearchResult) {
        when (result.kind) {
            SearchKind.Subject -> nav.openSubject(result.targetId)
            SearchKind.Note, SearchKind.Topic -> if (result.subjectId.isNotEmpty()) nav.openSubject(result.subjectId) else nav.openPlanner(PlannerTab.Timetable)
            SearchKind.Task -> nav.openPlanner(PlannerTab.Tasks)
            SearchKind.Exam -> nav.openPlanner(PlannerTab.Exams)
            SearchKind.Planned -> nav.openPlanner(PlannerTab.Month)
            SearchKind.Session -> nav.openProfile(ProfileTab.Stats)
        }
    }

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconAction(R.drawable.ic_arrow_left, "Close search", onClick = onClose, tint = colors.foreground)
            SearchField(query, { query = it }, placeholder = "Search studytime", modifier = Modifier.weight(1f).focusRequester(focus))
        }

        when {
            query.trim().length < 2 -> EmptyHint("Type at least two letters to search your subjects, tasks, notes, topics, exams, planned sessions and study history.")
            results.isEmpty() -> EmptyHint("Nothing matches \"${query.trim()}\".")
            else -> SearchKind.entries.forEach { kind ->
                val group = results.filter { it.kind == kind }
                if (group.isNotEmpty()) {
                    SectionCard(title = kind.label) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            group.forEach { result ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(TileShape)
                                        .clickable(role = Role.Button, onClickLabel = "Open") { open(result) }
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    StudyIcon(
                                        when (kind) {
                                            SearchKind.Subject -> R.drawable.ic_book_open
                                            SearchKind.Task -> R.drawable.ic_list_checks
                                            SearchKind.Note -> R.drawable.ic_file_text
                                            SearchKind.Topic -> R.drawable.ic_lightbulb
                                            SearchKind.Exam -> R.drawable.ic_graduation_cap
                                            SearchKind.Planned -> R.drawable.ic_calendar_clock
                                            SearchKind.Session -> R.drawable.ic_history
                                        },
                                        Modifier.size(18.dp),
                                        tint = colors.mutedForeground,
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(result.title, style = MaterialTheme.typography.titleSmall, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(result.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    StudyIcon(R.drawable.ic_chevron_right, Modifier.size(16.dp), tint = colors.mutedForeground)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
