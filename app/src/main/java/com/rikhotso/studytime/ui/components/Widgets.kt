package com.rikhotso.studytime.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikhotso.studytime.R
import com.rikhotso.studytime.data.AvatarFrame
import com.rikhotso.studytime.data.PastelColor
import com.rikhotso.studytime.data.Priority
import com.rikhotso.studytime.data.Profile
import com.rikhotso.studytime.data.SubjectShare
import com.rikhotso.studytime.data.formatMinutes
import com.rikhotso.studytime.data.initials
import com.rikhotso.studytime.ui.rememberProfilePhoto
import com.rikhotso.studytime.ui.theme.Ink
import com.rikhotso.studytime.ui.theme.Quicksand
import com.rikhotso.studytime.ui.theme.StudyTheme
import com.rikhotso.studytime.ui.theme.soft
import com.rikhotso.studytime.ui.theme.solid

/** Smaller shared pieces: icon buttons, tags, switches, menus, confirm dialogs and the profile avatar. */

/** A round icon button with a 40dp touch area. [description] is read out by screen readers. */
@Composable
fun IconAction(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = StudyTheme.colors.mutedForeground,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier.size(40.dp), enabled = enabled) {
        StudyIcon(icon, Modifier.size(18.dp), tint = if (enabled) tint else tint.copy(alpha = 0.35f), contentDescription = description)
    }
}

/** A small accent-coloured text button for the corner of a card. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = StudyTheme.colors.primary,
    )
}

/** A small rounded label, for example a priority or a status. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, background: Color = StudyTheme.colors.muted, content: Color = StudyTheme.colors.mutedForeground) {
    Text(
        text = text,
        modifier = modifier.clip(CircleShape).background(background).padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = content,
        maxLines = 1,
    )
}

@Composable
fun PriorityTag(priority: Priority, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    when (priority) {
        Priority.Urgent -> Tag(priority.label, modifier, colors.destructive.copy(alpha = 0.16f), colors.destructive)
        Priority.High -> Tag(priority.label, modifier, colors.accent, colors.onAccent)
        Priority.Medium -> Tag(priority.label, modifier, colors.secondary, colors.onSecondary)
        Priority.Low -> Tag(priority.label, modifier)
    }
}

/** A round tick box. Filled with the accent colour when checked. */
@Composable
fun CheckCircle(checked: Boolean, onToggle: () -> Unit, description: String, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) colors.primary else Color.Transparent)
                .border(2.dp, if (checked) colors.primary else colors.mutedForeground.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) StudyIcon(R.drawable.ic_check, Modifier.size(14.dp), tint = colors.onPrimary)
        }
    }
}

/** A setting with a switch. The whole row can be tapped. */
@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, subtitle: String = "") {
    val colors = StudyTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.foreground)
            if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.mutedForeground,
                uncheckedTrackColor = colors.muted,
                uncheckedBorderColor = colors.border,
            ),
        )
    }
}

/** Asks before something that cannot be undone. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    val colors = StudyTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { StudyButton(confirmLabel, onClick = onConfirm, kind = if (danger) ButtonKind.Danger else ButtonKind.Primary) },
        dismissButton = { StudyButton("Cancel", onClick = onDismiss, kind = ButtonKind.Ghost) },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        containerColor = colors.background,
        titleContentColor = colors.foreground,
        textContentColor = colors.mutedForeground,
        shape = CardShape,
    )
}

/** A message with a single OK button. */
@Composable
fun NoticeDialog(title: String, text: String, onDone: () -> Unit) {
    val colors = StudyTheme.colors
    AlertDialog(
        onDismissRequest = onDone,
        confirmButton = { StudyButton("OK", onClick = onDone) },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        containerColor = colors.background,
        titleContentColor = colors.foreground,
        textContentColor = colors.mutedForeground,
        shape = CardShape,
    )
}

data class MenuItem(val label: String, val danger: Boolean = false, val onClick: () -> Unit)

/** The three-dot button that opens a short list of actions for a row. */
@Composable
fun MoreMenu(description: String, items: List<MenuItem>, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        IconAction(R.drawable.ic_ellipsis_vertical, description, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label, style = MaterialTheme.typography.bodyLarge, color = if (item.danger) colors.destructive else colors.foreground) },
                    onClick = {
                        open = false
                        item.onClick()
                    },
                )
            }
        }
    }
}

