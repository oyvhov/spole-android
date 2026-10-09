package app.reelstack.ui.screens
import app.reelstack.ui.components.SpoleDropdownMenu as DropdownMenu
import app.reelstack.ui.components.SpoleDropdownMenuItem as DropdownMenuItem

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton
import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.LibraryFilters
import app.reelstack.data.model.libraryArtType
import app.reelstack.data.model.libraryArtworkUrl
import app.reelstack.data.model.librarySizedArtwork
import app.reelstack.data.model.libraryNextItems
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.steadyRemoteRows

@Composable
fun LibraryScreen(state: ReelstackUiState, onLoad: (Boolean) -> Unit, onOpen: (String) -> Unit, onBack: () -> Unit,
    onFilter: (app.reelstack.data.model.LibraryFilters) -> Unit = {},
    onShelfOpen: (String) -> Unit = {}, cardActions: MediaCardActions? = null,
    onSource: (ServiceKind) -> Unit = {}, onCustomize: () -> Unit = {}) {
    val sources = state.connections.filter { it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
        it.baseUrl.isNotBlank() && it.token.isNotBlank() }.map { it.kind }.distinct()
    key(state.librarySource) {
        LibraryContent(state, onLoad, onOpen, onBack, onFilter, onShelfOpen, cardActions, onCustomize,
            sourcePicker = { if (sources.size > 1) LibrarySourceMenu(state.librarySource, sources, onSource) })
    }
}

@Composable
private fun LibrarySourceMenu(selected: ServiceKind, sources: List<ServiceKind>, onSource: (ServiceKind) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.testTag("library-source-menu")) {
            Text(selected.displayName)
            Icon(app.reelstack.ui.components.SpoleIcons.ChevronDown, null, Modifier.padding(start = 6.dp).size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sources.forEach { source ->
                DropdownMenuItem(text = { Text(source.displayName) }, selected = source == selected, onClick = {
                    expanded = false
                    if (source != selected) onSource(source)
                }, leadingIcon = { app.reelstack.ui.components.ServiceSymbol(source, Modifier.size(24.dp)) },
                    trailingIcon = { if (source == selected) Icon(app.reelstack.ui.components.SpoleIcons.Done, null) },
                    modifier = Modifier.testTag("library-source-${source.name}"))
            }
        }
    }
}

