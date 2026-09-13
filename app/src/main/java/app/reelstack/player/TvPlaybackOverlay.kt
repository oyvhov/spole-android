package app.reelstack.player

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.ui.components.SpoleIcons

/** One transport row, one timeline and one tools row, with explicit vertical remote paths. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BoxScope.TvPlaybackOverlay(state: PlayerScreenState, shown: Boolean, seekPreview: Long?,
    playFocus: FocusRequester, nextFocus: FocusRequester?, onToggle: () -> Unit, onSeek: (Long) -> Unit,
    onAudio: () -> Unit, onSubtitles: () -> Unit, onQuality: () -> Unit, fillVideo: Boolean,
    onFrame: () -> Unit, onInteraction: () -> Unit, onFocusWithin: (Boolean) -> Unit) {
    val timeline = remember { FocusRequester() }
    val tools = remember { FocusRequester() }
    var timelineFocused by remember { mutableStateOf(false) }
    var target by remember(state.itemId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(target) { if (target != null) { kotlinx.coroutines.delay(1600); target = null } }
    val position = (target ?: seekPreview ?: state.positionMs).coerceIn(0, state.durationMs.coerceAtLeast(0))
    val seek: (Long) -> Unit = { delta ->
        if (state.durationMs > 0) {
            val value = (position + delta).coerceIn(0, state.durationMs)
            target = value; onInteraction(); onSeek(value)
        }
    }
    if (!shown) {
        if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp),
            color = Color.White.copy(alpha = .7f), strokeWidth = 2.dp)
        if (seekPreview != null) Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
            .testTag("player-seek-feedback"), shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = .76f)) {
            Text(playbackTime(position) + " / " + playbackTime(state.durationMs),
                Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = Color.White)
        }
        return
    }
    Box(Modifier.matchParentSize().background(Brush.verticalGradient(
        0f to Color.Black.copy(alpha = .65f), .25f to Color.Transparent,
        .52f to Color.Transparent, 1f to Color.Black.copy(alpha = .90f))).testTag("player-controls"))
    Column(Modifier.align(Alignment.TopStart).padding(horizontal = 48.dp, vertical = 27.dp)
        .fillMaxWidth(if (nextFocus == null) .9f else .46f)) {
        Text(state.title, style = MaterialTheme.typography.titleLarge, color = Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(app.reelstack.ui.components.episodeLine(state.season, state.episode, state.subtitle),
            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .72f),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().onFocusChanged { onFocusWithin(it.hasFocus) }.focusGroup()
        .padding(horizontal = 48.dp, vertical = 27.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TvPlayerAction(SpoleIcons.Replay10, stringResource(R.string.player_rewind), "player-rewind",
                Modifier.focusProperties { down = timeline; up = nextFocus ?: FocusRequester.Default }, state.durationMs > 0) { seek(-10_000) }
            TvPlayerAction(if (state.playing) SpoleIcons.Pause else SpoleIcons.Play,
                stringResource(if (state.playing) R.string.player_pause else R.string.player_play), "player-toggle",
                Modifier.focusRequester(playFocus).focusProperties { down = timeline; up = nextFocus ?: FocusRequester.Default }, !state.busy || state.durationMs > 0) { onInteraction(); onToggle() }
            TvPlayerAction(SpoleIcons.Forward10, stringResource(R.string.player_forward), "player-forward",
                Modifier.focusProperties { down = timeline; up = nextFocus ?: FocusRequester.Default }, state.durationMs > 0) { seek(10_000) }
        }
        val progress = if (state.durationMs > 0) position.toFloat() / state.durationMs else 0f
        val timelineLabel = stringResource(R.string.player_timeline)
        Box(Modifier.fillMaxWidth().height(48.dp).focusRequester(timeline)
            .focusProperties { up = playFocus; down = tools }
            .onFocusChanged { timelineFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (native.keyCode !in listOf(android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT)) false
                else {
                    if (native.action == android.view.KeyEvent.ACTION_DOWN)
                        seek(if (native.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) -10_000 else 10_000)
                    true
                }
            }.semantics {
                contentDescription = timelineLabel
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                setProgress { fraction -> onSeek((fraction.coerceIn(0f, 1f) * state.durationMs).toLong()); true }
            }.focusable(state.durationMs > 0)
            .border(if (timelineFocused) 2.dp else 0.dp, if (timelineFocused) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp).testTag("player-timeline"), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(if (timelineFocused) 8.dp else 4.dp).background(Color.White.copy(alpha = .28f), RoundedCornerShape(4.dp)))
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(if (timelineFocused) 8.dp else 4.dp).background(Color.White, RoundedCornerShape(4.dp)))
            Canvas(Modifier.matchParentSize()) {
                val radius = if (timelineFocused) 7.dp.toPx() else 4.dp.toPx()
                drawCircle(Color.White, radius, androidx.compose.ui.geometry.Offset(
                    (size.width * progress).coerceIn(radius, size.width.coerceAtLeast(radius * 2) - radius), size.height / 2))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(playbackTime(position), style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(playbackTime(state.durationMs), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = .72f))
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TvPlayerAction(SpoleIcons.Sound, stringResource(R.string.player_audio), "player-audio",
                Modifier.then(if (state.audio.isNotEmpty()) Modifier.focusRequester(tools) else Modifier).focusProperties { up = timeline },
                enabled = !state.busy && state.audio.isNotEmpty(), labelVisible = true) { onInteraction(); onAudio() }
            TvPlayerAction(SpoleIcons.Subtitles, stringResource(R.string.player_subtitles_button), "player-subtitles",
                Modifier.then(if (state.audio.isEmpty() && state.subtitles.isNotEmpty()) Modifier.focusRequester(tools) else Modifier)
                    .focusProperties { up = timeline }, enabled = !state.busy && state.subtitles.isNotEmpty(), labelVisible = true) { onInteraction(); onSubtitles() }
            TvPlayerAction(SpoleIcons.Tune, stringResource(R.string.player_quality), "player-quality",
                Modifier.then(if (state.audio.isEmpty() && state.subtitles.isEmpty()) Modifier.focusRequester(tools) else Modifier)
                    .focusProperties { up = timeline }, enabled = !state.busy, labelVisible = true) { onInteraction(); onQuality() }
            TvPlayerAction(if (fillVideo) SpoleIcons.Contract else SpoleIcons.Expand,
                stringResource(if (fillVideo) R.string.player_frame_fit else R.string.player_frame_fill), "player-frame-mode",
                Modifier.focusProperties { up = timeline }, labelVisible = true) { onInteraction(); onFrame() }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color.White, trackColor = Color.White.copy(alpha = .15f))
        state.warning?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .72f)) }
    }
}

@Composable
private fun TvPlayerAction(icon: ImageVector, label: String, tag: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, labelVisible: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(onClick, modifier.heightIn(min = 48.dp).testTag(tag).semantics { contentDescription = label },
        enabled = enabled, shape = RoundedCornerShape(12.dp), interactionSource = interaction,
        color = if (focused) Color.White.copy(alpha = .24f) else Color.White.copy(alpha = .08f),
        border = BorderStroke(1.5.dp, if (focused) Color.White else Color.Transparent),
        contentColor = Color.White.copy(alpha = if (enabled) 1f else .38f)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(24.dp))
            if (labelVisible) Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
