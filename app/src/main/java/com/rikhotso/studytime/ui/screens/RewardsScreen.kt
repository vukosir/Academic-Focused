@file:OptIn(ExperimentalLayoutApi::class)

package com.rikhotso.studytime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.StudyViewModel
import com.rikhotso.studytime.data.Accent
import com.rikhotso.studytime.data.AppData
import com.rikhotso.studytime.data.AvatarFrame
import com.rikhotso.studytime.data.Challenge
import com.rikhotso.studytime.data.ChallengeType
import com.rikhotso.studytime.data.MAX_STREAK_FREEZES
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.STREAK_DAYS_PER_FREEZE
import com.rikhotso.studytime.data.TimerFace
import com.rikhotso.studytime.data.XP_PER_PERSONAL_CHALLENGE
import com.rikhotso.studytime.data.XP_PER_TASK
import com.rikhotso.studytime.data.badges
import com.rikhotso.studytime.data.challengePercent
import com.rikhotso.studytime.data.challengeProgress
import com.rikhotso.studytime.data.dailyQuests
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.formatNumber
import com.rikhotso.studytime.data.levelInfo
import com.rikhotso.studytime.data.minutesByDay
import com.rikhotso.studytime.data.streakInfo
import com.rikhotso.studytime.data.weeklyQuests
import com.rikhotso.studytime.data.xpForLevel
import com.rikhotso.studytime.ui.components.ButtonKind
import com.rikhotso.studytime.ui.components.EmptyHint
import com.rikhotso.studytime.ui.components.FormDialog
import com.rikhotso.studytime.ui.components.InfoNote
import com.rikhotso.studytime.ui.components.MiniStat
import com.rikhotso.studytime.ui.components.ProgressBar
import com.rikhotso.studytime.ui.components.RowText
import com.rikhotso.studytime.ui.components.SectionCard
import com.rikhotso.studytime.ui.components.SelectField
import com.rikhotso.studytime.ui.components.StudyButton
import com.rikhotso.studytime.ui.components.StudyIcon
import com.rikhotso.studytime.ui.components.StudyTextField
import com.rikhotso.studytime.ui.components.Tag
import com.rikhotso.studytime.ui.components.TileShape
import com.rikhotso.studytime.ui.components.TrashButton
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft
import java.time.LocalDate

/** Points and levels, the streak, daily and weekly challenges, badges, your own challenges and unlocks. */
@Composable
fun RewardsTab(data: AppData, viewModel: StudyViewModel) {
    val colors = StudyTheme.colors
    val today = LocalDate.now()
    val level = levelInfo(data)
    val streak = streakInfo(remember(data.sessions) { minutesByDay(data.sessions) }, today, data.profile.streakMinimumMinutes)
    var adding by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionCard(title = "Level ${level.level}", action = { Text("${level.xp} points", style = MaterialTheme.typography.titleMedium, color = colors.primary) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressBar(level.percent)
                Text(
                    "${level.intoLevel} of ${level.neededForNext} points to level ${level.level + 1} (${xpForLevel(level.level + 1)} in total)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
                InfoNote(
                    "1 point for every minute of a finished focus session, $XP_PER_TASK for each task you complete, $XP_PER_PERSONAL_CHALLENGE for a challenge of your own, " +
                        "plus the bonus on each daily and weekly challenge. Points are counted from your records, so they cannot be earned twice.",
                    icon = R.drawable.ic_zap,
                )
            }
        }

        SectionCard(title = "Study streak") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("Current", "${streak.current}", Modifier.weight(1f))
                    MiniStat("Best", "${streak.best}", Modifier.weight(1f))
                    MiniStat("Freezes", "${streak.freezesLeft}/$MAX_STREAK_FREEZES", Modifier.weight(1f))
                }
                Text(
                    "A day counts once you study ${formatMinutes(data.profile.streakMinimumMinutes)}. Every $STREAK_DAYS_PER_FREEZE streak days earn a streak freeze, " +
                        "and a freeze is used by itself when you miss a day, so one off day does not undo weeks of work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                )
                if (streak.frozenDays.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StudyIcon(R.drawable.ic_snowflake, Modifier.size(14.dp), tint = colors.mutedForeground)
                        Text(
                            "Freezes used so far: ${streak.frozenDays.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.mutedForeground,
                        )
                    }
                }
            }
        }

        SectionCard(title = "Daily challenges") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                dailyQuests(data, today).forEach { QuestRow(it) }
                Text("Three new ones every day. They are optional and pay out by themselves when completed.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            }
        }

        SectionCard(title = "Weekly challenges") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                weeklyQuests(data, today).forEach { QuestRow(it) }
                Text("These run from Monday to Sunday.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            }
        }

        SectionCard(title = "Badges") {
            val all = badges(data, today)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${all.count { it.earned }} of ${all.size} earned", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
                all.forEach { badge ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(TileShape)
                            .then(if (badge.earned) Modifier.background(PastelColor.Butter.soft) else Modifier.border(1.dp, colors.border, TileShape))
                            .padding(12.dp)
                            .semantics(mergeDescendants = true) { contentDescription = "${badge.title}: ${badge.description}. ${if (badge.earned) "Earned" else "Not earned yet"}" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(if (badge.earned) PastelColor.Butter.soft else colors.muted),
                            contentAlignment = Alignment.Center,
                        ) {
                            StudyIcon(
                                if (badge.earned) R.drawable.ic_award else R.drawable.ic_lock,
                                Modifier.size(16.dp),
                                tint = if (badge.earned) colors.primary else colors.mutedForeground,
                            )
                        }
                        RowText(badge.title, badge.description)
                        if (badge.earned) Tag("Earned", background = colors.background.copy(alpha = 0.7f), content = colors.foreground)
                    }
                }
            }
        }

        SectionCard(
            title = "Your own challenges",
            action = { StudyButton("New", onClick = { adding = true }, kind = ButtonKind.Secondary, small = true, leadingIcon = R.drawable.ic_plus) },
        ) {
            if (data.challenges.isEmpty()) {
                EmptyHint("No challenges yet. Set a streak or milestone to keep yourself motivated.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.challenges.forEach { challenge ->
                        ChallengeCard(
                            challenge = challenge,
                            progress = challengeProgress(challenge, data, today),
                            onStep = { viewModel.stepMilestone(challenge.id) },
                            onDelete = { viewModel.removeChallenge(challenge.id) },
                        )
                    }
                }
            }
        }

        SectionCard(title = "Unlocks") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Levelling up unlocks new looks. Choose them in Settings and in Edit profile.", style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
                UnlockGroup("Accent colours", Accent.entries.map { it.label to it.requiredLevel }, level.level)
                UnlockGroup("Timer faces", TimerFace.entries.map { it.label to it.requiredLevel }, level.level)
                UnlockGroup("Avatar frames", AvatarFrame.entries.filter { it != AvatarFrame.None }.map { it.label to it.requiredLevel }, level.level)
            }
        }
    }

    if (adding) ChallengeDialog(viewModel, onDone = { adding = false })
}

