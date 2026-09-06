package app.reelstack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.RequestStage
import app.reelstack.ui.theme.*

/** A small film-strip composition, drawn locally: crisp at every scale and ready offline. */
@Composable
fun SpoleWelcomeArt(modifier: Modifier = Modifier) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(480)) }
    Box(modifier.fillMaxWidth().height(148.dp).clip(RoundedCornerShape(24.dp))
        .background(Surface).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize().graphicsLayer {
            alpha = reveal.value
            translationY = (1 - reveal.value) * 14.dp.toPx()
        }) {
            val stripHeight = size.height * .64f
            rotate(-12f) {
                drawRoundRect(SurfaceRaised, Offset(-20f, size.height * .18f),
                    Size(size.width + 40f, stripHeight), CornerRadius(8.dp.toPx()))
                val step = 22.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    listOf(size.height * .23f, size.height * .70f).forEach { y ->
                        drawRoundRect(Ink, Offset(x, y), Size(10.dp.toPx(), 5.dp.toPx()), CornerRadius(2.dp.toPx()))
                    }
                    x += step
                }
            }
        }
        Box(Modifier.size(88.dp).clip(RoundedCornerShape(24.dp)).background(Ink), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.spole_mark), null, Modifier.size(48.dp), colorFilter = ColorFilter.tint(Primary))
        }
    }
}

/** A shared, truthful three-step journey; errors never light up a completed step. */
@Composable
fun RequestJourney(stage: RequestStage?, modifier: Modifier = Modifier) {
    val completed = when (stage) {
        RequestStage.AVAILABLE -> 3
        RequestStage.DOWNLOADING, RequestStage.IMPORTING -> 1
        RequestStage.REQUESTED -> 1
        else -> 0
    }
    val current = when (stage) {
        RequestStage.DOWNLOADING, RequestStage.IMPORTING -> 1
        RequestStage.AVAILABLE -> 2
        else -> 0
    }
    val icons = listOf(Icons.Rounded.Schedule, Icons.Rounded.Download, Icons.Rounded.VideoLibrary)
    val labels = listOf("Førespurd", "Lastar ned", "I biblioteket")
    Row(modifier.fillMaxWidth().clearAndSetSemantics {
        contentDescription = stage?.let { "Framdrift: ${it.label}" }
            ?: "Førespurd, lastar ned, i biblioteket"
    }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            val done = index < completed
            val active = stage != null && stage !in setOf(RequestStage.UNKNOWN, RequestStage.FAILED, RequestStage.DECLINED) && index == current
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape)
                    .background(if (done || active) Primary else Divider))
                Icon(if (done) Icons.Rounded.Check else icons[index], null,
                    tint = if (done) Success else if (active) MaterialTheme.colorScheme.onSurface else Muted,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp).size(19.dp))
                Text(label, color = if (done || active) MaterialTheme.colorScheme.onSurface else Muted,
                    fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}
