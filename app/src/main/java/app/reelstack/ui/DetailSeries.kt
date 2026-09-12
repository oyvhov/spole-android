package app.reelstack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.SeriesBrowse
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.screens.Chip
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft

/**
 * What the title *is*, as opposed to what you can do with it right now.
 *
 * On television this sits under the artwork, in the column that used to be a metre of black next to
 * a 230 dp thumbnail. Year, running time, quality, genres and the tagline all describe the object
 * and none of them are decisions, so putting them beside the picture leaves the reading column for
 * the title, the actions and the seasons.
 */
@Composable
internal fun DetailAside(
    details: ContentDetails,
    opening: ContentDetails,
    facts: List<String>,
    tv: Boolean,
    synopsis: (@Composable () -> Unit)? = null,
    cast: (@Composable () -> Unit)? = null,
) {
    val remaining = facts
        .filterNot { it.matches(Regex("^S\\d\\d+ E\\d\\d+$")) }
        .filterNot { it in setOf("Film", "Serie", "Episode", "Movie", "Series") }
    val tagline = details.tagline?.takeIf(String::isNotBlank) ?: opening.tagline?.takeIf(String::isNotBlank)
    if (remaining.isEmpty() && details.genres.isEmpty() && tagline == null && synopsis == null && cast == null) return
    Column(
        Modifier.fillMaxWidth().padding(top = if (tv) 16.dp else 18.dp).testTag("detail-aside"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        app.reelstack.ui.components.PlaybackMetadata(details, remaining)
        if (details.genres.isNotEmpty()) Text(
            details.genres.take(4).joinToString(" · "),
            color = PrimarySoft,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (tagline != null) Text(
            tagline,
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        // On television the synopsis comes down here too. The reading column's job is what you can
        // do with this title — play it, pick a track, pick an episode — and a paragraph of prose in
        // the middle of that pushed the episode list off the bottom of the screen while the column
        // beside the picture stayed empty. A description is not a decision.
        synopsis?.invoke()
        // And who is in it. On television this was the last thing on the page, under a status card
        // and fourteen episode rows — while the column beside the picture, which is exactly where a
        // reader looks for faces, stayed black.
        cast?.invoke()
    }
}

/**
 * The seasons of a series and the episodes of the one being looked at.
 *
 * Before this, a series page had a button that said "choose an episode" and handed the whole job to
 * the player's own browser — so the one page that knows which series it is could not tell you what
 * the series contains. Every episode here starts from the page, at its own resume point.
 */
@Composable
internal fun SeriesEpisodes(browse: SeriesBrowse, detailKey: String, onSeason: (String) -> Unit) {
    if (browse.openedFor != detailKey) return
    if (browse.seasons.isEmpty() && !browse.loading && browse.error == null) return
    Column(Modifier.fillMaxWidth().padding(top = 22.dp).testTag("detail-seasons"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (browse.seasons.size > 1 || browse.seasons.size == 1 && browse.seasons.first().title.isNotBlank()) {
            // A little air past the last chip, so a season that runs off the edge looks like a row
            // that continues rather than one that was cut.
            // A series with twenty-two seasons shows four of them, and without this the reader is
            // looking at season one's chips while season seven's episodes are listed underneath.
            val strip = androidx.compose.foundation.lazy.rememberLazyListState()
            val chosenIndex = browse.seasons.indexOfFirst { it.remoteId == browse.selectedSeasonId }
            androidx.compose.runtime.LaunchedEffect(browse.selectedSeasonId, browse.seasons.size) {
                if (chosenIndex >= 0) runCatching { strip.animateScrollToItem(chosenIndex) }
            }
            LazyRow(state = strip, horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 24.dp)) {
                items(browse.seasons, key = { it.id }) { season ->
                    val id = season.remoteId.orEmpty()
                    Chip(
                        text = seasonLabel(season),
                        chosen = id == browse.selectedSeasonId,
                        tag = "season-$id",
                        role = Role.RadioButton,
                    ) { onSeason(id) }
                }
            }
        }
        when {
            browse.error != null -> Text(browse.error, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium)
            browse.loading && browse.episodes.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                Text(stringResource(R.string.detail_loading_episodes), color = Muted,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
            }
            browse.episodes.isEmpty() -> Text(stringResource(R.string.detail_no_episodes), color = Muted,
                style = MaterialTheme.typography.bodyMedium)
            else -> {
                // The list lives inside a scrolling column, so every row it holds is composed
                // whether or not anyone can see it. Twenty-six is a long season; two hundred is a
                // long-running anime, and composing two hundred rows to show six is what turns a
                // page open into a visible pause.
                var showAll by remember(browse.selectedSeasonId) { mutableStateOf(false) }
                val visible = if (showAll) browse.episodes else browse.episodes.take(EPISODE_PREVIEW)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    visible.forEach { episode -> EpisodeRow(episode, episode.id == detailKey) }
                    if (visible.size < browse.episodes.size) Chip(
                        text = pluralStringResource(
                            R.plurals.detail_more_episodes,
                            browse.episodes.size - visible.size,
                            browse.episodes.size - visible.size,
                        ),
                        chosen = false,
                        tag = "episodes-more",
                    ) { showAll = true }
                }
            }
        }
    }
}

private const val EPISODE_PREVIEW = 12

/** "Sesong 2 · 8 episodar" when the count is known, otherwise the server's own name. */
@Composable
private fun seasonLabel(season: LibraryMedia): String {
    // A season's own number lands in `episode`; `season` on a season item is the series' index.
    val name = season.episode?.takeIf { it > 0 }?.let { stringResource(R.string.episode_season, it) }
        ?: season.title.takeIf(String::isNotBlank)
        ?: season.subtitle
    val count = season.childCount?.takeIf { it > 0 }
        ?.let { androidx.compose.ui.res.pluralStringResource(R.plurals.detail_season_episodes, it, it) }
    return listOfNotNull(name, count).joinToString(" · ")
}

/**
 * One episode, playable from here.
 *
 * The still is the widest thing in the row because it is the only part that identifies the episode
 * at a glance; everything else is one line so a season of twenty-four does not become a wall.
 */
@Composable
private fun EpisodeRow(episode: LibraryMedia, current: Boolean = false) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    val itemId = episode.remoteId.orEmpty()
    val progress = episode.progress?.coerceIn(0f, 1f) ?: 0f
    Row(
        Modifier.fillMaxWidth().clip(shape)
            // The episode this page is about. On an episode page the list opens on its own season
            // and the reader lands somewhere in the middle of it; without a mark, nothing on screen
            // says which of the fourteen rows is the one they came from.
            .then(if (current) Modifier.background(PrimarySoft.copy(alpha = .10f)) else Modifier)
            .focusOutline(interaction, shape)
            .clickable(
                interactionSource = interaction,
                indication = app.reelstack.ui.components.mediaCardIndication(),
                role = Role.Button,
                enabled = itemId.isNotBlank() && episode.source == ServiceKind.JELLYFIN,
            ) { app.reelstack.player.JellyfinPlayerActivity.open(context, itemId) }
            .padding(6.dp)
            .testTag("episode-$itemId"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // The still shrinks when the reader has asked for big text, so the words it sits beside
        // keep the room they need instead of wrapping into a column two characters wide.
        val stillWidth = if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f) 104.dp else 148.dp
        Box(Modifier.width(stillWidth).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) {
            MediaArtwork(episode.artworkUrl, null, Modifier.fillMaxSize(), fallbackRes = episode.artworkRes, ContentScale.Crop, episode.source)
            if (progress > 0) Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp)
                    .background(Color.Black.copy(alpha = .55f)),
            ) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Primary))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (episode.played) Icon(SpoleIcons.Done, stringResource(R.string.library_played_unmark),
                    Modifier.size(15.dp).padding(end = 5.dp), tint = Primary)
                else if (current) Icon(SpoleIcons.Play, null,
                    Modifier.size(15.dp).padding(end = 5.dp), tint = PrimarySoft)
                // "4 · Getaway Sticks" when there is a name, "Episode 4" when there is not — never
                // a number with a lonely separator hanging off it.
                val numbered = episode.episode?.let { stringResource(R.string.episode_number, it) }
                // A name that is only the number restated is not a name, however the server spelled
                // it. Without this last check a row could still print "1 · Episode 1".
                val name = episodeName(episode).takeUnless { it.equals(numbered, ignoreCase = true) }.orEmpty()
                Text(
                    when {
                        episode.episode == null -> name
                        name.isBlank() -> numbered.orEmpty()
                        else -> "${episode.episode} · $name"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                episode.runtimeMinutes?.takeIf { it > 0 }?.let {
                    Text(stringResource(R.string.detail_minutes, it), color = Muted,
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 10.dp))
                }
            }
            episode.overview?.takeIf(String::isNotBlank)?.let {
                Text(it, color = Muted, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The episode's own name without the "Episode 9 - " the number already said. */
@Composable
private fun episodeName(episode: LibraryMedia): String =
    app.reelstack.ui.components.episodeTitle(episode.subtitle, episode.episode)
        .takeIf(String::isNotBlank)
        ?: episode.subtitle.substringAfter(" · ", episode.subtitle)

/**
 * Where "Play" on a series page should land.
 *
 * The half-watched episode first, then the first unwatched one, then the first one at all. That is
 * the order a person resumes a series in, and it is the order Jellyfin's own clients use — the
 * point is that pressing Play never starts something already finished.
 */
internal fun resumeTarget(browse: SeriesBrowse): LibraryMedia? =
    browse.nextUp
        ?: browse.episodes.firstOrNull { (it.progress ?: 0f) > 0f && !it.played }
        ?: browse.episodes.firstOrNull { !it.played }
        ?: browse.episodes.firstOrNull()

/** Says why Play is missing while the episodes are still on their way. */
@Composable
internal fun SeriesPlayNote(browse: SeriesBrowse, detailKey: String) {
    // Once the seasons are on screen the episode area says all of this in its own place; the note
    // is only for the moment before that, and for a series with nothing in it at all.
    if (browse.openedFor != detailKey || browse.seasons.isNotEmpty() || resumeTarget(browse) != null) return
    val message = when {
        browse.error != null -> browse.error
        browse.loading -> stringResource(R.string.detail_loading_episodes)
        else -> stringResource(R.string.detail_no_episodes)
    }
    Text(message, color = Muted, style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 14.dp).testTag("series-play-note"))
}
