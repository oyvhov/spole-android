package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.DiscoverGenre
import app.reelstack.data.model.DiscoverGridTarget
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.DiscoverPerson
import app.reelstack.data.model.DiscoverRowKind
import app.reelstack.data.model.DiscoverRowState
import app.reelstack.data.model.RequestStage
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.model.myRequestsPreview
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.focusScale
import app.reelstack.ui.components.isTelevision
import app.reelstack.ui.components.mediaCardIndication
import app.reelstack.ui.components.steadyRemoteRows
import app.reelstack.ui.state.DiscoverUiState
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor

data class DiscoverHubActions(
    val onTitle: (String) -> Unit,
    val onPerson: (Int) -> Unit,
    val onMyRequests: () -> Unit,
    val onRequestLine: (String) -> Unit,
    val onGrid: (DiscoverGridTarget) -> Unit,
    val onRetry: () -> Unit,
    val onSearch: () -> Unit,
    /** A television asks for the length and genres of whatever title has focus. */
    val onFocusTitle: (DiscoverMedia) -> Unit = {},
)

@Composable
internal fun DiscoverRowKind.title(): String = stringResource(when (this) {
    DiscoverRowKind.TRENDING -> R.string.discover_row_trending
    DiscoverRowKind.UPCOMING_MOVIES -> R.string.discover_row_upcoming_movies
    DiscoverRowKind.UPCOMING_SERIES -> R.string.discover_row_upcoming_series
})

/**
 * The rows of Oppdag.
 *
 * On a television the title in focus owns the top of the page, and that top stays put while the rows
 * scroll under it: were it the first item of the list, it would scroll away the moment the remote
 * reached the third row, and the header exists to describe the poster the remote is on.
 */
@Composable
fun DiscoverHubContent(state: DiscoverUiState, contentPadding: PaddingValues, actions: DiscoverHubActions,
    header: @Composable () -> Unit = {}) {
    val tv = isTelevision()
    var focused by remember { mutableStateOf<DiscoverMedia?>(null) }
    val first = state.hub.rows[DiscoverRowKind.TRENDING]?.items?.firstOrNull()
    val shown = focused ?: first
    val mine = myRequestsPreview(state.trackedRequests, if (tv) 12 else 3)
    val posterWidth = (if (tv) 120.dp else 112.dp) * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
    val seerr = state.connections.any { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() }
    val visibleRows = DiscoverRowKind.entries.filter { kind ->
        val row = state.hub.rows[kind]
        row != null && !row.failed && (row.loading || row.items.isNotEmpty())
    }
    // The remote starts on the first poster, not on the search icon above it: search is one press
    // up, and a fresh page should start where the titles are.
    val firstPoster = remember { FocusRequester() }
    val firstPosterId = visibleRows.firstNotNullOfOrNull { state.hub.rows[it]?.items?.firstOrNull() }?.id
    var claimed by remember { mutableStateOf(false) }
    LaunchedEffect(tv, firstPosterId) {
        if (!tv || claimed || firstPosterId == null) return@LaunchedEffect
        repeat(40) {
            withFrameNanos { }
            if (runCatching { firstPoster.requestFocus() }.isSuccess) { claimed = true; return@LaunchedEffect }
        }
    }
    Box(Modifier.fillMaxSize().testTag("discover-hub")) {
        if (tv && shown != null) FocusBackdrop(shown)
        Column(Modifier.fillMaxSize()) {
            if (tv) Row(Modifier.fillMaxWidth().padding(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) {
                    if (shown != null) FocusHeader(shown, state.hub.focusFacts[shown.id].orEmpty())
                }
                header()
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (tv) 22.dp else 24.dp),
                contentPadding = PaddingValues(top = if (tv) 8.dp else ReelLayout.PageTop,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp)) {
                if (!tv) item(key = "top") { Box(Modifier.padding(horizontal = ReelLayout.Gutter)) { header() } }
                if (!seerr) item(key = "not-connected") {
                    app.reelstack.ui.components.QuietEmptyState(stringResource(R.string.search_not_connected),
                        icon = SpoleIcons.Search, modifier = Modifier.padding(horizontal = ReelLayout.Gutter))
                }
                if (mine.isNotEmpty()) item(key = "mine") {
                    Section(stringResource(R.string.my_requests_title), Modifier.testTag("discover-my-requests"),
                        trailing = { RowEndLink(stringResource(R.string.discover_all_count, state.trackedRequests.size),
                            "discover-my-requests-all", actions.onMyRequests) }) {
                        if (tv) LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            items(mine, key = { "mine-${it.key}" }) { request ->
                                RequestPoster(request, posterWidth) { actions.onRequestLine(request.key) }
                            }
                        } else Column(Modifier.padding(horizontal = ReelLayout.Gutter)) {
                            mine.forEachIndexed { index, request ->
                                if (index > 0) androidx.compose.material3.HorizontalDivider(color = LineDivider)
                                RequestLine(request, { actions.onRequestLine(request.key) })
                            }
                        }
                    }
                }
                visibleRows.forEach { kind ->
                    val row = state.hub.rows.getValue(kind)
                    item(key = "row-$kind") {
                        Section(kind.title(), Modifier.testTag("discover-row-$kind")) {
                            LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter),
                                horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 10.dp)) {
                                if (row.loading && row.items.isEmpty()) items(6) {
                                    Box(Modifier.width(posterWidth).aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(ReelLayout.ArtworkCorner)).background(SurfaceRaised))
                                }
                                items(row.items, key = { "$kind-${it.id}" }) { media ->
                                    PosterOnlyCard(media, { actions.onTitle(media.id) },
                                        Modifier.width(posterWidth).then(if (media.id == firstPosterId && kind == visibleRows.first())
                                            Modifier.focusRequester(firstPoster) else Modifier),
                                        onFocused = { focused = media; actions.onFocusTitle(media) })
                                }
                                if (row.items.isNotEmpty()) item(key = "$kind-all") {
                                    RowEndTile(posterWidth) { actions.onGrid(DiscoverGridTarget.Row(kind)) }
                                }
                            }
                        }
                    }
                }
                if (state.hub.people.isNotEmpty()) item(key = "people") {
                    Section(stringResource(R.string.discover_people), Modifier.testTag("discover-people")) {
                        LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(state.hub.people, key = { "person-${it.id}" }) { person -> PersonPortrait(person) { actions.onPerson(person.id) } }
                        }
                    }
                }
                if (state.hub.genres.isNotEmpty()) item(key = "genres") {
                    Section(stringResource(R.string.discover_genres), Modifier.testTag("discover-genres")) {
                        LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.hub.genres, key = { "genre-${it.mediaType}-${it.id}" }) { genre ->
                                GenreCard(genre) { actions.onGrid(DiscoverGridTarget.Genre(genre)) }
                            }
                        }
                    }
                }
                if (state.hub.anyFailed) item(key = "failed") {
                    Row(Modifier.padding(horizontal = ReelLayout.Gutter), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.discover_rows_failed), color = Muted, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f, fill = false))
                        RowEndLink(stringResource(R.string.action_retry), "discover-rows-retry", actions.onRetry)
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = ReelLayout.Gutter), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextColor, modifier = Modifier.weight(1f))
            trailing()
        }
        content()
    }
}

