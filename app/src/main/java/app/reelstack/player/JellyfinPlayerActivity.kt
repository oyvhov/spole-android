package app.reelstack.player

import app.reelstack.ui.components.focusOutline

import androidx.compose.ui.res.stringResource
import app.reelstack.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import app.reelstack.ReelstackApplication
import app.reelstack.ui.theme.ReelstackTheme
import app.reelstack.ui.components.NativeClientLauncher
import app.reelstack.ui.components.MediaArtwork
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.reelstack.data.model.ServiceKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PlayerMenu(val label: Int) {
    AUDIO(R.string.player_audio_tracks), SUBTITLES(R.string.player_subtitles), QUALITY(R.string.player_quality)
}
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class JellyfinPlayerActivity : app.reelstack.localization.LocalizedActivity() {
    internal lateinit var model: JellyfinPlayerModel
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        enterFullscreen()
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                JellyfinPlayerModel((application as ReelstackApplication).container) as T
        })[JellyfinPlayerModel::class.java]
        val id = intent.getStringExtra(ITEM_ID).orEmpty()
        if (id.isBlank() || id.length > 128) { finish(); return }
        // What the title page promised. -1 is a real subtitle value ("off"), so absence has to be
        // something else; absent still means "whatever the server would have picked".
        val preferredAudio = intent.getIntExtra(AUDIO_INDEX, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        val preferredSubtitle = intent.getIntExtra(SUBTITLE_INDEX, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        val preferredSource = intent.getStringExtra(SOURCE_ID)?.takeIf { it.isNotBlank() && it.length <= 128 }
        // Covers Back during the first frame too; the screen's menu handler takes precedence later.
        onBackPressedDispatcher.addCallback(this) { if (!model.back()) finish() }
        setContent {
            ReelstackTheme {
                LaunchedEffect(id) { model.open(id, preferredAudio, preferredSubtitle, preferredSource) }
                val state by model.state.collectAsStateWithLifecycle()
                DisposableEffect(state.playing, state.busy) {
                    if (state.playing || state.busy) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                }
                PlayerScreen(state, model.player, { if (!model.back()) finish() }, model::toggle, model::seek, model::retry, model::choose,
                    model::loadChildren, model::audio, model::subtitles, model::quality,
                    onExternal = {
                        model.background()
                        model.fallbackUrl()?.let { url -> NativeClientLauncher.open(this, url, NativeClientLauncher.resolve(this, url).packageName) }
                    }, onResume = model::resume, onRotate = {
                        requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT)
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    }, onNextEpisode = model::playNext, onCancelNextEpisode = model::cancelNextEpisode,
                    onSkipSegment = model::skipSegment)
            }
        }
    }
    private fun enterFullscreen() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterFullscreen()
    }
    override fun onStart() { super.onStart(); if (::model.isInitialized) model.foreground() }
    override fun onStop() { if (::model.isInitialized && !isChangingConfigurations) model.background(); super.onStop() }
    companion object {
        private const val ITEM_ID = "jellyfin_item_id"
        private const val AUDIO_INDEX = "jellyfin_audio_index"
        private const val SUBTITLE_INDEX = "jellyfin_subtitle_index"
        private const val SOURCE_ID = "jellyfin_source_id"
        fun open(context: Context, itemId: String, audioIndex: Int? = null, subtitleIndex: Int? = null,
            sourceId: String? = null) =
            context.startActivity(
                Intent(context, JellyfinPlayerActivity::class.java).putExtra(ITEM_ID, itemId).apply {
                    audioIndex?.let { putExtra(AUDIO_INDEX, it) }
                    subtitleIndex?.let { putExtra(SUBTITLE_INDEX, it) }
                    sourceId?.takeIf(String::isNotBlank)?.let { putExtra(SOURCE_ID, it) }
                },
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    state: PlayerScreenState, player: androidx.media3.common.Player?, onClose: () -> Unit,
    onToggle: () -> Unit, onSeek: (Long) -> Unit, onRetry: () -> Unit, onChoose: (PlayableItem) -> Unit,
    onMore: () -> Unit, onAudio: (Int) -> Unit, onSubtitle: (Int) -> Unit, onQuality: (Int) -> Unit, onExternal: () -> Unit,
    onResume: (Boolean) -> Unit = {},
    onRotate: () -> Unit = {},
    onNextEpisode: () -> Unit = {},
    onCancelNextEpisode: () -> Unit = {},
    onSkipSegment: () -> Unit = {},
    isTelevision: Boolean = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
) {
    val videoFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val nextFocus = remember { FocusRequester() }
    var nextHasFocus by remember { mutableStateOf(false) }
    var controlsHaveFocus by remember { mutableStateOf(false) }
    var consumedRemoteKey by remember { mutableIntStateOf(-1) }
    val showControlsLabel = stringResource(R.string.player_show_controls)
    var controls by remember { mutableStateOf(true) }
    var fillVideo by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf<PlayerMenu?>(null) }
    var scrubbing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var remoteSeekTargetMs by remember { mutableStateOf<Long?>(null) }
    var remoteSeekJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val accessibility = LocalAccessibilityManager.current
    // A seek may briefly buffer. That is not a pause and must not reveal the whole OSD.
    val canHide = (state.playing || state.busy && state.playWhenReady) &&
        state.error == null && !state.ended && !state.awaitingResume
    val finishedWithNext = state.ended && state.showNextEpisodeOffer()
    val showControls = controls || (!canHide && !finishedWithNext) || menu != null
    val showNextOffer = state.showNextEpisodeOffer() && menu == null && !scrubbing
    val latestShown by rememberUpdatedState(showControls)
    val latestCanHide by rememberUpdatedState(canHide && !scrubbing && menu == null)
    LaunchedEffect(canHide, finishedWithNext) { if (!canHide) controls = !finishedWithNext }
    LaunchedEffect(controls, canHide, interaction, menu, scrubbing) {
        if (controls && canHide && menu == null && !scrubbing) {
            delay(accessibility?.calculateRecommendedTimeoutMillis(3500, containsControls = true) ?: 3500)
            controls = false
        }
    }
    LaunchedEffect(isTelevision, showControls, state.busy, state.browsing, state.awaitingResume, state.error, menu) {
        if (isTelevision && menu == null && !state.browsing) {
            if (showNextOffer && state.ended) nextFocus.requestFocus()
            else if (!showControls) videoFocus.requestFocus()
            else if (!state.busy && !state.awaitingResume && state.error == null && !controlsHaveFocus && !nextHasFocus)
                playFocus.requestFocus()
        }
    }
    LaunchedEffect(isTelevision, showNextOffer, state.ended) {
        if (isTelevision && showNextOffer && state.ended) nextFocus.requestFocus()
    }
    BackHandler {
        if (menu != null) menu = null
        else if (showNextOffer && (!state.ended || state.nextEpisodeCountdown != null)) onCancelNextEpisode()
        else if (isTelevision && showControls && canHide) controls = false
        else onClose()
    }
    CompositionLocalProvider(LocalContentColor provides if (isTelevision) Color.White else MaterialTheme.colorScheme.onSurface) {
    Box(Modifier.fillMaxSize().background(Color.Black).testTag("jellyfin-player")
        .onPreviewKeyEvent { event ->
            if (!isTelevision) return@onPreviewKeyEvent false
            val native = event.nativeKeyEvent
            if (native.action == android.view.KeyEvent.ACTION_UP && consumedRemoteKey == native.keyCode) {
                consumedRemoteKey = -1
                return@onPreviewKeyEvent true
            }
            if (native.action != android.view.KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
            val key = when (native.keyCode) {
                android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER,
                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER, android.view.KeyEvent.KEYCODE_BUTTON_A -> RemotePlaybackKey.SELECT
                android.view.KeyEvent.KEYCODE_DPAD_UP -> RemotePlaybackKey.UP
                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> RemotePlaybackKey.DOWN
                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> RemotePlaybackKey.LEFT
                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> RemotePlaybackKey.RIGHT
                android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> RemotePlaybackKey.TOGGLE
                android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> RemotePlaybackKey.PLAY
                android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> RemotePlaybackKey.PAUSE
                android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> RemotePlaybackKey.REWIND
                android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> RemotePlaybackKey.FORWARD
                else -> return@onPreviewKeyEvent false
            }
            interaction++
            if (native.repeatCount > 0 && consumedRemoteKey == native.keyCode && key !in setOf(
                    RemotePlaybackKey.LEFT, RemotePlaybackKey.RIGHT, RemotePlaybackKey.REWIND, RemotePlaybackKey.FORWARD)) return@onPreviewKeyEvent true
            val action = remotePlaybackAction(key, showControls || nextHasFocus, state.playing,
                (state.busy && state.durationMs <= 0) || state.browsing || state.awaitingResume || state.error != null || menu != null)
            if (action == RemotePlaybackAction.DEFAULT) return@onPreviewKeyEvent false
            consumedRemoteKey = native.keyCode
            if (action == RemotePlaybackAction.REVEAL || action == RemotePlaybackAction.TOGGLE) controls = true
            when (action) {
                RemotePlaybackAction.TOGGLE -> onToggle()
                RemotePlaybackAction.REWIND -> {
                    val target = ((remoteSeekTargetMs ?: state.positionMs) - 10_000).coerceAtLeast(0)
                    remoteSeekTargetMs = target
                    remoteSeekJob?.cancel()
                    remoteSeekJob = scope.launch {
                        delay(180)
                        onSeek(target)
                        delay(2020)
                        remoteSeekTargetMs = null
                    }
                }
                RemotePlaybackAction.FORWARD -> if (state.durationMs > 0) {
                    val target = ((remoteSeekTargetMs ?: state.positionMs) + 10_000).coerceAtMost(state.durationMs)
                    remoteSeekTargetMs = target
                    remoteSeekJob?.cancel()
                    remoteSeekJob = scope.launch {
                        delay(180)
                        onSeek(target)
                        delay(2020)
                        remoteSeekTargetMs = null
                    }
                }
                else -> Unit
            }
            true
        }.focusRequester(videoFocus).focusable(enabled = isTelevision)) {
        if (!state.browsing && player != null) AndroidView(
            factory = { context -> PlayerView(context).apply {
                useController = false; this.player = player; setKeepContentOnPlayerReset(false)
                isFocusable = false
                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
            } },
            update = {
                it.player = player
                it.resizeMode = if (fillVideo) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize().testTag("player-video"),
        )
        // An ancestor receives unconsumed video taps; a sibling behind the scroll container cannot.
        Box(Modifier.fillMaxSize().testTag("player-touch-surface")
            .semantics { if (!showControls) onClick(showControlsLabel) { controls = true; interaction++; true } }
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Observe before the fading scroll layer: it may still own this touch.
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val revealing = !latestShown
                    // Keep the pinned Back button's original gesture intact as well.
                    if (revealing) { controls = true; interaction++ }
                    var handledByControl = false
                    var moved = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        handledByControl = handledByControl || event.changes.any { it.isConsumed }
                        moved = moved || event.changes.any { (it.position - down.position).getDistance() > viewConfiguration.touchSlop }
                    } while (event.changes.any { it.pressed })
                    if (!revealing && !handledByControl && !moved && latestCanHide) {
                        controls = false; interaction++
                    }
                }
            }) {
        if (state.browsing) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
                PlayerHeader(state.title, state.subtitleLine(), onClose)
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("player-episodes"), contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.choices, key = { it.id }) { item ->
                        Surface(onClick = { if (!state.busy) onChoose(item) }, enabled = !state.busy,
                            shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (item.type == "Season") app.reelstack.ui.components.SpoleIcons.Movie else app.reelstack.ui.components.SpoleIcons.Play, null)
                                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                    Text(
                        if (item.type == "Episode")
                            app.reelstack.ui.components.episodeLine(item.season, item.episode, item.subtitle)
                        else item.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                                    Text(when { item.played -> stringResource(R.string.player_watched); item.resumeMs > 0 -> stringResource(R.string.player_resume, playbackTime(item.resumeMs))
                                        else -> stringResource(R.string.player_available) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (state.hasMore && !state.busy) item { app.reelstack.ui.components.SpoleSecondaryButton(onClick = onMore) { Text(stringResource(R.string.action_more)) } }
                    if (state.choices.isEmpty() && !state.busy && state.error == null) item { Text(stringResource(R.string.player_no_episodes)) }
                    if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    state.error?.let { error -> item { Text(error); app.reelstack.ui.components.SpoleSecondaryButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) } } }
                }
            }
        } else if (isTelevision && !state.awaitingResume && state.error == null) {
            TvPlaybackOverlay(state, showControls, remoteSeekTargetMs, playFocus, nextFocus.takeIf { showNextOffer },
                onToggle, onSeek, { menu = PlayerMenu.AUDIO }, { menu = PlayerMenu.SUBTITLES },
                { menu = PlayerMenu.QUALITY }, fillVideo, { fillVideo = !fillVideo },
                onInteraction = { interaction++ }, onFocusWithin = { controlsHaveFocus = it })
            if (showNextOffer) NextEpisodeCard(state, onNextEpisode, onCancelNextEpisode, nextFocus,
                Modifier.align(if (showControls) Alignment.TopEnd else Alignment.BottomEnd)
                    .padding(horizontal = 48.dp, vertical = 27.dp).onFocusChanged { nextHasFocus = it.hasFocus })
            else Box(Modifier.align(Alignment.TopEnd).padding(horizontal = 48.dp, vertical = 80.dp)) {
                SkipSegmentButton(state, onSkipSegment)
            }
        } else {
            Column(Modifier.fillMaxSize()
                // The controls sit a little further in from the edges on television, so the dark
                // part of the scrim has to start further in too — otherwise the bottom row ends up
                // on the brightest part of the picture instead of on the shadow meant for it.
                .background(if (showControls) Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .72f),
                    .22f to Color.Transparent,
                    .62f to Color.Transparent,
                    .82f to Color.Black.copy(alpha = .62f),
                    1f to Color.Black.copy(alpha = .92f),
                ) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                .safeDrawingPadding()
                // A television reports no insets for the frame around its own picture, and plenty
                // of sets still crop a few percent of every edge. `safeDrawingPadding` covers the
                // system bars a phone has and nothing at all here, which is why the controls sat
                // against the very bottom of the panel. Five per cent of 960x540 dp is the margin
                // Android TV asks every app to keep.
                .then(if (isTelevision) Modifier.padding(horizontal = 48.dp, vertical = 27.dp) else Modifier)) {
                if (showControls) PlayerHeader(state.title, state.subtitleLine(), onClose, showBack = !isTelevision)
                if (showNextOffer) NextEpisodeCard(state, onNextEpisode, onCancelNextEpisode, nextFocus,
                    Modifier.align(Alignment.End))
                if (!showNextOffer) SkipSegmentButton(state, onSkipSegment)
            AnimatedVisibility(visible = showControls, enter = fadeIn(tween(90)), exit = fadeOut(tween(140)),
                modifier = Modifier.weight(1f).testTag("player-controls")) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                val viewportHeight = maxHeight
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = viewportHeight)) {
                    Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
                    if (state.awaitingResume) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(onClick = { onResume(false) }) { Text(stringResource(R.string.player_resume, playbackTime(state.positionMs))) }
                            app.reelstack.ui.components.SpoleSecondaryButton(onClick = { onResume(true) }) { Text(stringResource(R.string.player_restart)) }
                        }
                    } else if (state.error != null) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(app.reelstack.ui.components.SpoleIcons.Alert, null, tint = MaterialTheme.colorScheme.error)
                            Text(state.error, modifier = Modifier.padding(vertical = 12.dp))
                            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                            app.reelstack.ui.components.SpoleSecondaryButton(onClick = onExternal) { Text(stringResource(R.string.player_external)) }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                interaction++
                                val target = ((remoteSeekTargetMs ?: state.positionMs) - 10_000).coerceAtLeast(0)
                                remoteSeekTargetMs = target
                                onSeek(target)
                                remoteSeekJob?.cancel()
                                remoteSeekJob = scope.launch {
                                    delay(2200)
                                    remoteSeekTargetMs = null
                                }
                            }, enabled = !state.busy, modifier = Modifier.remoteFocus(isTelevision)) {
                                Icon(app.reelstack.ui.components.SpoleIcons.Replay10, stringResource(R.string.player_rewind), Modifier.size(32.dp))
                            }
                            FilledIconButton(onClick = { interaction++; onToggle() }, enabled = !state.busy, modifier = Modifier.size(72.dp).remoteFocus(isTelevision).focusRequester(playFocus).testTag("player-toggle")) {
                                if (state.busy) CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 2.dp)
                                else Icon(if (state.playing) app.reelstack.ui.components.SpoleIcons.Pause else app.reelstack.ui.components.SpoleIcons.Play,
                                    if (state.playing) stringResource(R.string.player_pause) else stringResource(R.string.player_play), Modifier.size(36.dp))
                            }
                            IconButton(onClick = {
                                interaction++
                                if (state.durationMs > 0) {
                                    val target = ((remoteSeekTargetMs ?: state.positionMs) + 10_000).coerceAtMost(state.durationMs)
                                    remoteSeekTargetMs = target
                                    onSeek(target)
                                    remoteSeekJob?.cancel()
                                    remoteSeekJob = scope.launch {
                                        delay(2200)
                                        remoteSeekTargetMs = null
                                    }
                                }
                            }, enabled = !state.busy, modifier = Modifier.remoteFocus(isTelevision)) {
                                Icon(app.reelstack.ui.components.SpoleIcons.Forward10, stringResource(R.string.player_forward), Modifier.size(32.dp))
                            }
                        }
                        if (state.busy) Text(stringResource(R.string.player_preparing), Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
                    }
                    Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
                    if (!state.awaitingResume) Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                        var dragging by remember { mutableStateOf<Float?>(null) }
                        val previewPositionMs = dragging?.toLong() ?: remoteSeekTargetMs
                        if (previewPositionMs != null && state.durationMs > 0) {
                            TimelineThumbnailPreview(
                                previewPositionMs = previewPositionMs,
                                durationMs = state.durationMs,
                                chapters = state.chapters,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 12.dp)
                                    .testTag("player-timeline-preview"),
                            )
                        }
                        Slider(value = dragging ?: remoteSeekTargetMs?.toFloat() ?: state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
                            thumb = { Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) },
                            track = { SliderDefaults.Track(it, modifier = Modifier.height(4.dp), thumbTrackGapSize = 0.dp) },
                            onValueChange = { dragging = it; scrubbing = true; interaction++ },
                            onValueChangeFinished = { dragging?.let { onSeek(it.toLong()) }; dragging = null; scrubbing = false; interaction++ },
                            valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(), enabled = !state.busy && state.error == null && state.durationMs > 0,
                            modifier = Modifier.testTag("player-timeline"))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(playbackTime(dragging?.toLong() ?: remoteSeekTargetMs ?: state.positionMs)); Text(playbackTime(state.durationMs))
                        }
                        FlowRow(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            app.reelstack.ui.components.SpoleSecondaryButton(onClick = { menu = PlayerMenu.AUDIO }, enabled = state.audio.isNotEmpty() && !state.busy) { Icon(app.reelstack.ui.components.SpoleIcons.Sound, null); Text(stringResource(R.string.player_audio)) }
                            app.reelstack.ui.components.SpoleSecondaryButton(onClick = { menu = PlayerMenu.SUBTITLES }, enabled = state.subtitles.isNotEmpty() && !state.busy) { Icon(app.reelstack.ui.components.SpoleIcons.Subtitles, null); Text(stringResource(R.string.player_subtitles_button)) }
                            app.reelstack.ui.components.SpoleSecondaryButton(onClick = { menu = PlayerMenu.QUALITY }, enabled = !state.busy) { Icon(app.reelstack.ui.components.SpoleIcons.Tune, null); Text(stringResource(R.string.player_quality)) }
                            app.reelstack.ui.components.SpoleSecondaryButton(onClick = { fillVideo = !fillVideo; interaction++ }, modifier = Modifier.testTag("player-frame-mode")) {
                                Icon(if (fillVideo) app.reelstack.ui.components.SpoleIcons.Contract else app.reelstack.ui.components.SpoleIcons.Expand, null)
                                Text(stringResource(if (fillVideo) R.string.player_frame_fit else R.string.player_frame_fill))
                            }
                            if (!isTelevision) IconButton(onClick = onRotate) { Icon(app.reelstack.ui.components.SpoleIcons.Rotate, stringResource(R.string.player_rotate)) }
                        }
                        Text(if (state.direct) stringResource(R.string.player_direct) else stringResource(R.string.player_transcoded), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium)
                        state.warning?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                    }
                }
            }
            }
            }
        }
        }
        menu?.let { title ->
            val selectedTrackFocus = remember(title) { FocusRequester() }
            LaunchedEffect(title) {
                withFrameNanos { }
                runCatching { selectedTrackFocus.requestFocus() }
            }
            AlertDialog(onDismissRequest = { menu = null }, title = { Text(stringResource(title.label)) },
                containerColor = if (isTelevision) Color(0xFF181A1C) else MaterialTheme.colorScheme.surface,
                titleContentColor = if (isTelevision) Color.White else MaterialTheme.colorScheme.onSurface,
                textContentColor = if (isTelevision) Color.White else MaterialTheme.colorScheme.onSurface,
                text = {
                    val options = when (title) {
                        PlayerMenu.AUDIO -> state.audio.map { it.index to it.label }
                        PlayerMenu.SUBTITLES -> listOf(-1 to stringResource(R.string.player_off)) + state.subtitles.map { it.index to it.label }
                        else -> listOf(0 to stringResource(R.string.player_auto), 80_000_000 to stringResource(R.string.player_quality_ultra),
                            20_000_000 to stringResource(R.string.player_quality_high), 4_000_000 to stringResource(R.string.player_medium_data), 2_000_000 to stringResource(R.string.player_low_data))
                    }
                    Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                        val selectedId = when (title) { PlayerMenu.AUDIO -> state.audioIndex; PlayerMenu.SUBTITLES -> state.subtitleIndex; else -> state.quality }
                        options.forEachIndexed { index, (id, label) ->
                            val selected = id == selectedId
                            val initialFocus = selected || (index == 0 && options.none { it.first == selectedId })
                            val choiceInteraction = remember(title, id) { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            val choose: () -> Unit = {
                                when (title) { PlayerMenu.AUDIO -> onAudio(id); PlayerMenu.SUBTITLES -> onSubtitle(id); else -> onQuality(id) }
                                menu = null; interaction++
                            }
                            OutlinedButton(onClick = choose, interactionSource = choiceInteraction,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .2f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White,
                                    containerColor = if (selected) Color.White.copy(alpha = .12f) else Color.Transparent),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .then(if (initialFocus) Modifier.focusRequester(selectedTrackFocus) else Modifier)
                                    .neutralPlayerFocus(choiceInteraction, RoundedCornerShape(12.dp))) {
                                if (selected) Icon(app.reelstack.ui.components.SpoleIcons.Done, null, Modifier.padding(end = 8.dp))
                                Text(label, Modifier.weight(1f))
                            }
                        }
                    }
                }, confirmButton = {
                    val closeInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    TextButton(onClick = { menu = null }, interactionSource = closeInteraction,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                        modifier = Modifier.heightIn(min = 48.dp).neutralPlayerFocus(closeInteraction, RoundedCornerShape(12.dp))) {
                        Text(stringResource(R.string.action_close))
                    }
                })
        }
    }
    }
}

