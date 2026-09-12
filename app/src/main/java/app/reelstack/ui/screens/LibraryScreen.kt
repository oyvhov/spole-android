package app.reelstack.ui.screens

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
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.focusOutline

@Composable
fun LibraryScreen(state: ReelstackUiState, onLoad: (Boolean) -> Unit, onOpen: (String) -> Unit, onBack: () -> Unit,
    onFilter: (app.reelstack.data.model.LibraryFilters) -> Unit = {},
    onShelfOpen: (String) -> Unit = {}, cardActions: MediaCardActions? = null) {
    BackHandler(state.libraryPath.isNotEmpty() && state.activeSheet == null) { onBack() }
    val connected = state.connections.any { it.kind == ServiceKind.JELLYFIN && it.token.isNotBlank() }
    val tv = LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val folders = state.libraryPath.isEmpty()
    val libraryId = state.libraryPath.lastOrNull()?.first.orEmpty()
    val (display, saveDisplay) = rememberLibraryDisplay(libraryId)
    // AUTO keeps the old behaviour: episodes and video get a wide frame, everything else a poster.
    val autoWide = folders || state.libraryEntries.any { it.mediaType in setOf("Episode", "Video", "Photo") }
    val wideCards = if (display.artType == app.reelstack.data.model.LibraryArtType.AUTO) autoWide
        else display.artType.ratio > 1f
    val ratio = if (display.artType == app.reelstack.data.model.LibraryArtType.AUTO) {
        if (autoWide) 16f / 9f else 2f / 3f
    } else display.artType.ratio
    val listView = !folders && display.view == app.reelstack.data.model.LibraryView.LIST
    val size = app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale * display.size.scale
    // Resume and Next up for this library, fetched by the library page rather than sliced out of
    // Home's twelve mixed cards.
    val shelves = state.libraryShelves.takeIf { it.libraryId == libraryId }
    val shelfResume = shelves?.resume.orEmpty()
    val shelfNextUp = shelves?.nextUp.orEmpty()
    val showShelves = !folders && state.libraryPath.size == 1 &&
        state.libraryFilters == app.reelstack.data.model.LibraryFilters() &&
        (shelfResume.isNotEmpty() || shelfNextUp.isNotEmpty())
    // One list, not a fresh one per recomposition: an unstable argument makes the whole landing
    // page unskippable.
    val inProgress = remember(state.resume, state.nextUp) { state.resume + state.nextUp }
    // The root of Bibliotek is a page about libraries, not a grid of four folders — see
    // [LibraryLanding] for why. Everything below the root is still the grid it always was.
    if (folders) {
        LibraryLanding(
            libraries = state.libraryEntries,
            peeks = state.libraryPeeks,
            inProgress = inProgress,
            icons = state.libraryIcons,
            loading = state.libraryPeeksLoading || state.libraryLoading,
            tv = tv,
            onOpenLibrary = onOpen,
            onOpenTitle = onShelfOpen,
            cardActions = cardActions,
            connected = connected,
            error = state.libraryError,
        )
        return
    }
    // Next up is not a resume shelf, so its cards do not offer to clear a resume point.
    val nextUpActions = cardActions.withoutResumeRemoval()
    val pageStates = rememberSaveableStateHolder()
    pageStates.SaveableStateProvider(state.libraryPath.joinToString("/") { it.first }) {
        val grid = rememberLazyGridState()
        val cell = (if (wideCards) 240.dp else if (tv) 155.dp else 145.dp) * size
        LazyVerticalGrid(state = grid,
            columns = if (listView) GridCells.Fixed(1) else GridCells.Adaptive(cell),
            contentPadding = PaddingValues(if (tv) 32.dp else 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("library-browser")) {
            item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // One library deep, the sidebar already says Bibliotek. The trail is worth a
                    // line only once there is something in it the sidebar cannot say.
                    if (state.libraryPath.size > 1) Text((listOf(stringResource(R.string.nav_library)) +
                        state.libraryPath.dropLast(1).map { it.second }).joinToString("  /  "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    val heading: @Composable () -> Unit = {
                        Text(state.libraryPath.lastOrNull()?.second ?: stringResource(R.string.nav_library),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.displaySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val controls: @Composable () -> Unit = {
                        LibraryFilterBar(state.libraryFilters, onFilter, state.libraryFacets, display, saveDisplay)
                    }
                    // On a television the title and its three controls fit side by side, and the
                    // line they save is a whole row of covers: stacked, the heading block pushed
                    // the first row's titles past the bottom edge of a 1080p screen.
                    if (tv) Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        heading()
                        controls()
                    } else {
                        heading()
                        controls()
                        TextButton(onClick = onBack) { Text(stringResource(R.string.library_back)) }
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
                Column(Modifier.padding(bottom = 8.dp)) {
                    if (shelfResume.isNotEmpty()) {
                        LibraryShelfTitle(stringResource(R.string.home_continue), cardActions != null)
                        ResumeRail(shelfResume, onShelfOpen, cardActions)
                    }
                    if (shelfNextUp.isNotEmpty()) {
                        LibraryShelfTitle(stringResource(R.string.tv_next_up), cardActions != null && shelfResume.isEmpty())
                        ResumeRail(shelfNextUp, onShelfOpen, nextUpActions)
                    }
                }
            }
            if (!connected) item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.library_connect)) }
            items(state.libraryEntries, key = { it.id }) { entry ->
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val focused by interaction.collectIsFocusedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (focused) 1.08f else if (pressed) 0.965f else 1f,
                    animationSpec = spring(stiffness = 380f, dampingRatio = 0.75f),
                    label = "library-grid-spring",
                )
                val shape = RoundedCornerShape(app.reelstack.ui.theme.ReelLayout.ArtworkCorner)
                val card = Modifier.fillMaxWidth()
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
                val artwork: @Composable (Modifier) -> Unit = { artModifier ->
                    Box(artModifier.aspectRatio(ratio)
                        .focusOutline(interaction, shape).clip(shape).testTag("library-art-${entry.id}")) {
                        MediaArtwork(display.artType.applyTo(entry.artworkUrl), null, Modifier.fillMaxSize(), fallbackRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN)
                        entry.progress?.takeIf { it > 0f }?.let { progress ->
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp)
                                .align(androidx.compose.ui.Alignment.BottomCenter))
                        }
                    }
                }
                val title: @Composable () -> Unit = {
                    Text(entry.title, color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    if (!folders && entry.subtitle.isNotBlank()) Text(
                        app.reelstack.ui.components.episodeLine(entry.season, entry.episode, entry.subtitle),
                        modifier = Modifier.padding(top = 4.dp),
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
                        artwork(Modifier.height(if (tv) 118.dp * size else 96.dp * size))
                        Column(Modifier.weight(1f)) {
                            title()
                            val facts = entry.facts
                                .filterNot { it in setOf("Film", "Serie", "Episode", "Movie", "Series") }
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
                    } else if (connected && state.libraryEntries.isEmpty()) Text(stringResource(R.string.tv_library_empty))
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