/** A poster and nothing else: title, year and status live in the header or on the title page. */
@Composable
internal fun PosterOnlyCard(media: DiscoverMedia, onClick: () -> Unit, modifier: Modifier = Modifier, onFocused: () -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    val focusedNow by interaction.collectIsFocusedAsState()
    LaunchedEffect(focusedNow) { if (focusedNow) onFocused() }
    val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    val scale = focusScale(focusedNow, false)
    Box(modifier.aspectRatio(2f / 3f).graphicsLayer { scaleX = scale; scaleY = scale }.focusOutline(interaction, shape).clip(shape)
        .background(SurfaceRaised)
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = media.title }
        .testTag("discover-poster-${media.id}")) {
        MediaArtwork(media.artworkUrl, null, Modifier.matchParentSize(), fallbackRes = media.artworkRes, contentScale = ContentScale.Crop,
            source = ServiceKind.SEERR)
    }
}

/** On TV your requests are posters too; work in flight carries a thin line along the bottom. */
@Composable
private fun RequestPoster(request: TrackedRequest, width: Dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    Box(Modifier.width(width).aspectRatio(2f / 3f).focusOutline(interaction, shape).clip(shape).background(SurfaceRaised)
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = request.title }.testTag("my-request-${request.key}")) {
        MediaArtwork(request.artworkUrl, null, Modifier.matchParentSize(), fallbackRes = R.drawable.media_placeholder,
            contentScale = ContentScale.Crop, source = ServiceKind.SEERR)
        val percent = request.percent
        if (request.stage == RequestStage.DOWNLOADING && percent != null) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = .5f))) {
                Box(Modifier.fillMaxWidth(percent.coerceIn(0, 100) / 100f).height(3.dp).background(TextColor))
            }
        }
    }
}

@Composable
private fun RowEndTile(width: Dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(Modifier.width(width).aspectRatio(2f / 3f).focusOutline(interaction, RoundedCornerShape(ReelLayout.ArtworkCorner))
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.discover_all), color = Muted)
            Icon(SpoleIcons.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RowEndLink(text: String, tag: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(Modifier.heightIn(min = 44.dp).focusOutline(interaction, RoundedCornerShape(10.dp))
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .padding(horizontal = 6.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Muted, style = MaterialTheme.typography.labelLarge)
        Icon(SpoleIcons.ChevronRight, null, tint = Muted, modifier = Modifier.size(16.dp))
    }
}

/** A face and a name: the only text under a picture on this page. */
@Composable
internal fun PersonPortrait(person: DiscoverPerson, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val size = if (isTelevision()) 96.dp else 72.dp
    Column(Modifier.width(size + 8.dp).focusOutline(interaction, RoundedCornerShape(12.dp))
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .testTag("person-${person.id}"), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size).clip(CircleShape).background(SurfaceRaised), contentAlignment = Alignment.Center) {
            if (person.portraitUrl != null) MediaArtwork(person.portraitUrl, null, Modifier.matchParentSize(),
                fallbackRes = R.drawable.media_placeholder, contentScale = ContentScale.Crop, source = ServiceKind.SEERR)
            else Text(person.name.split(' ').filter(String::isNotBlank).take(2).map { it.first() }.joinToString(""), color = Muted)
        }
        Text(person.name, style = MaterialTheme.typography.labelMedium, color = TextColor, textAlign = TextAlign.Center, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
    }
}

