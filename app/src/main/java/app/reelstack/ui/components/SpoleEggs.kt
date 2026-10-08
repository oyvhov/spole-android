package app.reelstack.ui.components

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.ui.theme.LocalMotionEnabled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small things that are there for whoever finds them.
 *
 * Nothing here consumes a key or a touch: the activity only reports what it saw, so an egg can never
 * cost a press on the remote. The same reports tell the season how long the remote has been left
 * alone, which is when the Halloween spider comes down the menu.
 */
internal object SpoleEggs {
    private val rewinds = MutableStateFlow(0)
    private val lastInput = MutableStateFlow(SystemClock.uptimeMillis())
    private val konami = KonamiCode()

    /** Counts up each time the reel is asked to spin back. 0 means it never has. */
    val rewind: StateFlow<Int> = rewinds.asStateFlow()

    /** Uptime of the last key or touch, for anything that waits for the room to go quiet. */
    val lastInputAt: StateFlow<Long> = lastInput.asStateFlow()

    fun key(event: KeyEvent) {
        lastInput.value = SystemClock.uptimeMillis()
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && konami.feed(event.keyCode)) rewind()
    }

    fun touch() { lastInput.value = SystemClock.uptimeMillis() }

    fun rewind() { rewinds.value++ }
}

/**
 * ↑ ↑ ↓ ↓ ← → ← → on a D-pad. B and A do not exist on a television remote, and the arrows alone in
 * this order are not something anyone presses while moving through a menu.
 *
 * It compares the last eight presses rather than counting progress, so an extra ↑ at the start,
 * which is how almost everyone types it, still counts.
 */
internal class KonamiCode {
    private val sequence = intArrayOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
    )
    private val recent = ArrayDeque<Int>()

    fun feed(keyCode: Int): Boolean {
        recent.addLast(keyCode)
        if (recent.size > sequence.size) recent.removeFirst()
        if (recent.size == sequence.size && recent.indices.all { recent[it] == sequence[it] }) {
            recent.clear()
            return true
        }
        return false
    }
}

/**
 * Seven taps on the Spole mark within three seconds, like the build number in Android's own
 * settings. It only watches: a mark inside a button still works as the button.
 */
internal fun Modifier.rewindOnSevenTaps(): Modifier = pointerInput(Unit) {
    var taps = 0
    var first = 0L
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        if (waitForUpOrCancellation() == null) return@awaitEachGesture
        val now = SystemClock.uptimeMillis()
        if (taps == 0 || now - first > 3_000) { taps = 0; first = now }
        if (++taps == 7) { taps = 0; SpoleEggs.rewind() }
    }
}

/**
 * The reel spinning back: an old film leader counting 3, 2, 1 with the Spole mark turning
 * backwards inside it, then gone. About two and a half seconds, drawn over everything and touching
 * nothing — no focus, no input, no screen reader. With motion turned down the mark and the words
 * show for a moment without turning.
 */
@Composable
internal fun SpoleRewindOverlay() {
    val requested by SpoleEggs.rewind.collectAsState()
    var shown by remember { mutableIntStateOf(requested) }
    val progress = remember { Animatable(1f) }
    val motion = LocalMotionEnabled.current
    LaunchedEffect(requested) {
        if (requested == shown) return@LaunchedEffect
        shown = requested
        progress.snapTo(0f)
        progress.animateTo(1f, tween(if (motion) 2_600 else 1_400, easing = LinearEasing))
    }
    if (progress.value >= 1f) return
    val accent = MaterialTheme.colorScheme.primary
    val t = progress.value
    val fade = when {
        t < .08f -> t / .08f
        t > .88f -> (1f - t) / .12f
        else -> 1f
    }.coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().testTag("egg-rewind").clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Column(Modifier.graphicsLayer { alpha = fade }, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                // Three counts over the first three quarters; the last quarter is the mark alone.
                val count = (t / .25f).toInt().coerceAtMost(3)
                val sweep = (t % .25f) / .25f
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.Black.copy(alpha = .72f))
                    if (count < 3 && motion) drawArc(accent.copy(alpha = .28f), -90f, -360f * sweep, true,
                        Offset.Zero, Size(size.width, size.height))
                    drawCircle(Color.White.copy(alpha = .5f), size.minDimension * .42f, style = Stroke(1.5.dp.toPx()))
                    drawCircle(Color.White.copy(alpha = .3f), size.minDimension * .34f, style = Stroke(1.dp.toPx()))
                    drawLine(Color.White.copy(alpha = .3f), Offset(center.x, 0f), Offset(center.x, size.height), 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = .3f), Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx())
                }
                if (count < 3 && motion) Text((3 - count).toString(), color = Color.White,
                    style = MaterialTheme.typography.displayLarge)
                else {
                    val turn = if (motion) -720f * FastOutSlowInEasing.transform(((t - .75f) / .25f).coerceIn(0f, 1f)) else 0f
                    SpoleBrandMark(Modifier.size(84.dp).graphicsLayer { rotationZ = turn })
                }
            }
            Text(stringResource(R.string.egg_rewind), Modifier.padding(top = 14.dp)
                .background(Color.Black.copy(alpha = .72f), androidx.compose.foundation.shape.RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 6.dp),
                color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}