@Composable
private fun UnlockGroup(title: String, items: List<Pair<String, Int>>, level: Int) {
    val colors = StudyTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = colors.foreground)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { (label, required) ->
                if (required <= level) {
                    Tag(label, background = colors.secondary, content = colors.onSecondary)
                } else {
                    Tag("$label · level $required")
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(challenge: Challenge, progress: Double, onStep: () -> Unit, onDelete: () -> Unit) {
    val colors = StudyTheme.colors
    val shape = TileShape
    val base = Modifier.fillMaxWidth().clip(shape)
    val styled = if (challenge.completed) {
        base.background(PastelColor.Butter.soft).border(1.dp, PastelColor.Butter.soft, shape)
    } else {
        base.border(1.dp, colors.border, shape)
    }
    Column(styled.padding(start = 12.dp, top = 6.dp, bottom = 12.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(if (challenge.completed) PastelColor.Butter.soft else PastelColor.Lavender.soft),
                contentAlignment = Alignment.Center,
            ) {
                StudyIcon(if (challenge.completed) R.drawable.ic_award else R.drawable.ic_sparkles, Modifier.size(16.dp), tint = colors.primary)
            }
            RowText(
                title = challenge.name,
                subtitle = if (challenge.completed) "Completed · ${challenge.completedDate}" else "${formatNumber(progress)}/${challenge.target} ${challenge.type.unit}",
            )
            if (!challenge.completed && challenge.type == ChallengeType.Milestone) {
                StudyButton("+1", onClick = onStep, kind = ButtonKind.Secondary, small = true)
            }
            TrashButton(onClick = onDelete, description = "Delete ${challenge.name}")
        }
        if (!challenge.completed) {
            ProgressBar(challengePercent(progress, challenge.target), Modifier.padding(end = 8.dp))
        }
        if (challenge.reward.isNotEmpty()) {
            Text("Reward: ${challenge.reward}", style = MaterialTheme.typography.labelSmall, color = colors.mutedForeground)
        }
    }
}

@Composable
private fun ChallengeDialog(viewModel: StudyViewModel, onDone: () -> Unit) {
    val colors = StudyTheme.colors
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ChallengeType.Streak) }
    var target by remember { mutableStateOf("7") }
    var reward by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = "Create a challenge",
        confirmLabel = "Create",
        onDismiss = onDone,
        onConfirm = {
            error = viewModel.addChallenge(name, type, target, reward)
            if (error == null) onDone()
        },
        error = error,
    ) {
        StudyTextField(name, { name = it }, label = "Name", placeholder = "e.g. 7-day focus streak")
        SelectField("Type", ChallengeType.entries.map { it to it.label }, type, { type = it })
        StudyTextField(target, { target = it }, label = "Target (${type.unit})", keyboardType = KeyboardType.Number)
        StudyTextField(reward, { reward = it }, label = "Reward / badge (optional)", placeholder = "e.g. Spring blossom")
        Text(
            "Streaks, goal days, sessions and hours fill in on their own from your focus sessions, counted from now. A milestone is ticked up by hand.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedForeground,
        )
    }
}
