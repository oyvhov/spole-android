package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.components.SettingsToggleRow
import app.reelstack.ui.components.SettingsActionRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
internal fun LibraryFilterBar(filters: LibraryFilters, onApply: (LibraryFilters) -> Unit, facets: LibraryFacets = LibraryFacets()) {
    var open by remember { mutableStateOf(false) }
    val sorts = stringArrayResource(R.array.library_sorts)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.testTag("library-filters")) { Text(stringResource(R.string.library_filters) +
            (if (filters.activeCount > 0) " (${filters.activeCount})" else "") + " · " + sorts[filters.sort.ordinal]) }
        if (filters != LibraryFilters()) TextButton(onClick = { onApply(LibraryFilters()) }) { Text(stringResource(R.string.library_reset)) }
    }
    if (open) LibraryFiltersDialog(filters, facets, { open = false }) { onApply(it); open = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryFiltersDialog(initial: LibraryFilters, facets: LibraryFacets, close: () -> Unit, apply: (LibraryFilters) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    val sorts = stringArrayResource(R.array.library_sorts)
    val watched = stringArrayResource(R.array.library_watched)
    val resolutions = stringArrayResource(R.array.library_resolutions)
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 760.dp).fillMaxWidth(.94f).fillMaxHeight(.9f), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.library_filters), style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(draft.search, { draft = draft.copy(search = it.take(150)) }, label = { Text(stringResource(R.string.library_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.library_sort), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LibrarySort.entries.forEach { sort ->
                        FilterChip(draft.sort == sort, { draft = draft.copy(sort = sort, descending = sort != LibrarySort.TITLE) }, label = { Text(sorts[sort.ordinal]) })
                    } }
                    SettingsToggleRow(stringResource(R.string.library_descending), "", draft.descending, "library-descending") { draft = draft.copy(descending = it) }
                    Text(stringResource(R.string.library_watch_status), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LibraryWatched.entries.forEach { value ->
                        FilterChip(draft.watched == value, { draft = draft.copy(watched = value) }, label = { Text(watched[value.ordinal]) })
                    } }
                    SettingsToggleRow(stringResource(R.string.library_favourites), "", draft.favourites, "library-favourites") { draft = draft.copy(favourites = it) }
                    Text(stringResource(R.string.library_resolution), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LibraryResolution.entries.forEach { value ->
                        FilterChip(draft.resolution == value, { draft = draft.copy(resolution = value) }, label = { Text(resolutions[value.ordinal]) })
                    } }
                    if (facets.genres.isNotEmpty()) LibraryFacetChoice(stringResource(R.string.library_genre), draft.genre, facets.genres) { draft = draft.copy(genre = it) }
                    else OutlinedTextField(draft.genre, { draft = draft.copy(genre = it.take(100)) }, label = { Text(stringResource(R.string.library_genre)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (facets.years.isNotEmpty()) LibraryFacetChoice(stringResource(R.string.library_year), draft.year, facets.years) { draft = draft.copy(year = it) }
                    else OutlinedTextField(draft.year, { draft = draft.copy(year = it.filter(Char::isDigit).take(4)) }, label = { Text(stringResource(R.string.library_year)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        isError = draft.year.isNotEmpty() && draft.year.toIntOrNull() !in 1800..2200)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { apply(draft) }, enabled = draft.year.isBlank() || draft.year.toIntOrNull() in 1800..2200, modifier = Modifier.testTag("library-filter-apply")) { Text(stringResource(R.string.library_apply)) }
                    TextButton(onClick = { draft = LibraryFilters() }) { Text(stringResource(R.string.library_reset)) }
                    TextButton(onClick = close) { Text(stringResource(R.string.library_cancel)) }
                }
            }
        }
    }
}

@Composable
private fun LibraryFacetChoice(label: String, selected: String, values: List<String>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val all = stringResource(R.string.library_all)
    SettingsActionRow(label, selected.ifBlank { all }, "library-facet-$label") { open = true }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(label) },
        text = { LazyColumn { items(listOf("") + values) { value ->
            TextButton(onClick = { onSelect(value); open = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(value.ifBlank { all }) }
        } } }, confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.library_cancel)) } })
}
