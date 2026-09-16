package app.reelstack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
internal fun SpoleChoiceDialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val height = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .82f }
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 480.dp).fillMaxWidth(.92f).heightIn(max = height).testTag("choice-dialog"),
            shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) { Icon(SpoleIcons.Close, stringResource(R.string.action_close)) }
                }
                HorizontalDivider(Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(1f, fill = false)) { content() }
            }
        }
    }
}

@Composable
internal fun SpoleChoiceRow(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
        role = Role.RadioButton; this.selected = selected
    },
        shape = RoundedCornerShape(4.dp), interactionSource = interaction,
        color = if (focused) MaterialTheme.colorScheme.onSurface.copy(alpha = .14f) else androidx.compose.ui.graphics.Color.Transparent,
        border = if (focused) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface) else null) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Box(Modifier.size(20.dp)) { if (selected) Icon(SpoleIcons.Done, null) }
        }
    }
}
