package app.reelstack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import app.reelstack.ui.components.focusOutline
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.SurfaceRaised

/** No size animation when the code arrives. Only the dialog body can scroll. */
@Composable
internal fun TvQuickConnectPanel(draft: ConnectionDraft) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember(draft.quickConnectCode) { mutableStateOf(false) }
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Surface(color = SurfaceRaised, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("tv-quick-panel")) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.tv_quick_ready), fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(draft.quickConnectCode?.chunked(3)?.joinToString("  ") ?: "— — —  — — —",
                fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("tv-quick-code"))
            Text(stringResource(if (draft.quickConnectCode == null) R.string.tv_quick_hint else R.string.tv_quick_steps),
                color = Muted, fontSize = 16.sp, lineHeight = 23.sp)
            if (draft.quickConnectCode != null) Text(stringResource(
                if (draft.quickConnectWaiting) R.string.quick_waiting else R.string.quick_finishing),
                color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
            draft.quickConnectCode?.let { code ->
                TextButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(code)); copied = true },
                    interactionSource = interaction,
                    modifier = Modifier.heightIn(min = 48.dp).focusOutline(interaction, RoundedCornerShape(24.dp))) {
                    Text(stringResource(if (copied) R.string.quick_copied else R.string.quick_copy))
                }
            }
        }
    }
}
