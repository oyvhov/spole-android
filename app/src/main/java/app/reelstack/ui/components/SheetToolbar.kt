package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.ui.theme.SurfaceRaised

/** Outside every scrolling body. Labels may grow; the close target stays at the top end. */
@Composable
internal fun SheetToolbar(
    title: String,
    closeDescription: String,
    onClose: () -> Unit,
    enabled: Boolean = true,
    onBack: (() -> Unit)? = null,
) {
    val closeFocus = remember { FocusRequester() }
    val closeInteraction = remember { MutableInteractionSource() }
    val backInteraction = remember { MutableInteractionSource() }
    val keyboardEntry = LocalSheetKeyboardEntry.current
    // One safe, visible entry target. Metadata changes must never request focus again.
    // Touch users keep normal behaviour: no automatic field focus or soft keyboard.
    LaunchedEffect(Unit) {
        if (enabled && keyboardEntry) closeFocus.requestFocus()
    }
    Row(
        Modifier.fillMaxWidth().testTag("sheet-toolbar").padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.weight(1f).heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
            if (onBack != null) {
                TextButton(onClick = onBack, enabled = enabled, interactionSource = backInteraction,
                    modifier = Modifier.focusOutline(backInteraction, CircleShape)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp))
                    Text(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_calendar), Modifier.padding(start = 8.dp))
                }
            } else {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 12.dp))
            }
        }
        IconButton(onClick = onClose, enabled = enabled, interactionSource = closeInteraction,
            modifier = Modifier.size(48.dp).focusRequester(closeFocus)
                .focusOutline(closeInteraction, CircleShape).testTag("sheet-close")) {
            Box(Modifier.size(36.dp).background(SurfaceRaised, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, closeDescription, Modifier.size(20.dp))
            }
        }
    }
}
