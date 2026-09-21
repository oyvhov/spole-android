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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.Text as KidsText

private enum class KidsLibraryStatus {
    ALL, UNWATCHED, FAVOURITES,
}

private enum class KidsLibrarySort {
    ADDED, RELEASED, RATING,
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
    var status by rememberSaveable(library.id) { mutableStateOf(KidsLibraryStatus.ALL) }
    var sort by rememberSaveable(library.id) { mutableStateOf(KidsLibrarySort.ADDED) }
    val filtered = remember(media, status, sort) {
        media.asSequence()
            .filter { item ->
                when (status) {
                    KidsLibraryStatus.ALL -> true
                    KidsLibraryStatus.UNWATCHED -> !item.played
                    KidsLibraryStatus.FAVOURITES -> item.favourite
                }
            }
            .sortedWith(kidsLibraryComparator(sort))
            .toList()
    }
    val configuration = LocalConfiguration.current
    val tablet = configuration.screenWidthDp >= 600

    Column(
        // The shell owns the status-bar inset. TV passes it explicitly because its shell keeps
        // the cinematic edge-to-edge layout; mobile/tablet get it once from KidsApp.
        modifier = modifier.fillMaxSize(),
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
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.kids_sort_label), color = Muted, style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            KidsFilterChip(sort == KidsLibrarySort.ADDED, stringResource(R.string.kids_sort_added), "kids-sort-added") {
                                sort = KidsLibrarySort.ADDED
                            }
                        }
                        item {
                            KidsFilterChip(sort == KidsLibrarySort.RELEASED, stringResource(R.string.kids_sort_released), "kids-sort-released") {
                                sort = KidsLibrarySort.RELEASED
                            }
                        }
                        item {
                            KidsFilterChip(sort == KidsLibrarySort.RATING, stringResource(R.string.kids_sort_rating), "kids-sort-rating") {
                                sort = KidsLibrarySort.RATING
                            }
                        }
                    }
                    Text(stringResource(R.string.kids_status_label), color = Muted, style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            KidsFilterChip(status == KidsLibraryStatus.ALL, stringResource(R.string.kids_filter_all), "kids-status-all") {
                                status = KidsLibraryStatus.ALL
                            }
                        }
                        item {
                            KidsFilterChip(status == KidsLibraryStatus.UNWATCHED, stringResource(R.string.kids_filter_unwatched), "kids-status-unwatched") {
                                status = KidsLibraryStatus.UNWATCHED
                            }
                        }
                        if (media.any { it.favourite }) {
                            item {
                                KidsFilterChip(status == KidsLibraryStatus.FAVOURITES, stringResource(R.string.kids_filter_favourites), "kids-status-favourites") {
                                    status = KidsLibraryStatus.FAVOURITES
                                }
                            }
                        }
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
private fun KidsFilterChip(selected: Boolean, label: String, tag: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = if (selected) ({ Icon(SpoleIcons.Done, contentDescription = null) }) else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary.copy(alpha = 0.22f),
            selectedLabelColor = KidsText,
        ),
        modifier = Modifier.testTag(tag),
    )
}

private fun kidsLibraryComparator(sort: KidsLibrarySort): Comparator<LibraryMedia> = when (sort) {
    KidsLibrarySort.ADDED -> compareByDescending<LibraryMedia> { it.addedAtEpochMillis ?: Long.MIN_VALUE }
    KidsLibrarySort.RELEASED -> compareByDescending<LibraryMedia> { it.premiereDate?.take(10).orEmpty() }
    KidsLibrarySort.RATING -> compareByDescending<LibraryMedia> { it.ratingForKidsSort() }
}.thenBy { it.title.lowercase() }

private fun LibraryMedia.ratingForKidsSort(): Float =
    tmdbRating ?: mdblistRating ?: criticRating?.div(10f) ?: -1f
