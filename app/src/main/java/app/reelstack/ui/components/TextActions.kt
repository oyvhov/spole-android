package app.reelstack.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.reelstack.ui.theme.Text as TextColor

data class TextAction(val label: String, val icon: ImageVector, val tag: String, val onClick: () -> Unit)

/** Actions as words with an icon, in a line that wraps. The first can take a remote's focus. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextActions(actions: List<TextAction>, modifier: Modifier = Modifier, focusFirst: Boolean = false) {
    if (actions.isEmpty()) return
    val first = remember { FocusRequester() }
    LaunchedEffect(focusFirst, actions.first().tag) {
        if (!focusFirst) return@LaunchedEffect
        repeat(40) {
            withFrameNanos { }
            if (runCatching { first.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        actions.forEachIndexed { index, action ->
            key(action.tag) {
                val interaction = remember { MutableInteractionSource() }
                Row(Modifier.heightIn(min = 48.dp)
                    .then(if (index == 0) Modifier.focusRequester(first) else Modifier)
                    .focusOutline(interaction, RoundedCornerShape(24.dp), glow = false)
                    .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = action.onClick)
                    .padding(horizontal = 16.dp).testTag(action.tag),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(action.icon, null, Modifier.size(20.dp), tint = TextColor)
                    Text(action.label, color = TextColor, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
