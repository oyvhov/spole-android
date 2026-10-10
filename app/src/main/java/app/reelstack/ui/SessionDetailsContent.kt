package app.reelstack.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.PlaybackSession
import app.reelstack.ui.components.*
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.SurfaceRaised

/** Live details with artwork in its own space and the remote action always within reach. */
@Composable
internal fun SessionDetailsContent(
    sessions: List<PlaybackSession>,
    sessionKey: String,
    pendingKey: String?,
    allowDemoArtwork: Boolean,
    scroll: ScrollState,
    onToggle: (String) -> Unit,
    onSelect: (String) -> Unit,
    unavailable: Boolean = false,
) {
    val session = sessions.firstOrNull { it.key == sessionKey }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().testTag("session-scroll").verticalScroll(scroll)
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (sessions.size > 1 || (session == null && sessions.isNotEmpty())) {
                SessionChooser(sessions, sessionKey, onSelect)
            }
            if (session == null) {
                Column(Modifier.testTag(if (unavailable) "session-unavailable" else "session-ended")
                    .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(if (unavailable) R.string.session_update_unavailable else R.string.session_ended),
                        style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(if (unavailable) R.string.session_update_retry else R.string.session_ended_description), color = Muted,
                        style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val beside = maxWidth >= 320.dp && LocalDensity.current.fontScale < 1.6f
                    if (beside) Row(horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        SessionArtwork(session, allowDemoArtwork, Modifier.width(120.dp).height(180.dp))
                        SessionHeading(session, Modifier.weight(1f))
                    } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SessionArtwork(session, allowDemoArtwork,
                            Modifier.fillMaxWidth().height(180.dp))
                        SessionHeading(session, Modifier.fillMaxWidth())
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LinearProgressIndicator(
                        progress = { session.progress.coerceIn(0f, 1f) },
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = SurfaceRaised,
                        drawStopIndicator = {}, gapSize = 0.dp,
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                    )
                    Text(sessionTimeLeft(session), color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SessionFact(stringResource(R.string.details_playback), sessionMethod(session))
                    session.quality.takeIf(String::isNotBlank)?.let {
                        SessionFact(stringResource(R.string.player_quality), it)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (session != null) {
            val interaction = remember { MutableInteractionSource() }
            val shape = RoundedCornerShape(18.dp)
            Button(
                onClick = { onToggle(session.key) },
                enabled = pendingKey == null,
                interactionSource = interaction, shape = shape,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)
                    .heightIn(min = 56.dp).testTag("session-toggle").focusOutline(interaction, shape),
            ) {
                if (pendingKey == session.key) {
                    CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.remote_sending_command))
                } else {
                    Icon(if (session.paused) SpoleIcons.Play else SpoleIcons.Pause, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(if (session.paused) R.string.remote_resume else R.string.remote_pause))
                }
            }
        }
    }
}

@Composable
private fun SessionHeading(session: PlaybackSession, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        session.source?.let { source ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ServiceSymbol(source, Modifier.size(16.dp))
                Text(source.displayName, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(session.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("session-title"))
        sessionSubtitle(session).takeIf(String::isNotBlank)?.let {
            Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        Text(sessionWho(session), style = MaterialTheme.typography.titleSmall)
        Text(sessionDevice(session), color = Muted, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            Icon(if (session.paused) SpoleIcons.Pause else SpoleIcons.Play, null,
                tint = Muted, modifier = Modifier.size(16.dp))
            Text(stringResource(if (session.paused) R.string.session_paused else R.string.session_playing_now),
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SessionChooser(sessions: List<PlaybackSession>, selectedKey: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(pluralStringResource(R.plurals.home_playback_count, sessions.size, sessions.size),
            color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            sessions.forEach { session ->
                val selected = session.key == selectedKey
                val interaction = remember(session.key) { MutableInteractionSource() }
                val shape = RoundedCornerShape(14.dp)
                OutlinedButton(
                    onClick = { onSelect(session.key) }, enabled = !selected,
                    interactionSource = interaction, shape = shape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = SurfaceRaised,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.widthIn(max = 220.dp).heightIn(min = 48.dp)
                        .testTag("session-switch-${session.key}").focusOutline(interaction, shape),
                ) {
                    Text("${sessionWho(session)} · ${sessionDevice(session)}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SessionArtwork(session: PlaybackSession, allowDemo: Boolean, modifier: Modifier) {
    // The outer slot stays still while decoding. Only the picture inside adopts its own ratio.
    var ratio by remember(session.artworkUrl, session.key) { mutableFloatStateOf(2f / 3f) }
    BoxWithConstraints(modifier.testTag("session-artwork"), contentAlignment = Alignment.Center) {
        val pictureWidth = minOf(maxWidth, maxHeight * ratio)
        MediaArtwork(session.artworkUrl, null,
            modifier = Modifier.width(pictureWidth).aspectRatio(ratio).clip(RoundedCornerShape(12.dp)),
            fallbackRes = if (allowDemo) demoSessionArtwork(session) else R.drawable.media_placeholder,
            source = session.source, contentScale = ContentScale.Fit,
            onAspectRatio = { if (it.isFinite() && it > 0f) ratio = it },
        )
    }
}

@Composable
private fun SessionFact(label: String, value: String) {
    if (LocalDensity.current.fontScale >= 1.6f) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(label, color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}
