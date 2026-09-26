package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchScreen(
    state: GlobalSearchUiState,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    onLibraryDetails: (String) -> Unit,
    onDiscoverDetails: (String) -> Unit,
    onRequest: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val field = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    val search = state.search
    val screenTitle = stringResource(R.string.search_open)
    // The field is why the screen opened: the cursor and the keyboard are there at once.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (runCatching { field.requestFocus() }.isSuccess) keyboard?.show()
    }
    // Scrolling the results is reading, not typing. The keyboard steps aside.
    LaunchedEffect(grid) {
        snapshotFlow { grid.isScrollInProgress }.collect { scrolling -> if (scrolling) focusManager.clearFocus() }
    }
    ReelPage(Modifier.background(Ink).semantics { paneTitle = screenTitle }, media = true) {
        Column(Modifier.fillMaxSize().testTag("global-search")
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = ReelLayout.Gutter, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("global-search-close")) {
                    Icon(SpoleIcons.ArrowBack, stringResource(R.string.action_back))
                }
                OutlinedTextField(
                    value = search.query,
                    onValueChange = onQuery,
                    singleLine = true,
                    leadingIcon = { Icon(SpoleIcons.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.searching) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else if (search.query.isNotEmpty()) {
                            IconButton(onClick = { onQuery(""); runCatching { field.requestFocus() } },
                                modifier = Modifier.testTag("global-search-clear")) {
                                Icon(SpoleIcons.Close, contentDescription = stringResource(R.string.search_clear))
                            }
                        }
                    },
                    placeholder = {
                        Text(stringResource(R.string.home_search), fontSize = 14.sp, lineHeight = 20.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = ControlOutline,
                        focusedContainerColor = SurfaceRaised,
                        unfocusedContainerColor = SurfaceRaised,
                        cursorColor = Primary,
                    ),
                    modifier = Modifier.weight(1f).focusRequester(field).testTag("global-search-field"),
                )
            }
            val query = search.query.trim()
            val requestable = state.requestable
            LazyVerticalGrid(
                columns = GridCells.Adaptive(posterCell()),
                state = grid,
                contentPadding = PaddingValues(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .testTag("global-search-results"),
            ) {
                if (query.length < 2) {
                    item(key = "intro", span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (state.history.isNotEmpty()) {
                                SearchHeading(stringResource(R.string.search_recent))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.history.forEach { previous ->
                                        AssistChip(
                                            onClick = { onQuery(previous) },
                                            label = { Text(previous, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            leadingIcon = { Icon(SpoleIcons.Clock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            modifier = Modifier.heightIn(min = 48.dp),
                                        )
                                    }
                                }
                            }
                            Text(stringResource(R.string.search_everywhere_hint), color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                    return@LazyVerticalGrid
                }
                if (search.libraryResults.isNotEmpty()) {
                    item(key = "library-heading", span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.padding(top = 4.dp)) {
                            SearchHeading(stringResource(R.string.search_libraries))
                            Text(stringResource(R.string.search_ready), color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                    items(search.libraryResults, key = { "library-${it.id}" }) { media ->
                        LibraryHitCard(media) { onLibraryDetails(media.id) }
                    }
                }
                when {
                    search.searching -> items(6, key = { "loading-$it" }) { DiscoverSkeleton(Modifier.fillMaxWidth()) }
                    requestable.isNotEmpty() -> {
                        item(key = "requestable-heading", span = { GridItemSpan(maxLineSpan) }) {
                            SearchHeading(stringResource(R.string.search_add_new), Modifier.padding(top = 12.dp))
                        }
                        items(requestable, key = { "seerr-${it.id}" }) { media ->
                            DiscoverCard(media, media.id in state.requestingMediaIds,
                                allowed = state.canRequest(media),
                                onRequest = { onRequest(media.id) }, onDetails = { onDiscoverDetails(media.id) })
                        }
                        // Seerr answers twenty at a time; the next page is a choice, not a scroll trap.
                        if (search.hasMore) item(key = "more", span = { GridItemSpan(maxLineSpan) }) {
                            OutlinedButton(
                                onClick = onLoadMore,
                                enabled = !search.loadingMore,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("global-search-more"),
                            ) {
                                if (search.loadingMore) {
                                    CircularProgressIndicator(Modifier.size(16.dp), color = Primary, strokeWidth = 2.dp)
                                    Text(stringResource(R.string.search_loading_more), modifier = Modifier.padding(start = 10.dp))
                                } else Text(stringResource(R.string.search_more))
                            }
                        }
                    }
                    search.libraryResults.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        Text(search.error ?: stringResource(R.string.search_global_no_results, query),
                            color = Muted, style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 24.dp).testTag("global-search-empty"))
                    }
                }
                // One service failing leaves the other's answers in place, with a line saying which.
                val partialError = search.error
                if (!search.searching && partialError != null && (search.libraryResults.isNotEmpty() || requestable.isNotEmpty())) {
                    item(key = "partial-error", span = { GridItemSpan(maxLineSpan) }) {
                        Text(partialError, color = Muted, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, color = TextColor, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
        modifier = modifier.semantics { heading() })
}
