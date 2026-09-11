package app.reelstack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.PlaybackSession
import app.reelstack.ui.theme.*
// The warm-white token shares its name with the Material Text composable; alias it like HomeScreen.
import app.reelstack.ui.theme.Text as TextColor

/**
 * A live session on a wide screen: useful context, not a second full-width hero.
 *
 * This used to be a grey `SurfaceRaised` plate with a 128 dp thumbnail beside a text column, which
 * made it the only thing on Home that looked like a card — Continue watching, the library rails and
 * Activity are all borderless artwork on the page ground. Two more things followed from the plate:
 * every card was as tall as its own text, so a row of them had ragged bottoms, and the progress bar
 * sat at a different height in each one.
 *
 * Now it is the phone hero at rail scale — artwork, scrim, text over the bottom — so the row reads
 * as part of the same feed. Every line is single-line, so all cards in a rail measure the same at
 * any font scale, and the progress bar is pinned to the artwork's bottom edge exactly as it is on a
 * Continue watching card.
 */
@Composable
internal fun CompactSessionCard(session: PlaybackSession, pending: Boolean, controlsLocked: Boolean,
    onOpen: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val toggleInteraction = remember { MutableInteractionSource() }
    val progress by animateFloatAsState(session.progress.coerceIn(0f, 1f), label = "compact-session-progress")
    val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    Box(
        // Minimum, not fixed: the four text lines must be able to grow with the font scale. They
        // grow together, because every card holds the same number of single lines.
        modifier
            .heightIn(min = 182.dp)
            .clip(shape)
            .focusOutline(interaction, shape)
            .clickable(interactionSource = interaction, indication = mediaCardIndication(),
                onClickLabel = stringResource(R.string.details_playback), onClick = onOpen)
            .testTag("compact-session-${session.key}"),
    ) {
        MediaArtwork(
            session.artworkUrl,
            if (session.sessionId?.startsWith("demo-") == true) R.drawable.session_still else R.drawable.media_placeholder,
            null, Modifier.matchParentSize(), ContentScale.Crop, source = session.source,
        )
        // Legibility only. The same three-stop ramp the phone card uses, so a session looks the
        // same on both form factors.
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color(0x4204050A),
                    0.42f to Color(0x7A04050A),
                    1f to Color(0xF5080710),
                ),
            ),
        )

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        "${session.userName} · ${session.deviceName}",
                        color = PrimarySoft, style = MaterialTheme.typography.labelMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        session.title, color = TextColor, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    Text(
                        session.subtitle, color = Muted, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                    )
                }
                // Playback stays its own focusable action, separate from opening the details.
                Surface(
                    onClick = onToggle,
                    enabled = !controlsLocked && !pending,
                    shape = CircleShape,
                    color = Primary,
                    contentColor = Ink,
                    interactionSource = toggleInteraction,
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(48.dp).focusOutline(toggleInteraction, CircleShape)
                        .testTag("compact-session-toggle-${session.key}"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (pending) CircularProgressIndicator(Modifier.size(21.dp), color = Ink, strokeWidth = 2.dp)
                        else AnimatedContent(session.paused, label = "compact-play-pause") { paused ->
                            Icon(
                                if (paused) app.reelstack.ui.components.SpoleIcons.Play else app.reelstack.ui.components.SpoleIcons.Pause,
                                stringResource(if (paused) R.string.player_play else R.string.player_pause),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
            Text(
                session.timeLeft, color = Muted, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp),
            )
        }

        // How far in the session is, on the artwork's own bottom edge — the same place a Continue
        // watching card puts it, rather than a separate row that shifts with the text above it.
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp)
                .background(Color.Black.copy(alpha = 0.55f)),
        ) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Primary))
        }
    }
}
