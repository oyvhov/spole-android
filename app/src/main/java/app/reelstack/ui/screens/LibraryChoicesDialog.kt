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
    onSave: (Set<String>, List<String>, Map<String, LibraryIcon>) -> Unit) {
    var selected by remember(state.libraryChoices, state.selectedLibraryIds) { mutableStateOf(state.selectedLibraryIds) }
    // A list, not a set: the menu shows these in the order they are given, and the order is the
    // reader's to choose. Appending on pin and removing on unpin keeps a newly pinned library at
    // the end rather than silently back in the server's order.
    var pinned by remember(state.libraryShortcuts) { mutableStateOf(state.libraryShortcuts.map { it.first }) }
    var icons by remember(state.libraryIcons) { mutableStateOf(state.libraryIcons) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 840.dp).fillMaxWidth(.94f).fillMaxHeight(.9f),
            shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.library_manage), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    app.reelstack.ui.components.SpoleSecondaryButton(onClick = { selected = state.libraryChoices.mapTo(mutableSetOf()) { it.id } }) { Text(stringResource(R.string.library_all)) }
                    app.reelstack.ui.components.SpoleSecondaryButton(onClick = { selected = emptySet() }) { Text(stringResource(R.string.library_none)) }
                }
                Text(stringResource(R.string.library_choice_intro), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                if (pinned.size > 1) {
                    Text(stringResource(R.string.library_menu_order), style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp))
                    Column(Modifier.fillMaxWidth().testTag("library-order")) {
                        pinned.forEachIndexed { index, id ->
                            val name = state.libraryChoices.firstOrNull { it.id == id }?.name ?: id
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("library-order-$id"),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                fun move(target: Int) {
                                    pinned = pinned.toMutableList().apply { removeAt(index); add(target, id) }
                                }
                                IconButton(onClick = { move(index - 1) }, enabled = index > 0,
                                    modifier = Modifier.testTag("library-order-up-$id")) {
                                    Icon(SpoleIcons.ChevronUp, stringResource(R.string.tv_move_up, name))
                                }
                                IconButton(onClick = { move(index + 1) }, enabled = index < pinned.lastIndex,
                                    modifier = Modifier.testTag("library-order-down-$id")) {
                                    Icon(SpoleIcons.ChevronDown, stringResource(R.string.tv_move_down, name))
                                }
                            }
                        }
                    }
                }
                LazyVerticalGrid(columns = GridCells.Adaptive(300.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
                    modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.libraryChoicesLoading) item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    state.libraryChoicesError?.let { error -> item(span = { GridItemSpan(maxLineSpan) }) { Column { Text(error); Button(onClick = onRetry) { Text(stringResource(R.string.library_retry)) } } } }
                    items(state.libraryChoices, key = { it.id }) { view ->
                        val icon = icons[view.id] ?: LibraryIcon.forCollection(view.collectionType)
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon.vector(), null, Modifier.padding(start = 10.dp).size(28.dp), tint = MaterialTheme.colorScheme.primary)
                                    Box(Modifier.weight(1f)) {
                                        // No per-card hint. The same sentence under four cards is
                                        // not four explanations, it is one sentence repeated; it
                                        // now sits once at the top where it is read once.
                                        SettingsToggleRow(view.name, "", view.id in selected, "library-choice-${view.id}") {
                                            selected = if (it) selected + view.id else selected - view.id
                                        }
                                    }
                                }
                                if (view.id in selected) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                                        FilterChip(selected = view.id in pinned, onClick = {
                                            pinned = if (view.id in pinned) pinned - view.id else pinned + view.id
                                        }, label = { Text(stringResource(R.string.tv_pin_library)) },
                                            modifier = Modifier.testTag("library-pin-${view.id}"))
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
                    Button(onClick = { onSave(selected, pinned.filter { it in selected }, state.libraryChoices.associate { view ->
                        view.id to (icons[view.id] ?: LibraryIcon.forCollection(view.collectionType)) }) },
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
    app.reelstack.ui.components.SpoleSecondaryButton(onClick = { open = true }, modifier = Modifier.testTag("library-icon")) {
        Icon(selected.vector(), null, Modifier.size(20.dp))
        Text(names[selected.ordinal], Modifier.padding(start = 8.dp))
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(stringResource(R.string.library_icon)) },
        text = { LazyColumn { items(LibraryIcon.entries) { icon ->
            app.reelstack.ui.components.SpoleSecondaryButton(onClick = { onSelect(icon); open = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(icon.vector(), null); Text(names[icon.ordinal], Modifier.weight(1f).padding(start = 16.dp))
            }
        } } }, confirmButton = { app.reelstack.ui.components.SpoleSecondaryButton(onClick = { open = false }) { Text(stringResource(R.string.library_cancel)) } })
}
