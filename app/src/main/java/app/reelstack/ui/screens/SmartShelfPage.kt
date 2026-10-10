package app.reelstack.ui.screens

import androidx.compose.animation.core.animate
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.reelstack.R
import app.reelstack.data.model.LibraryArtType
import app.reelstack.data.model.LibraryDisplay
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.SmartShelfSort
import app.reelstack.data.model.libraryArtworkUrl
import app.reelstack.data.model.librarySizedArtwork
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.LibraryCardStatus
import app.reelstack.ui.components.LocalSmartShelfActions
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.OpenMenuAction
import app.reelstack.ui.components.QuietEmptyState
import app.reelstack.ui.components.RailArtwork
import app.reelstack.ui.components.SpoleChoiceDialog
import app.reelstack.ui.components.SpoleChoiceRow
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.SpoleSecondaryButton
import app.reelstack.ui.components.cinematicBleed
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.focusScale
import app.reelstack.ui.components.isTelevision
import app.reelstack.ui.components.mediaCardIndication
import app.reelstack.ui.components.smartShelfName
import app.reelstack.ui.components.steadyRemoteRows
import app.reelstack.ui.components.vector
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** What the page shows of a shelf that holds both: everything, or one kind. */
private enum class ShelfView { ALL, MOVIES, SERIES }

/** A title added this recently is marked «Ny» on the shelf. */
private const val NEW_FOR_MILLIS = 14L * 24 * 60 * 60_000

