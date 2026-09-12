package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.theme.Muted

private enum class Panel { NONE, FILTER, VIEW, SEARCH }

/**
 * Filters on the page, not in a popup.
 *
 * The old dialog led with a search field, and a dialog gives focus to its first focusable child —
 * so opening the filters on a television immediately threw the on-screen keyboard over half of
 * them. Inline, nothing steals focus, the whole set is visible at once, and a D-pad reaches any of
 * it without opening anything first.
 *
 * Each choice applies immediately. A draft with Apply and Cancel made sense when the options were
 * hidden behind a modal; with the grid right there, the result is the confirmation.
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
    // One toolbar owning both panels: two chips of the same kind on the same line, and only one
    // panel open at a time. Two different button styles on two lines made the tools louder than
    // the library they sit above.
    var panel by remember { mutableStateOf(Panel.NONE) }
    val open = panel == Panel.FILTER
    val sorts = stringArrayResource(R.array.library_sorts)
    val watched = stringArrayResource(R.array.library_watched)
    val resolutions = stringArrayResource(R.array.library_resolutions)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(
                text = stringResource(R.string.library_filters) +
                    (if (filters.activeCount > 0) " ${filters.activeCount}" else "") +
                    " · " + sorts[filters.sort.ordinal],
                chosen = panel == Panel.FILTER,
                tag = "library-filters",
            ) { panel = if (panel == Panel.FILTER) Panel.NONE else Panel.FILTER }
            Chip(
                text = stringResource(R.string.library_display),
                chosen = panel == Panel.VIEW,
                tag = "library-display-toggle",
            ) { panel = if (panel == Panel.VIEW) Panel.NONE else Panel.VIEW }
            Chip(
                text = stringResource(R.string.library_search) +
                    filters.search.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty(),
                chosen = panel == Panel.SEARCH || filters.search.isNotBlank(),
                tag = "library-search-toggle",
            ) { panel = if (panel == Panel.SEARCH) Panel.NONE else Panel.SEARCH }
            if (filters != LibraryFilters()) {
                Chip(text = stringResource(R.string.library_reset), chosen = false, tag = "library-filter-reset") {
                    onApply(LibraryFilters())
                }
            }
        }

        if (panel == Panel.VIEW) LibraryDisplayPanel(display, onDisplayChange)

        if (panel == Panel.SEARCH) {
            // Typed locally and sent on Enter or when the field is left. Applying per keystroke
            // would reload the whole library once per character. Opened deliberately, so no
            // keyboard appears until it is asked for.
            var typed by remember(filters.search) { mutableStateOf(filters.search) }
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.take(150) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { if (typed != filters.search) onApply(filters.copy(search = typed)) },
                ),
                modifier = Modifier.widthIn(min = 220.dp, max = 420.dp)
                    .onFocusChanged { if (!it.isFocused && typed != filters.search) onApply(filters.copy(search = typed)) }
                    .testTag("library-search"),
                placeholder = { Text(stringResource(R.string.library_all)) },
            )
        }

        if (open) FlowRow(
            Modifier.fillMaxWidth().testTag("library-filter-panel"),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Segments(
                label = stringResource(R.string.library_sort),
                options = LibrarySort.entries,
                selected = filters.sort,
                tag = "sort",
                name = { sorts[it.ordinal] },
            ) { onApply(filters.copy(sort = it, descending = it != LibrarySort.TITLE)) }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.library_order), style = MaterialTheme.typography.labelSmall, color = Muted)
                Chip(
                    text = stringResource(if (filters.descending) R.string.library_descending_on else R.string.library_descending_off),
                    chosen = filters.descending,
                    tag = "library-descending",
                ) { onApply(filters.copy(descending = !filters.descending)) }
            }

            Segments(
                label = stringResource(R.string.library_watch_status),
                options = LibraryWatched.entries,
                selected = filters.watched,
                tag = "watched",
                name = { watched[it.ordinal] },
            ) { onApply(filters.copy(watched = it)) }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.library_favourites), style = MaterialTheme.typography.labelSmall, color = Muted)
                Chip(
                    text = stringResource(if (filters.favourites) R.string.library_favourites_on else R.string.library_favourites_off),
                    chosen = filters.favourites,
                    tag = "library-favourites",
                ) { onApply(filters.copy(favourites = !filters.favourites)) }
            }

            Segments(
                label = stringResource(R.string.library_resolution),
                options = LibraryResolution.entries,
                selected = filters.resolution,
                tag = "resolution",
                name = { resolutions[it.ordinal] },
            ) { onApply(filters.copy(resolution = it)) }

            // Genre and year can run to hundreds of values, so those stay a chosen-from list. The
            // list opens only when asked for, which is the difference that matters.
            if (facets.genres.isNotEmpty()) {
                FacetChip(stringResource(R.string.library_genre), filters.genre, facets.genres) {
                    onApply(filters.copy(genre = it))
                }
            }
            if (facets.years.isNotEmpty()) {
                FacetChip(stringResource(R.string.library_year), filters.year, facets.years) {
                    onApply(filters.copy(year = it))
                }
            }
        }
    }
}

/**
 * One long list of values behind one chip.
 *
 * Opened deliberately and never given focus on its own, so this cannot summon a keyboard the way
 * the old filter dialog did.
 */
@Composable
private fun FacetChip(label: String, selected: String, values: List<String>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val all = stringResource(R.string.library_all)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
        Chip(
            text = selected.ifBlank { all },
            chosen = selected.isNotBlank(),
            tag = "library-facet-$label",
        ) { open = true }
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(label) },
        text = {
            LazyColumn {
                items(listOf("") + values) { value ->
                    app.reelstack.ui.components.SpoleSecondaryButton(
                        onClick = { onSelect(value); open = false },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(value.ifBlank { all }) }
                }
            }
        },
        confirmButton = {
            app.reelstack.ui.components.SpoleSecondaryButton(onClick = { open = false }) { Text(stringResource(R.string.library_cancel)) }
        },
    )
}
