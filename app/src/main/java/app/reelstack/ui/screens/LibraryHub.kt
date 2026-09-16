package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.*
import app.reelstack.ui.theme.*

/** A personal front door. Browsing tools stay one press away, each source keeps its own shelves. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun LibraryHub(state: ReelstackUiState, onLibrary: (String) -> Unit,
    onTitle: (String) -> Unit, actions: MediaCardActions?, onRetry: () -> Unit,
    sourcePicker: @Composable () -> Unit = {}) {
    val options = LocalPersonalization.current
    val context = LocalContext.current
    val preferences = remember(context) { app.reelstack.data.repository.AppPreferencesRepository(context) }
    var editing by remember { mutableStateOf(false) }
    if (editing) LibraryCustomizationDialog(state, options, { preferences.personalization = it }) { editing = false }
    val libraries = state.libraryEntries.sortedBy { options.libraryOrder.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
    val libraryIds = libraries.map { it.id }.toSet()
    val resume = state.resume.filter { state.configuredCount == 0 || it.source == state.librarySource }
    val next = state.nextUp.filter { state.configuredCount == 0 || it.source == state.librarySource }
    val featured = remember(state.recentMovies, state.recentSeries, state.libraryPeeks, libraryIds, state.configuredCount, state.librarySource) {
        val movies = state.recentMovies.filter { it.source == state.librarySource }
        val series = state.recentSeries.filter { it.source == state.librarySource }
        (movies.zip(series).flatMap { listOf(it.first, it.second) } + movies + series +
            state.libraryPeeks.filterKeys { it in libraryIds }.values.flatten()).distinctBy { it.id }.filter { app.reelstack.data.network.libraryHeroArtworkUrl(it) != null || state.configuredCount == 0 }.sortedByDescending { it.heroUrl != null }
    }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val tv = isTelevision()
    val large = LocalTabletCanvas.current || tv
    val inlineHeader = large && androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.5f
    val gutter = ReelLayout.Gutter
    val sections = (options.libraryHubOrder + DEFAULT_LIBRARY_HUB).distinct().filter { it !in options.libraryHubHidden }
    val leadingHero = tv && options.showHero && featured.isNotEmpty() && sections.firstOrNull() == "FEATURE"
    val hero: @Composable () -> Unit = {
        TabletLibraryFeature(featured.first(), onTitle,
            modifier = if (tv) Modifier.cinematicBleed(gutter) else Modifier, candidates = featured.take(5),
            account = if (leadingHero) sourcePicker else null,
            rotationEnabled = list.layoutInfo.visibleItemsInfo.any { it.key == "feature" } && state.activeSheet == null,
            onFocusWithin = { focused -> if (tv && focused) scope.launch {
                androidx.compose.runtime.withFrameNanos { }
                val index = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "feature" }?.index ?: 0
                list.scrollToItem(index)
            } })
    }
    LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("library-hub"),
        contentPadding = PaddingValues(start = gutter, end = gutter, top = if (leadingHero) 0.dp else 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (leadingHero) item("feature") { hero() }
        if (options.showLibraryTitle || !leadingHero) item("header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (options.showLibraryTitle && inlineHeader) Text(stringResource(R.string.nav_library),
                    style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(.32f).testTag("library-heading"))
                Column(Modifier.weight(1f)) {
                    if (options.showLibraryTitle && !inlineHeader) Text(stringResource(R.string.nav_library),
                        style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 12.dp).testTag("library-heading"))
                }
                if (!leadingHero) sourcePicker()
            }
        }
        item("libraries") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(4.dp)) {
                items(libraries, key = { it.id }) { library ->
                    LibraryImageTile(library.id, library.title, library.artworkUrl, options.libraryCardsWide, state.librarySource) { onLibrary(library.id) }
                }
            }
        }
        sections.forEach { section ->
        if (!leadingHero && section == "FEATURE" && large && options.showHero && featured.isNotEmpty()) item("feature") { hero() }
        val continued = if (options.combineContinueWatching && options.showNextUp) combinedWatching(resume, next) else resume
        if (section == "CONTINUE" && continued.isNotEmpty()) item("resume") {
            HubShelf(stringResource(R.string.home_continue)) {
                ResumeRail(continued, onTitle, actions, "CONTINUE_WATCHING")
            }
        }
        if (section == "NEXT" && options.showNextUp && !options.combineContinueWatching && next.isNotEmpty()) item("next") {
            HubShelf(stringResource(R.string.tv_next_up)) { ResumeRail(next, onTitle, actions.withoutResumeRemoval(), "NEXT_UP") }
        }
        if (section == "FAVOURITES" && state.favourites.any { it.source == state.librarySource }) item("favourites") {
            HubShelf(stringResource(R.string.home_favourites)) {
                LibraryRail(state.favourites.filter { it.source == state.librarySource }, onTitle, wide = false, rowKey = "FAVOURITES", actions = actions)
            }
        }
        if (section == "LIBRARIES") items(libraries, key = { "shelf-${it.id}" }) { library ->
            val titles = state.libraryPeeks[library.id].orEmpty()
            HubShelf(library.title) {
                if (titles.isNotEmpty()) LibraryRail(titles, onTitle, wide = library.collectionType == "tvshows",
                    rowKey = when(library.collectionType) { "movies" -> "${state.librarySource.name}_MOVIES"; "tvshows" -> "${state.librarySource.name}_SERIES"; else -> null }, actions = actions)
                else if (state.libraryPeeksLoading) app.reelstack.ui.components.LibraryRailSkeleton(description = stringResource(R.string.library_peek_loading), wide = false)
                AppNavigationChip(stringResource(R.string.design_browse_all), "hub-all-${library.id}") { onLibrary(library.id) }
            }
        }
        }
        if (state.libraryLoading && libraries.isEmpty()) item("loading") { LibraryRailSkeleton(description = stringResource(R.string.library_peek_loading)) }
        if (state.libraryError != null) item("error") {
            Text(state.libraryError, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SpoleSecondaryButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
        if (!state.libraryLoading && libraries.isEmpty() && state.libraryError == null) item("empty") {
            Text(stringResource(R.string.tv_library_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item("customize") {
            Row(Modifier.fillMaxWidth().testTag("hub-footer"), horizontalArrangement = Arrangement.End) {
                val label = stringResource(R.string.refine_library_edit)
                TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
                    IconButton(onClick = { editing = true }, modifier = Modifier.testTag("hub-customize")) {
                        Icon(SpoleIcons.Tune, label)
                    }
                }
            }
        }
    }
}

@Composable
private fun HubShelf(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}
@Composable
private fun LibraryImageTile(id: String, title: String, image: String?, wide: Boolean, source: ServiceKind, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.width(if (wide) 174.dp else 112.dp)) {
    Box(Modifier.fillMaxWidth().aspectRatio(if (wide) 1.9f else 1f)
        .clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
        .focusOutline(interaction, shape).clickable(interactionSource = interaction,
            indication = mediaCardIndication(), onClick = onClick).semantics { contentDescription = title }.testTag("hub-library-$id")) {
        Icon(SpoleIcons.Library, null, Modifier.align(Alignment.Center).size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (image != null) MediaArtwork(image, null, Modifier.matchParentSize(), source = source)
    }
    if (LocalPersonalization.current.showLibraryCardNames) Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 6.dp).testTag("hub-library-name-$id"))
    }
}
