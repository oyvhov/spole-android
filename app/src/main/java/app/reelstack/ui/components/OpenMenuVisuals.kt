package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Lightweight action with a full touch/remote target and no resting container. */
@Composable
internal fun OpenMenuAction(onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    TextButton(onClick, modifier.heightIn(min = 48.dp)
        .focusOutline(interaction, RoundedCornerShape(10.dp), glow = false),
        enabled = enabled, interactionSource = interaction,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), content = content)
}

@Composable
internal fun OpenFilterTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.heightIn(min = 48.dp).focusOutline(interaction, RoundedCornerShape(8.dp), glow = false)
        .selectable(selected, role = Role.Tab, interactionSource = interaction,
            indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.width(24.dp).height(2.dp).background(
            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(1.dp)))
    }
}

/** Useful empty states stay part of the page, without a card or a warning treatment. */
@Composable
internal fun QuietEmptyState(message: String, icon: ImageVector, modifier: Modifier = Modifier,
    hint: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.padding(top = 3.dp).size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            action?.invoke()
        }
    }
}
