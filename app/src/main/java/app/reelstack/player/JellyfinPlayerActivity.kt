package app.reelstack.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class JellyfinPlayerActivity : ComponentActivity() {
    internal lateinit var model: JellyfinPlayerModel
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                JellyfinPlayerModel((application as ReelstackApplication).container) as T
        })[JellyfinPlayerModel::class.java]
        val id = intent.getStringExtra(ITEM_ID).orEmpty()
        if (id.isBlank() || id.length > 128) { finish(); return }
        model.open(id)
        setContent {
            ReelstackTheme {
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
) {
    var controls by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf<String?>(null) }
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(state.playing, state.busy, state.error, interaction, menu) {
        controls = true
        if (state.playing && !state.busy && state.error == null && menu == null) {
            delay(accessibility?.calculateRecommendedTimeoutMillis(3500, containsControls = true) ?: 3500)
            controls = false
        }
    }
    BackHandler { if (menu != null) menu = null else onClose() }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    Box(Modifier.fillMaxSize().background(Color.Black).testTag("jellyfin-player")) {
        if (!state.browsing && player != null) AndroidView(
            factory = { context -> PlayerView(context).apply {
                useController = false; this.player = player; setKeepContentOnPlayerReset(false)
            } },
            update = { it.player = player },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize().testTag("player-video"),
        )
        Box(Modifier.fillMaxSize().clickable { controls = !controls; if (controls) interaction++ })
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
                                    Text(when { item.played -> "Sett"; item.resumeMs > 0 -> "Hald fram frå ${playbackTime(item.resumeMs)}"
                                        else -> "I biblioteket" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (state.hasMore && !state.busy) item { TextButton(onClick = onMore) { Text("Vis fleire") } }
                    if (state.choices.isEmpty() && !state.busy && state.error == null) item { Text("Ingen tilgjengelege episodar her enno.") }
                    if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    state.error?.let { error -> item { Text(error); TextButton(onClick = onRetry) { Text("Prøv igjen") } } }
                }
            }
        } else {
            AnimatedVisibility(visible = controls || state.busy || state.error != null || state.ended, enter = fadeIn(), exit = fadeOut()) {
                Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .65f), Color.Transparent, Color.Black.copy(alpha = .85f))))
                    .safeDrawingPadding().verticalScroll(rememberScrollState())) {
                    PlayerHeader(state.title, state.subtitle, onClose)
                    Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
                    if (state.awaitingResume) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(onClick = { onResume(false) }) { Text("Hald fram frå ${playbackTime(state.positionMs)}") }
                            TextButton(onClick = { onResume(true) }) { Text("Spel frå byrjinga") }
                        }
                    } else if (state.error != null) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Text(state.error, modifier = Modifier.padding(vertical = 12.dp))
                            Button(onClick = onRetry) { Text("Prøv igjen") }
                            TextButton(onClick = onExternal) { Text("Opne i Jellyfin") }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { interaction++; onSeek(state.positionMs - 10_000) }, enabled = !state.busy) {
                                Icon(Icons.Rounded.Replay10, "10 sekund tilbake", Modifier.size(32.dp))
                            }
                            FilledIconButton(onClick = { interaction++; onToggle() }, enabled = !state.busy, modifier = Modifier.size(72.dp).testTag("player-toggle")) {
                                if (state.busy) CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 2.dp)
                                else Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    if (state.playing) "Set på pause" else "Spel av", Modifier.size(36.dp))
                            }
                            IconButton(onClick = { interaction++; onSeek(state.positionMs + 10_000) }, enabled = !state.busy) {
                                Icon(Icons.Rounded.Forward10, "10 sekund fram", Modifier.size(32.dp))
                            }
                        }
                        if (state.busy) Text("Gjer klar avspelinga …", Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
                    }
                    Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
                    if (!state.awaitingResume) Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                        var dragging by remember { mutableStateOf<Float?>(null) }
                        Slider(value = dragging ?: state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
                            thumb = { Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) },
                            track = { SliderDefaults.Track(it, modifier = Modifier.height(4.dp), thumbTrackGapSize = 0.dp) },
                            onValueChange = { dragging = it; interaction++ },
                            onValueChangeFinished = { dragging?.let { onSeek(it.toLong()) }; dragging = null; interaction++ },
                            valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(), enabled = !state.busy && state.error == null && state.durationMs > 0,
                            modifier = Modifier.testTag("player-timeline"))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(playbackTime(dragging?.toLong() ?: state.positionMs)); Text(playbackTime(state.durationMs))
                        }
                        FlowRow(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { menu = "Lydspor" }, enabled = state.audio.isNotEmpty() && !state.busy) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, null); Text(" Lyd") }
                            TextButton(onClick = { menu = "Undertekstar" }, enabled = state.subtitles.isNotEmpty() && !state.busy) { Icon(Icons.Rounded.Subtitles, null); Text(" Tekst") }
                            TextButton(onClick = { menu = "Kvalitet" }, enabled = !state.busy) { Icon(Icons.Rounded.Tune, null); Text(" Kvalitet") }
                            IconButton(onClick = onRotate) { Icon(Icons.Rounded.ScreenRotation, "Snu skjermen") }
                        }
                        Text(if (state.direct) "Direkte frå Jellyfin" else "Tilpassa avspeling frå Jellyfin", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium)
                        state.warning?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                    }
                }
            }
        }
        menu?.let { title ->
            AlertDialog(onDismissRequest = { menu = null }, title = { Text(title) },
                text = {
                    val options = when (title) {
                        "Lydspor" -> state.audio.map { it.index to it.label }
                        "Undertekstar" -> listOf(-1 to "Av") + state.subtitles.map { it.index to it.label }
                        else -> listOf(0 to "Automatisk", 4_000_000 to "Mindre data · 4 Mbit/s", 2_000_000 to "Lite data · 2 Mbit/s")
                    }
                    Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                        options.forEach { (id, label) ->
                            val selected = id == when (title) { "Lydspor" -> state.audioIndex; "Undertekstar" -> state.subtitleIndex; else -> state.quality }
                            TextButton(onClick = {
                                when (title) { "Lydspor" -> onAudio(id); "Undertekstar" -> onSubtitle(id); else -> onQuality(id) }
                                menu = null; interaction++
                            }, modifier = Modifier.fillMaxWidth()) {
                                if (selected) Icon(Icons.Rounded.Check, null, Modifier.padding(end = 8.dp))
                                Text(label, Modifier.weight(1f))
                            }
                        }
                    }
                }, confirmButton = { TextButton(onClick = { menu = null }) { Text("Lukk") } })
        }
    }
    }
}

@Composable
private fun PlayerHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = .45f), CircleShape).testTag("player-close")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Tilbake")
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, top = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
