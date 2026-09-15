package app.reelstack.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.reelstack.ui.components.SpoleStartupArt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import coil3.imageLoader
import coil3.network.httpHeaders
import kotlinx.coroutines.withTimeoutOrNull

/** A bounded opening reveal. Network work continues behind the cover. */
@Composable
fun StartupReveal(viewModel: ReelstackViewModel, content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    StartupCover(awaitContentReady = {
        val state = viewModel.uiState.first { state ->
            state.showOnboarding || state.configuredCount == 0 ||
                state.resume.isNotEmpty() || state.nextUp.isNotEmpty() ||
                state.recentMovies.isNotEmpty() || state.recentSeries.isNotEmpty()
        }
        if (!state.showOnboarding && state.configuredCount > 0) coroutineScope {
            // Bounded first-screen warming. The rest continues loading in the composed page.
            (state.resume + state.nextUp + state.recentMovies + state.recentSeries)
                .filter { it.artworkUrl != null }.distinctBy { it.artworkUrl }.take(4).map { item -> async {
                    val request = coil3.request.ImageRequest.Builder(context).data(item.artworkUrl).size(640, 360)
                    app.reelstack.ui.components.MediaAuthHeaders.forUrl(context, item.source, item.artworkUrl!!)?.let(request::httpHeaders)
                    context.imageLoader.execute(request.build())
                } }.awaitAll()
        }
    }, content = content)
}

@Composable
internal fun StartupCover(awaitContentReady: suspend () -> Unit, content: @Composable () -> Unit) {
    val slow = app.reelstack.ui.theme.LocalPersonalization.current.slowStartup
    var opening by rememberSaveable { mutableStateOf(true) }
    var formed by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (formed || !opening) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!opening) return@LaunchedEffect
        coroutineScope {
            val ready = async { withTimeoutOrNull(5_000) { awaitContentReady() } }
            if (!formed) reveal.animateTo(1f, tween(if (slow) 1_200 else 900, easing = androidx.compose.animation.core.LinearEasing))
            formed = true
            delay(if (slow) 400 else 300)
            ready.await()
            opening = false
        }
    }
    val coverVisible by remember { derivedStateOf { opening } }
    Box(Modifier.fillMaxSize()) {
        // Loading continues, but TalkBack must not focus controls hidden by the cover.
        // Compose immediately: image requests and network data load throughout the animation.
        Box(if (opening) Modifier.clearAndSetSemantics {}.focusProperties { canFocus = false }
            .onPreviewKeyEvent { true } else Modifier) { content() }
        AnimatedVisibility(visible = coverVisible, exit = fadeOut(tween(280))) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    .testTag("startup-cover")
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                SpoleStartupArt(progress = { reveal.value }, modifier = Modifier.safeDrawingPadding().padding(24.dp))
            }
        }
    }
}
