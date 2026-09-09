package app.reelstack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.PlaybackSession
import app.reelstack.ui.theme.*

/** A live session is useful context, not a second full-width hero on a large screen. */
@Composable
internal fun CompactSessionCard(session: PlaybackSession, pending: Boolean, controlsLocked: Boolean,
    onOpen: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val toggleInteraction = remember { MutableInteractionSource() }
    val progress by animateFloatAsState(session.progress.coerceIn(0f, 1f), label = "compact-session-progress")
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.clip(shape).background(SurfaceRaised).focusOutline(interaction, shape)
        .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
            onClickLabel = stringResource(R.string.details_playback), onClick = onOpen)
        .padding(16.dp).testTag("compact-session-${session.key}")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MediaArtwork(session.artworkUrl,
                if (session.sessionId?.startsWith("demo-") == true) R.drawable.session_still else R.drawable.media_placeholder,
                null, Modifier.width(128.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)),
                ContentScale.Crop, source = session.source)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${session.userName} · ${session.deviceName}", color = Muted, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(session.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(session.subtitle, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggle, enabled = !controlsLocked && !pending, interactionSource = toggleInteraction,
                modifier = Modifier.size(48.dp).focusOutline(toggleInteraction, CircleShape)
                    .testTag("compact-session-toggle-${session.key}")) {
                if (pending) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(if (session.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    stringResource(if (session.paused) R.string.player_play else R.string.player_pause))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(3.dp),
                color = Primary, trackColor = Ink, drawStopIndicator = {}, gapSize = 0.dp)
            Text(session.timeLeft, color = Muted, fontSize = 12.sp)
        }
    }
}
