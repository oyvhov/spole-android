package app.reelstack.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.ui.KidsBrowse
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.*
import coil3.compose.AsyncImage

/**
 * The middle level: stills and big numbers, one tap to play.
 *
 * For the older half of the age range, who want to pick the episode themselves. There is no step
 * between an episode and the picture starting — tapping a still plays it.
 */
@Composable
fun KidsEpisodesScreen(
    browse: KidsBrowse,
    onPlay: (LibraryMedia) -> Unit,
    onSelectSeason: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    // D-pad lands in the episodes, not on the season chips or the back button. Moving down out of
    // a LazyRow that sits inside a grid item does not find the cards on its own.
    val firstEpisode = remember { FocusRequester() }
    LaunchedEffect(browse.selectedSeasonId, browse.episodes.firstOrNull()?.id) {
        if (browse.episodes.isNotEmpty()) runCatching { firstEpisode.requestFocus() }
    }

    Column(modifier = modifier.fillMaxSize().testTag("kids-episodes")) {
        KidsEpisodesHeader(title = browse.title, onBack = onBack)

        if (browse.loading && browse.episodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(44.dp))
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // No season picker when the series has one season. A single chip is a decision the
            // child does not have to make.
            if (browse.seasons.size > 1) {
                item(span = { GridItemSpan(columns) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 6.dp),
                    ) {
                        items(browse.seasons, key = { it.id }) { season ->
                            SeasonChip(
                                season = season,
                                selected = season.remoteId == browse.selectedSeasonId,
                                onClick = { season.remoteId?.let(onSelectSeason) },
                            )
                        }
                    }
                }
            }

            itemsIndexed(browse.episodes, key = { _, item -> item.id }) { index, episode ->
                EpisodeCard(
                    episode = episode,
                    onPlay = { onPlay(episode) },
                    modifier = if (index == 0) Modifier.focusRequester(firstEpisode) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun KidsEpisodesHeader(title: String, onBack: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val backDescription = stringResource(R.string.action_back)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SurfaceRaised)
                .focusOutline(interaction, CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onBack,
                )
                .semantics { contentDescription = backDescription }
                .testTag("kids-episodes-back"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(SpoleIcons.ArrowBack, contentDescription = null, tint = app.reelstack.ui.theme.Text, modifier = Modifier.size(26.dp))
        }

        Spacer(Modifier.width(18.dp))

        Text(
            text = title,
            color = app.reelstack.ui.theme.Text,
            fontSize = 26.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SeasonChip(season: LibraryMedia, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    val number = season.episode

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 64.dp, minWidth = 64.dp)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else SurfaceRaised)
            .border(if (selected) 2.dp else 1.dp, if (selected) Primary else ControlOutline, shape)
            .focusOutline(interaction, shape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (number != null && number > 0) {
                stringResource(R.string.kids_season_number, number)
            } else {
                season.title
            },
            color = if (selected) Primary else app.reelstack.ui.theme.Text,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun EpisodeCard(episode: LibraryMedia, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale = rememberKidsFocusScale(focused)
    val shape = RoundedCornerShape(20.dp)
    val accent = Primary

    // A watched episode steps back; the one to play next is the one that stands out.
    val watched = episode.played
    val isNext = !watched && (episode.progress ?: 0f) > 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onPlay,
            )
            .testTag("kids-episode-${episode.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .kidsFocusLift(focused, scale, accent)
                .clip(shape)
                .background(SurfaceRaised),
        ) {
            MediaArtwork(
                url = episode.heroUrl ?: episode.artworkUrl ?: episode.posterUrl,
                contentDescription = episode.title,
                source = episode.source,
                fallbackRes = R.drawable.media_placeholder,
                modifier = Modifier.fillMaxSize().alpha(if (watched) 0.45f else 1f),
            )

            val number = episode.episode
            if (number != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        // Readable for a seven-year-old across a room: 19 sp, heavy.
                        text = number.toString(),
                        color = Color.White,
                        fontSize = 19.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (watched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(SpoleIcons.Done, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            val progress = episode.progress
            if (progress != null && progress > 0f && !watched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(Primary),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = episode.title,
            color = if (watched) Muted else app.reelstack.ui.theme.Text,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        )
    }
}
