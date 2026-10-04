package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import app.reelstack.ui.components.SpoleDropdownMenu as DropdownMenu
import app.reelstack.ui.components.SpoleDropdownMenuItem as DropdownMenuItem
import app.reelstack.ui.components.SpoleChoiceDialog
import app.reelstack.ui.components.SpoleChoiceRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.SpoleSecondaryButton
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.SurfaceRaised

/**
 * Filter toolbar with non-intrusive floating dropdown menus and dialogs.
 * Opening menus or choosing options never shifts or jumps the underlying artwork.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibraryFilterBar(
    filters: LibraryFilters,
    onApply: (LibraryFilters) -> Unit,
    facets: LibraryFacets = LibraryFacets(),
    display: LibraryDisplay = LibraryDisplay(),
    onDisplayChange: (LibraryDisplay) -> Unit = {},
) {
    var sortOpen by remember { mutableStateOf(false) }
    var statusOpen by remember { mutableStateOf(false) }
    var resolutionOpen by remember { mutableStateOf(false) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var displayOpen by remember { mutableStateOf(false) }
    var searchDialogOpen by remember { mutableStateOf(false) }
    var genreDialogOpen by remember { mutableStateOf(false) }
    var yearDialogOpen by remember { mutableStateOf(false) }
    var alphabetDialogOpen by remember { mutableStateOf(false) }

    val sorts = stringArrayResource(R.array.library_sorts)
    val watched = stringArrayResource(R.array.library_watched)
    val resolutions = stringArrayResource(R.array.library_resolutions)

    val currentSortLabel = when {
        filters.sort == LibrarySort.TITLE && !filters.descending -> stringResource(R.string.library_sort_title_asc)
        filters.sort == LibrarySort.TITLE && filters.descending -> stringResource(R.string.library_sort_title_desc)
        filters.sort == LibrarySort.ADDED && filters.descending -> stringResource(R.string.library_sort_added_desc)
        filters.sort == LibrarySort.YEAR && filters.descending -> stringResource(R.string.library_sort_year_desc)
        filters.sort == LibrarySort.RATING -> stringResource(R.string.library_sort_rating_desc)
        filters.sort == LibrarySort.RUNTIME -> stringResource(R.string.library_sort_runtime_desc)
        filters.sort == LibrarySort.PLAYED -> stringResource(R.string.library_sort_played_desc)
        else -> sorts[filters.sort.ordinal]
    }

    val summary = listOfNotNull(
        watched[filters.watched.ordinal].takeIf { filters.watched != LibraryWatched.ALL },
        resolutions[filters.resolution.ordinal].takeIf { filters.resolution != LibraryResolution.ALL },
        stringResource(R.string.library_favourites).takeIf { filters.favourites },
        filters.genre.takeIf(String::isNotBlank), filters.year.takeIf(String::isNotBlank),
        filters.initial.takeIf(String::isNotBlank), filters.search.takeIf(String::isNotBlank))
    if (summary.isNotEmpty()) Text(summary.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).testTag("library-filter-summary"))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 1. Sortering
        Box {
            app.reelstack.ui.components.OpenMenuAction(
                onClick = { sortOpen = true },
                modifier = Modifier.testTag("library-sort-choice"),
            ) {
                Text(currentSortLabel)
                Icon(SpoleIcons.ChevronDown, null, modifier = Modifier.size(16.dp).padding(start = 4.dp))
            }
            DropdownMenu(
                expanded = sortOpen,
                onDismissRequest = { sortOpen = false },
                containerColor = SurfaceRaised,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_title_asc)) },
                    selected = filters.sort == LibrarySort.TITLE && !filters.descending,
                    leadingIcon = if (filters.sort == LibrarySort.TITLE && !filters.descending) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-TITLE"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.TITLE, descending = false)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_title_desc)) },
                    selected = filters.sort == LibrarySort.TITLE && filters.descending,
                    leadingIcon = if (filters.sort == LibrarySort.TITLE && filters.descending) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onApply(filters.copy(sort = LibrarySort.TITLE, descending = true)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_added_desc)) },
                    selected = filters.sort == LibrarySort.ADDED,
                    leadingIcon = if (filters.sort == LibrarySort.ADDED) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-ADDED"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.ADDED, descending = true)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_year_desc)) },
                    selected = filters.sort == LibrarySort.YEAR,
                    leadingIcon = if (filters.sort == LibrarySort.YEAR) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-YEAR"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.YEAR, descending = true)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_rating_desc)) },
                    selected = filters.sort == LibrarySort.RATING,
                    leadingIcon = if (filters.sort == LibrarySort.RATING) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-RATING"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.RATING, descending = true)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_runtime_desc)) },
                    selected = filters.sort == LibrarySort.RUNTIME,
                    leadingIcon = if (filters.sort == LibrarySort.RUNTIME) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-RUNTIME"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.RUNTIME, descending = true)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_played_desc)) },
                    selected = filters.sort == LibrarySort.PLAYED,
                    leadingIcon = if (filters.sort == LibrarySort.PLAYED) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-PLAYED"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.PLAYED, descending = true)); sortOpen = false },
                )
            }
        }

        // 4. Filter (Flere filter)
        Box {
            app.reelstack.ui.components.OpenMenuAction(
                onClick = { filterMenuOpen = true },
                modifier = Modifier.testTag("library-filters"),
            ) {
                Icon(
                    SpoleIcons.Tune,
                    null,
                    modifier = Modifier.size(16.dp).padding(end = 4.dp),
                    tint = if (filters.activeCount > 0) Primary else LocalContentColor.current,
                )
                Text(
                    stringResource(R.string.menu_filters_short) + if (filters.activeCount > 0) " (${filters.activeCount})" else "",
                    color = if (filters.activeCount > 0) Primary else Color.Unspecified,
                )
                Icon(SpoleIcons.ChevronDown, null, modifier = Modifier.size(16.dp).padding(start = 4.dp))
            }
            DropdownMenu(
                expanded = filterMenuOpen,
                onDismissRequest = { filterMenuOpen = false },
                containerColor = SurfaceRaised,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_watched_short) + " · " + watched[filters.watched.ordinal]) },
                    leadingIcon = { Icon(SpoleIcons.Eye, null) },
                    trailingIcon = { Icon(SpoleIcons.ChevronRight, null) },
                    modifier = Modifier.testTag("library-watched-choice"),
                    onClick = { filterMenuOpen = false; statusOpen = true })
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_resolution) + " · " + resolutions[filters.resolution.ordinal]) },
                    leadingIcon = { Icon(SpoleIcons.Screen, null) },
                    trailingIcon = { Icon(SpoleIcons.ChevronRight, null) },
                    modifier = Modifier.testTag("library-resolution-choice"),
                    onClick = { filterMenuOpen = false; resolutionOpen = true })
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_favourites)) },
                    leadingIcon = { Icon(if (filters.favourites) SpoleIcons.HeartFilled else SpoleIcons.Heart, null) },
                    modifier = Modifier.testTag("library-favourites"),
                    onClick = { onApply(filters.copy(favourites = !filters.favourites)); filterMenuOpen = false },
                )
                if (facets.genres.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.menu_genre_short) + filters.genre.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty())
                        },
                        leadingIcon = { Icon(SpoleIcons.Movie, null) },
                        onClick = { filterMenuOpen = false; genreDialogOpen = true },
                    )
                }
                if (facets.years.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.library_year) + filters.year.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty())
                        },
                        leadingIcon = { Icon(SpoleIcons.Calendar, null) },
                        onClick = { filterMenuOpen = false; yearDialogOpen = true },
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.design_alphabet) + filters.initial.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty())
                    },
                    leadingIcon = { Icon(SpoleIcons.ListLines, null) },
                    onClick = { filterMenuOpen = false; alphabetDialogOpen = true },
                )
                if (filters != LibraryFilters()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_reset), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(SpoleIcons.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                        // Keep the open-menu reset distinct from the persistent quick reset.
                        modifier = Modifier.testTag("library-filter-menu-reset"),
                        onClick = {
                            onApply(LibraryFilters(sort = filters.sort, descending = filters.descending))
                            filterMenuOpen = false
                        },
                    )
                }
            }
        }

        // 5. Visning
        Box {
            app.reelstack.ui.components.OpenMenuAction(
                onClick = { displayOpen = true },
                modifier = Modifier.testTag("library-display-toggle"),
            ) {
                Icon(SpoleIcons.ListLines, null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                Text(stringResource(R.string.menu_view_short))
                Icon(SpoleIcons.ChevronDown, null, modifier = Modifier.size(16.dp).padding(start = 4.dp))
            }
        }

        // 6. Søk
        app.reelstack.ui.components.OpenMenuAction(
            onClick = { searchDialogOpen = true },
            modifier = Modifier.testTag("library-search-toggle"),
        ) {
            Icon(
                SpoleIcons.Search,
                null,
                modifier = Modifier.size(16.dp).padding(end = 4.dp),
                tint = if (filters.search.isNotBlank()) Primary else LocalContentColor.current,
            )
            Text(
                stringResource(R.string.search_open),
                color = if (filters.search.isNotBlank()) Primary else Color.Unspecified,
            )
        }

        // 7. Nullstill hurtigknapp
        if (filters.activeCount > 0) {
            app.reelstack.ui.components.OpenMenuAction(
                onClick = { onApply(LibraryFilters(sort = filters.sort, descending = filters.descending)) },
                modifier = Modifier.testTag("library-filter-reset"),
            ) {
                Icon(SpoleIcons.Close, null, modifier = Modifier.size(16.dp).padding(end = 4.dp), tint = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.library_reset), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (displayOpen) SpoleChoiceDialog(stringResource(R.string.library_display), { displayOpen = false }, SpoleIcons.ListLines) {
        LibraryDisplayPanel(display, onDisplayChange, Modifier
            .verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp))
    }
    if (statusOpen) SpoleChoiceDialog(stringResource(R.string.filter_status), { statusOpen = false }, SpoleIcons.Eye) {
        LazyColumn {
            items(LibraryWatched.entries) { value ->
                SpoleChoiceRow(watched[value.ordinal], filters.watched == value,
                    Modifier.testTag("library-watched-${value.name}")) {
                    onApply(filters.copy(watched = value)); statusOpen = false
                }
            }
        }
    }
    if (resolutionOpen) SpoleChoiceDialog(stringResource(R.string.library_resolution), { resolutionOpen = false }, SpoleIcons.Screen) {
        LazyColumn {
            items(LibraryResolution.entries) { value ->
                SpoleChoiceRow(resolutions[value.ordinal], filters.resolution == value,
                    Modifier.testTag("library-resolution-${value.name}")) {
                    onApply(filters.copy(resolution = value)); resolutionOpen = false
                }
            }
        }
    }
    // Modal dialogs that do not push content down
    if (searchDialogOpen) {
        var typed by remember(filters.search) { mutableStateOf(filters.search) }
        AlertDialog(
            onDismissRequest = { searchDialogOpen = false },
            title = { Text(stringResource(R.string.library_search)) },
            text = {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.take(150) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            onApply(filters.copy(search = typed))
                            searchDialogOpen = false
                        },
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("library-search"),
                    placeholder = { Text(stringResource(R.string.library_all)) },
                )
            },
            confirmButton = {
                SpoleSecondaryButton(onClick = {
                    onApply(filters.copy(search = typed))
                    searchDialogOpen = false
                }) { Text(stringResource(R.string.library_search)) }
            },
            dismissButton = {
                if (filters.search.isNotBlank()) {
                    SpoleSecondaryButton(onClick = {
                        onApply(filters.copy(search = ""))
                        searchDialogOpen = false
                    }) { Text(stringResource(R.string.library_reset)) }
                } else {
                    SpoleSecondaryButton(onClick = { searchDialogOpen = false }) { Text(stringResource(R.string.library_cancel)) }
                }
            },
        )
    }

    if (genreDialogOpen) {
        val all = stringResource(R.string.library_all)
        SpoleChoiceDialog(stringResource(R.string.library_genre), { genreDialogOpen = false }) {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(listOf("") + facets.genres) { value ->
                        SpoleChoiceRow(value.ifBlank { all }, filters.genre == value) {
                            onApply(filters.copy(genre = value)); genreDialogOpen = false
                        }
                    }
                }
        }
    }

    if (yearDialogOpen) {
        val all = stringResource(R.string.library_all)
        SpoleChoiceDialog(stringResource(R.string.library_year), { yearDialogOpen = false }, icon = SpoleIcons.Calendar) {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(listOf("") + facets.years) { value ->
                        SpoleChoiceRow(value.ifBlank { all }, filters.year == value) {
                            onApply(filters.copy(year = value)); yearDialogOpen = false
                        }
                    }
                }
        }
    }

    if (alphabetDialogOpen) {
        SpoleChoiceDialog(stringResource(R.string.design_alphabet), { alphabetDialogOpen = false }, icon = SpoleIcons.Language) {
            androidx.compose.foundation.lazy.LazyColumn {
                item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Chip(stringResource(R.string.design_all), filters.initial.isBlank(), "letter-all") {
                        onApply(filters.copy(initial = ""))
                        alphabetDialogOpen = false
                    }
                    ("ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ").forEach { letter ->
                        Chip(letter.toString(), filters.initial == letter.toString(), "letter-$letter") {
                            onApply(filters.copy(initial = letter.toString(), sort = LibrarySort.TITLE, descending = false))
                            alphabetDialogOpen = false
                        }
                    }
                }
                }
            }
        }
    }
}