/** TMDB names what a person is known for in English; the common ones are said in the reader's language. */
@Composable
internal fun departmentLabel(raw: String?): String? = when (raw) {
    null -> null
    "Acting" -> stringResource(R.string.person_dept_acting)
    "Directing" -> stringResource(R.string.person_dept_directing)
    "Writing" -> stringResource(R.string.person_dept_writing)
    "Production" -> stringResource(R.string.person_dept_production)
    "Sound" -> stringResource(R.string.person_dept_sound)
    "Camera" -> stringResource(R.string.person_dept_camera)
    "Editing" -> stringResource(R.string.person_dept_editing)
    else -> raw
}

/** A person in search results: picture, name, what they are known for. */
@Composable
internal fun PersonLine(person: DiscoverPerson, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().focusOutline(interaction, RoundedCornerShape(10.dp))
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .testTag("search-person-${person.id}")) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(SurfaceRaised), contentAlignment = Alignment.Center) {
                if (person.portraitUrl != null) MediaArtwork(person.portraitUrl, null, Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop, source = ServiceKind.SEERR)
                else Text(person.name.split(' ').filter(String::isNotBlank).take(2).map { it.first() }.joinToString(""), color = Muted)
            }
            Column(Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.titleSmall, color = TextColor)
                departmentLabel(person.role)?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
            }
            Icon(SpoleIcons.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
        androidx.compose.material3.HorizontalDivider(color = LineDivider)
    }
}

@Composable
private fun GenreCard(genre: DiscoverGenre, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.width(168.dp).aspectRatio(16f / 9f).focusOutline(interaction, shape).clip(shape).background(SurfaceRaised)
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .testTag("genre-${genre.mediaType}-${genre.id}")) {
        genre.backdropUrl?.let { MediaArtwork(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, source = ServiceKind.SEERR) }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(.3f to Color.Transparent, 1f to Color.Black.copy(alpha = .7f))))
        Text(genre.name, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
    }
}

@Composable
private fun FocusBackdrop(media: DiscoverMedia) {
    Box(Modifier.fillMaxWidth().fillMaxHeight(.62f)) {
        MediaArtwork(media.backdropUrl ?: media.artworkUrl, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop,
            source = ServiceKind.SEERR)
        val ground = MaterialTheme.colorScheme.background
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(0f to ground, .45f to ground.copy(alpha = .6f), .8f to Color.Transparent)))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(.4f to Color.Transparent, 1f to ground)))
    }
}

/**
 * Title, facts, status and two lines of synopsis for whatever has focus.
 *
 * Every line keeps its height from one title to the next (one line of title, two of synopsis even
 * when the synopsis is shorter), so the rows below never jump while the remote moves.
 */
@Composable
private fun FocusHeader(media: DiscoverMedia, extraFacts: List<String>) {
    Column(Modifier.fillMaxWidth(.62f).testTag("discover-focus-header"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(media.title, style = MaterialTheme.typography.headlineMedium, color = TextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text((listOf(media.metadata) + extraFacts + media.facts).filter(String::isNotBlank).distinct().joinToString("  ·  "),
            color = Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(app.reelstack.localization.localizedSeerrStatus(media.seerrStatus, media.inLibrary, media.requested),
            color = TextColor, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(media.overview.orEmpty(), color = Muted, style = MaterialTheme.typography.bodyMedium, minLines = 2, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
    }
}

/** Everything in one row or one genre, a page at a time, as posters. */
@Composable
fun DiscoverGridContent(title: String, row: DiscoverRowState, contentPadding: PaddingValues, onTitle: (String) -> Unit,
    onMore: () -> Unit, onBack: () -> Unit) {
    val grid = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(posterCell()), state = grid,
        contentPadding = PaddingValues(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().steadyRemoteRows(grid).testTag("discover-grid-all")) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("discover-grid-back")) {
                    Icon(SpoleIcons.ArrowBack, stringResource(R.string.action_back))
                }
                Text(title, style = MaterialTheme.typography.headlineSmall, color = TextColor)
            }
        }
        items(row.items.size, key = { row.items[it].id }) { index ->
            val media = row.items[index]
            if (index >= row.items.size - 6) LaunchedEffect(row.items.size) { onMore() }
            PosterOnlyCard(media, { onTitle(media.id) })
        }
        if (row.loading) items(6, key = { "grid-loading-$it" }) {
            Box(Modifier.aspectRatio(2f / 3f).clip(RoundedCornerShape(ReelLayout.ArtworkCorner)).background(SurfaceRaised))
        }
        if (row.failed) item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.discover_rows_failed), color = Muted, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f, fill = false))
                RowEndLink(stringResource(R.string.action_retry), "discover-grid-retry", onMore)
            }
        }
    }
}
