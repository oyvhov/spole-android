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
import kotlinx.coroutines.withTimeoutOrNull

/** A bounded opening reveal. Network work continues behind the cover. */
@Composable
fun StartupReveal(viewModel: ReelstackViewModel, content: @Composable () -> Unit) {
    StartupCover(awaitContentReady = {
        // Keep launch responsive, but only lift the cover once first rails are visible.
        // Start painting content quickly after a short safety window. If data has not arrived yet,
        // we still reveal so the user gets an immediate interactive skeleton instead of a blank gap.
        withTimeoutOrNull(220) {
            viewModel.uiState.first { state ->
                (state.showOnboarding || state.configuredCount == 0 ||
                    state.hasCachedData ||
                    state.sessions.isNotEmpty() ||
                    state.resume.isNotEmpty() ||
                    state.recentMovies.isNotEmpty() ||
                    state.recentSeries.isNotEmpty() ||
                    state.recommendations.any { !it.artworkUrl.isNullOrBlank() } ||
                    state.recentMovies.any { !it.artworkUrl.isNullOrBlank() } ||
                    state.recentSeries.any { !it.artworkUrl.isNullOrBlank() } ||
                    state.sessions.any { !it.artworkUrl.isNullOrBlank() } ||
                    state.resume.any { !it.artworkUrl.isNullOrBlank() })
            }
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
        if (!formed) reveal.animateTo(1f, tween(if (slow) 680 else 220, easing = androidx.compose.animation.core.LinearEasing))
        formed = true
        // Let the initial frame settle before we lift the cover.
        // The ViewModel has already started its network refresh independently of this UI.
        withFrameNanos { }
        withTimeoutOrNull(220) { awaitContentReady() }
        opening = false
    }
    val coverVisible by remember { derivedStateOf { opening } }
    Box(Modifier.fillMaxSize()) {
        // Loading continues, but TalkBack must not focus controls hidden by the cover.
        // Compose immediately: image requests and network data load throughout the animation.
        Box(if (opening) Modifier.clearAndSetSemantics {}.focusProperties { canFocus = false }
            .onPreviewKeyEvent { true } else Modifier) { content() }
        AnimatedVisibility(visible = coverVisible, exit = fadeOut(tween(if (slow) 180 else 90))) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).alpha(if (opening) 1f else 0.2f)
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
