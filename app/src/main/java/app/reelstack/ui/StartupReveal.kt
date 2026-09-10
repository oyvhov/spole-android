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
    StartupCover(awaitContentReady = { viewModel.uiState.first { !it.isRefreshing } }, content = content)
}

@Composable
internal fun StartupCover(awaitContentReady: suspend () -> Unit, content: @Composable () -> Unit) {
    val slow = app.reelstack.ui.theme.LocalPersonalization.current.slowStartup
    var opening by rememberSaveable { mutableStateOf(true) }
    var formed by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (formed || !opening) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!opening) return@LaunchedEffect
        if (!formed) reveal.animateTo(1f, tween(if (slow) 1650 else 800, easing = androidx.compose.animation.core.LinearEasing))
        formed = true
        // Let the expensive home composition settle behind a completed, stationary mark.
        // The ViewModel has already started its network refresh independently of this UI.
        withFrameNanos { }
        withFrameNanos { }
        withTimeoutOrNull(if (slow) 1500L else 500L) { awaitContentReady() }
        opening = false
    }
    Box(Modifier.fillMaxSize()) {
        // Loading continues, but TalkBack must not focus controls hidden by the cover.
        // Compose immediately: image requests and network data load throughout the animation.
        Box(if (opening) Modifier.clearAndSetSemantics {}.focusProperties { canFocus = false }
            .onPreviewKeyEvent { true } else Modifier) { content() }
        AnimatedVisibility(visible = opening, exit = fadeOut(tween(240))) {
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
