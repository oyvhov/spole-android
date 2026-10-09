package app.reelstack.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.ui.theme.ReelLayout

/**
 * One row of a page editor. «Tilpass framsida» and «Tilpass biblioteksida» both use it, so the two
 * pages are arranged the same way: the switch on the first line, the row's own options and its
 * place on the second. Two lines rather than one, so the controls never squeeze the title at large
 * text sizes.
 *
 * Test tags are `$prefix-row-$id`, `$prefix-visible-$id`, `$prefix-up-$id` and `$prefix-down-$id`.
 */
@Composable
internal fun LayoutEditorRow(
    prefix: String,
    id: String,
    title: String,
    subtitle: String,
    visible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    upFocus: FocusRequester,
    downFocus: FocusRequester,
    onVisibleChange: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
    /** What the arrows say they move; the title alone when it names the row on its own. */
    moveLabel: String = title,
    leading: (@Composable () -> Unit)? = null,
    options: (@Composable RowScope.() -> Unit)? = null,
    /** Lets an editor put a remote's first focus on this row's switch. */
    switchFocus: FocusRequester? = null,
) {
    // A row with nothing but its place to change keeps the arrows beside the switch. A second line
    // holding only two arrows was mostly empty space; at large text sizes the title needs the room.
    val oneLine = options == null && androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.5f
    val arrows: @Composable () -> Unit = {
        Row { listOf(-1 to canMoveUp, 1 to canMoveDown).forEach { (direction, enabled) ->
            val arrow = remember { MutableInteractionSource() }
            IconButton(onClick = { onMove(direction) }, enabled = enabled, interactionSource = arrow,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).focusOutline(arrow, CircleShape)
                    .focusRequester(if (direction < 0) upFocus else downFocus)
                    .testTag("$prefix-${if (direction < 0) "up" else "down"}-$id")) {
                Icon(if (direction < 0) SpoleIcons.ChevronUp else SpoleIcons.ChevronDown,
                    stringResource(if (direction < 0) R.string.home_order_up else R.string.home_order_down, moveLabel))
            }
        } }
    }
    // Down from the switch goes to this row's own buttons first. Left to itself, a remote went from the
    // switch to the arrows on the right and on to the next row, and a button on the left was never reached.
    val optionsEntry = remember { androidx.compose.ui.focus.FocusRequester() }
    Column(Modifier.fillMaxWidth()
        .testTag("$prefix-row-$id").padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val interaction = remember { MutableInteractionSource() }
            val inner = RoundedCornerShape(8.dp)
            Row(Modifier.weight(1f).heightIn(min = 48.dp).then(switchFocus?.let { Modifier.focusRequester(it) } ?: Modifier)
                .then(if (options != null && !oneLine) Modifier.focusProperties { down = optionsEntry } else Modifier)
                .settingsSurface(interaction, inner)
                .toggleable(visible, role = Role.Switch, interactionSource = interaction, indication = LocalIndication.current,
                    onValueChange = onVisibleChange)
                .testTag("$prefix-visible-$id").padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                leading?.invoke()
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        color = if (visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(visible, null)
            }
            if (oneLine) {
                Spacer(Modifier.width(8.dp))
                arrows()
            }
        }
        // The options take what they need and the arrows keep to the right. Giving both halves a
        // weight split the line in two and broke "Alle bibliotek" over two lines.
        if (!oneLine) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            if (options != null) Row(Modifier.weight(1f, fill = false).focusRequester(optionsEntry).focusGroup(),
                verticalAlignment = Alignment.CenterVertically) { options() } else Spacer(Modifier.width(0.dp))
            arrows()
        }
    }
}

/**
 * The window this editor opens over, read before the dialog so it is the app's window and not the
 * dialog's. The same measure the Home feature and the menu use, so an option shows where it acts.
 */
@Composable
internal fun windowLayoutPolicy(): app.reelstack.ui.layout.WindowLayoutPolicy {
    val size = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val density = androidx.compose.ui.platform.LocalDensity.current
    return with(density) { app.reelstack.ui.layout.WindowLayoutPolicy(size.width.toDp().value, size.height.toDp().value) }
}

/**
 * Where focus goes after a move. Moving a row to an end disables the arrow that moved it; focus goes
 * to the other arrow first, or Compose's own recovery would throw it somewhere else on a TV.
 */
internal fun arrowAfterMove(destination: Int, lastIndex: Int, direction: Int): Int = when {
    destination <= 0 -> 1
    destination >= lastIndex -> 0
    direction < 0 -> 0
    else -> 1
}
