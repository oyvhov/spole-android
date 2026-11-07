package app.reelstack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.reelstack.ui.theme.Primary

/** Test tag of the filled part. The caller's own tag, if it passes one, sits on the whole bar. */
const val WatchProgressFillTag = "watch-progress-fill"

/**
 * How far through a title someone is. The one bar every card, hero and list row shares.
 *
 * It is 4 dp tall (3 dp was barely visible on a tile) and goes flush along the bottom edge of the
 * artwork, inside the artwork's own clip, so it reads as the edge of the picture and not as a
 * second object floating on top of it. The caller places it, usually `Modifier.align(BottomStart)`
 * in the artwork's `Box`; the bar fills the width it is given and nothing in here adds padding.
 *
 * The track is white at 30 % across the *whole* width, and the accent covers the share already
 * watched. Read that way, the track is how long the title is and the fill is how far you are; a
 * dark track reads as "something is missing here", and a grey one disappears on a light poster,
 * where a white veil does not.
 *
 * [round] is for a bar that stands free in a row, away from any artwork edge. On artwork it stays
 * square, because there it forms the edge of the image and a rounded end would sit in front of it.
 *
 * Callers pass a fraction. A fraction outside 0..1, or not a number, is clamped rather than thrown.
 */
@Composable
fun WatchProgressBar(progress: Float, modifier: Modifier = Modifier, round: Boolean = false) {
    val share = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    // No clip layer on artwork: the fill stays inside the track anyway, and a clip is a node of its own in the semantics
    // tree, which Compose sorts about every 100 ms while a page moves (the Shield, 2026-11-07). A free-standing round
    // bar clips by drawing, which adds no node.
    val bar = modifier.fillMaxWidth().height(4.dp)
    Box(if (round) bar.drawWithCache {
        val path = androidx.compose.ui.graphics.Path().apply {
            addOutline(CircleShape.createOutline(size, layoutDirection, this@drawWithCache))
        }
        onDrawWithContent { clipPath(path) { this@onDrawWithContent.drawContent() } }
    }.drawnBackground(Color.White.copy(alpha = .3f)) else bar.drawnBackground(Color.White.copy(alpha = .3f))) {
        Box(Modifier.fillMaxWidth(share).fillMaxHeight().drawnBackground(Primary).devTag(WatchProgressFillTag))
    }
}
