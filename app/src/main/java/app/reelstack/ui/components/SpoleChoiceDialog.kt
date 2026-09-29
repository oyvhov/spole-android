package app.reelstack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reelstack.R

/** Shared compact menu surface. Only the options scroll; the title and close target stay put. */
@Composable
internal fun SpoleChoiceDialog(title: String, onDismiss: () -> Unit, icon: ImageVector = SpoleIcons.Tune, content: @Composable () -> Unit) {
    val height = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .82f }
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 480.dp).fillMaxWidth(.92f).heightIn(max = height).testTag("choice-dialog"),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    SettingsIconBadge(icon, MaterialTheme.colorScheme.primary, 40.dp)
                    Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(SpoleIcons.Close, stringResource(R.string.action_close)) }
                }
                Box(Modifier.weight(1f, fill = false)) { content() }
            }
        }
    }
}

@Composable
internal fun SpoleChoiceRow(label: String, selected: Boolean, modifier: Modifier = Modifier,
    icon: ImageVector? = null, supportingText: String? = null, checkmark: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(modifier.padding(vertical = 3.dp).fillMaxWidth().heightIn(min = 56.dp)
        .settingsSurface(interaction, selected = selected)
        .selectable(selected, role = Role.RadioButton, interactionSource = interaction,
            indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) SettingsIconBadge(icon, size = 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (checkmark) {
            if (selected) Icon(SpoleIcons.Done, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            else Spacer(Modifier.size(24.dp))
        } else RadioButton(selected, onClick = null, modifier = Modifier.size(24.dp))
    }
}
