package app.reelstack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.reelstack.ui.theme.ReelLayout

/**
 * A skeleton stands in for content on the page it appears on, so it takes its colours from the
 * chosen mood. These used to be fixed forest greys, which meant every refresh in MIDNIGHT, CINEMA
 * or PLUM shimmered green against a ground that was not.
 */
@Composable
private fun ShimmerBlock(
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(ReelLayout.ArtworkCorner),
) {
    val skeletonBase = MaterialTheme.colorScheme.surfaceVariant
    val skeletonGlow = MaterialTheme.colorScheme.onSurfaceVariant
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
                    colors = listOf(skeletonBase, skeletonGlow.copy(alpha = 0.26f), skeletonBase),
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
            .height(292.dp)
            .clearAndSetSemantics { contentDescription = "Lastar aktive avspelingar" },
        shape = RoundedCornerShape(ReelLayout.ArtworkCorner),
    )
}

@Composable
fun LibraryRailSkeleton(description: String, modifier: Modifier = Modifier, wide: Boolean = false,
    tabletArtwork: Boolean = app.reelstack.ui.theme.LocalTabletCanvas.current) {
    val cardWidth = (if (wide) { if (tabletArtwork) 292.dp else ReelLayout.EpisodeWidth }
        else { if (tabletArtwork) 158.dp else ReelLayout.PosterWidth }) * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
    val artworkHeight = if (wide) cardWidth * 9f / 16f else cardWidth * 1.5f
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        items(3) { index ->
            Column(Modifier.width(cardWidth)) {
                ShimmerBlock(
                    modifier = Modifier.fillMaxWidth().height(artworkHeight),
                    shape = RoundedCornerShape(ReelLayout.ArtworkCorner),
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
fun RecommendationSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        modifier = modifier.clearAndSetSemantics { contentDescription = "Lastar anbefalingar" },
    ) {
        items(3) {
            ShimmerBlock(Modifier.width(164.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale).height(258.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale), RoundedCornerShape(ReelLayout.ArtworkCorner))
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
            ShimmerBlock(Modifier.width(280.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale).height(226.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale), RoundedCornerShape(ReelLayout.ArtworkCorner))
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
                ShimmerBlock(Modifier.size(width = 66.dp, height = 82.dp), RoundedCornerShape(ReelLayout.ArtworkCorner))
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
    ShimmerBlock(
        modifier = modifier.height(300.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale)
            .clearAndSetSemantics { contentDescription = "Lastar søkjeresultat" },
        shape = RoundedCornerShape(ReelLayout.ArtworkCorner),
    )
}

@Composable
fun ActivitySkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "Lastar aktivitet" },
    ) {
        repeat(3) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                ShimmerBlock(Modifier.size(width = 72.dp, height = 96.dp), RoundedCornerShape(ReelLayout.ArtworkCorner))
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
