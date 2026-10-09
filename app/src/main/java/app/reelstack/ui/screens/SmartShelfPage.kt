package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.reelstack.R
import app.reelstack.data.model.LibraryArtType
import app.reelstack.data.model.LibraryDisplay
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.libraryArtworkUrl
import app.reelstack.data.model.librarySizedArtwork
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.LibraryCardStatus
import app.reelstack.ui.components.OpenMenuAction
import app.reelstack.ui.components.QuietEmptyState
import app.reelstack.ui.components.RailArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.SpoleSecondaryButton
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.focusScale
import app.reelstack.ui.components.isTelevision
import app.reelstack.ui.components.mediaCardIndication
import app.reelstack.ui.components.smartShelfCounts
import app.reelstack.ui.components.smartShelfName
import app.reelstack.ui.components.smartShelfRule
import app.reelstack.ui.components.vector
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelLayout

/**
 * A smart shelf's own page.
 *
 * Films and series stand apart under their own headings, so a shelf that holds both still reads as
 * two answers. The covers are smaller than a library's grid: a shelf is for choosing among a few
 * dozen titles, and on a television one more column is one less press to reach the last of them.
 */
@Composable
internal fun SmartShelfPage(state: ReelstackUiState, shelf: SmartShelf, onOpen: (String) -> Unit, onBack: () -> Unit,
    onEdit: () -> Unit, onRetry: () -> Unit) {
    val tv = isTelevision()
    val gutter = if (tv) 32.dp else ReelLayout.Gutter
    // About seven covers across a television and three or four on a phone, where the library grid fits five and two.
    val cell = (if (tv) 108.dp else 96.dp) * LocalPersonalization.current.artworkSize.scale
    val entries = state.libraryEntries
    val series = entries.filter { it.mediaType.equals("Series", ignoreCase = true) }
    val movies = entries.filterNot { it.mediaType.equals("Series", ignoreCase = true) }
    val firstContent = remember { FocusRequester() }
    val editFocus = remember { FocusRequester() }
    // Closing the builder hands the remote back to «Endre», where it left from.
    val editing = state.smartShelfEditor != null
    var wasEditing by remember { mutableStateOf(false) }
    LaunchedEffect(editing) {
        if (tv && wasEditing && !editing) { withFrameNanos { }; runCatching { editFocus.requestFocus() } }
        wasEditing = editing
    }
    val firstId = (movies.firstOrNull() ?: series.firstOrNull())?.id
    val loading = state.libraryLoading && entries.isEmpty()
    val moviesTitle = stringResource(R.string.smart_shelf_movies)
    val seriesTitle = stringResource(R.string.smart_shelf_series)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(cell),
        contentPadding = PaddingValues(start = gutter, end = gutter, top = 24.dp,
            bottom = if (tv) ReelLayout.TvSafeEdge + 24.dp else 32.dp),
        horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("smart-shelf-page"),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            SmartShelfHeader(shelf, movies.size, series.size, loading, tv, onBack, onEdit, editFocus,
                Modifier.focusProperties { if (firstId != null) down = firstContent })
        }
        if (loading) items(12, key = { "skeleton-$it" }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(ReelLayout.ArtworkCorner))
                    .background(MaterialTheme.colorScheme.surfaceVariant))
                Box(Modifier.fillMaxWidth(.7f).height(14.dp).clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant))
            }
        }
        section("movies", moviesTitle, movies, state.librarySource, firstId, firstContent, onOpen)
        section("series", seriesTitle, series, state.librarySource, firstId, firstContent, onOpen)
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

private fun LazyGridScope.section(key: String, title: String, items: List<RemoteLibraryItem>, source: ServiceKind,
    firstId: String?, firstContent: FocusRequester, onOpen: (String) -> Unit) {
    if (items.isEmpty()) return
    item(key = "$key-heading", span = { GridItemSpan(maxLineSpan) }) {
        Row(Modifier.padding(top = 10.dp).semantics(mergeDescendants = true) { heading() }.testTag("smart-shelf-$key"),
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(items.size.toString(), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    items(items, key = { "$key-${it.id}" }) { entry ->
        SmartShelfCard(entry, source, { onOpen(entry.id) },
            if (entry.id == firstId) Modifier.focusRequester(firstContent) else Modifier)
    }
}

@Composable
private fun SmartShelfHeader(shelf: SmartShelf, movies: Int, series: Int, loading: Boolean, tv: Boolean,
    onBack: () -> Unit, onEdit: () -> Unit, editFocus: FocusRequester, modifier: Modifier = Modifier) {
    val name = smartShelfName(shelf)
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!tv) IconButton(onClick = onBack, modifier = Modifier.testTag("library-back")) {
                Icon(SpoleIcons.ArrowBack, stringResource(R.string.library_back))
            }
            Box(Modifier.size(if (tv) 60.dp else 48.dp).background(accent.copy(alpha = .16f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(shelf.icon.vector(), null, Modifier.size(if (tv) 30.dp else 24.dp), tint = accent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = if (tv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("smart-shelf-title"))
                Text(smartShelfRule(shelf), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            SpoleSecondaryButton(onClick = onEdit, modifier = Modifier.focusRequester(editFocus).testTag("smart-shelf-edit")) {
                Icon(SpoleIcons.Edit, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.smart_shelf_edit_action))
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        else if (movies + series > 0) Text(smartShelfCounts(movies, series), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("smart-shelf-counts"))
    }
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
