package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
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

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 1. Sortering
        Box {
            SpoleSecondaryButton(
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
                    leadingIcon = if (filters.sort == LibrarySort.TITLE && !filters.descending) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-TITLE"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.TITLE, descending = false)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_title_desc)) },
                    leadingIcon = if (filters.sort == LibrarySort.TITLE && filters.descending) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onApply(filters.copy(sort = LibrarySort.TITLE, descending = true)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_added_desc)) },
                    leadingIcon = if (filters.sort == LibrarySort.ADDED) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-ADDED"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.ADDED, descending = true)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_year_desc)) },
                    leadingIcon = if (filters.sort == LibrarySort.YEAR) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-YEAR"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.YEAR, descending = true)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_rating_desc)) },
                    leadingIcon = if (filters.sort == LibrarySort.RATING) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-RATING"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.RATING, descending = true)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_runtime_desc)) },
                    leadingIcon = if (filters.sort == LibrarySort.RUNTIME) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-RUNTIME"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.RUNTIME, descending = true)); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_sort_played_desc)) },
                    leadingIcon = if (filters.sort == LibrarySort.PLAYED) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-sort-PLAYED"),
                    onClick = { onApply(filters.copy(sort = LibrarySort.PLAYED, descending = true)); sortOpen = false },
                )
            }
        }

        // 2. Visingsstatus
        Box {
            SpoleSecondaryButton(
                onClick = { statusOpen = true },
                modifier = Modifier.testTag("library-watched-choice"),
            ) {
                Text(watched[filters.watched.ordinal])
                Icon(SpoleIcons.ChevronDown, null, modifier = Modifier.size(16.dp).padding(start = 4.dp))
            }
            DropdownMenu(
                expanded = statusOpen,
                onDismissRequest = { statusOpen = false },
                containerColor = SurfaceRaised,
            ) {
                LibraryWatched.entries.forEach { w ->
                    DropdownMenuItem(
                        text = { Text(watched[w.ordinal]) },
                        leadingIcon = if (filters.watched == w) {
                            { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.testTag("library-watched-${w.name}"),
                        onClick = { onApply(filters.copy(watched = w)); statusOpen = false },
                    )
                }
            }
        }

        // 3. Oppløysing
        Box {
            SpoleSecondaryButton(
                onClick = { resolutionOpen = true },
                modifier = Modifier.testTag("library-resolution-choice"),
            ) {
                Text(resolutions[filters.resolution.ordinal])
                Icon(SpoleIcons.ChevronDown, null, modifier = Modifier.size(16.dp).padding(start = 4.dp))
            }
            DropdownMenu(
                expanded = resolutionOpen,
                onDismissRequest = { resolutionOpen = false },
                containerColor = SurfaceRaised,
            ) {
                LibraryResolution.entries.forEach { res ->
                    DropdownMenuItem(
                        text = { Text(resolutions[res.ordinal]) },
                        leadingIcon = if (filters.resolution == res) {
                            { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.testTag("library-resolution-${res.name}"),
                        onClick = { onApply(filters.copy(resolution = res)); resolutionOpen = false },
                    )
                }
            }
        }

        // 4. Filter (Flere filter)
        Box {
            SpoleSecondaryButton(
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
                    stringResource(R.string.library_filters) + if (filters.activeCount > 0) " (${filters.activeCount})" else "",
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
                    text = { Text(stringResource(R.string.library_favourites)) },
                    leadingIcon = if (filters.favourites) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.testTag("library-favourites"),
                    onClick = { onApply(filters.copy(favourites = !filters.favourites)); filterMenuOpen = false },
                )
                if (facets.genres.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.library_genre) + filters.genre.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty())
                        },
                        leadingIcon = if (filters.genre.isNotBlank()) {
                            { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                        } else null,
                        onClick = { filterMenuOpen = false; genreDialogOpen = true },
                    )
                }
                if (facets.years.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.library_year) + filters.year.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty())
                        },
                        leadingIcon = if (filters.year.isNotBlank()) {
                            { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                        } else null,
                        onClick = { filterMenuOpen = false; yearDialogOpen = true },
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.design_alphabet) + filters.initial.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty())
                    },
                    leadingIcon = if (filters.initial.isNotBlank()) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
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
            SpoleSecondaryButton(
                onClick = { displayOpen = true },
                modifier = Modifier.testTag("library-display-toggle"),
            ) {
                Icon(SpoleIcons.ListLines, null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                Text(stringResource(R.string.library_display))
                Icon(SpoleIcons.ChevronDown, null, modifier = Modifier.size(16.dp).padding(start = 4.dp))
            }
            DropdownMenu(
                expanded = displayOpen,
                onDismissRequest = { displayOpen = false },
                containerColor = SurfaceRaised,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_view_grid)) },
                    leadingIcon = if (display.view == LibraryView.GRID) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onDisplayChange(display.copy(view = LibraryView.GRID)); displayOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_view_list)) },
                    leadingIcon = if (display.view == LibraryView.LIST) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onDisplayChange(display.copy(view = LibraryView.LIST)); displayOpen = false },
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_size_small)) },
                    leadingIcon = if (display.size == LibraryCardSize.SMALL) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onDisplayChange(display.copy(size = LibraryCardSize.SMALL)); displayOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_size_medium)) },
                    leadingIcon = if (display.size == LibraryCardSize.MEDIUM) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onDisplayChange(display.copy(size = LibraryCardSize.MEDIUM)); displayOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_size_large)) },
                    leadingIcon = if (display.size == LibraryCardSize.LARGE) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onDisplayChange(display.copy(size = LibraryCardSize.LARGE)); displayOpen = false },
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                DropdownMenuItem(
                    text = { Text(stringResource(if (display.showTitles) R.string.library_titles_on else R.string.library_titles_off)) },
                    leadingIcon = if (display.showTitles) {
                        { Icon(SpoleIcons.Done, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onDisplayChange(display.copy(showTitles = !display.showTitles)); displayOpen = false },
                )
            }
        }

        // 6. Søk
        SpoleSecondaryButton(
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
                stringResource(R.string.library_search) + filters.search.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty(),
                color = if (filters.search.isNotBlank()) Primary else Color.Unspecified,
            )
        }

        // 7. Nullstill hurtigknapp
        if (filters.activeCount > 0) {
            SpoleSecondaryButton(
                onClick = { onApply(LibraryFilters(sort = filters.sort, descending = filters.descending)) },
                modifier = Modifier.testTag("library-filter-reset"),
            ) {
                Icon(SpoleIcons.Close, null, modifier = Modifier.size(16.dp).padding(end = 4.dp), tint = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.library_reset), color = MaterialTheme.colorScheme.error)
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
        AlertDialog(
            onDismissRequest = { genreDialogOpen = false },
            title = { Text(stringResource(R.string.library_genre)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(listOf("") + facets.genres) { value ->
                        SpoleSecondaryButton(
                            onClick = { onApply(filters.copy(genre = value)); genreDialogOpen = false },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text(value.ifBlank { all }) }
                    }
                }
            },
            confirmButton = {
                SpoleSecondaryButton(onClick = { genreDialogOpen = false }) { Text(stringResource(R.string.library_cancel)) }
            },
        )
    }

    if (yearDialogOpen) {
        val all = stringResource(R.string.library_all)
        AlertDialog(
            onDismissRequest = { yearDialogOpen = false },
            title = { Text(stringResource(R.string.library_year)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(listOf("") + facets.years) { value ->
                        SpoleSecondaryButton(
                            onClick = { onApply(filters.copy(year = value)); yearDialogOpen = false },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text(value.ifBlank { all }) }
                    }
                }
            },
            confirmButton = {
                SpoleSecondaryButton(onClick = { yearDialogOpen = false }) { Text(stringResource(R.string.library_cancel)) }
            },
        )
    }

    if (alphabetDialogOpen) {
        AlertDialog(
            onDismissRequest = { alphabetDialogOpen = false },
            title = { Text(stringResource(R.string.design_alphabet)) },
            text = {
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
            },
            confirmButton = {
                SpoleSecondaryButton(onClick = { alphabetDialogOpen = false }) { Text(stringResource(R.string.library_cancel)) }
            },
        )
    }
}