/**
 * A smart shelf's own page.
 *
 * It opens on the shelf itself: a backdrop from one of its titles, a fan of its posters, its name and
 * its two actions, and nothing more to read; the rule is in the builder, and the counts are on the
 * filter. Under that, the choices a shelf of a few dozen titles needs and no more: films, series or
 * both, an order the server sorts by, and watched titles out of the way. «Overrask meg» opens a title
 * not seen yet. The covers are a size smaller than a library's grid, so more of the shelf is on screen
 * at once; with both kinds shown, films and series stand under their own headings.
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun SmartShelfPage(state: ReelstackUiState, shelf: SmartShelf, onOpen: (String) -> Unit, onBack: () -> Unit,
    onEdit: () -> Unit, onRetry: () -> Unit) {
    val tv = isTelevision()
    val gutter = if (tv) 32.dp else ReelLayout.Gutter
    // About seven covers across a television and three or four on a phone, where the library grid fits five and two.
    val cell = (if (tv) 108.dp else 96.dp) * LocalPersonalization.current.artworkSize.scale
    val actions = LocalSmartShelfActions.current
    val entries = state.libraryEntries
    val series = entries.filter { it.mediaType.equals("Series", ignoreCase = true) }
    val movies = entries.filterNot { it.mediaType.equals("Series", ignoreCase = true) }
    var view by rememberSaveable(shelf.id) { mutableStateOf(ShelfView.ALL) }
    var hideWatched by rememberSaveable(shelf.id) { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    // Films and series as a choice only when the shelf has both; otherwise there is nothing to choose.
    val bothKinds = movies.isNotEmpty() && series.isNotEmpty()
    val shownView = if (bothKinds) view else ShelfView.ALL
    fun List<RemoteLibraryItem>.unseen() = if (hideWatched) filterNot { it.played } else this
    val shownMovies = if (shownView == ShelfView.SERIES) emptyList() else movies.unseen()
    val shownSeries = if (shownView == ShelfView.MOVIES) emptyList() else series.unseen()
    val shown = shownMovies + shownSeries
    val anyWatched = entries.any { it.played }
    // Something not seen yet, from what the page shows; everything seen leaves the choice to all of it.
    val surprisePool = shown.filterNot { it.played }.ifEmpty { shown }
    val surpriseFocus = remember { FocusRequester() }
    val editFocus = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    var pageHasFocus by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var heroHeight by remember { mutableIntStateOf(0) }
    var controlsHeight by remember { mutableIntStateOf(0) }
    val rowGap = with(LocalDensity.current) { 18.dp.roundToPx() }
    val top = remember { TopFocus() }
    // A remote coming back up from the titles gets the top of the page whole again: title, rule and
    // actions together, not only the row it landed on. A top taller than the screen, as with the
    // largest text, is left to the ordinary scrolling, which keeps the focused button in view.
    fun topInView(part: Int, bottom: () -> Int) = Modifier.onFocusChanged { focus ->
        if (top.focus(part, focus.hasFocus) && tv && heroHeight > 0 && bottom() <= grid.layoutInfo.viewportSize.height) {
            top.scroll?.cancel()
            top.scroll = scope.launch { grid.scrollToTop(heroHeight + rowGap) }
        }
    }
    // A remote arriving from a tile starts on the shelf's own action, which sits at the top: the whole
    // top of the page stays in view and the first title is one press down. Focus that has moved already
    // is left where it is, and nothing is scrolled, so a page someone is reading never jumps back.
    LaunchedEffect(shelf.id, entries.isNotEmpty()) {
        if (tv && entries.isNotEmpty() && !pageHasFocus) {
            withFrameNanos { }
            runCatching { (if (surprisePool.isNotEmpty()) surpriseFocus else editFocus).requestFocus() }
        }
    }
    // Closing the builder hands the remote back to «Endre», where it left from.
    val editing = state.smartShelfEditor != null
    var wasEditing by remember { mutableStateOf(false) }
    LaunchedEffect(editing) {
        if (tv && wasEditing && !editing) { withFrameNanos { }; runCatching { editFocus.requestFocus() } }
        wasEditing = editing
    }
    val loading = state.libraryLoading && entries.isEmpty()
    val moviesTitle = stringResource(R.string.smart_shelf_movies)
    val seriesTitle = stringResource(R.string.smart_shelf_series)
    // The platform's own spec pulls focus towards a third of the screen, which cut the top of the page
    // off on arrival; this one scrolls only when a focused card needs it, with a glimpse of the next row.
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides
        if (tv) ShelfBringIntoView else androidx.compose.foundation.gestures.LocalBringIntoViewSpec.current) {
    LazyVerticalGrid(
        state = grid,
        columns = GridCells.Adaptive(cell),
        contentPadding = PaddingValues(start = gutter, end = gutter, top = 0.dp,
            bottom = if (tv) ReelLayout.TvSafeEdge + 24.dp else 32.dp),
        horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .onFocusChanged { pageHasFocus = it.hasFocus }.steadyRemoteRows(grid).testTag("smart-shelf-page"),
    ) {
        item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.onSizeChanged { heroHeight = it.height }.then(topInView(0) { heroHeight })) {
                SmartShelfHero(shelf, entries, tv, gutter, state.librarySource, editFocus,
                    surpriseFocus, onBack, onEdit, onSurprise = surprisePool.takeIf { it.isNotEmpty() }?.let { pool -> { onOpen(pool.random().id) } })
            }
        }
        if (entries.isNotEmpty()) item(key = "controls", span = { GridItemSpan(maxLineSpan) }) {
            FlowRow(Modifier.fillMaxWidth().onSizeChanged { controlsHeight = it.height }
                .then(topInView(1) { heroHeight + rowGap + controlsHeight }).testTag("smart-shelf-controls"), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (bothKinds) {
                    ShelfPill(stringResource(R.string.smart_shelf_view_all), shownView == ShelfView.ALL, "smart-view-ALL",
                        count = entries.size, role = Role.Tab) { view = ShelfView.ALL }
                    ShelfPill(moviesTitle, shownView == ShelfView.MOVIES, "smart-view-MOVIES", leading = SpoleIcons.Movie,
                        count = movies.size, role = Role.Tab) { view = ShelfView.MOVIES }
                    ShelfPill(seriesTitle, shownView == ShelfView.SERIES, "smart-view-SERIES", leading = SpoleIcons.Screen,
                        count = series.size, role = Role.Tab) { view = ShelfView.SERIES }
                    Spacer(Modifier.width(8.dp))
                }
                ShelfPill(smartShelfSortLabel(shelf.sort), selected = false, tag = "smart-shelf-sort",
                    leading = SpoleIcons.ListLines, trailing = SpoleIcons.ChevronDown, role = Role.Button,
                    description = stringResource(R.string.smart_shelf_sort_title) + ": " + smartShelfSortLabel(shelf.sort)) { sortOpen = true }
                if (anyWatched) ShelfPill(stringResource(R.string.smart_shelf_hide_watched), hideWatched, "smart-hide-watched",
                    leading = if (hideWatched) SpoleIcons.EyeOff else SpoleIcons.Eye) { hideWatched = !hideWatched }
            }
        }
        if (loading) items(12, key = { "skeleton-$it" }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(ReelLayout.ArtworkCorner))
                    .background(MaterialTheme.colorScheme.surfaceVariant))
                Box(Modifier.fillMaxWidth(.7f).height(14.dp).clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant))
            }
        }
        // Both kinds side by side read as two answers, so each gets its heading; one kind needs none.
        if (shownView == ShelfView.ALL && bothKinds) {
            section("movies", moviesTitle, shownMovies, state.librarySource, onOpen)
            section("series", seriesTitle, shownSeries, state.librarySource, onOpen)
        } else items(shown, key = { "title-${it.id}" }) { entry ->
            SmartShelfCard(entry, state.librarySource, { onOpen(entry.id) })
        }
        if (entries.isNotEmpty() && shown.isEmpty()) item(key = "all-watched", span = { GridItemSpan(maxLineSpan) }) {
            QuietEmptyState(stringResource(R.string.smart_shelf_all_watched), SpoleIcons.DoneCircle,
                action = { OpenMenuAction(onClick = { hideWatched = false }, modifier = Modifier.testTag("smart-show-watched")) {
                    Text(stringResource(R.string.smart_shelf_show_watched))
                } })
        }
        state.libraryError?.let { error ->
            item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    OpenMenuAction(onClick = onRetry, modifier = Modifier.testTag("smart-shelf-retry")) {
                        Text(stringResource(R.string.library_retry))
                    }
                }
            }
        }
        if (!state.libraryLoading && state.libraryError == null && entries.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                QuietEmptyState(stringResource(R.string.smart_shelf_empty), shelf.icon.vector(),
                    hint = stringResource(R.string.smart_shelf_empty_hint),
                    action = { OpenMenuAction(onClick = onEdit, modifier = Modifier.testTag("smart-shelf-empty-edit")) {
                        Text(stringResource(R.string.smart_shelf_edit))
                    } })
            }
        }
    }
    }
    if (sortOpen) SpoleChoiceDialog(stringResource(R.string.smart_shelf_sort_title), { sortOpen = false }, SpoleIcons.ListLines) {
        LazyColumn(Modifier.testTag("smart-sort-choices")) {
            items(SmartShelfSort.entries.size) { index ->
                val sort = SmartShelfSort.entries[index]
                SpoleChoiceRow(smartShelfSortLabel(sort), shelf.sort == sort, Modifier.testTag("smart-sort-${sort.name}"),
                    supportingText = smartShelfSortHint(sort)) {
                    sortOpen = false
                    actions?.sort(shelf.id, sort)
                }
            }
        }
    }
}

private fun LazyGridScope.section(key: String, title: String, items: List<RemoteLibraryItem>, source: ServiceKind,
    onOpen: (String) -> Unit) {
    if (items.isEmpty()) return
    item(key = "$key-heading", span = { GridItemSpan(maxLineSpan) }) {
        Row(Modifier.padding(top = 10.dp).semantics(mergeDescendants = true) { heading() }.testTag("smart-shelf-$key"),
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
    }
    items(items, key = { "$key-${it.id}" }) { entry ->
        SmartShelfCard(entry, source, { onOpen(entry.id) })
    }
}

/**
 * The top of the page: a backdrop from the shelf's own titles under two soft shades, a fan of three
 * posters on a wide screen, the shelf's name and its two actions. It is kept low, so a television shows
 * the first row of covers under it. Without a backdrop the accent's own wash stands in, so a shelf
 * whose titles have no art still opens on something.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartShelfHero(shelf: SmartShelf, entries: List<RemoteLibraryItem>,
    tv: Boolean, gutter: Dp, source: ServiceKind, editFocus: FocusRequester, surpriseFocus: FocusRequester,
    onBack: () -> Unit, onEdit: () -> Unit, onSurprise: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val name = smartShelfName(shelf)
    val backdrop = entries.firstNotNullOfOrNull { it.backdropUrl ?: it.heroUrl }
    val posters = entries.mapNotNull { libraryArtworkUrl(it, LibraryArtType.POSTER) }.distinct().take(3)
    BoxWithConstraints(Modifier.cinematicBleed(gutter).fillMaxWidth().heightIn(min = if (tv) 236.dp else 196.dp)
        .testTag("smart-shelf-hero")) {
        val wide = maxWidth > 760.dp
        if (backdrop != null) MediaArtwork(librarySizedArtwork(backdrop, 1280), null,
            Modifier.matchParentSize().graphicsLayer { alpha = .55f }, source = source)
        else Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(colors.primaryContainer, colors.background))))
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to colors.background.copy(alpha = .95f), .5f to colors.background.copy(alpha = .62f), 1f to colors.background.copy(alpha = .2f))))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to colors.background.copy(alpha = .3f), .55f to Color.Transparent, 1f to colors.background)))
        // A fixed height: inside a scrolling grid the hero has no height of its own to fill.
        if (wide && posters.isNotEmpty()) PosterFan(posters, source,
            Modifier.align(Alignment.CenterEnd).padding(end = gutter + 56.dp).height(if (tv) 196.dp else 170.dp))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(if (wide) .6f else 1f)
            .padding(start = gutter, end = gutter, top = if (tv) 40.dp else 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // On a phone the way back shares the name's line, its arrow on the gutter.
            Row(Modifier.offset(x = if (tv) 0.dp else (-12).dp), verticalAlignment = Alignment.CenterVertically) {
                if (!tv) IconButton(onClick = onBack, modifier = Modifier.testTag("library-back")) {
                    Icon(SpoleIcons.ArrowBack, stringResource(R.string.library_back))
                }
                Text(name, style = if (tv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }.testTag("smart-shelf-title"))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSurprise != null) {
                    val surpriseInteraction = remember { MutableInteractionSource() }
                    Button(onClick = onSurprise, interactionSource = surpriseInteraction,
                        modifier = Modifier.heightIn(min = 48.dp).focusRequester(surpriseFocus)
                            .focusOutline(surpriseInteraction, CircleShape, glow = false).testTag("smart-shelf-surprise")) {
                        Icon(SpoleIcons.Shuffle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.smart_shelf_surprise))
                    }
                }
                SpoleSecondaryButton(onClick = onEdit, modifier = Modifier.focusRequester(editFocus).testTag("smart-shelf-edit")) {
                    Icon(SpoleIcons.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.smart_shelf_edit_action))
                }
            }
        }
    }
}

/** Three of the shelf's posters, turned a little against each other, like the tile in the library. */
@Composable
private fun PosterFan(posters: List<String>, source: ServiceKind, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    Box(modifier.aspectRatio(1.1f), contentAlignment = Alignment.Center) {
        posters.reversed().forEachIndexed { reversedIndex, url ->
            val depth = posters.size - 1 - reversedIndex
            Box(Modifier.fillMaxHeight(1f - depth * .08f).aspectRatio(2f / 3f)
                .offset(x = (depth * 46 - 46).dp)
                .graphicsLayer { rotationZ = (depth - 1) * 6f }
                .shadow(16.dp, shape).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                MediaArtwork(librarySizedArtwork(url, 360), null, Modifier.fillMaxSize(), source = source)
            }
        }
    }
}

