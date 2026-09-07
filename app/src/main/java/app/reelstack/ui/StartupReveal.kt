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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** A bounded opening reveal. Network work continues behind the cover. */
@Composable
fun StartupReveal(viewModel: ReelstackViewModel, content: @Composable () -> Unit) {
    var opening by rememberSaveable { mutableStateOf(true) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!opening) return@LaunchedEffect
        reveal.animateTo(1f, tween(360))
        withTimeoutOrNull(1_200) { viewModel.uiState.first { !it.isRefreshing } }
        opening = false
    }
    Box(Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(visible = opening, exit = fadeOut(tween(240))) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.graphicsLayer {
                        alpha = reveal.value
                        translationY = (1f - reveal.value) * 12.dp.toPx()
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Spole", fontSize = 58.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(24.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.width(48.dp).height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}
