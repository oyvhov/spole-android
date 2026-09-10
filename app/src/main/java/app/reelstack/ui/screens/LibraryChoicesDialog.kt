package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reelstack.R
import app.reelstack.data.model.LibraryIcon
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibraryChoicesDialog(state: ReelstackUiState, onDismiss: () -> Unit, onRetry: () -> Unit,
    onSave: (Set<String>, Set<String>, Map<String, LibraryIcon>) -> Unit) {
    var selected by remember(state.libraryChoices, state.selectedLibraryIds) { mutableStateOf(state.selectedLibraryIds) }
    var pinned by remember(state.libraryShortcuts) { mutableStateOf(state.libraryShortcuts.map { it.first }.toSet()) }
    var icons by remember(state.libraryIcons) { mutableStateOf(state.libraryIcons) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 840.dp).fillMaxWidth(.94f).fillMaxHeight(.9f),
            shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.library_manage), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = { selected = state.libraryChoices.mapTo(mutableSetOf()) { it.id } }) { Text(stringResource(R.string.library_all)) }
                    TextButton(onClick = { selected = emptySet() }) { Text(stringResource(R.string.library_none)) }
                }
                Text(stringResource(R.string.library_choice_intro), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                LazyVerticalGrid(columns = GridCells.Adaptive(300.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
                    modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.libraryChoicesLoading) item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    state.libraryChoicesError?.let { error -> item(span = { GridItemSpan(maxLineSpan) }) { Column { Text(error); Button(onClick = onRetry) { Text(stringResource(R.string.library_retry)) } } } }
                    items(state.libraryChoices, key = { it.id }) { view ->
                        val icon = icons[view.id] ?: when(view.collectionType) { "movies" -> LibraryIcon.MOVIES; "tvshows" -> LibraryIcon.SERIES; "music" -> LibraryIcon.MUSIC; else -> LibraryIcon.LIBRARY }
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon.vector(), null, Modifier.padding(start = 10.dp).size(28.dp), tint = MaterialTheme.colorScheme.primary)
                                    Box(Modifier.weight(1f)) {
                                        SettingsToggleRow(view.name, stringResource(R.string.library_include_hint), view.id in selected, "library-choice-${view.id}") {
                                            selected = if (it) selected + view.id else selected - view.id
                                        }
                                    }
                                }
                                if (view.id in selected) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                                        FilterChip(selected = view.id in pinned, onClick = {
                                            pinned = if (view.id in pinned) pinned - view.id else pinned + view.id
                                        }, label = { Text(stringResource(R.string.tv_pin_library)) }, modifier = Modifier.testTag("library-pin-${view.id}"))
                                        if (view.id in pinned) LibraryIconPicker(icon) { icons = icons + (view.id to it) }
                                    }
                                }
                            }
                        }
                    }
                    if (selected.isEmpty() && !state.libraryChoicesLoading) item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.library_none_help)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text(androidx.compose.ui.res.pluralStringResource(R.plurals.library_selected_count, selected.size, selected.size), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.library_cancel)) }
                    Button(onClick = { onSave(selected, pinned.intersect(selected), state.libraryChoices.associate { view ->
                        view.id to (icons[view.id] ?: when(view.collectionType) { "movies" -> LibraryIcon.MOVIES; "tvshows" -> LibraryIcon.SERIES; "music" -> LibraryIcon.MUSIC; else -> LibraryIcon.LIBRARY }) }) },
                        enabled = !state.libraryChoicesLoading && state.libraryChoicesError == null,
                        modifier = Modifier.testTag("library-selection-save")) { Text(stringResource(R.string.library_save)) }
                }
            }
        }
    }
}

@Composable
private fun LibraryIconPicker(selected: LibraryIcon, onSelect: (LibraryIcon) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val names = stringArrayResource(R.array.library_icons)
    TextButton(onClick = { open = true }, modifier = Modifier.testTag("library-icon")) {
        Icon(selected.vector(), null, Modifier.size(20.dp))
        Text(names[selected.ordinal], Modifier.padding(start = 8.dp))
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(stringResource(R.string.library_icon)) },
        text = { LazyColumn { items(LibraryIcon.entries) { icon ->
            TextButton(onClick = { onSelect(icon); open = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(icon.vector(), null); Text(names[icon.ordinal], Modifier.weight(1f).padding(start = 16.dp))
            }
        } } }, confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.library_cancel)) } })
}
