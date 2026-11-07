@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package app.reelstack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.reelstack.ui.theme.Success
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.SeriesBrowse
import app.reelstack.data.model.ServiceKind
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import app.reelstack.ui.components.NoBringIntoView
import app.reelstack.ui.components.rememberKeylineEndPadding
import app.reelstack.ui.components.rowEdgeFade
import app.reelstack.ui.components.rowKeylineScroll
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.screens.Chip
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.ReelLayout

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
    centered: Boolean = false,
) {
    // The kind word leads a fact list and the heading above already says it. It used to be
    // removed by matching a set of Norwegian and English words, which left it in place in any
    // other language; comparing against the word this reader actually sees works everywhere.
    val kindWord = stringResource(app.reelstack.ui.components.mediaKindRes(details.mediaType ?: opening.mediaType))
    val remaining = facts
        .filterNot { it.matches(Regex("^S\\d\\d+ E\\d\\d+$")) }
        .filterNot { it == kindWord }
    val tagline = details.tagline?.takeIf(String::isNotBlank) ?: opening.tagline?.takeIf(String::isNotBlank)
    // Availability is useful in discovery, but redundant inside Jellyfin/Emby library details.
    val inLibrary = details.libraryAvailable && details.source !in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)
    if (!inLibrary && remaining.isEmpty() && details.criticRating == null && details.tmdbRating == null && details.mdblistRating == null && details.quality.isEmpty() && details.genres.isEmpty() && tagline == null && synopsis == null && cast == null) return
    Column(
        Modifier.fillMaxWidth().padding(top = if (tv) 12.dp else 18.dp).testTag("detail-aside"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        // Whether you already have it is the first thing worth knowing about a title you
        // reached from Seerr, and it used to be a muted line at the very bottom of the page —
        // suppressed, in fact, exactly when it was true. It is a mark beside the facts now.
        if (inLibrary) InLibraryBadge()
        app.reelstack.ui.components.PlaybackMetadata(details, remaining, details.source, centered)
        if (details.genres.isNotEmpty()) Text(
            details.genres.take(4).map { genre ->
                standardGenreResource(genre)?.let { stringResource(it) } ?: genre
            }.joinToString(" · "),
            color = Muted,
            style = MaterialTheme.typography.labelLarge,
            textAlign = if (centered) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
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
 * "In your library", as a mark rather than as a sentence at the bottom of the page.
 *
 * A title reached from Seerr looked identical whether you owned it or not: the one line that said
 * otherwise sat below the cast, and was hidden when `libraryAvailable` was true — the very
 * condition it existed to announce.
 */
@Composable
private fun InLibraryBadge() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("detail-in-library")) {
        Icon(
            app.reelstack.ui.components.SpoleIcons.DoneCircle,
            contentDescription = null,
            tint = Success,
            modifier = Modifier.size(18.dp),
        )
        Text(
            stringResource(R.string.details_in_library_badge),
            color = Success,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 7.dp),
        )
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
internal fun SeriesEpisodes(
    browse: SeriesBrowse,
    detailKey: String,
    onSeason: (String) -> Unit,
    onEpisodeClick: (LibraryMedia) -> Unit = {},
) {
    val firstEpisode = remember { androidx.compose.ui.focus.FocusRequester() }
    val tv = app.reelstack.ui.components.isTelevision()
    // Only this season's own episodes decide anything. While the next season loads, the old ones
    // stay in the state so the list never collapses to nothing, but they are neither shown nor
    // consulted; a placeholder with their height stands in for them.
    val episodes = browse.currentEpisodes
    val waiting = browse.loading || browse.episodesStale
    // The series Play button already resolves this target. Giving the same episode initial focus
    // in the season row makes a remote's first Select do the unsurprising thing as well.
    val preferredEpisode = nextEpisodeTarget(browse)?.remoteId?.let { id ->
        episodes.firstOrNull { it.remoteId == id }
    }
    val focusEpisode = preferredEpisode ?: episodes.firstOrNull()
    // The row starts at the next episode, or at the first one in a season that has none. A fresh
    // state per season (and per wait) means the first frame is already right and there is no layout
    // left over from the season before to be mistaken for this one.
    val startIndex = focusEpisode?.let { target -> episodes.indexOfFirst { it.id == target.id } }?.coerceAtLeast(0) ?: 0
    val rowState = remember(detailKey, browse.selectedSeasonId, waiting) { LazyListState(startIndex) }
    // How tall the episode area was the last time it held a list; the placeholder takes that height.
    var keptHeight by remember(detailKey) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    if (browse.openedFor != detailKey) return
    if (browse.seasons.isEmpty() && !browse.loading && browse.error == null) return
    Column(Modifier.fillMaxWidth().padding(top = if (tv) 10.dp else 22.dp).testTag("detail-seasons"),
        verticalArrangement = Arrangement.spacedBy(if (tv) 8.dp else 12.dp)) {
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
            // A lazy row can only take focus on a tile it has laid out, so Down goes to the next
            // episode only while that tile is there; otherwise the search falls back to geometry.
            // On a television a tab is moved to the start of the strip when it does not fit whole, and the
            // strip fades where it runs on, so no tab is left cut through its name at either edge
            // (Shield, 2026-11-07: «esong 6»).
            CompositionLocalProvider(LocalBringIntoViewSpec provides
                if (tv) NoBringIntoView else LocalBringIntoViewSpec.current) {
            LazyRow(state = strip, modifier = Modifier.testTag("season-strip")
                .then(if (tv) Modifier.rowKeylineScroll(strip, 0.dp, TV_ROW_FADE).rowEdgeFade(strip, endFade = TV_ROW_FADE) else Modifier)
                .focusProperties { if (tv && rowState.shows(focusEpisode?.id)) down = firstEpisode }, horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = if (tv) rememberKeylineEndPadding(strip, 0.dp, 6.dp, 24.dp) else 24.dp)) {
                items(browse.seasons, key = { it.id }) { season ->
                    val id = season.remoteId.orEmpty()
                    SeasonTab(
                        text = seasonLabel(season),
                        chosen = id == browse.selectedSeasonId,
                        tag = "season-$id",
                    ) { onSeason(id) }
                }
            }
            }
        }
        browse.upcomingError?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
        // A season that is still loading takes the place, and the height, of the list it will become.
        // Before, the list was emptied and a one-line spinner stood in for it, so everything under
        // the episodes (the cast row, on a television) jumped up and then down again.
        if (waiting || browse.error == null && episodes.isNotEmpty()) {
            Box(Modifier.testTag("episode-row")
                .then(if (waiting && keptHeight > 0) Modifier.height(with(density) { keptHeight.toDp() }).clipToBounds() else Modifier)
                .onSizeChanged { if (!waiting) keptHeight = it.height }) {
                when {
                    waiting -> app.reelstack.ui.components.EpisodeRowSkeleton(stringResource(R.string.detail_loading_episodes), tv)
                    tv -> TvEpisodeRow(episodes, rowState, startIndex, preferredEpisode?.id, focusEpisode?.id, detailKey,
                        firstEpisode, onEpisodeClick)
                    else -> {
                        // The list lives inside a scrolling column, so every row it holds is composed
                        // whether or not anyone can see it. Twenty-six is a long season; two hundred is a
                        // long-running anime, and composing two hundred rows to show six is what turns a
                        // page open into a visible pause.
                        var showAll by remember(browse.selectedSeasonId) { mutableStateOf(false) }
                        val visible = if (showAll) episodes else episodes.take(EPISODE_PREVIEW)
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            visible.forEachIndexed { index, episode ->
                                val ready = episode.id == preferredEpisode?.id
                                Box(if (episode.id == focusEpisode?.id || preferredEpisode !in visible && index == 0) Modifier.focusRequester(firstEpisode) else Modifier) {
                                    EpisodeRow(episode, episode.id == detailKey, ready, onEpisodeClick)
                                }
                            }
                            if (visible.size < episodes.size) Chip(
                                text = pluralStringResource(
                                    R.plurals.detail_more_episodes,
                                    episodes.size - visible.size,
                                    episodes.size - visible.size,
                                ),
                                chosen = false,
                                tag = "episodes-more",
                            ) { showAll = true }
                        }
                    }
                }
            }
        } else if (browse.error != null) {
            Text(browse.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(stringResource(R.string.detail_no_episodes), color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private const val EPISODE_PREVIEW = 12

/** True when the lazy row has laid out the tile with this key, which is when it can be given focus. */
private fun LazyListState.shows(key: Any?): Boolean = key != null && layoutInfo.visibleItemsInfo.any { it.key == key }

/**
 * The television's episode row, opened at [startIndex] so the next episode is its first tile and a
 * remote's Down from the buttons above lands on it.
 *
 * [state] is created already positioned there (see [SeriesEpisodes]); the effect below only covers
 * the next episode changing while the row is on screen. Even then it never moves the row while
 * focus is inside it: a reload of the same season replaces the list, and a jump would pull the
 * row away from under the tile the reader is standing on. There is room after the last episode for
 * it to sit first as well, which is what the end padding is for.
 *
 * `FocusProperties.onEnter` is still marked experimental. It is the only way to say "whichever tile
 * the search would have picked, go to this one" for a group, and it only changes where focus lands.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun TvEpisodeRow(
    episodes: List<LibraryMedia>,
    state: LazyListState,
    startIndex: Int,
    readyId: String?,
    targetId: String?,
    detailKey: String,
    firstEpisode: androidx.compose.ui.focus.FocusRequester,
    onEpisodeClick: (LibraryMedia) -> Unit,
) {
    var focusInside by remember { mutableStateOf(false) }
    LaunchedEffect(targetId) {
        if (!focusInside) state.scrollToItem(startIndex)
    }
    // As wide as the screen is, minus one tile: whatever the row's own margins, the last episode can
    // then be scrolled to the start. A little too much is harmless; too little would cap the scroll.
    val endPadding = (LocalConfiguration.current.screenWidthDp.dp - ReelLayout.EpisodeWidth).coerceAtLeast(4.dp)
    CompositionLocalProvider(LocalBringIntoViewSpec provides NoBringIntoView) {
    LazyRow(
        state = state,
        modifier = Modifier
            .rowKeylineScroll(state, 4.dp, TV_ROW_FADE)
            .rowEdgeFade(state, endFade = TV_ROW_FADE)
            // Entering the row from above goes to the next episode, whichever tile happens to be
            // straight below the button that had focus. Coming up from the cast row is left to geometry.
            .focusProperties {
                onEnter = {
                    if (requestedFocusDirection == FocusDirection.Down && state.shows(targetId)) firstEpisode.requestFocus()
                }
            }
            .onFocusChanged { focusInside = it.hasFocus }
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = endPadding, bottom = 4.dp),
    ) {
        items(episodes, key = { it.id }) { episode ->
            Box(Modifier.width(ReelLayout.EpisodeWidth).then(if (episode.id == targetId)
                Modifier.focusRequester(firstEpisode) else Modifier)) {
                TvEpisodeCard(episode, episode.id == detailKey, episode.id == readyId, onEpisodeClick = onEpisodeClick)
            }
        }
    }
    }
}

/** How far a series page's rows fade out where they run on; focus never stops inside it. */
private val TV_ROW_FADE = 40.dp

/** Text tabs let the season title lead; only the selected season has an underline. */
@Composable
private fun SeasonTab(text: String, chosen: Boolean, tag: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(Modifier.heightIn(min = 48.dp).focusOutline(interaction, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
        .selectable(chosen, role = Role.RadioButton, interactionSource = interaction,
            indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
        .testTag(tag).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, style = MaterialTheme.typography.titleSmall,
            color = if (chosen || focused) MaterialTheme.colorScheme.onSurface else Muted)
        Box(Modifier.width(28.dp).height(2.dp).background(
            if (chosen) Primary else Color.Transparent, RoundedCornerShape(1.dp)))
    }
}

@Composable
private fun TvEpisodeCard(episode: LibraryMedia, current: Boolean, ready: Boolean = false,
    showOverview: Boolean = false,
    onEpisodeClick: (LibraryMedia) -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    val tv = app.reelstack.ui.components.isTelevision()
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().clip(shape)
        .then(if (!episode.available) Modifier.focusable(interactionSource = interaction) else Modifier)
        .clickable(interactionSource = interaction, indication = app.reelstack.ui.components.mediaCardIndication(),
            enabled = episode.available && !episode.remoteId.isNullOrBlank() && episode.source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY),
            role = Role.Button) { onEpisodeClick(episode) }
        .padding(6.dp).testTag("episode-${episode.remoteId ?: episode.id}"), verticalArrangement = Arrangement.spacedBy(if (tv) 6.dp else 10.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).focusOutline(interaction, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
            MediaArtwork(episode.artworkUrl, null, Modifier.fillMaxSize(), episode.artworkRes, source = episode.source)
            if (!episode.available) EpisodeStatusBadge(episode, Modifier.align(Alignment.TopStart).padding(8.dp))
            if (ready) Icon(SpoleIcons.PlaySimple, stringResource(R.string.detail_next_to_play),
                Modifier.align(Alignment.Center).size(28.dp).background(Color.Black.copy(alpha = .68f), RoundedCornerShape(20.dp)).padding(6.dp),
                tint = Color.White)
            if (episode.played) Icon(SpoleIcons.Done, stringResource(R.string.library_played_unmark),
                Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = .7f), RoundedCornerShape(8.dp)).padding(4.dp), tint = Color.White)
            if (tv) episode.runtimeMinutes?.let { minutes ->
                Text(stringResource(R.string.detail_minutes, minutes), color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        .background(Color.Black.copy(alpha = .72f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp))
            }
        }
        episode.progress?.takeIf { it > 0f }?.let { progress ->
            Box(Modifier.fillMaxWidth().height(2.dp).background(Muted.copy(alpha = .16f))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(Primary))
            }
        }
        val numberLabel = episode.episode?.let { stringResource(R.string.episode_number, it) }
        Text(episodeCardName(episode.subtitle, episode.episode, numberLabel),
            style = MaterialTheme.typography.titleMedium, maxLines = if (showOverview) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
            color = if (current || ready) PrimarySoft else MaterialTheme.colorScheme.onSurface)
        // On TV the play glyph already identifies the next episode. Keep the caption for its name.
        if (ready && !tv) Text(stringResource(R.string.detail_next_to_play), color = Primary,
            style = MaterialTheme.typography.labelMedium, maxLines = 1)
        if (!episode.available) EpisodeAvailability(episode)
        if (!tv) episode.runtimeMinutes?.let { Text(stringResource(R.string.detail_minutes, it), color = Muted, style = MaterialTheme.typography.labelMedium) }
        if (showOverview) episode.overview?.takeIf(String::isNotBlank)?.let {
            Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Season buttons stay compact; the episode count belongs in the list below. */
@Composable
private fun seasonLabel(season: LibraryMedia): String {
    // A season's own number lands in `episode`; `season` on a season item is the series' index.
    val name = season.episode?.takeIf { it > 0 }?.let { stringResource(R.string.episode_season, it) }
        ?: season.title.takeIf(String::isNotBlank)
        ?: season.subtitle
    return name
}

/**
 * One episode, playable from here.
 *
 * The still is the widest thing in the row because it is the only part that identifies the episode
 * at a glance; everything else is one line so a season of twenty-four does not become a wall.
 */
@Composable
private fun EpisodeRow(episode: LibraryMedia, current: Boolean = false, ready: Boolean = false,
    onEpisodeClick: (LibraryMedia) -> Unit = {}) {
    TvEpisodeCard(episode, current, ready, showOverview = true, onEpisodeClick = onEpisodeClick)
}

/**
 * What an episode card is called: «5 · Pilot», or «Episode 5» when the episode has no name of its own.
 *
 * A server line carries the number first ("S01 E05 · Pilot"); an episode announced through Seerr has
 * only its bare name. Either way the name is cleaned, so «TBA», a release file name or a repeated
 * «Episode 5» never reaches the card. The raw text used to be the fallback, and it printed «5 · TBA».
 */
internal fun episodeCardName(subtitle: String, episode: Int?, numberLabel: String?): String {
    val name = if (subtitle.contains(" · ")) app.reelstack.ui.components.episodeTitle(subtitle, episode)
        else app.reelstack.data.model.episodeNameOf(subtitle, episode)
    return when {
        name.isNotBlank() && !name.equals(numberLabel, true) -> listOfNotNull(episode?.toString(), name).joinToString(" · ")
        numberLabel != null -> numberLabel
        else -> name
    }
}

/**
 * Where "Play" on a series page should land.
 *
 * The half-watched episode comes first, then the first unwatched one. A fully watched series can
 * still be intentionally restarted from episode one, but it never gets a misleading “next” mark.
 */
internal fun nextEpisodeTarget(browse: SeriesBrowse): LibraryMedia? =
    browse.nextUp?.takeIf { it.available }
        ?: browse.currentEpisodes.firstOrNull { it.available && (it.progress ?: 0f) > 0f && !it.played }
        ?: browse.currentEpisodes.firstOrNull { it.available && !it.played }

/** The first available episode is a sensible first start, but never a false “next” marker. */
internal fun resumeTarget(browse: SeriesBrowse): LibraryMedia? =
    nextEpisodeTarget(browse)
        ?: browse.currentEpisodes.firstOrNull { it.available }

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

@Composable
private fun EpisodeAvailability(episode: LibraryMedia) {
    val date = episode.premiereDate?.let { runCatching { java.time.LocalDate.parse(it.take(10)) }.getOrNull() }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val label = if (date != null && !date.isBefore(java.time.LocalDate.now())) {
        val formatted = date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale))
        stringResource(R.string.design_coming_on, formatted)
    } else if (date != null) stringResource(R.string.refine_missing_date, date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale)))
    else stringResource(R.string.design_not_available)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(SpoleIcons.Calendar, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
@Composable
private fun EpisodeStatusBadge(episode: LibraryMedia, modifier: Modifier = Modifier) {
    val date = episode.premiereDate?.let { runCatching { java.time.LocalDate.parse(it.take(10)) }.getOrNull() }
    val upcoming = date != null && !date.isBefore(java.time.LocalDate.now())
    Text(stringResource(if (upcoming) R.string.refine_coming_badge else R.string.refine_missing_badge),
        modifier.background(Color.Black.copy(alpha = .86f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium, color = if (upcoming) Color.White else Color(0xFFFFD478))
}
