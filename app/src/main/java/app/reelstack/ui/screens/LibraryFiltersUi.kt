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

private enum class Panel { NONE, FILTER, VIEW, SEARCH, ALPHABET }

/**
 * Filters on the page, not in a popup.
 *
 * The old dialog led with a search field, and a dialog gives focus to its first focusable child —
 * so opening the filters on a television immediately threw the on-screen keyboard over half of
 * them. The inline panel shows one current value per category. Alternatives open only when the
 * user activates that category, without a search field or automatic on-screen keyboard.
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
    val sorts = stringArrayResource(R.array.library_sorts)
    val watched = stringArrayResource(R.array.library_watched)
    val resolutions = stringArrayResource(R.array.library_resolutions)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                FilterChoice(
                    label = stringResource(R.string.library_sort),
                    options = LibrarySort.entries,
                    selected = filters.sort,
                    tag = "sort",
                    name = { sorts[it.ordinal] },
                ) { onApply(filters.copy(sort = it, descending = it != LibrarySort.TITLE)) }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.library_order), style = MaterialTheme.typography.labelSmall, color = Muted)
                    Chip(
                        text = stringResource(if (filters.descending) R.string.library_descending_on else R.string.library_descending_off),
                        chosen = filters.descending,
                        tag = "library-descending",
                    ) { onApply(filters.copy(descending = !filters.descending)) }
                }
            }
            item {
                FilterChoice(
                    label = stringResource(R.string.library_watch_status),
                    options = LibraryWatched.entries,
                    selected = filters.watched,
                    tag = "watched",
                    name = { watched[it.ordinal] },
                ) { onApply(filters.copy(watched = it)) }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.library_favourites), style = MaterialTheme.typography.labelSmall, color = Muted)
                    Chip(
                        text = stringResource(if (filters.favourites) R.string.library_favourites_on else R.string.library_favourites_off),
                        chosen = filters.favourites,
                        tag = "library-favourites",
                    ) { onApply(filters.copy(favourites = !filters.favourites)) }
                }
            }
            item {
                FilterChoice(
                    label = stringResource(R.string.library_resolution),
                    options = LibraryResolution.entries,
                    selected = filters.resolution,
                    tag = "resolution",
                    name = { resolutions[it.ordinal] },
                ) { onApply(filters.copy(resolution = it)) }
            }
            if (facets.genres.isNotEmpty()) {
                item {
                    FacetChip(stringResource(R.string.library_genre), filters.genre, facets.genres) {
                        onApply(filters.copy(genre = it))
                    }
                }
            }
            if (facets.years.isNotEmpty()) {
                item {
                    FacetChip(stringResource(R.string.library_year), filters.year, facets.years) {
                        onApply(filters.copy(year = it))
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.library_display), style = MaterialTheme.typography.labelSmall, color = Muted)
                    Chip(
                        text = stringResource(R.string.library_display),
                        chosen = panel == Panel.VIEW,
                        tag = "library-display-toggle",
                    ) { panel = if (panel == Panel.VIEW) Panel.NONE else Panel.VIEW }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.library_search), style = MaterialTheme.typography.labelSmall, color = Muted)
                    Chip(
                        text = stringResource(R.string.library_search) +
                            filters.search.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty(),
                        chosen = panel == Panel.SEARCH || filters.search.isNotBlank(),
                        tag = "library-search-toggle",
                    ) { panel = if (panel == Panel.SEARCH) Panel.NONE else Panel.SEARCH }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.design_alphabet), style = MaterialTheme.typography.labelSmall, color = Muted)
                    Chip(text = filters.initial.ifBlank { stringResource(R.string.design_alphabet) },
                        chosen = panel == Panel.ALPHABET || filters.initial.isNotBlank(), tag = "library-alphabet") {
                        panel = if (panel == Panel.ALPHABET) Panel.NONE else Panel.ALPHABET
                    }
                }
            }
            if (filters != LibraryFilters()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.library_reset), style = MaterialTheme.typography.labelSmall, color = Muted)
                        Chip(text = stringResource(R.string.library_reset), chosen = false, tag = "library-filter-reset") {
                            onApply(LibraryFilters())
                        }
                    }
                }
            }
        }

        if (panel == Panel.ALPHABET) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Chip(stringResource(R.string.design_all), filters.initial.isBlank(), "letter-all") {
                onApply(filters.copy(initial = ""))
            }
            ("ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ").forEach { letter ->
                Chip(letter.toString(), filters.initial == letter.toString(), "letter-$letter") {
                    onApply(filters.copy(initial = letter.toString(), sort = LibrarySort.TITLE, descending = false))
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
    }
}

/** One current value per filter; the alternatives never push the artwork down the page. */
@Composable
private fun <T : Enum<T>> FilterChoice(label: String, options: List<T>, selected: T, tag: String,
    name: @Composable (T) -> String, onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
        Box {
            Chip(name(selected) + " ▾", false, "library-$tag-choice") { expanded = true }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(name(option)) },
                        leadingIcon = { if (option == selected) Icon(app.reelstack.ui.components.SpoleIcons.Done, null) },
                        modifier = Modifier.testTag("library-$tag-${option.name}"),
                        onClick = { onSelect(option); expanded = false })
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
