package app.reelstack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.focusOutline

@Composable
fun LibraryScreen(state: ReelstackUiState, onLoad: (Boolean) -> Unit, onOpen: (String) -> Unit, onBack: () -> Unit,
    onFilter: (app.reelstack.data.model.LibraryFilters) -> Unit = {}) {
    BackHandler(state.libraryPath.isNotEmpty() && state.activeSheet == null) { onBack() }
    val connected = state.connections.any { it.kind == ServiceKind.JELLYFIN && it.token.isNotBlank() }
    val tv = LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val folders = state.libraryPath.isEmpty()
    val wideCards = folders || state.libraryEntries.any { it.mediaType in setOf("Episode", "Video", "Photo") }
    val size = app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
    val pageStates = rememberSaveableStateHolder()
    pageStates.SaveableStateProvider(state.libraryPath.joinToString("/") { it.first }) {
        val grid = rememberLazyGridState()
        LazyVerticalGrid(state = grid, columns = GridCells.Adaptive((if (wideCards) 240.dp else if (tv) 155.dp else 145.dp) * size),
            contentPadding = PaddingValues(if (tv) 32.dp else 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("library-browser")) {
            item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.libraryPath.isNotEmpty()) Text((listOf(stringResource(R.string.nav_library)) +
                        state.libraryPath.dropLast(1).map { it.second }).joinToString("  /  "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Text(state.libraryPath.lastOrNull()?.second ?: stringResource(R.string.nav_library),
                        color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.displaySmall)
                    if (folders) Text(stringResource(R.string.tv_library_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!folders) LibraryFilterBar(state.libraryFilters, onFilter, state.libraryFacets)
                    if (!tv && !folders) TextButton(onClick = onBack) { Text(stringResource(R.string.library_back)) }
                }
            }
            if (!connected) item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.library_connect)) }
            items(state.libraryEntries, key = { it.id }) { entry ->
                val interaction = remember { MutableInteractionSource() }
                val shape = RoundedCornerShape(app.reelstack.ui.theme.ReelLayout.ArtworkCorner)
                Column(modifier = Modifier.fillMaxWidth()
                    .clickable(interactionSource = interaction,
                        indication = app.reelstack.ui.components.mediaCardIndication(),
                        role = Role.Button, onClick = { onOpen(entry.id) })
                    .testTag("library-item-${entry.id}")) {
                    Box(Modifier.fillMaxWidth().aspectRatio(if (wideCards) 16f / 9f else 2f / 3f)
                        .focusOutline(interaction, shape).clip(shape).testTag("library-art-${entry.id}")) {
                        MediaArtwork(entry.artworkUrl, R.drawable.media_placeholder, null, Modifier.fillMaxSize(), source = ServiceKind.JELLYFIN)
                        entry.progress?.takeIf { it > 0f }?.let { progress ->
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp)
                                .align(androidx.compose.ui.Alignment.BottomCenter))
                        }
                    }
                    Text(entry.title, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp),
                        maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    if (!folders && entry.subtitle.isNotBlank()) Text(entry.subtitle, modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
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
