package app.reelstack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R

/** One vector master, assembled in three portions. No duplicate logo geometry or bitmap assets. */
@Composable
internal fun SpoleFormationMark(progress: () -> Float, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.spole_mark),
        contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
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

@Composable
internal fun SpoleStartupArt(progress: () -> Float, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
      Canvas(Modifier.size(380.dp).graphicsLayer { alpha = (1f - formationPhase(progress(), .72f, 1f)) }) {
        val p = progress().coerceIn(0f, 1f)
        val enter = formationPhase(p, 0f, .35f)
        val settle = formationPhase(p, .15f, .9f)
        val radius = size.minDimension * (.2f + .3f * settle)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .12f * enter), accent.copy(alpha = 0f))), radius, center)
        // Twelve strips frame a projector-like sweep and settle into the original Spole mark.
        repeat(12) { index ->
            val angle = (index * 30f + (1f - settle) * 65f) * Math.PI / 180
            val distance = size.minDimension * (.46f - .3f * settle)
            val point = Offset(center.x + kotlin.math.cos(angle).toFloat() * distance, center.y + kotlin.math.sin(angle).toFloat() * distance)
            drawLine(accent.copy(alpha = .34f * enter), point,
                Offset(point.x + kotlin.math.cos(angle).toFloat() * 16.dp.toPx(), point.y + kotlin.math.sin(angle).toFloat() * 16.dp.toPx()),
                strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        drawArc(accent.copy(alpha = .45f * enter), -90f + 280f * p, 90f * enter,
            false, Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SpoleFormationMark(progress)
        Spacer(Modifier.height(20.dp))
        Text(
            "Spole", fontSize = 52.sp, lineHeight = 58.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("startup-wordmark").graphicsLayer {
                val wordmark = formationPhase(progress(), .34f, .90f)
                alpha = wordmark
                translationY = (1f - wordmark) * 6.dp.toPx()
            },
        )
      }
    }
}

private fun formationPhase(progress: Float, start: Float, end: Float): Float =
    FastOutSlowInEasing.transform(((progress - start) / (end - start)).coerceIn(0f, 1f))
