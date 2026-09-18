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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.focusScale

/** One transport row, one timeline and one tools row, with explicit vertical remote paths. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BoxScope.TvPlaybackOverlay(state: PlayerScreenState, shown: Boolean, seekPreview: Long?,
    playFocus: FocusRequester, nextFocus: FocusRequester?, onToggle: () -> Unit, onSeek: (Long) -> Unit,
    onAudio: () -> Unit, onSubtitles: () -> Unit, onQuality: () -> Unit, fillVideo: Boolean,
    onFrame: () -> Unit, onInteraction: () -> Unit, onFocusWithin: (Boolean) -> Unit,
    onChapters: () -> Unit = {}, onStats: () -> Unit = {}) {
    val timeline = remember { FocusRequester() }
    val tools = remember { FocusRequester() }
    val wantsPlayback = state.playing || state.playWhenReady && !state.ended
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
        if (seekPreview != null && state.chapters.any { it.imageUrl != null }) TimelineThumbnailPreview(
            position, state.durationMs, state.chapters, Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp), source = state.source)
        else if (seekPreview != null) Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
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
        var logoFailed by remember(state.logoUrl) { mutableStateOf(false) }
        if (state.logoUrl != null && !logoFailed) {
            app.reelstack.ui.components.MediaArtwork(state.logoUrl, state.title,
                Modifier.width(240.dp).height(72.dp).testTag("player-clearlogo"),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                alignment = Alignment.CenterStart,
                trimTransparent = true,
                source = state.source, onError = { logoFailed = true })
        } else Text(state.title, style = MaterialTheme.typography.titleLarge, color = Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(app.reelstack.ui.components.episodeLine(state.season, state.episode, state.subtitle),
            modifier = Modifier.padding(top = 8.dp).testTag("player-episode-label"),
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
            TvPlayerAction(if (wantsPlayback) SpoleIcons.Pause else SpoleIcons.PlaySimple,
                stringResource(if (wantsPlayback) R.string.player_pause else R.string.player_play), "player-toggle",
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
            .padding(horizontal = 12.dp).testTag("player-timeline"), contentAlignment = Alignment.CenterStart) {
            // A 4 dp line with a 4 dp dot is a control you have to lean forward to read. The
            // unfocused state is what you look at while the film is playing, so it is the one that
            // had to grow.
            Box(Modifier.fillMaxWidth().height(if (timelineFocused) 8.dp else 6.dp).background(Color.White.copy(alpha = .28f), RoundedCornerShape(4.dp)))
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(if (timelineFocused) 8.dp else 6.dp).background(Color.White, RoundedCornerShape(4.dp)))
            Canvas(Modifier.matchParentSize()) {
                val radius = if (timelineFocused) 11.dp.toPx() else 6.dp.toPx()
                drawCircle(Color.White, radius, androidx.compose.ui.geometry.Offset(
                    (size.width * progress).coerceIn(radius, size.width.coerceAtLeast(radius * 2) - radius), size.height / 2))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(playbackTime(position), style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(playbackTime(state.durationMs), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = .72f))
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TvPlayerAction(SpoleIcons.Info, "Stats for Nerds", "player-stats", Modifier.focusProperties { up = timeline }, labelVisible = true) { onInteraction(); onStats() }
            if (state.chapters.isNotEmpty()) TvPlayerAction(SpoleIcons.Library, stringResource(R.string.phase_chapters), "player-chapters",
                Modifier.focusProperties { up = timeline }, labelVisible = true) { onInteraction(); onChapters() }
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
        // Four modes, not two. Copying the picture and converting only the sound is a different
        // thing from re-encoding the picture, and this is the line where the household finds out
        // which one their server is doing.
        Text(
            listOfNotNull(
                stringResource(playbackModeLabel(state.mode), state.source.displayName),
                playbackReasonFor(state)?.let { stringResource(R.string.player_reason_because, stringResource(it)) },
            ).joinToString(". "),
            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .65f),
            modifier = Modifier.testTag("player-mode-line"),
        )
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color.White, trackColor = Color.White.copy(alpha = .15f))
        state.warning?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .72f)) }
        if (state.subtitleUnavailable) Text(stringResource(R.string.player_subtitle_unavailable),
            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .72f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TvPlayerAction(icon: ImageVector, label: String, tag: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, labelVisible: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val tooltip = rememberTooltipState()
    val windowFocused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused, windowFocused) { if (focused && windowFocused) tooltip.show() else tooltip.dismiss() }
    val shape = RoundedCornerShape(8.dp)
    // The transport row used to draw its own 1.5 dp edge while every card in the app used
    // `focusOutline` — two different answers to the same question, and the weaker one sat over
    // moving video where contrast is worst. One treatment now, plus the same lift the rails have.
    val scale = focusScale(focused, pressed = false)
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = tooltip, focusable = false) {
    Surface(onClick,
        modifier.size(48.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .focusOutline(interaction, shape)
            .testTag(tag).semantics(mergeDescendants = true) { contentDescription = label; role = Role.Button },
        enabled = enabled, shape = shape, interactionSource = interaction,
        color = if (focused) Color.White.copy(alpha = .20f) else Color.Transparent,
        contentColor = Color.White.copy(alpha = if (enabled) 1f else .38f)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(if (labelVisible) 22.dp else 28.dp))
        }
    }
    }
}