/**
 * A choice on the page: a pill that fills with the accent's container when chosen, with a ring on focus
 * and the count where it helps choosing.
 */
@Composable
private fun ShelfPill(label: String, selected: Boolean, tag: String, leading: ImageVector? = null, trailing: ImageVector? = null,
    count: Int? = null, role: Role = Role.Checkbox, description: String? = null, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val content = if (selected) colors.onPrimaryContainer else colors.onSurface
    Row(Modifier.heightIn(min = 44.dp).clip(CircleShape)
        .background(if (selected) colors.primaryContainer else colors.surfaceVariant, CircleShape)
        .focusOutline(interaction, CircleShape, glow = false)
        .selectable(selected, interactionSource = interaction, indication = LocalIndication.current, role = role, onClick = onClick)
        .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
        .testTag(tag).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        leading?.let { Icon(it, null, Modifier.size(18.dp), tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant) }
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
        count?.let {
            Text(it.toString(), style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.onPrimaryContainer.copy(alpha = .75f) else colors.onSurfaceVariant)
        }
        trailing?.let { Icon(it, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant) }
    }
}

@Composable
internal fun smartShelfSortLabel(sort: SmartShelfSort): String = stringResource(when (sort) {
    SmartShelfSort.RULE -> R.string.smart_sort_rule
    SmartShelfSort.ADDED -> R.string.smart_sort_added
    SmartShelfSort.NEWEST -> R.string.smart_sort_newest
    SmartShelfSort.TITLE -> R.string.smart_sort_title
    SmartShelfSort.RATING -> R.string.smart_sort_rating
})

