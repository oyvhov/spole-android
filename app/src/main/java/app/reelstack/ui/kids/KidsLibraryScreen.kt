package app.reelstack.ui.kids

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.Text as KidsText

private enum class KidsLibraryFilter {
    ALL, MOVIES, SERIES, UNWATCHED, FAVOURITES,
}

/** A small, child-friendly equivalent of the adult library page. */
@Composable
internal fun KidsLibraryScreen(
    library: RemoteLibraryView,
    media: List<LibraryMedia>,
    onPlay: (LibraryMedia) -> Unit,
    onBack: () -> Unit,
    columns: Int,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable(library.id) { mutableStateOf(KidsLibraryFilter.ALL) }
    val filtered = remember(media, filter) {
        media.filter { item ->
            when (filter) {
                KidsLibraryFilter.ALL -> true
                KidsLibraryFilter.MOVIES -> item.mediaType.equals("Movie", ignoreCase = true)
                KidsLibraryFilter.SERIES -> item.isSeries
                KidsLibraryFilter.UNWATCHED -> !item.played
                KidsLibraryFilter.FAVOURITES -> item.favourite
            }
        }
    }
    val configuration = LocalConfiguration.current
    val tablet = configuration.screenWidthDp >= 600

    Column(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                Icon(SpoleIcons.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = KidsText)
            }
            Column(Modifier.weight(1f)) {
                Text(library.name, style = MaterialTheme.typography.headlineSmall, color = KidsText)
                Text(stringResource(R.string.kids_library_page_hint), color = Muted)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(if (tablet) 18.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (tablet) 20.dp else 16.dp),
        ) {
            item(span = { GridItemSpan(columns) }) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    item { KidsFilterChip(filter == KidsLibraryFilter.ALL, stringResource(R.string.kids_filter_all)) { filter = KidsLibraryFilter.ALL } }
                    item { KidsFilterChip(filter == KidsLibraryFilter.MOVIES, stringResource(R.string.kids_filter_movies)) { filter = KidsLibraryFilter.MOVIES } }
                    item { KidsFilterChip(filter == KidsLibraryFilter.SERIES, stringResource(R.string.kids_filter_series)) { filter = KidsLibraryFilter.SERIES } }
                    item { KidsFilterChip(filter == KidsLibraryFilter.UNWATCHED, stringResource(R.string.kids_filter_unwatched)) { filter = KidsLibraryFilter.UNWATCHED } }
                    if (media.any { it.favourite }) {
                        item { KidsFilterChip(filter == KidsLibraryFilter.FAVOURITES, stringResource(R.string.kids_filter_favourites)) { filter = KidsLibraryFilter.FAVOURITES } }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    Text(
                        stringResource(R.string.kids_library_empty),
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 28.dp),
                    )
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    KidsPosterCard(media = item, onPlay = { onPlay(item) })
                }
            }
        }
    }
}

@Composable
private fun KidsFilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = if (selected) ({ Icon(SpoleIcons.Done, contentDescription = null) }) else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary.copy(alpha = 0.22f),
            selectedLabelColor = KidsText,
        ),
    )
}
