package app.reelstack.player

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
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
import kotlinx.coroutines.delay

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
        // Covers Back during the first frame too; the screen's menu handler takes precedence later.
        onBackPressedDispatcher.addCallback(this) { if (!model.back()) finish() }
        setContent {
            ReelstackTheme {
                LaunchedEffect(id) { model.open(id) }
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
                    })
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
        fun open(context: Context, itemId: String) = context.startActivity(Intent(context, JellyfinPlayerActivity::class.java).putExtra(ITEM_ID, itemId))
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
    isTelevision: Boolean = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
) {
    val videoFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    var consumedRemoteKey by remember { mutableIntStateOf(-1) }
    val showControlsLabel = stringResource(R.string.player_show_controls)
    var controls by remember { mutableStateOf(true) }
    var fillVideo by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf<PlayerMenu?>(null) }
    var scrubbing by remember { mutableStateOf(false) }
    val accessibility = LocalAccessibilityManager.current
    val canHide = state.playing && !state.busy && state.error == null && !state.ended && !state.awaitingResume
    val showControls = controls || !canHide || menu != null
    val latestShown by rememberUpdatedState(showControls)
    val latestCanHide by rememberUpdatedState(canHide && !scrubbing && menu == null)
    LaunchedEffect(canHide) { if (!canHide) controls = true }
    LaunchedEffect(controls, canHide, interaction, menu, scrubbing) {
        if (controls && canHide && menu == null && !scrubbing) {
            delay(accessibility?.calculateRecommendedTimeoutMillis(3500, containsControls = true) ?: 3500)
            controls = false
        }
    }
    LaunchedEffect(isTelevision, showControls, state.busy, state.browsing, state.awaitingResume, state.error, menu) {
        if (isTelevision && menu == null && !state.browsing) {
            if (!showControls) videoFocus.requestFocus()
            else if (!state.busy && !state.awaitingResume && state.error == null) playFocus.requestFocus()
        }
    }
    BackHandler {
        if (menu != null) menu = null
        else if (isTelevision && showControls && canHide) controls = false
        else onClose()
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
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
            if (native.repeatCount > 0 && consumedRemoteKey == native.keyCode) return@onPreviewKeyEvent true
            val action = remotePlaybackAction(key, showControls, state.playing,
                state.busy || state.browsing || state.awaitingResume || state.error != null || menu != null)
            if (action == RemotePlaybackAction.DEFAULT) return@onPreviewKeyEvent false
            consumedRemoteKey = native.keyCode
            if (action != RemotePlaybackAction.IGNORE) controls = true
            when (action) {
                RemotePlaybackAction.TOGGLE -> onToggle()
                RemotePlaybackAction.REWIND -> onSeek((state.positionMs - 10_000).coerceAtLeast(0))
                RemotePlaybackAction.FORWARD -> if (state.durationMs > 0) onSeek((state.positionMs + 10_000).coerceAtMost(state.durationMs))
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
                PlayerHeader(state.title, state.subtitle, onClose)
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("player-episodes"), contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.choices, key = { it.id }) { item ->
                        Surface(onClick = { if (!state.busy) onChoose(item) }, enabled = !state.busy,
                            shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (item.type == "Season") Icons.Rounded.VideoLibrary else Icons.Rounded.PlayArrow, null)
                                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                    Text(if (item.type == "Episode") item.subtitle else item.title, style = MaterialTheme.typography.titleMedium)
                                    Text(when { item.played -> stringResource(R.string.player_watched); item.resumeMs > 0 -> stringResource(R.string.player_resume, playbackTime(item.resumeMs))
                                        else -> stringResource(R.string.player_available) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (state.hasMore && !state.busy) item { TextButton(onClick = onMore) { Text(stringResource(R.string.action_more)) } }
                    if (state.choices.isEmpty() && !state.busy && state.error == null) item { Text(stringResource(R.string.player_no_episodes)) }
                    if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    state.error?.let { error -> item { Text(error); TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) } } }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()
                .background(Brush.verticalGradient(if (showControls)
                    listOf(Color.Black.copy(alpha = .65f), Color.Transparent, Color.Black.copy(alpha = .85f))
                    else listOf(Color.Transparent, Color.Transparent)))
                .safeDrawingPadding()) {
                if (showControls) PlayerHeader(state.title, state.subtitle, onClose, showBack = !isTelevision)
            AnimatedVisibility(visible = showControls, enter = fadeIn(tween(90)), exit = fadeOut(tween(140)),
                modifier = Modifier.weight(1f).testTag("player-controls")) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                val viewportHeight = maxHeight
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = viewportHeight)) {
                    Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
                    if (state.awaitingResume) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(onClick = { onResume(false) }) { Text(stringResource(R.string.player_resume, playbackTime(state.positionMs))) }
                            TextButton(onClick = { onResume(true) }) { Text(stringResource(R.string.player_restart)) }
                        }
                    } else if (state.error != null) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Text(state.error, modifier = Modifier.padding(vertical = 12.dp))
                            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                            TextButton(onClick = onExternal) { Text(stringResource(R.string.player_external)) }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { interaction++; onSeek(state.positionMs - 10_000) }, enabled = !state.busy, modifier = Modifier.remoteFocus(isTelevision)) {
                                Icon(Icons.Rounded.Replay10, stringResource(R.string.player_rewind), Modifier.size(32.dp))
                            }
                            FilledIconButton(onClick = { interaction++; onToggle() }, enabled = !state.busy, modifier = Modifier.size(72.dp).remoteFocus(isTelevision).focusRequester(playFocus).testTag("player-toggle")) {
                                if (state.busy) CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 2.dp)
                                else Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    if (state.playing) stringResource(R.string.player_pause) else stringResource(R.string.player_play), Modifier.size(36.dp))
                            }
                            IconButton(onClick = { interaction++; onSeek(state.positionMs + 10_000) }, enabled = !state.busy, modifier = Modifier.remoteFocus(isTelevision)) {
                                Icon(Icons.Rounded.Forward10, stringResource(R.string.player_forward), Modifier.size(32.dp))
                            }
                        }
                        if (state.busy) Text(stringResource(R.string.player_preparing), Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
                    }
                    Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
                    if (!state.awaitingResume) Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                        var dragging by remember { mutableStateOf<Float?>(null) }
                        Slider(value = dragging ?: state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
                            thumb = { Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) },
                            track = { SliderDefaults.Track(it, modifier = Modifier.height(4.dp), thumbTrackGapSize = 0.dp) },
                            onValueChange = { dragging = it; scrubbing = true; interaction++ },
                            onValueChangeFinished = { dragging?.let { onSeek(it.toLong()) }; dragging = null; scrubbing = false; interaction++ },
                            valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(), enabled = !state.busy && state.error == null && state.durationMs > 0,
                            modifier = Modifier.testTag("player-timeline"))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(playbackTime(dragging?.toLong() ?: state.positionMs)); Text(playbackTime(state.durationMs))
                        }
                        FlowRow(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { menu = PlayerMenu.AUDIO }, enabled = state.audio.isNotEmpty() && !state.busy) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, null); Text(stringResource(R.string.player_audio)) }
                            TextButton(onClick = { menu = PlayerMenu.SUBTITLES }, enabled = state.subtitles.isNotEmpty() && !state.busy) { Icon(Icons.Rounded.Subtitles, null); Text(stringResource(R.string.player_subtitles_button)) }
                            TextButton(onClick = { menu = PlayerMenu.QUALITY }, enabled = !state.busy) { Icon(Icons.Rounded.Tune, null); Text(stringResource(R.string.player_quality)) }
                            TextButton(onClick = { fillVideo = !fillVideo; interaction++ }, modifier = Modifier.testTag("player-frame-mode")) {
                                Icon(if (fillVideo) Icons.Rounded.FitScreen else Icons.Rounded.Fullscreen, null)
                                Text(stringResource(if (fillVideo) R.string.player_frame_fit else R.string.player_frame_fill))
                            }
                            if (!isTelevision) IconButton(onClick = onRotate) { Icon(Icons.Rounded.ScreenRotation, stringResource(R.string.player_rotate)) }
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
            AlertDialog(onDismissRequest = { menu = null }, title = { Text(stringResource(title.label)) },
                text = {
                    val options = when (title) {
                        PlayerMenu.AUDIO -> state.audio.map { it.index to it.label }
                        PlayerMenu.SUBTITLES -> listOf(-1 to stringResource(R.string.player_off)) + state.subtitles.map { it.index to it.label }
                        else -> listOf(0 to stringResource(R.string.player_auto), 80_000_000 to stringResource(R.string.player_quality_ultra),
                            20_000_000 to stringResource(R.string.player_quality_high), 4_000_000 to stringResource(R.string.player_medium_data), 2_000_000 to stringResource(R.string.player_low_data))
                    }
                    Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                        options.forEach { (id, label) ->
                            val selected = id == when (title) { PlayerMenu.AUDIO -> state.audioIndex; PlayerMenu.SUBTITLES -> state.subtitleIndex; else -> state.quality }
                            TextButton(onClick = {
                                when (title) { PlayerMenu.AUDIO -> onAudio(id); PlayerMenu.SUBTITLES -> onSubtitle(id); else -> onQuality(id) }
                                menu = null; interaction++
                            }, modifier = Modifier.fillMaxWidth()) {
                                if (selected) Icon(Icons.Rounded.Check, null, Modifier.padding(end = 8.dp))
                                Text(label, Modifier.weight(1f))
                            }
                        }
                    }
                }, confirmButton = { TextButton(onClick = { menu = null }) { Text(stringResource(R.string.action_close)) } })
        }
    }
    }
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
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, top = 10.dp)
            .graphicsLayer { alpha = if (showTitle) 1f else 0f }
            .then(if (showTitle) Modifier else Modifier.clearAndSetSemantics {})) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
