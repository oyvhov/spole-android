package app.reelstack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.reelstack.ui.components.OpenMenuAction
import app.reelstack.ui.components.QuietEmptyState
import app.reelstack.ui.components.focusRing
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.ui.components.DiscoverSkeleton
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.steadyRemoteRows
import app.reelstack.ui.state.GlobalSearchUiState
import app.reelstack.ui.theme.ControlOutline
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.ReelPage
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor

/**
 * Search as a place of its own (docs/GLOBALT_SOK_PLAN.md, GS-1). It opens over whatever the reader
 * was doing and Back returns there. It used to be a second copy of Discover — heading, filters and
 * suggestions included — so a search from Home looked like a jump to Oppdag, and it shared Discover's
 * field. Now it owns its query and shows only what search is for: what you can watch now, then what
 * you can ask for.
 *
 * On a television there is no text field. The system keyboard would cover the results, so the page
 * has a keyboard card of its own beside them ([TvSearchKeyboard]) and the results change after every
 * key. A phone and a tablet keep the field.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchScreen(
    state: GlobalSearchUiState,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    onLibraryDetails: (String) -> Unit,
    onDiscoverDetails: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPerson: (Int) -> Unit = {},
    /** On TV, leave the first input row for the optional top navigation. */
    onNavigateUp: (() -> Unit)? = null,
    /**
     * The reader has moved from typing into the results, so the term is worth remembering now: a
     * result got focus (television), a result was opened, or the keyboard's search action was
     * pressed (phone). It is never called for the terms typed on the way.
     */
    onCommitSearch: () -> Unit = {},
) {
    val television = app.reelstack.ui.components.isTelevision()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val field = remember { FocusRequester() }
    val keys = remember { SearchKeyFocus() }
    val grid = rememberLazyGridState()
    val search = state.search
    val screenTitle = stringResource(R.string.search_open)
    val openLibrary: (String) -> Unit = { onCommitSearch(); onLibraryDetails(it) }
    val openDiscover: (String) -> Unit = { onCommitSearch(); onDiscoverDetails(it) }
    val openPerson: (Int) -> Unit = { onCommitSearch(); onPerson(it) }
    // An earlier search, picked again, is one worth keeping on top. On a television the keys are
    // where the reader goes on typing from there.
    val pickEarlier: (String) -> Unit = { onQuery(it); onCommitSearch(); if (television) keys.toLastKey() }
    // Opening search puts the reader where typing starts: the keys on a television, the field and its
    // keyboard elsewhere.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (television) keys.toLastKey()
        else if (runCatching { field.requestFocus() }.isSuccess) keyboard?.show()
    }
    // Scrolling the results is reading, not typing. The keyboard steps aside — only from the field: on a
    // television the remote's own moves scroll the grid, and clearing focus then lost it altogether.
    var fieldFocused by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(grid) {
        snapshotFlow { grid.isScrollInProgress }.collect { scrolling -> if (scrolling && fieldFocused) focusManager.clearFocus() }
    }
    ReelPage(Modifier.background(Ink).semantics { paneTitle = screenTitle }, media = true) {
        Column(Modifier.fillMaxSize().testTag("global-search")
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            if (television) {
                TvSearchPage(state, keys, grid, onQuery, onNavigateUp, onCommitSearch, pickEarlier, openLibrary, openDiscover,
                    onLoadMore, openPerson)
                return@Column
            }
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = ReelLayout.Gutter, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp).focusRing(CircleShape).testTag("global-search-close")) {
                    Icon(SpoleIcons.ArrowBack, stringResource(R.string.action_back))
                }
                TextField(
                    value = search.query,
                    onValueChange = onQuery,
                    singleLine = true,
                    leadingIcon = { Icon(SpoleIcons.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.searching) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else if (search.query.isNotEmpty()) {
                            IconButton(onClick = { onQuery(""); runCatching { field.requestFocus() } },
                                modifier = Modifier.size(48.dp).focusRing(CircleShape).testTag("global-search-clear")) {
                                Icon(SpoleIcons.Close, contentDescription = stringResource(R.string.search_clear))
                            }
                        }
                    },
                    placeholder = {
                        Text(stringResource(R.string.discover_search_hint), fontSize = 14.sp, lineHeight = 20.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onCommitSearch(); focusManager.clearFocus() }),
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Primary,
                        unfocusedIndicatorColor = ControlOutline,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Primary,
                    ),
                    // A text field keeps the remote's down for itself; on a television the results are
                    // what down is for, so it leaves the field and the keyboard steps aside.
                    modifier = Modifier.weight(1f).focusRequester(field).onFocusChanged { fieldFocused = it.isFocused }.onPreviewKeyEvent { event ->
                        if (onNavigateUp != null && event.key == androidx.compose.ui.input.key.Key.DirectionUp &&
                            event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                            keyboard?.hide()
                            onNavigateUp()
                            true
                        } else if (event.key == androidx.compose.ui.input.key.Key.DirectionDown &&
                            event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                            keyboard?.hide()
                            focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                        } else false
                    }.testTag("global-search-field"),
                )
            }
            SearchResults(
                state, grid,
                contentPadding = PaddingValues(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = 8.dp, bottom = 24.dp),
                onPickEarlier = pickEarlier, onLibraryDetails = openLibrary, onDiscoverDetails = openDiscover,
                onLoadMore = onLoadMore, onPerson = openPerson, modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The television's search page: the keyboard card on the left, the results beside it.
 *
 * Moving from the keys into the results is the moment a term counts ([onCommitSearch]); "Ga" and
 * "Gam" on the way never do. Back with focus in the results goes to the key that was pressed last, so
 * the letters typed so far are not thrown away; Back from the keys leaves search, as before.
 */
@Composable
private fun TvSearchPage(
    state: GlobalSearchUiState,
    keys: SearchKeyFocus,
    grid: LazyGridState,
    onQuery: (String) -> Unit,
    onNavigateUp: (() -> Unit)?,
    onCommitSearch: () -> Unit,
    onPickEarlier: (String) -> Unit,
    onLibraryDetails: (String) -> Unit,
    onDiscoverDetails: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPerson: (Int) -> Unit,
) {
    val term = state.search.query
    var inResults by remember { mutableStateOf(false) }
    // Registered after the app's own handler for the page, so it is asked first, and only in the results.
    BackHandler(enabled = inResults) { keys.toLastKey() }
    val speak = rememberSpeechSearch(onResult = onQuery, onFinished = { runCatching { keys.requester("speak").requestFocus() } })
    Row(
        Modifier.fillMaxSize().padding(start = ReelLayout.Gutter, top = 12.dp)
            // A hardware keyboard types into the query wherever focus is, read before the focused key.
            .onPreviewKeyEvent { typeFromHardwareKeyboard(it, term, onQuery) },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TvSearchKeyboard(term, state.search.searching, onQuery, keys, speak, onNavigateUp)
        SearchResults(
            state, grid,
            contentPadding = PaddingValues(end = ReelLayout.Gutter, top = 8.dp, bottom = 24.dp),
            onPickEarlier = onPickEarlier, onLibraryDetails = onLibraryDetails, onDiscoverDetails = onDiscoverDetails,
            onLoadMore = onLoadMore, onPerson = onPerson,
            modifier = Modifier.weight(1f).fillMaxHeight().onFocusChanged { focus ->
                val entered = focus.hasFocus && !inResults
                inResults = focus.hasFocus
                if (entered) onCommitSearch()
            },
        )
    }
}

/** What was found, or what to do before anything is: shared by the phone's page and the television's. */
@Composable
private fun SearchResults(
    state: GlobalSearchUiState,
    grid: LazyGridState,
    contentPadding: PaddingValues,
    onPickEarlier: (String) -> Unit,
    onLibraryDetails: (String) -> Unit,
    onDiscoverDetails: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPerson: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val search = state.search
    val query = search.query.trim()
    val requestable = state.requestable
    LazyVerticalGrid(
        columns = GridCells.Adaptive(posterCell()),
        state = grid,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .steadyRemoteRows(grid)
            .testTag("global-search-results"),
    ) {
        if (query.length < 2) {
            item(key = "intro", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.history.isNotEmpty()) {
                        SearchHeading(stringResource(R.string.search_recent))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            state.history.forEach { previous ->
                                OpenMenuAction(onClick = { onPickEarlier(previous) }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(SpoleIcons.Clock, null, Modifier.size(20.dp), tint = Muted)
                                    Text(previous, Modifier.weight(1f).padding(horizontal = 14.dp), style = MaterialTheme.typography.bodyLarge)
                                    Icon(SpoleIcons.ArrowForward, null, Modifier.size(18.dp), tint = Muted)
                                }
                            }
                        }
                    }
                    QuietEmptyState(stringResource(R.string.home_search), SpoleIcons.Search,
                        hint = stringResource(R.string.search_everywhere_hint))
                }
            }
            return@LazyVerticalGrid
        }
        if (search.people.isNotEmpty()) {
            item(key = "people-heading", span = { GridItemSpan(maxLineSpan) }) {
                SearchHeading(stringResource(R.string.discover_people), Modifier.padding(top = 12.dp), SpoleIcons.Person)
            }
            items(search.people.take(SEARCH_PEOPLE_SHOWN), key = { "person-${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { person ->
                PersonLine(person) { onPerson(person.id) }
            }
        }
        if (search.libraryResults.isNotEmpty()) {
            item(key = "library-heading", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 4.dp)) {
                    SearchHeading(stringResource(R.string.search_libraries), icon = SpoleIcons.Library)
                    Text(stringResource(R.string.search_ready), color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            search.libraryResults.groupBy { it.mediaType }.forEach { (kind, mediaItems) ->
                item(key = "library-kind-$kind", span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (kind.equals("Movie", true)) SpoleIcons.Movie else SpoleIcons.Screen,
                            null, Modifier.size(20.dp), tint = Muted)
                        Text(stringResource(app.reelstack.ui.components.mediaKindRes(kind)) + " · ${mediaItems.size}",
                            style = MaterialTheme.typography.bodyMedium, color = Muted)
                    }
                }
                items(mediaItems, key = { "library-${it.id}" }) { media ->
                    LibraryHitCard(media) { onLibraryDetails(media.id) }
                }
            }
        }
        when {
            search.searching && requestable.isEmpty() && search.libraryResults.isEmpty() && search.people.isEmpty() ->
                items(6, key = { "loading-$it" }) { DiscoverSkeleton(Modifier.fillMaxWidth()) }
            requestable.isNotEmpty() -> {
                item(key = "requestable-heading", span = { GridItemSpan(maxLineSpan) }) {
                    SearchHeading(stringResource(R.string.search_add_new), Modifier.padding(top = 12.dp), SpoleIcons.Discover)
                }
                items(requestable, key = { "seerr-${it.id}" }) { media ->
                    if (app.reelstack.ui.components.isTelevision()) NamedPoster(media, { onDiscoverDetails(media.id) }, Modifier.fillMaxWidth())
                    else PosterOnlyCard(media, { onDiscoverDetails(media.id) }, Modifier.fillMaxWidth())
                }
                // Seerr answers twenty at a time; the next page is a choice, not a scroll trap.
                if (search.hasMore) item(key = "more", span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedButton(
                        onClick = onLoadMore,
                        enabled = !search.loadingMore && !search.searching,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).focusRing(RoundedCornerShape(14.dp)).testTag("global-search-more"),
                    ) {
                        if (search.loadingMore) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Primary, strokeWidth = 2.dp)
                            Text(stringResource(R.string.search_loading_more), modifier = Modifier.padding(start = 10.dp))
                        } else Text(stringResource(R.string.search_more))
                    }
                }
            }
            !search.searching && search.libraryResults.isEmpty() && search.people.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                QuietEmptyState(search.error ?: stringResource(R.string.search_global_no_results, query),
                    if (search.error != null) SpoleIcons.Wifi else SpoleIcons.Search,
                    modifier = Modifier.testTag("global-search-empty"))
            }
        }
        // One service failing leaves the other's answers in place, with a line saying which.
        val partialError = search.error
        if (!search.searching && partialError != null && (search.libraryResults.isNotEmpty() || requestable.isNotEmpty() || search.people.isNotEmpty())) {
            item(key = "partial-error", span = { GridItemSpan(maxLineSpan) }) {
                Text(partialError, color = Muted, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun SearchHeading(text: String, modifier: Modifier = Modifier, icon: ImageVector = SpoleIcons.Clock) {
    Row(modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = Muted)
        Text(text, color = TextColor, style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() })
    }
}
