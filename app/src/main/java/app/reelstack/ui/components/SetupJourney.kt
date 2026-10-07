package app.reelstack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.SurfaceRaised

/**
 * The first-run journey as one quiet scene, with no words in it.
 *
 * The server in the house sends out two soft rings, which is Spole finding it. An arc draws itself
 * to the phone, whose screen lights with a tick for the approval. A second arc reaches the
 * television, which wakes: its screen fills, the same faint accent glow as the startup mark rises
 * behind it, and the play mark appears.
 *
 * It is drawn, not played back from a file, so it stays sharp at any television's density and costs
 * no memory. It plays a few times and comes to rest on the finished scene, because something that
 * loops for ever on a welcome screen pulls the eye from the one choice there is. With motion turned
 * down it is the finished scene from the start. A screen reader hears one sentence.
 */
@Composable
internal fun SetupJourney(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.setup_journey_description)
    val motion = LocalMotionEnabled.current
    var progress by remember { mutableFloatStateOf(if (motion) 0f else REST) }
    LaunchedEffect(motion) {
        if (!motion) { progress = REST; return@LaunchedEffect }
        // Frames come through the infinite-animation channel on purpose: a UI test pauses it, so
        // a test sees a still picture instead of waiting out the whole animation.
        val start = withInfiniteAnimationFrameNanos { it }
        while (true) {
            val elapsed = (withInfiniteAnimationFrameNanos { it } - start) / 1_000_000L
            if (elapsed >= PLAYS * CYCLE_MILLIS) break
            progress = (elapsed % CYCLE_MILLIS) / CYCLE_MILLIS.toFloat()
        }
        val from = progress
        val settle = withInfiniteAnimationFrameNanos { it }
        while (true) {
            val t = ((withInfiniteAnimationFrameNanos { it } - settle) / 1_000_000f / SETTLE_MILLIS).coerceAtMost(1f)
            progress = from + (REST - from) * FastOutSlowInEasing.transform(t)
            if (t >= 1f) break
        }
    }
    val palette = JourneyPalette(
        body = SurfaceRaised,
        edge = MaterialTheme.colorScheme.outline.copy(alpha = .55f),
        screen = MaterialTheme.colorScheme.background,
        lit = lerp(SurfaceRaised, MaterialTheme.colorScheme.onSurface, .2f),
        ink = MaterialTheme.colorScheme.onSurface,
        trail = MaterialTheme.colorScheme.outlineVariant,
        accent = MaterialTheme.colorScheme.primary,
    )
    Canvas(modifier.widthIn(max = 520.dp).fillMaxWidth().aspectRatio(2f).testTag("setup-journey")
        .clearAndSetSemantics { contentDescription = description }) {
        drawJourney(progress, palette)
    }
}

private const val PLAYS = 3
private const val CYCLE_MILLIS = 7_000
private const val SETTLE_MILLIS = 700f

/** The point in a cycle where everything has arrived: the scene the animation rests on. */
private const val REST = .84f

private class JourneyPalette(
    val body: Color, val edge: Color, val screen: Color, val lit: Color,
    val ink: Color, val trail: Color, val accent: Color,
)

private fun phase(p: Float, start: Float, end: Float, ease: androidx.compose.animation.core.Easing = FastOutSlowInEasing) =
    ease.transform(((p - start) / (end - start)).coerceIn(0f, 1f))

/**
 * Drawn on a 400 × 200 stage scaled to the canvas, so every television gets the same picture.
 * One cycle: rings 0–.24, first arc .16–.36, phone .34–.46, second arc .44–.62, television
 * .60–.74, rest, then a slow fade back .90–1.
 */
