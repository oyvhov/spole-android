package app.reelstack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.Season
import app.reelstack.data.model.SeasonalDecor
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.LocalPersonalization
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Opens a season's shelf in the library. Provided once beside the view model, so the greeting on
 * Home can be a door without every screen between them passing a callback along.
 */
internal val LocalOpenSeasonShelf = androidx.compose.runtime.staticCompositionLocalOf<((Season) -> Unit)?> { null }

/** The season a small touch shows: NONE outside a season, and when decorations are off. */
@Composable
internal fun touchSeason(): Season {
    val options = LocalPersonalization.current
    return if (options.seasonalDecor == SeasonalDecor.OFF) Season.NONE else Season.of(options)
}

/** The season whose scenes, particles and creatures may show: FULL only. */
@Composable
internal fun fullSeason(): Season {
    val options = LocalPersonalization.current
    return if (options.seasonalDecor == SeasonalDecor.FULL) Season.of(options) else Season.NONE
}

/**
 * The handle on the playback timeline. In a season it is the one decoration on a playing screen: a
 * pumpkin or a snowflake of the same size and in the same place as the white dot, so seeking reads
 * exactly as before. Nothing else in the player changes.
 */
internal fun DrawScope.drawTimelineHandle(season: Season, center: Offset, radius: Float, tint: Color = Color.White) {
    when (season) {
        Season.NONE -> drawCircle(tint, radius, center)
        Season.HALLOWEEN -> drawPumpkin(center + Offset(0f, radius * .12f), radius)
        Season.CHRISTMAS -> {
            // Thick arms with round ends read as a flake even at six pixels across; thin ones vanish.
            val arm = radius * 1.05f
            val width = (radius * .34f).coerceAtLeast(1.4.dp.toPx())
            repeat(3) { index ->
                val angle = index * PI.toFloat() / 3f + PI.toFloat() / 2f
                val offset = Offset(cos(angle), sin(angle)) * arm
                drawLine(tint, center - offset, center + offset, width, StrokeCap.Round)
            }
            drawCircle(tint, width * .9f, center)
        }
    }
}

/** The menu icon for a season's shelf. */
internal fun Season.shelfIcon(): ImageVector = if (this == Season.CHRISTMAS) SpoleIcons.Snowflake else SpoleIcons.Pumpkin

/**
 * A progress wheel with the season's figure resting in the middle. The wheel itself is unchanged,
 * so its speed and its meaning are the same in every season; only the centre has something in it.
 */
@Composable
internal fun SeasonalSpinner(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
) {
    val season = touchSeason()
    // 40 dp is Material's own size; a size the caller sets comes first and wins.
    Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.matchParentSize(), color = color, strokeWidth = strokeWidth)
        if (season != Season.NONE) Canvas(Modifier.fillMaxSize(.46f).testTag("spinner-season-$season").clearAndSetSemantics { }) {
            // Below nine density-independent pixels a figure is a smudge; leave the wheel plain.
            if (size.minDimension >= 9.dp.toPx()) drawTimelineHandle(season, center, size.minDimension / 2f, color)
        }
    }
}

/**
 * Halloween only, FULL only: when the remote has been left alone for a while, a spider lets itself
 * down the edge of the menu on a thread, and climbs back up at the first press. It is drawn under
 * the menu's labels and is gone with motion turned down.
 */
@Composable
internal fun SeasonalIdleSpider(modifier: Modifier = Modifier, idleMillis: Long = 45_000) {
    if (fullSeason() != Season.HALLOWEEN || !LocalMotionEnabled.current) return
    val lastInput by SpoleEggs.lastInputAt.collectAsState()
    val drop = remember { Animatable(0f) }
    LaunchedEffect(lastInput) {
        drop.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        delay(idleMillis)
        drop.animateTo(1f, tween(5_000, easing = LinearOutSlowInEasing))
    }
    if (drop.value <= 0f) return
    Canvas(modifier.testTag("season-idle-spider").clearAndSetSemantics { }) {
        val x = size.width - 18.dp.toPx()
        val y = 30.dp.toPx() + drop.value * size.height * .3f
        drawLine(Color(0xFFD8D1E9).copy(alpha = .5f), Offset(x, 0f), Offset(x, y), 1.dp.toPx())
        drawSpider(Offset(x, y), 7.dp.toPx())
    }
}
