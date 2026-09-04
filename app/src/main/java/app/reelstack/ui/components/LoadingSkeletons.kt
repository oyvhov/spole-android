package app.reelstack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

private val SkeletonBase = Color(0xFF211B2C)
private val SkeletonGlow = Color(0xFF9A70C2)

@Composable
private fun ShimmerBlock(
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton-shimmer-progress",
    )
    Box(
        modifier
            .clip(shape)
            .drawWithCache {
                val start = Offset(size.width * (progress - 1f), 0f)
                val end = Offset(size.width * progress, size.height)
                val brush = Brush.linearGradient(
                    colors = listOf(SkeletonBase, SkeletonGlow.copy(alpha = 0.32f), SkeletonBase),
                    start = start,
                    end = end,
                )
                onDrawBehind { drawRect(brush) }
            },
    )
}

@Composable
fun NowPlayingSkeleton(modifier: Modifier = Modifier) {
    ShimmerBlock(
        modifier = modifier
            .fillMaxWidth()
            .height(306.dp)
            .clearAndSetSemantics { contentDescription = "Lastar aktive avspelingar" },
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 86.dp, bottomEnd = 34.dp, bottomStart = 34.dp),
    )
}

@Composable
fun LibraryRailSkeleton(description: String, wide: Boolean = false, modifier: Modifier = Modifier) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        items(3) { index ->
            val cardWidth = if (wide) 224.dp else 146.dp
            val artworkHeight = if (wide) 126.dp else 192.dp
            Column(Modifier.width(cardWidth)) {
                ShimmerBlock(
                    modifier = Modifier.fillMaxWidth().height(artworkHeight),
                    shape = RoundedCornerShape(20.dp),
                )
                ShimmerBlock(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .width(if (index == 1) 105.dp else 126.dp)
                        .height(13.dp),
                    shape = RoundedCornerShape(7.dp),
                )
                ShimmerBlock(
                    modifier = Modifier.padding(top = 7.dp).width(70.dp).height(9.dp),
                    shape = RoundedCornerShape(5.dp),
                )
            }
        }
    }
}

@Composable
fun UpcomingSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        modifier = modifier.clearAndSetSemantics { contentDescription = "Lastar komande utgjevingar" },
    ) {
        items(3) { index ->
            Column(Modifier.width(178.dp)) {
                ShimmerBlock(Modifier.fillMaxWidth().height(224.dp), RoundedCornerShape(24.dp))
                ShimmerBlock(
                    Modifier.padding(top = 10.dp).width(if (index == 1) 118.dp else 150.dp).height(15.dp),
                    RoundedCornerShape(8.dp),
                )
                ShimmerBlock(Modifier.padding(top = 7.dp).width(96.dp).height(9.dp), RoundedCornerShape(5.dp))
            }
        }
    }
}

@Composable
fun IncomingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "Lastar nedlastingar" },
    ) {
        repeat(2) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                ShimmerBlock(Modifier.size(width = 66.dp, height = 82.dp), RoundedCornerShape(13.dp))
                Column(Modifier.padding(start = 16.dp)) {
                    ShimmerBlock(Modifier.width(170.dp).height(15.dp), RoundedCornerShape(8.dp))
                    ShimmerBlock(Modifier.padding(top = 9.dp).width(92.dp).height(10.dp), RoundedCornerShape(5.dp))
                    ShimmerBlock(Modifier.padding(top = 8.dp).width(132.dp).height(10.dp), RoundedCornerShape(5.dp))
                }
            }
        }
    }
}

@Composable
fun DiscoverSkeleton(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(13.dp),
        modifier = modifier.clearAndSetSemantics { contentDescription = "Lastar søkjeresultat" },
    ) {
        repeat(2) {
            Row(modifier = Modifier.fillMaxWidth().height(184.dp).padding(13.dp)) {
                ShimmerBlock(Modifier.size(width = 116.dp, height = 158.dp), RoundedCornerShape(18.dp))
                Column(Modifier.padding(start = 17.dp, top = 4.dp)) {
                    ShimmerBlock(Modifier.width(84.dp).height(9.dp), RoundedCornerShape(5.dp))
                    ShimmerBlock(Modifier.padding(top = 12.dp).width(156.dp).height(20.dp), RoundedCornerShape(10.dp))
                    ShimmerBlock(Modifier.padding(top = 9.dp).width(98.dp).height(10.dp), RoundedCornerShape(5.dp))
                    ShimmerBlock(Modifier.padding(top = 48.dp).width(104.dp).height(34.dp), RoundedCornerShape(12.dp))
                }
            }
        }
    }
}

@Composable
fun ActivitySkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "Lastar aktivitet" },
    ) {
        repeat(3) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                ShimmerBlock(Modifier.size(width = 72.dp, height = 96.dp), RoundedCornerShape(15.dp))
                Column(Modifier.padding(start = 16.dp)) {
                    ShimmerBlock(Modifier.width(64.dp).height(9.dp), RoundedCornerShape(5.dp))
                    ShimmerBlock(Modifier.padding(top = 9.dp).width(174.dp).height(15.dp), RoundedCornerShape(8.dp))
                    ShimmerBlock(Modifier.padding(top = 8.dp).width(138.dp).height(10.dp), RoundedCornerShape(5.dp))
                }
            }
        }
    }
}

@Composable
fun DetailTextSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "Hentar fleire detaljar" },
    ) {
        ShimmerBlock(Modifier.fillMaxWidth().height(13.dp), RoundedCornerShape(7.dp))
        ShimmerBlock(Modifier.padding(top = 9.dp).fillMaxWidth().height(13.dp), RoundedCornerShape(7.dp))
        ShimmerBlock(Modifier.padding(top = 9.dp).fillMaxWidth(0.68f).height(13.dp), RoundedCornerShape(7.dp))
    }
}