private fun DrawScope.drawJourney(p: Float, c: JourneyPalette) {
    val s = size.width / 400f
    fun o(x: Float, y: Float) = Offset(x * s, y * s)
    val fade = 1f - phase(p, .90f, 1f)
    val arcOne = phase(p, .16f, .36f) * fade
    val phone = phase(p, .34f, .46f) * fade
    val arcTwo = phase(p, .44f, .62f) * fade
    val tv = phase(p, .60f, .74f) * fade

    // Everything stands on one shelf (y 176), the way the three things stand in a living room.
    // Equal gaps (48) on either side of the phone, so the two arcs are the same gesture.
    val server = Rect(o(12f, 108f), o(72f, 176f))
    val handset = Rect(o(120f, 94f), o(162f, 176f))
    val screen = Rect(o(216f, 58f), o(388f, 158f))

    // Discovery: two rings leave the server and thin out. Nothing is left behind once they pass.
    repeat(2) { ring ->
        val t = phase(p, .02f + ring * .08f, .24f + ring * .08f, LinearOutSlowInEasing)
        if (t > 0f && t < 1f) drawCircle(c.ink.copy(alpha = .28f * (1f - t)), radius = (34f + 70f * t) * s,
            center = server.center, style = Stroke(1.4.dp.toPx()))
    }

    // The two arcs: a faint dotted guide always, and the travelled part drawn in over it.
    // Low, even arcs from the middle of one thing to the middle of the next: a reach, not a jump.
    val first = Path().apply { moveTo(server.right + 8 * s, server.center.y)
        quadraticTo((server.right + handset.left) / 2f, server.center.y - 24 * s, handset.left - 8 * s, handset.center.y) }
    val second = Path().apply { moveTo(handset.right + 8 * s, handset.center.y)
        quadraticTo((handset.right + screen.left - 6 * s) / 2f, handset.center.y - 24 * s, screen.left - 14 * s, screen.center.y + 8 * s) }
    val guide = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 6.dp.toPx())))
    drawPath(first, c.trail, style = guide)
    drawPath(second, c.trail, style = guide)
    drawTravelled(first, arcOne, c.ink.copy(alpha = .55f))
    drawTravelled(second, arcTwo, c.ink.copy(alpha = .55f))

    // Server: two stacked units with a status light each.
    val unit = (server.height - 8 * s) / 2f
    repeat(2) { i ->
        val top = server.top + i * (unit + 8 * s)
        drawRoundRect(c.body, Offset(server.left, top), Size(server.width, unit), CornerRadius(9 * s))
        drawRoundRect(c.edge, Offset(server.left, top), Size(server.width, unit), CornerRadius(9 * s), style = Stroke(1.2.dp.toPx()))
        drawCircle(c.ink.copy(alpha = .75f), 3.2f * s, Offset(server.left + 15 * s, top + unit / 2f))
        drawLine(c.ink.copy(alpha = .45f), Offset(server.left + 28 * s, top + unit / 2f),
            Offset(server.right - 14 * s, top + unit / 2f), 2.2f * s, StrokeCap.Round)
    }

    // Phone: the screen lights, then the approval tick draws itself.
    drawRoundRect(c.body, handset.topLeft, handset.size, CornerRadius(11 * s))
    drawRoundRect(c.edge, handset.topLeft, handset.size, CornerRadius(11 * s), style = Stroke(1.2.dp.toPx()))
    val glass = Rect(handset.left + 5 * s, handset.top + 8 * s, handset.right - 5 * s, handset.bottom - 12 * s)
    drawRoundRect(lerp(c.screen, c.lit, phone), glass.topLeft, glass.size, CornerRadius(6 * s))
    drawLine(c.ink.copy(alpha = .4f), Offset(handset.center.x - 7 * s, handset.bottom - 6 * s),
        Offset(handset.center.x + 7 * s, handset.bottom - 6 * s), 2f * s, StrokeCap.Round)
    if (phone > 0f) {
        val tick = Path().apply {
            moveTo(glass.center.x - 9 * s, glass.center.y + 1 * s)
            lineTo(glass.center.x - 2.5f * s, glass.center.y + 7 * s)
            lineTo(glass.center.x + 10 * s, glass.center.y - 7 * s)
        }
        drawTravelled(tick, phase(p, .38f, .48f) * fade, c.ink, width = 3f * s, lead = false)
    }

    // Television: a faint accent glow rises behind it, the screen warms and the play mark appears.
    if (tv > 0f) drawCircle(Brush.radialGradient(listOf(c.accent.copy(alpha = .14f * tv), Color.Transparent),
        center = screen.center, radius = 120 * s), radius = 120 * s, center = screen.center)
    drawRoundRect(c.body, Offset(screen.left - 6 * s, screen.top - 6 * s), Size(screen.width + 12 * s, screen.height + 12 * s), CornerRadius(14 * s))
    drawRoundRect(c.edge, Offset(screen.left - 6 * s, screen.top - 6 * s), Size(screen.width + 12 * s, screen.height + 12 * s),
        CornerRadius(14 * s), style = Stroke(1.2.dp.toPx()))
    drawRoundRect(Brush.verticalGradient(listOf(lerp(c.screen, c.lit, tv), lerp(c.screen, c.body, tv)),
        startY = screen.top, endY = screen.bottom), screen.topLeft, screen.size, CornerRadius(8 * s))
    drawLine(c.edge, Offset(screen.center.x, screen.bottom + 6 * s), Offset(screen.center.x, screen.bottom + 16 * s), 3f * s)
    drawLine(c.edge, Offset(screen.center.x - 30 * s, screen.bottom + 17 * s), Offset(screen.center.x + 30 * s, screen.bottom + 17 * s),
        3f * s, StrokeCap.Round)
    val play = Path().apply {
        moveTo(screen.center.x - 9 * s, screen.center.y - 13 * s)
        lineTo(screen.center.x + 14 * s, screen.center.y)
        lineTo(screen.center.x - 9 * s, screen.center.y + 13 * s); close()
    }
    drawPath(play, c.ink.copy(alpha = .12f + .78f * tv),
        style = Stroke(2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Draw the first [amount] of [path]; an arc carries a small leading point while it travels. */
private fun DrawScope.drawTravelled(path: Path, amount: Float, color: Color, width: Float = 1.6.dp.toPx(), lead: Boolean = true) {
    if (amount <= 0f) return
    val measure = PathMeasure().apply { setPath(path, false) }
    val part = Path()
    measure.getSegment(0f, measure.length * amount, part, true)
    drawPath(part, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    if (lead && amount < 1f) drawCircle(color.copy(alpha = 1f), width * 1.9f, measure.getPosition(measure.length * amount))
}
