package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R

/** Each row keeps its identity and focus while it moves. No nested movement dialog. */
@Composable
internal fun OrderEditor(order: List<String>, label: (String) -> String, hidden: Set<String> = emptySet(),
    required: Set<String> = emptySet(), prefix: String, onOrder: (List<String>) -> Unit,
    onVisible: ((String, Boolean) -> Unit)? = null) {
    val focus = remember(order.toSet()) { order.associateWith { listOf(FocusRequester(), FocusRequester()) } }
    order.forEachIndexed { index, id -> key(id) {
        Row(Modifier.fillMaxWidth().testTag("$prefix-option-$id"), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(label(id), style = MaterialTheme.typography.bodyLarge)
                if (id in required) Text(stringResource(R.string.settings_menu_required), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onVisible != null) IconButton(onClick = { onVisible(id, id in hidden) }, enabled = id !in required,
                modifier = Modifier.testTag("$prefix-visible-$id")) {
                Icon(if (id in hidden) SpoleIcons.EyeOff else SpoleIcons.Eye,
                    stringResource(if (id in hidden) R.string.refine_show_named else R.string.refine_hide_named, label(id)))
            }
            listOf(-1, 1).forEachIndexed { button, direction ->
                val interaction = remember { MutableInteractionSource() }
                IconButton(onClick = {
                    val destination = index + direction
                    focus.getValue(id)[if (destination == 0) 1 else if (destination == order.lastIndex) 0 else button].requestFocus()
                    onOrder(order.toMutableList().apply { removeAt(index); add(destination, id) })
                }, enabled = index + direction in order.indices, interactionSource = interaction,
                    modifier = Modifier.focusRequester(focus.getValue(id)[button])
                        .focusOutline(interaction, RoundedCornerShape(8.dp))
                        .testTag("$prefix-${if (direction < 0) "up" else "down"}-$id")) {
                    Icon(if (direction < 0) SpoleIcons.ChevronUp else SpoleIcons.ChevronDown,
                        stringResource(if (direction < 0) R.string.home_order_up else R.string.home_order_down, label(id)))
                }
            }
        }
    } }
}
