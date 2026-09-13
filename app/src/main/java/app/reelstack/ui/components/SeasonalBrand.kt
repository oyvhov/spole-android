package app.reelstack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.Season
import app.reelstack.data.model.Personalization
import app.reelstack.ui.theme.LocalPersonalization
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Decorations stay inside the mark's existing slot, so changing season never moves a label. */
@Composable
internal fun SpoleBrandMark(modifier: Modifier = Modifier, contentDescription: String? = null,
    options: Personalization = LocalPersonalization.current) {
    val season = Season.of(options)
    Box(modifier) {
        Image(painterResource(R.drawable.spole_mark), contentDescription, Modifier.matchParentSize(),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
        if (options.seasonalOrnament && season != Season.NONE) Canvas(Modifier.matchParentSize()
            .testTag("brand-season-$season").clearAndSetSemantics { }) {
            if (season == Season.CHRISTMAS) {
                val w = size.width
                val h = size.height
                val hat = Path().apply {
                    moveTo(w * .20f, h * .18f)
                    cubicTo(w * .29f, -h * .01f, w * .50f, -h * .01f, w * .61f, h * .025f)
                    cubicTo(w * .76f, h * .04f, w * .86f, h * .16f, w * .86f, h * .23f)
                    lineTo(w * .76f, h * .23f)
                    quadraticTo(w * .70f, h * .11f, w * .61f, h * .14f)
                    lineTo(w * .76f, h * .20f)
                    close()
                }
                drawPath(hat, Color(0xFFC93645))
                drawRoundRect(Color(0xFFFFF4DF), Offset(w * .18f, h * .155f), Size(w * .61f, h * .09f),
                    CornerRadius(h * .045f))
                drawCircle(Color(0xFFFFF4DF), w * .09f, Offset(w * .84f, h * .235f))
            } else {
                drawGhost(Offset(size.width * .79f, size.height * .16f), size.width * .27f, .96f)
            }
        }
    }
}

/** A compact seasonal illustration, also used as the live theme preview in settings. */
@Composable
internal fun SeasonalThemeBanner(modifier: Modifier = Modifier, options: Personalization = LocalPersonalization.current) {
    val season = Season.of(options)
    if (season == Season.NONE) return
    Surface(modifier.fillMaxWidth().testTag("season-banner-$season"), shape = RoundedCornerShape(20.dp),
        color = Color(options.visualTheme.raised)) {
        Box(Modifier.heightIn(min = 110.dp)) {
            if (options.seasonalOrnament) Canvas(Modifier.matchParentSize().clearAndSetSemantics { }) {
                drawSeasonScene(season, 0f, true)
            }
            Row(Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpoleBrandMark(Modifier.size(44.dp), options = options)
                Column(Modifier.weight(1f).padding(end = 72.dp)) {
                    Text(stringResource(if (season == Season.CHRISTMAS) R.string.season_christmas_greeting else R.string.season_halloween_greeting),
                        style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if (season == Season.CHRISTMAS) R.string.season_christmas_note else R.string.season_halloween_note),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Static artwork reused from the seasonal theme; no timers, image fetches or blur layers. */
@Composable
internal fun SeasonalBackdrop(modifier: Modifier = Modifier, menu: Boolean = false) {
    val options = LocalPersonalization.current
    val season = Season.of(options)
    if (season == Season.NONE) return
    Canvas(modifier.testTag("season-backdrop-$season").clearAndSetSemantics { }) {
        val tint = if (season == Season.CHRISTMAS) Color(0xFF94713D) else Color(0xFF75428E)
        drawRect(Brush.linearGradient(listOf(tint.copy(alpha = if (menu) .22f else .16f),
            Color.Transparent, tint.copy(alpha = .08f))))
        if (options.seasonalOrnament) {
            // Decoration stays at the edge, away from menu labels and the reading column.
            val menuTop = size.height - 100.dp.toPx()
            if (menu) withTransform({ translate(0f, menuTop) }) {
                drawSeasonScene(season, 0f, true)
            } else drawSeasonScene(season, 0f)
        }
    }
}

/** Low-cost vector shapes: no image requests, blur layers or decoration over the reading column. */
internal fun DrawScope.drawSeasonScene(season: Season, time: Float, prominent: Boolean = false) {
    val edge = if (prominent) 78.dp.toPx() else minOf(size.width * .22f, 150.dp.toPx())
    val right = size.width
    if (season == Season.CHRISTMAS) {
        val pine = Color(0xFF517C65)
        val gold = Color(0xFFF4D48E)
        // A pine branch follows the top edge; the light string drops into the artwork corner.
        drawLine(pine, Offset(right - edge * 1.4f, 0f), Offset(right, edge * .22f), 2.dp.toPx())
        repeat(8) { index ->
            val x = right - edge * 1.35f + index * edge * .17f
            val y = index * edge * .025f
            drawLine(pine, Offset(x, y), Offset(x - edge * .1f, y + edge * .18f), 2.dp.toPx())
            drawLine(pine, Offset(x, y), Offset(x + edge * .12f, y + edge * .12f), 2.dp.toPx())
        }
        repeat(5) { index ->
            val x = right - edge * 1.15f + index * edge * .25f
            val y = edge * (.18f + .08f * sin(index.toFloat()))
            drawLine(gold.copy(alpha = .55f), Offset(x, 0f), Offset(x, y), 1.dp.toPx())
            drawCircle(if (index % 2 == 0) gold else Color(0xFFED8C83), 3.dp.toPx(), Offset(x, y))
        }
        val star = Offset(right - edge * .43f, minOf(size.height * .64f, edge * .80f))
        drawStar(star, edge * .15f, gold)
        drawStar(Offset(right - edge * .91f, edge * .49f), edge * .065f, gold.copy(alpha = .7f))
    } else {
        val web = Color(0xFFD8D1E9).copy(alpha = .48f)
        val corner = Offset(right, 0f)
        repeat(5) { ray ->
            val angle = PI.toFloat() * (.5f + ray * .125f)
            drawLine(web, corner, corner + Offset(cos(angle), sin(angle)) * edge, 1.dp.toPx())
        }
        repeat(3) { ring ->
            val radius = edge * (ring + 1) / 3f
            val path = Path()
            repeat(5) { ray ->
                val angle = PI.toFloat() * (.5f + ray * .125f)
                val point = corner + Offset(cos(angle), sin(angle)) * radius
                if (ray == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(path, web, style = Stroke(1.dp.toPx()))
        }
        val spider = Offset(right - edge * .30f, edge * (.71f + .03f * sin(time * .8f)))
        drawLine(web, Offset(spider.x, 0f), spider, 1.dp.toPx())
        drawSpider(spider, edge * .13f)
        drawGhost(Offset(right - edge * .83f, minOf(size.height * .72f, edge * 1.02f) + sin(time) * 3.dp.toPx()), edge * .40f, .86f)
        if (!prominent) {
            drawGhost(Offset(right - edge * .34f, size.height * .77f + sin(time + 2f) * 3.dp.toPx()), edge * .28f, .65f)
            drawSpider(Offset(right - edge * 1.17f, edge * .22f), edge * .09f)
        }
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, tint: Color) {
    val path = Path().apply {
        repeat(8) { index ->
            val angle = index * PI.toFloat() / 4f - PI.toFloat() / 2f
            val r = radius * if (index % 2 == 0) 1f else .25f
            val p = center + Offset(cos(angle), sin(angle)) * r
            if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    drawPath(path, tint)
}

private fun DrawScope.drawGhost(center: Offset, width: Float, opacity: Float) {
    val left = center.x - width / 2
    val top = center.y - width / 2
    val path = Path().apply {
        moveTo(left, top + width)
        lineTo(left, top + width * .48f)
        cubicTo(left, top - width * .18f, left + width, top - width * .18f, left + width, top + width * .48f)
        lineTo(left + width, top + width)
        quadraticTo(left + width * .83f, top + width * .76f, left + width * .67f, top + width)
        quadraticTo(left + width * .5f, top + width * .78f, left + width * .33f, top + width)
        quadraticTo(left + width * .17f, top + width * .76f, left, top + width)
        close()
    }
    drawPath(path, Color(0xFFF1EAF8).copy(alpha = opacity))
    listOf(.34f, .66f).forEach { x ->
        drawOval(Color(0xFF30223C), Offset(left + width * (x - .055f), top + width * .36f), Size(width * .11f, width * .19f))
    }
}

private fun DrawScope.drawSpider(center: Offset, radius: Float) {
    val tint = Color(0xFFB7A1CE)
    repeat(4) { index ->
        for (side in listOf(-1f, 1f)) {
            val knee = center + Offset(side * radius * 1.55f, radius * (index - 1.5f) * .65f)
            drawLine(tint, center, knee, 1.2.dp.toPx())
            drawLine(tint, knee, knee + Offset(side * radius * .35f, radius * .55f), 1.2.dp.toPx())
        }
    }
    drawOval(tint, center - Offset(radius * .6f, radius * .8f), Size(radius * 1.2f, radius * 1.6f))
    drawCircle(Color(0xFFFFB264), radius * .17f, center + Offset(-radius * .22f, -radius * .30f))
    drawCircle(Color(0xFFFFB264), radius * .17f, center + Offset(radius * .22f, -radius * .30f))
}
