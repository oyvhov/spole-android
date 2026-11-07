package app.reelstack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One vector master, assembled in three portions. No duplicate logo geometry or bitmap assets. */
@Composable
internal fun SpoleFormationMark(progress: () -> Float, modifier: Modifier = Modifier) {
    SpoleBrandMark(
        modifier = modifier.size(84.dp, 112.dp).drawWithContent {
            val amount = progress().coerceIn(0f, 1f)
            if (amount >= 1f) {
                drawContent()
            } else {
                val upper = formationPhase(amount, 0f, .64f)
                val lower = formationPhase(amount, .10f, .76f)
                val bridge = formationPhase(amount, .38f, .96f)
                // Film frames arrive from opposite directions, in a fixed layout slot.
                translate(top = -10.dp.toPx() * (1f - upper)) {
                    clipRect(left = size.width * (1f - upper), bottom = size.height * .45f) {
                        this@drawWithContent.drawContent()
                    }
                }
                translate(top = 10.dp.toPx() * (1f - lower)) {
                    clipRect(top = size.height * .55f, right = size.width * lower) {
                        this@drawWithContent.drawContent()
                    }
                }
                // The joining strip draws from left to right after the two frames begin to settle.
                clipRect(top = size.height * .45f, bottom = size.height * .55f, right = size.width * bridge) {
                    this@drawWithContent.drawContent()
                }
            }
        }.testTag("startup-logo"),
    )
}

/**
 * The mark sits on the centre of the box, and the name hangs below it, so the launch window (a plain
 * layer-list drawable with the mark in the middle of the screen) and this first frame agree to the pixel.
 * [markProgress] is separate from [progress] for the cover, which starts with the mark already formed:
 * the system window has just shown it whole, and forming it again would blank it first.
 */
@Composable
internal fun SpoleStartupArt(progress: () -> Float, modifier: Modifier = Modifier, markProgress: () -> Float = progress) {
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(300.dp)
                .graphicsLayer { alpha = (1f - formationPhase(progress(), .72f, 1f)) },
        ) {
            val p = progress().coerceIn(0f, 1f)
            val glow = formationPhase(p, 0f, .35f)
            val radius = size.minDimension * (.44f + .04f * glow)
            drawCircle(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = .16f * glow),
                        accent.copy(alpha = 0f),
                    ),
                ),
                radius = radius,
                center = center,
            )
        }
        SpoleFormationMark(markProgress)
        Text(
            "Spole", fontSize = 52.sp, lineHeight = 58.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                // Top of the name 76 dp below the centre: half the mark (56) and the old 20 dp gap.
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 76.dp.roundToPx() + placeable.height / 2) }
                }
                .testTag("startup-wordmark")
                .graphicsLayer {
                    val wordmark = formationPhase(progress(), .34f, .90f)
                    alpha = wordmark
                    translationY = (1f - wordmark) * 6.dp.toPx()
                },
        )
    }
}

private fun formationPhase(progress: Float, start: Float, end: Float): Float =
    FastOutSlowInEasing.transform(((progress - start) / (end - start)).coerceIn(0f, 1f))