/**
 * The header's second line, written the way every other screen writes it.
 *
 * When a series has no name of its own on the server the title falls back to the episode's, and the
 * line under it would then repeat the same words — so the name drops out and only the numbers stay.
 */
@Composable
private fun PlayerScreenState.subtitleLine(): String {
    if (season == null && episode == null) return subtitle
    val full = app.reelstack.ui.components.episodeLine(season, episode, subtitle)
    val name = app.reelstack.ui.components.episodeTitle(subtitle, episode)
    return if (name.isNotBlank() && title.contains(name, ignoreCase = true)) {
        app.reelstack.ui.components.episodeLine(season, episode, "")
    } else full
}

@Composable
private fun Modifier.neutralPlayerFocus(interaction: androidx.compose.foundation.interaction.MutableInteractionSource,
    shape: androidx.compose.ui.graphics.Shape): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    return border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, shape)
}

@Composable
private fun Modifier.remoteFocus(enabled: Boolean): Modifier {
    var focused by remember { mutableStateOf(false) }
    return if (!enabled) this else onFocusChanged { focused = it.isFocused }
        .border(if (focused) 3.dp else 0.dp, if (focused) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
}

@Composable
private fun PlayerHeader(title: String, subtitle: String, onClose: () -> Unit, showTitle: Boolean = true,
    showBack: Boolean = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) != android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
        if (showBack) IconButton(onClick = onClose, modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = .45f), CircleShape).testTag("player-close")) {
            Icon(app.reelstack.ui.components.SpoleIcons.ArrowBack, stringResource(R.string.action_back))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, top = 10.dp)
            .graphicsLayer { alpha = if (showTitle) 1f else 0f }
            .then(if (showTitle) Modifier else Modifier.clearAndSetSemantics {})) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun TimelineThumbnailPreview(
    previewPositionMs: Long,
    durationMs: Long,
    chapters: List<PlaybackChapter>,
    modifier: Modifier = Modifier,
) {
    val currentChapter = chapters.lastOrNull { it.startPositionMs <= previewPositionMs }
    Surface(
        modifier = modifier
            .width(180.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                if (currentChapter?.imageUrl != null) {
                    MediaArtwork(
                        url = currentChapter.imageUrl,
                        contentDescription = currentChapter.name,
                        contentScale = ContentScale.Crop,
                        source = ServiceKind.JELLYFIN,
                        fallbackRes = 0,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = app.reelstack.ui.components.SpoleIcons.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = playbackTime(previewPositionMs),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!currentChapter?.name.isNullOrBlank()) {
                Text(
                    text = currentChapter.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/** A quiet corner offer; its countdown starts at the user's chosen lead time. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun NextEpisodeCard(state: PlayerScreenState, onPlay: () -> Unit, onCancel: () -> Unit,
    focus: FocusRequester, modifier: Modifier = Modifier) {
    val next = state.nextEpisode ?: return
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Surface(modifier.widthIn(max = 360.dp).fillMaxWidth()
        .testTag("player-next-episode"), shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = .84f), contentColor = Color.White, tonalElevation = 0.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(app.reelstack.ui.components.SpoleIcons.Screen, null, Modifier.size(28.dp),
                    tint = Color.White.copy(alpha = .7f))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.player_next_episode), style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = .7f))
                    Text(next.title, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(app.reelstack.ui.components.episodeLine(next.season, next.episode, next.subtitle),
                        style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = Color.White.copy(alpha = .7f))
                }
            }
            state.nextEpisodeCountdown?.let {
                Text(stringResource(R.string.next_episode_countdown, it),
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .7f))
            }
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlay, shape = shape, interactionSource = interaction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = .18f), contentColor = Color.White),
                    modifier = Modifier.heightIn(min = 48.dp).focusRequester(focus)
                        .neutralPlayerFocus(interaction, shape).testTag("player-next-play")) {
                    Icon(app.reelstack.ui.components.SpoleIcons.Play, null, Modifier.size(20.dp))
                    Text(stringResource(R.string.player_next_play), Modifier.padding(start = 8.dp))
                }
                if (!state.ended || state.nextEpisodeCountdown != null) {
                    val cancelInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    TextButton(onClick = onCancel, interactionSource = cancelInteraction,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White), shape = shape,
                        modifier = Modifier.heightIn(min = 48.dp).neutralPlayerFocus(cancelInteraction, shape).testTag("player-next-cancel")) {
                        Text(stringResource(if (state.ended) R.string.next_episode_cancel else R.string.next_episode_dismiss))
                    }
                }
            }
        }
    }
}

/**
 * "Skip the intro", when the server knows where the intro is.
 *
 * This is the one thing people install a Jellyfin plugin for, and the marks it produces were
 * sitting on the server unused. The button only exists while the playhead is actually inside a
 * marked stretch, so it is never a control looking for something to do; most libraries have never
 * been scanned and will never see it at all.
 *
 * It sits above the transport controls rather than among them, because it is an offer with a
 * deadline and not a permanent part of the player — and it takes focus on television for exactly
 * as long as it is on screen, so a remote can reach it without hunting.
 */
@Composable
private fun SkipSegmentButton(state: PlayerScreenState, onSkip: () -> Unit) {
    val segment = state.activeSegment() ?: return
    if (state.busy || state.error != null || state.awaitingResume || state.ended) return
    val focus = remember(segment.startMs) { FocusRequester() }
    LaunchedEffect(segment.startMs) { androidx.compose.runtime.withFrameNanos { }; runCatching { focus.requestFocus() } }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
        Button(
            onClick = onSkip,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.heightIn(min = 48.dp).focusRequester(focus).testTag("player-skip-segment"),
        ) {
            Text(stringResource(
                if (segment.kind == PlaybackSegment.Kind.INTRO) R.string.player_skip_intro
                else R.string.player_skip_outro,
            ))
            Icon(app.reelstack.ui.components.SpoleIcons.ChevronRight, null, Modifier.padding(start = 6.dp).size(18.dp))
        }
    }
}
