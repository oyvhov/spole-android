package app.reelstack.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.ui.theme.*
import coil3.compose.AsyncImage

/**
 * Two rows. Not three, and never a recommendation.
 *
 * Everything on this screen plays on one tap — there is no step between the artwork and the
 * picture starting, because that step is the thing a four-year-old cannot cross on their own.
 */
@Composable
fun KidsHomeScreen(
    keepWatching: List<LibraryMedia>,
    yourShows: List<LibraryMedia>,
    onPlay: (LibraryMedia) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (keepWatching.isEmpty() && yourShows.isEmpty()) {
        KidsEmptyState(modifier = modifier)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize().testTag("kids-home"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (keepWatching.isNotEmpty()) {
            fullWidthItem(columns) {
                KidsSectionTitle(stringResource(R.string.kids_keep_watching))
            }
            fullWidthItem(columns) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    // A focused card grows 1,05x. Without room of its own the row clips the lift
                    // and shaves the top off the title underneath it.
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 6.dp),
                    modifier = Modifier.testTag("kids-keep-watching"),
                ) {
                    items(keepWatching, key = { it.id }) { media ->
                        KidsWideCard(media = media, onPlay = { onPlay(media) })
                    }
                }
            }
        }

        if (yourShows.isNotEmpty()) {
            fullWidthItem(columns) {
                KidsSectionTitle(stringResource(R.string.kids_your_shows))
            }
            items(yourShows, key = { it.id }) { media ->
                KidsPosterCard(media = media, onPlay = { onPlay(media) })
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullWidthItem(
    columns: Int,
    content: @Composable () -> Unit,
) {
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) { content() }
}

@Composable
private fun KidsSectionTitle(text: String) {
    Text(
        text = text,
        color = Primary,
        // 26 sp, per the plan's size table. A section heading a seven-year-old reads across a room.
        fontSize = 26.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

/** Wide 16:9 artwork with the progress line on the artwork's own bottom edge. */
@Composable
private fun KidsWideCard(media: LibraryMedia, onPlay: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(20.dp)
    val accent = Primary
    val scale = rememberKidsFocusScale(focused)

    Column(
        modifier = Modifier
            .width(268.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onPlay,
            )
            .testTag("kids-card-${media.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                // Only the artwork lifts. Scaling the whole card grew the poster down over its
                // own title, which read as clipped text.
                .kidsFocusLift(focused, scale, accent)
                .clip(shape)
                .background(SurfaceRaised),
        ) {
            AsyncImage(
                model = media.heroUrl ?: media.artworkUrl ?: media.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            val progress = media.progress
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(7.dp)
                        .background(Color.Black.copy(alpha = 0.45f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Primary),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = media.title,
            color = Primary,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        )
    }
}

/** Poster tile for the grid. No service badge, no rating, no overview — the plan forbids all three. */
@Composable
private fun KidsPosterCard(media: LibraryMedia, onPlay: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(20.dp)
    val accent = Primary
    val scale = rememberKidsFocusScale(focused)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onPlay,
            )
            .testTag("kids-card-${media.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .kidsFocusLift(focused, scale, accent)
                .clip(shape)
                .background(SurfaceRaised),
        ) {
            AsyncImage(
                model = media.posterUrl ?: media.artworkUrl ?: media.heroUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = media.title,
            color = Primary,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun KidsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // An empty planet, not an error code. Nothing here is the kid's fault, and naming a
        // service or a status code would only tell them to fetch an adult.
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(SurfaceRaised),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.kids_empty_title),
            color = Primary,
            fontSize = 26.sp,
            lineHeight = 33.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.kids_empty_body),
            color = Muted,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
    }
}