@Composable
private fun LibraryContent(state: ReelstackUiState, onLoad: (Boolean) -> Unit, onOpen: (String) -> Unit, onBack: () -> Unit,
    onFilter: (app.reelstack.data.model.LibraryFilters) -> Unit,
    onShelfOpen: (String) -> Unit, cardActions: MediaCardActions?, onCustomize: () -> Unit,
    sourcePicker: @Composable () -> Unit) {
    BackHandler(state.libraryPath.isNotEmpty() && state.activeSheet == null) { onBack() }
    val shelfActions = app.reelstack.ui.components.LocalSmartShelfActions.current
    // A smart shelf has its own page: films and series apart, on smaller covers than this grid.
    val smartShelf = app.reelstack.data.model.SmartShelf.idOfPath(state.libraryPath.lastOrNull()?.first)?.let { id ->
        app.reelstack.data.model.mergedSmartShelves(state.smartShelves).firstOrNull { it.id == id }
    }
    if (smartShelf != null) {
        SmartShelfPage(state, smartShelf, onOpen, onBack, onEdit = { shelfActions?.edit(smartShelf.id) }, onRetry = { onLoad(false) })
        return
    }
    val connected = state.libraryConnection != null
    val tv = LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val pageGutter = if (tv) 32.dp else app.reelstack.ui.theme.ReelLayout.Gutter
    val folders = state.libraryPath.isEmpty()
    val libraryId = state.libraryPath.lastOrNull()?.first.orEmpty()
    val (display, saveDisplay) = rememberLibraryDisplay(if (state.librarySource == ServiceKind.JELLYFIN) libraryId else "emby:$libraryId")
    val artType = libraryArtType(display, state.libraryCollectionType, state.libraryEntries)
    val ratio = artType.ratio
    val wideCards = ratio > 1f
    val listView = !folders && display.view == app.reelstack.data.model.LibraryView.LIST
    val size = app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale * display.size.scale
    // Resume and Next up for this library, fetched by the library page rather than sliced out of
    // Home's twelve mixed cards.
    val shelves = state.libraryShelves.takeIf { it.libraryId == libraryId }
    val shelfResume = shelves?.resume.orEmpty()
    val shelfNextUp = shelves?.nextUp.orEmpty()
    val nextItems = remember(shelfResume, shelfNextUp) { libraryNextItems(shelfResume, shelfNextUp) }
    val showShelves = !folders && state.libraryPath.size == 1 &&
        state.libraryFilters.activeCount == 0 &&
        (shelfResume.isNotEmpty() || shelfNextUp.isNotEmpty())
    // The root of Bibliotek is a page about libraries, not a grid of four folders. Everything
    // below the root is still the grid it always was.
    if (folders) {
        LibraryHub(state, onOpen, onShelfOpen, cardActions, onRetry = { onLoad(false) }, onCustomize = onCustomize,
            sourcePicker = sourcePicker)
        return
    }
    val pageStates = rememberSaveableStateHolder()
    pageStates.SaveableStateProvider(state.libraryPath.joinToString("/") { it.first }) {
        val grid = rememberLazyGridState()
        val firstContent = remember { FocusRequester() }
        val cell = (if (wideCards) { if (tv) 240.dp else 156.dp } else if (tv) 155.dp else 130.dp) * size
        val listArtHeight = (if (tv) 118.dp else 96.dp) * size
        val density = androidx.compose.ui.platform.LocalDensity.current
        app.reelstack.ui.components.PrefetchLibraryArtwork(state.libraryEntries, grid, artType, state.librarySource,
            listArtworkWidth = if (listView) with(density) { (listArtHeight * ratio).roundToPx() } else null)
        LazyVerticalGrid(state = grid,
            columns = if (listView) GridCells.Fixed(1) else GridCells.Adaptive(cell),
            contentPadding = PaddingValues(pageGutter),
            horizontalArrangement = Arrangement.spacedBy(if (tv) 20.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .steadyRemoteRows(grid).testTag("library-browser")) {
            item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.focusProperties { if (tv && (showShelves || state.libraryEntries.isNotEmpty())) down = firstContent },
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // One library deep, the sidebar already says Bibliotek. The trail is worth a
                    // line only once there is something in it the sidebar cannot say.
                    if (state.libraryPath.size > 1) Text((listOf(stringResource(R.string.nav_library)) +
                        state.libraryPath.dropLast(1).map { it.second }).joinToString("  /  "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    val heading: @Composable () -> Unit = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!tv) IconButton(onClick = onBack, modifier = Modifier.testTag("library-back")) {
                                Icon(app.reelstack.ui.components.SpoleIcons.ArrowBack, stringResource(R.string.library_back))
                            }
                            Text(state.libraryPath.lastOrNull()?.second ?: stringResource(R.string.nav_library),
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.displaySmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }
                    val controls: @Composable () -> Unit = {
                        LibraryFilterBar(state.libraryFilters, onFilter, state.libraryFacets, display, saveDisplay)
                    }
                    // On a television the title and its three controls fit side by side, and the
                    // line they save is a whole row of covers: stacked, the heading block pushed
                    // the first row's titles past the bottom edge of a 1080p screen.
                    if (tv && androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.5f) Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Box(Modifier.weight(.3f)) { heading() }
                        Box(Modifier.weight(.7f)) { controls() }
                    } else {
                        heading()
                        controls()
                    }
                    state.mediaActionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            // The library's own shelves, above its own grid — the thing a wall of covers cannot say.
            // Only on the library's front page and only unfiltered: a filtered view is a query
            // result, and "what you were watching" is not part of the answer to a query.
            if (showShelves) item(key = "shelves", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(bottom = 8.dp).focusRequester(firstContent)) {
                    LibraryShelfTitle(stringResource(R.string.library_next), cardActions != null)
                    ResumeRail(nextItems, onShelfOpen, cardActions, resumeIds = shelfResume.map { it.id }.toSet())
                }
            }
            if (!connected) item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.library_connect)) }
            if (state.libraryLoading && state.libraryEntries.isEmpty()) items(8, key = { "skeleton-$it" }) {
                val image: @Composable () -> Unit = {
                    Box(Modifier.then(if (listView) Modifier.height(listArtHeight) else Modifier.fillMaxWidth()).aspectRatio(ratio)
                        .clip(RoundedCornerShape(app.reelstack.ui.theme.ReelLayout.ArtworkCorner))
                        .background(MaterialTheme.colorScheme.surfaceVariant))
                }
                val caption: @Composable () -> Unit = {
                    if (display.showTitles) Box(Modifier.fillMaxWidth(.7f).height(20.dp)
                        .clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                }
                if (listView) Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    image(); Column(Modifier.weight(1f)) { caption() }
                } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { image(); caption() }
            }
            items(state.libraryEntries, key = { it.id }) { entry ->
                // The grid reads the sync layer's own rows, so the decisions become words here.
                val factContext = androidx.compose.ui.platform.LocalContext.current
                val factWords = remember(entry.id, entry.facts) { entry.facts.map { it.text(factContext) } }
                val rating = if (display.showRatings)
                    app.reelstack.data.model.communityRatingLabel(factWords) else null
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val focused by interaction.collectIsFocusedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed && app.reelstack.ui.theme.LocalMotionEnabled.current) 0.985f else 1f,
                    animationSpec = androidx.compose.animation.core.tween(120),
                    label = "library-grid-spring",
                )
                val shape = RoundedCornerShape(app.reelstack.ui.theme.ReelLayout.ArtworkCorner)
                val card = Modifier.fillMaxWidth()
                    .then(if (!showShelves && entry.id == state.libraryEntries.firstOrNull()?.id) Modifier.focusRequester(firstContent) else Modifier)
                    .zIndex(if (focused) 10f else 0f)
                    // A list row is already full width; scaling it on focus makes it collide with
                    // its neighbours. The focus ring carries the state instead.
                    .graphicsLayer {
                        scaleX = if (listView) 1f else scale
                        scaleY = if (listView) 1f else scale
                    }
                    .clickable(interactionSource = interaction,
                        indication = app.reelstack.ui.components.mediaCardIndication(),
                        role = Role.Button, onClick = { onOpen(entry.id) })
                    .testTag("library-item-${entry.id}")
                    .semantics { contentDescription = entry.title }
                val artwork: @Composable (Modifier) -> Unit = { artModifier ->
                    BoxWithConstraints(artModifier.aspectRatio(ratio)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape)
                        .focusOutline(interaction, shape).clip(shape).testTag("library-art-${entry.id}")) {
                        val widthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
                        val requestSize = androidx.compose.ui.unit.IntSize(widthPx, (widthPx / ratio).toInt().coerceAtLeast(1))
                        val url = librarySizedArtwork(libraryArtworkUrl(entry, artType), widthPx)
                        if (artType == app.reelstack.data.model.LibraryArtType.LOGO && entry.logoUrl != null) {
                            MediaArtwork(url, null, Modifier.fillMaxSize().padding(12.dp), source = state.librarySource,
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit, requestSize = requestSize)
                        } else app.reelstack.ui.components.RailArtwork(url, null, ratio, Modifier.fillMaxSize(),
                            fallbackRes = R.drawable.media_placeholder, source = state.librarySource, requestSize = requestSize)
                        app.reelstack.ui.components.LibraryCardStatus(entry, display,
                            Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(6.dp))
                        if (!listView && rating != null) app.reelstack.ui.components.LibraryRating(rating,
                            Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(6.dp).testTag("library-rating-${entry.id}"))
                        entry.progress?.takeIf { it > 0f && !entry.played }?.let { progress ->
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp)
                                .align(androidx.compose.ui.Alignment.BottomCenter), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f),
                                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = .65f), drawStopIndicator = {})
                        }
                    }
                }
                val title: @Composable () -> Unit = {
                    Text(entry.title, color = MaterialTheme.colorScheme.onBackground,
                        minLines = 1,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    if (!folders && entry.subtitle.isNotBlank() && !entry.mediaType.equals("Series", true)) Text(
                        app.reelstack.ui.components.episodeLine(entry.season, entry.episode, entry.subtitle),
                        modifier = Modifier.padding(top = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (listView) {
                    // A row, not a narrow card in a one-column grid. The artwork keeps its own
                    // ratio at a fixed height, so a poster row and a thumb row line up.
                    //
                    // A list exists to say more per title than a grid does. On a television the row
                    // is nearly two metres wide, and a title with a year in it left the other
                    // three-quarters black — so the facts and the opening of the synopsis go in the
                    // space the grid could never have given them.
                    Row(
                        card.padding(vertical = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        artwork(Modifier.height(listArtHeight))
                        Column(Modifier.weight(1f)) {
                            if (display.showTitles) title()
                            if (rating != null) app.reelstack.ui.components.LibraryRating(rating,
                                Modifier.padding(top = 6.dp).testTag("library-rating-${entry.id}"))
                            // The type word used to lead this list and had to be filtered out by
                            // name, which only ever worked in nynorsk. The parser does not write it
                            // any more — the kind is derived from `mediaType` where the language is
                            // known — so there is nothing to filter.
                            val facts = factWords
                                .filterNot { it.trimStart().startsWith("★") }
                                .filterNot { it.matches(Regex("""^S\d\d+ E\d\d+$""")) }
                                // The line under the title already carries the year for a film or a
                                // series; repeating it two lines later reads as a mistake.
                                .filterNot { it == entry.subtitle }
                                .take(4)
                            if (facts.isNotEmpty()) Text(
                                facts.joinToString("  ·  "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            entry.overview?.takeIf(String::isNotBlank)?.let {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                } else {
                    Column(card) {
                        artwork(Modifier.fillMaxWidth())
                        if (display.showTitles || folders) {
                            Column(Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp)) { title() }
                        }
                    }
                }
            }
            item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                // Reaching the end of a page is the request. A button at the bottom of sixty covers
                // asks the reader to confirm something they have already done by scrolling there,
                // and on a remote it is one more stop between them and the next row of artwork.
                // The manual button stays for the case the automatic load failed.
                LaunchedEffect(state.libraryOffset, state.libraryHasMore, state.libraryError) {
                    if (state.libraryHasMore && !state.libraryLoading && state.libraryError == null) onLoad(true)
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.libraryLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else if (state.libraryError != null) {
                        Text(state.libraryError, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { onLoad(state.libraryEntries.isNotEmpty()) }) { Text(stringResource(R.string.library_retry)) }
                    } else if (connected && state.libraryEntries.isEmpty()) {
                        app.reelstack.ui.components.QuietEmptyState(
                            stringResource(if (state.libraryFilters.activeCount > 0) R.string.search_empty_filter else R.string.tv_library_empty),
                            app.reelstack.ui.components.SpoleIcons.Library,
                            action = if (state.libraryFilters.activeCount > 0) {{
                                app.reelstack.ui.components.OpenMenuAction(onClick = {
                                    onFilter(LibraryFilters(sort = state.libraryFilters.sort, descending = state.libraryFilters.descending))
                                }) { Text(stringResource(R.string.library_reset)) }
                            }} else null)
                    }
                    if (state.libraryHasMore && !state.libraryLoading && state.libraryError == null)
                        Button(onClick = { onLoad(true) }) { Text(stringResource(R.string.library_more)) }
                }
            }
        }
    }
}

/** A shelf heading, with the gesture hint on the first one only. */
@Composable
private fun LibraryShelfTitle(title: String, hint: Boolean) {
    Column(Modifier.padding(top = app.reelstack.ui.theme.ReelLayout.SectionTop, bottom = app.reelstack.ui.theme.ReelLayout.SectionBottom)) {
        Text(title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
        if (hint) Text(stringResource(R.string.library_card_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 3.dp))
    }
}
