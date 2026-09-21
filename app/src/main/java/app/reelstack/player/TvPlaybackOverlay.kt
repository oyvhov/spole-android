package app.reelstack.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onChapters: () -> Unit = {}, onStats: () -> Unit = {},
    showPlaybackModeLine: Boolean = true,
    /** Kids mode: no tools row, parental defaults. */
    kids: Boolean = false) {
    val tools = remember { FocusRequester() }
    val wantsPlayback = state.playing || state.playWhenReady && !state.ended
    var timelineFocused by remember { mutableStateOf(false) }
    var target by remember(state.itemId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(target) { if (target != null) { kotlinx.coroutines.delay(1600); target = null } }
    val position = (target ?: seekPreview ?: state.positionMs).coerceIn(0, state.durationMs.coerceAtLeast(0))

    var transientSeekDelta by remember { mutableIntStateOf(0) }
    var transientSeekTime by remember { mutableLongStateOf(0L) }
    var transientSeekIsForward by remember { mutableStateOf(true) }

    val seek: (Long) -> Unit = { delta ->
        if (state.durationMs > 0) {
            val isFwd = delta > 0
            val now = System.currentTimeMillis()
            if (transientSeekTime > 0 && now - transientSeekTime < 1000L && transientSeekIsForward == isFwd) {
                transientSeekDelta += if (isFwd) 10 else -10
            } else {
                transientSeekDelta = if (isFwd) 10 else -10
                transientSeekIsForward = isFwd
            }
            transientSeekTime = now

            val value = (position + delta).coerceIn(0, state.durationMs)
            target = value; onInteraction(); onSeek(value)
        }
    }

    var lastObservedPreview by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(seekPreview) {
        if (seekPreview != null && seekPreview != lastObservedPreview) {
            val prev = lastObservedPreview ?: state.positionMs
            val delta = seekPreview - prev
            if (delta != 0L) {
                val isFwd = delta > 0
                val now = System.currentTimeMillis()
                if (transientSeekTime > 0 && now - transientSeekTime < 1000L && transientSeekIsForward == isFwd) {
                    transientSeekDelta += if (isFwd) 10 else -10
                } else {
                    transientSeekDelta = if (isFwd) 10 else -10
                    transientSeekIsForward = isFwd
                }
                transientSeekTime = now
            }
            lastObservedPreview = seekPreview
        } else if (seekPreview == null) {
            lastObservedPreview = null
        }
    }

    LaunchedEffect(transientSeekTime) {
        if (transientSeekTime > 0L) {
            kotlinx.coroutines.delay(1200)
            transientSeekDelta = 0
            transientSeekTime = 0L
        }
    }

    // Elegant transient "Spol 10" indicator in screen center (clean floating icon + text directly over video)
    val transientIndicator = @Composable {
        AnimatedVisibility(
            visible = transientSeekTime > 0L && transientSeekDelta != 0,
            enter = fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.82f),
            exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = 0.90f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .testTag("player-seek-transient-indicator")
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = if (transientSeekIsForward) SpoleIcons.Forward10 else SpoleIcons.Replay10,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp),
                )
                Text(
                    text = if (transientSeekDelta > 0) "+$transientSeekDelta s" else "$transientSeekDelta s",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
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
        transientIndicator()
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
        val progress = if (state.durationMs > 0) position.toFloat() / state.durationMs else 0f
        val timelineLabel = stringResource(R.string.player_timeline)
        Box(Modifier.fillMaxWidth().height(48.dp).focusRequester(playFocus)
            .focusProperties {
                up = nextFocus ?: FocusRequester.Default
                down = if (!kids) tools else FocusRequester.Default
            }
            .onFocusChanged { timelineFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (native.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (native.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            seek(-10_000)
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            seek(10_000)
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_BUTTON_A -> {
                            onInteraction()
                            onToggle()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onInteraction()
                onToggle()
            }
            .semantics {
                contentDescription = timelineLabel
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                setProgress { fraction -> onSeek((fraction.coerceIn(0f, 1f) * state.durationMs).toLong()); true }
            }.focusable(state.durationMs > 0)
            .padding(horizontal = 12.dp).testTag("player-timeline"), contentAlignment = Alignment.CenterStart) {
            val trackHeight = if (kids) (if (timelineFocused) 9.dp else 7.dp) else (if (timelineFocused) 8.dp else 6.dp)
            Box(Modifier.fillMaxWidth().height(trackHeight).background(Color.White.copy(alpha = .28f), RoundedCornerShape(4.dp)))
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(trackHeight).background(Color.White, RoundedCornerShape(4.dp)))
            Canvas(Modifier.matchParentSize()) {
                val radius = if (kids) (if (timelineFocused) 12.dp.toPx() else 10.dp.toPx())
                    else (if (timelineFocused) 11.dp.toPx() else 6.dp.toPx())
                drawCircle(Color.White, radius, androidx.compose.ui.geometry.Offset(
                    (size.width * progress).coerceIn(radius, size.width.coerceAtLeast(radius * 2) - radius), size.height / 2))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(playbackTime(position), style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(playbackTime(state.durationMs), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = .72f))
        }
        if (!kids) FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TvPlayerAction(SpoleIcons.Info, "Stats for Nerds", "player-stats", Modifier.focusProperties { up = playFocus }, labelVisible = true) { onInteraction(); onStats() }
            if (state.chapters.isNotEmpty()) TvPlayerAction(SpoleIcons.Library, stringResource(R.string.phase_chapters), "player-chapters",
                Modifier.focusProperties { up = playFocus }, labelVisible = true) { onInteraction(); onChapters() }
            TvPlayerAction(SpoleIcons.Sound, stringResource(R.string.player_audio), "player-audio",
                Modifier.then(if (state.audio.isNotEmpty()) Modifier.focusRequester(tools) else Modifier).focusProperties { up = playFocus },
                enabled = !state.busy && state.audio.isNotEmpty(), labelVisible = true) { onInteraction(); onAudio() }
            TvPlayerAction(SpoleIcons.Subtitles, stringResource(R.string.player_subtitles_button), "player-subtitles",
                Modifier.then(if (state.audio.isEmpty() && state.subtitles.isNotEmpty()) Modifier.focusRequester(tools) else Modifier)
                    .focusProperties { up = playFocus }, enabled = !state.busy && state.subtitles.isNotEmpty(), labelVisible = true) { onInteraction(); onSubtitles() }
            TvPlayerAction(SpoleIcons.Tune, stringResource(R.string.player_quality), "player-quality",
                Modifier.then(if (state.audio.isEmpty() && state.subtitles.isEmpty()) Modifier.focusRequester(tools) else Modifier)
                    .focusProperties { up = playFocus }, enabled = !state.busy, labelVisible = true) { onInteraction(); onQuality() }
            TvPlayerAction(if (fillVideo) SpoleIcons.Contract else SpoleIcons.Expand,
                stringResource(if (fillVideo) R.string.player_frame_fit else R.string.player_frame_fill), "player-frame-mode",
                Modifier.focusProperties { up = playFocus }, labelVisible = true) { onInteraction(); onFrame() }
        }
        if (showPlaybackModeLine) Text(
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
    transientIndicator()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TvPlayerAction(icon: ImageVector, label: String, tag: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, labelVisible: Boolean = false, showTooltip: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val tooltip = rememberTooltipState()
    val windowFocused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused, windowFocused) { if (focused && windowFocused && showTooltip) tooltip.show() else tooltip.dismiss() }
    val shape = RoundedCornerShape(8.dp)
    // The transport row used to draw its own 1.5 dp edge while every card in the app used
    // `focusOutline` — two different answers to the same question, and the weaker one sat over
    // moving video where contrast is worst. One treatment now, plus the same lift the rails have.
    val scale = focusScale(focused, pressed = false)
    val button = @Composable {
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
    if (showTooltip) {
        TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(label) } }, state = tooltip, focusable = false) {
            button()
        }
    } else {
        button()
    }
}