/** Five dots for rating how well a topic is understood. Tapping the current rating clears it. */
@Composable
fun ConfidencePicker(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    val words = listOf("Not rated", "Lost", "Shaky", "Getting there", "Good", "Solid")
    Row(modifier.semantics { stateDescription = "Confidence: ${words[value.coerceIn(0, 5)]}" }, verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { level ->
            val filled = level <= value
            Box(
                Modifier
                    .size(width = 26.dp, height = 36.dp)
                    .selectable(selected = level == value, role = Role.RadioButton, onClick = { onChange(if (level == value) 0 else level) })
                    .semantics { contentDescription = "${words[level]}, $level of 5" },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) colors.primary else Color.Transparent)
                        .border(2.dp, if (filled) colors.primary else colors.border, CircleShape),
                )
            }
        }
    }
}

/** One horizontal bar per subject, longest first, with the time and share written next to it. */
@Composable
fun ShareBars(shares: List<SubjectShare>, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    val highest = shares.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        shares.forEach { share ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorDot(share.color, size = 10.dp)
                    Text(share.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${formatMinutes(share.minutes)} · ${share.percent}%", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
                }
                Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(colors.muted)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((share.minutes / highest.toFloat()).coerceIn(0.02f, 1f)).clip(CircleShape).background(share.color.solid))
                }
            }
        }
    }
}

/**
 * The profile picture: the chosen photo when there is one, otherwise the initials on the chosen
 * colour. The frame is a cosmetic reward drawn around it.
 */
@Composable
fun ProfileAvatar(profile: Profile, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val colors = StudyTheme.colors
    val shape = RoundedCornerShape(size * 0.34f)
    val photo = rememberProfilePhoto(profile.picturePath)
    val framed = when (profile.avatarFrame) {
        AvatarFrame.None -> modifier
        AvatarFrame.Ring -> modifier.border(3.dp, colors.primary, RoundedCornerShape(size * 0.4f)).padding(5.dp)
        AvatarFrame.Double -> modifier
            .border(2.dp, colors.primary, RoundedCornerShape(size * 0.42f)).padding(4.dp)
            .border(2.dp, PastelColor.Blush.solid, RoundedCornerShape(size * 0.38f)).padding(4.dp)
        AvatarFrame.Rainbow -> modifier
            .border(
                4.dp,
                Brush.sweepGradient(listOf(PastelColor.Lavender.solid, PastelColor.BabyBlue.solid, PastelColor.Mint.solid, PastelColor.Butter.solid, PastelColor.Blush.solid, PastelColor.Lavender.solid)),
                RoundedCornerShape(size * 0.42f),
            )
            .padding(6.dp)
    }
    Box(
        framed.size(size).clip(shape).background(profile.avatarColor.soft).semantics { contentDescription = "Profile picture" },
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(bitmap = photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                initials(profile.name),
                color = if (colors.isDark) colors.foreground else Ink,
                fontFamily = Quicksand,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = (size.value * 0.32f).sp,
            )
        }
    }
}

/** A quiet explanatory line with a small icon, used for hints and honest limits. */
@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier, @DrawableRes icon: Int = R.drawable.ic_info) {
    val colors = StudyTheme.colors
    Row(
        modifier.fillMaxWidth().clip(TileShape).background(colors.muted.copy(alpha = 0.6f)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StudyIcon(icon, Modifier.padding(top = 2.dp).size(16.dp), tint = colors.mutedForeground)
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
    }
}

/** A number with a label under it, for a row of small figures inside a card. */
@Composable
fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    Column(
        modifier.clip(TileShape).background(colors.muted.copy(alpha = 0.6f)).padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = colors.foreground, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A single-line search box with a magnifier and a clear button. */
@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val colors = StudyTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = colors.mutedForeground) },
        leadingIcon = { StudyIcon(R.drawable.ic_search, Modifier.size(18.dp), tint = colors.mutedForeground) },
        trailingIcon = if (value.isEmpty()) null else {
            { IconAction(R.drawable.ic_x, "Clear search", onClick = { onChange("") }) }
        },
        singleLine = true,
        shape = CircleShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.foreground,
            unfocusedTextColor = colors.foreground,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.border,
            cursorColor = colors.primary,
            focusedContainerColor = colors.card,
            unfocusedContainerColor = colors.card,
        ),
    )
}