@Composable
private fun smartShelfSortHint(sort: SmartShelfSort): String? = when (sort) {
    SmartShelfSort.RULE -> stringResource(R.string.smart_sort_rule_hint)
    else -> null
}

/** A poster, its title and its year: the library card without the options a shelf has no use for. */
@Composable
private fun SmartShelfCard(entry: RemoteLibraryItem, source: ServiceKind, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val scale = focusScale(focused, pressed)
    val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    val density = LocalDensity.current
    val isNew = entry.addedAtEpochMillis?.let { System.currentTimeMillis() - it in 0..NEW_FOR_MILLIS } == true
    Column(modifier.zIndex(if (focused) 1f else 0f)
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .semantics { contentDescription = entry.title }
        .testTag("smart-item-${entry.id}")) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(2f / 3f)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .focusOutline(interaction, shape).clip(shape)) {
            val widthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
            RailArtwork(librarySizedArtwork(libraryArtworkUrl(entry, LibraryArtType.POSTER), widthPx), null, 2f / 3f,
                Modifier.fillMaxSize(), fallbackRes = R.drawable.media_placeholder, source = source,
                requestSize = IntSize(widthPx, widthPx * 3 / 2))
            if (isNew) Text(stringResource(R.string.smart_shelf_new_badge), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 2.dp)
                    .testTag("smart-new-${entry.id}"))
            LibraryCardStatus(entry, LibraryDisplay(), Modifier.align(Alignment.TopEnd).padding(5.dp))
            entry.progress?.takeIf { it > 0f }?.let { progress ->
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp)
                    .align(Alignment.BottomCenter), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f),
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = .65f), drawStopIndicator = {})
            }
        }
        Text(entry.title, Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp), style = MaterialTheme.typography.titleSmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (entry.subtitle.isNotBlank()) Text(entry.subtitle, Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Which part of a shelf page's top holds focus (0 the hero, 1 the controls), and the scroll that shows it. */
private class TopFocus {
    private val focused = BooleanArray(2)
    var scroll: Job? = null

    /**
     * Records a part's focus and says whether the part has just gained it. Leaving the top altogether
     * stops a scroll still on its way up, so a title focused below is brought into view as usual.
     */
    fun focus(part: Int, hasFocus: Boolean): Boolean {
        val gained = hasFocus && !focused[part]
        focused[part] = hasFocus
        if (focused.none { it }) scroll?.cancel()
        return gained
    }
}

/**
 * Scrolls a shelf page back to its top. It goes ahead of the focused button's own request to be
 * brought into view, which otherwise stopped the page part of the way up. [aboveControls] is how far
 * the top lies above the controls, for when the hero itself is already out of view.
 */
private suspend fun LazyGridState.scrollToTop(aboveControls: Int) {
    val items = layoutInfo.visibleItemsInfo
    val distance = items.firstOrNull { it.key == "hero" }?.offset?.y
        ?: items.firstOrNull { it.key == "controls" }?.let { it.offset.y - aboveControls }
        ?: return
    if (distance >= 0) return
    scroll(MutatePriority.UserInput) {
        var moved = 0f
        animate(0f, distance.toFloat()) { value, _ -> moved += scrollBy(value - moved) }
    }
}

/**
 * Scrolls a focused card into view with part of the next row showing past it, and leaves anything
 * already that far in view alone. The glimpse says there is more, and it keeps the next row laid out:
 * a row wholly out of view made the grid's focus search jump to the first column instead of moving
 * straight down. Something too tall to show with a glimpse on both sides only scrolls as far as needed.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal object ShelfBringIntoView : androidx.compose.foundation.gestures.BringIntoViewSpec {
    private const val GLIMPSE = .2f

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val glimpse = containerSize * GLIMPSE
        if (size + 2 * glimpse > containerSize) {
            return app.reelstack.ui.components.DetailBringIntoView.calculateScrollDistance(offset, size, containerSize)
        }
        return when {
            offset < glimpse -> offset - glimpse
            offset + size > containerSize - glimpse -> offset + size + glimpse - containerSize
            else -> 0f
        }
    }
}
