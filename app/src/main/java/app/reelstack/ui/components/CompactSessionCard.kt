package app.reelstack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.PlaybackSession
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.SurfaceRaised

/** A complete cover beside readable context. Geometry never depends on the decoded image. */
@Composable
internal fun CompactSessionCard(session: PlaybackSession, pending: Boolean, controlsLocked: Boolean,
    onOpen: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val toggleInteraction = remember { MutableInteractionSource() }
    val progress by animateFloatAsState(session.progress.coerceIn(0f, 1f), label = "compact-session-progress")
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.clip(shape).focusOutline(interaction, shape, glow = false)
        .clickable(interactionSource = interaction, indication = mediaCardIndication(),
            onClickLabel = stringResource(R.string.details_playback), onClick = onOpen)
        .testTag("compact-session-${session.key}").padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val beside = maxWidth >= 280.dp && LocalDensity.current.fontScale < 1.6f
            val artwork: @Composable () -> Unit = {
                Box(Modifier.width(84.dp).height(126.dp).testTag("compact-session-art-${session.key}")) {
                    MediaArtwork(session.artworkUrl, null, Modifier.fillMaxSize().clip(RoundedCornerShape(ReelLayout.ArtworkCorner)),
                        fallbackRes = app.reelstack.ui.demoSessionArtwork(session),
                        contentScale = ContentScale.Fit, source = session.source)
                }
            }
            if (beside) Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                artwork()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SessionCardCaption(session, Modifier.fillMaxWidth())
                    SessionCardFooter(session, pending, controlsLocked, progress, onToggle, toggleInteraction)
                }
            } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                artwork()
                SessionCardCaption(session, Modifier.fillMaxWidth())
                SessionCardFooter(session, pending, controlsLocked, progress, onToggle, toggleInteraction)
            }
        }
    }
}

@Composable
private fun SessionCardFooter(session: PlaybackSession, pending: Boolean, controlsLocked: Boolean,
    progress: Float, onToggle: () -> Unit, toggleInteraction: MutableInteractionSource) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(progress = { progress }, color = MaterialTheme.colorScheme.onSurface,
            trackColor = SurfaceRaised, drawStopIndicator = {}, gapSize = 0.dp,
            modifier = Modifier.fillMaxWidth().height(3.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(sessionTimeLeft(session), color = Muted, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f))
            Surface(onClick = onToggle, enabled = !controlsLocked && !pending, shape = CircleShape,
                color = Primary, contentColor = Ink, interactionSource = toggleInteraction,
                shadowElevation = 0.dp,
                modifier = Modifier.size(48.dp).focusOutline(toggleInteraction, CircleShape, glow = false)
                    .testTag("compact-session-toggle-${session.key}")) {
                Box(contentAlignment = Alignment.Center) {
                    if (pending) CircularProgressIndicator(Modifier.size(21.dp), color = Ink, strokeWidth = 2.dp)
                    else AnimatedContent(session.paused, label = "compact-play-pause") { paused ->
                        Icon(if (paused) SpoleIcons.Play else SpoleIcons.Pause,
                            stringResource(if (paused) R.string.home_resume_playback else R.string.home_pause_playback),
                            modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCardCaption(session: PlaybackSession, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        session.source?.let { source ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ServiceSymbol(source, Modifier.size(14.dp))
                Text(source.displayName, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(session.title, style = MaterialTheme.typography.titleMedium, minLines = 2, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("compact-session-title-${session.key}"))
        Text(sessionSubtitle(session), color = Muted, style = MaterialTheme.typography.bodySmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("${sessionWho(session)} · ${sessionDevice(session)}", color = Muted,
            style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
