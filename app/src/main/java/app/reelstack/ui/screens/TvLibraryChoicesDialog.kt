package app.reelstack.ui.screens

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reelstack.R
import app.reelstack.data.model.LibraryIcon
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.*

/** Library visibility on the left; only the focused library's menu options on the right. */
@Composable
internal fun TvLibraryChoicesDialog(state: ReelstackUiState, onDismiss: () -> Unit, onRetry: () -> Unit,
    onSave: (Set<String>, List<String>, Map<String, LibraryIcon>) -> Unit) {
    var selected by remember(state.libraryChoices, state.selectedLibraryIds) { mutableStateOf(state.selectedLibraryIds) }
    var pinned by remember(state.libraryShortcuts) { mutableStateOf(state.libraryShortcuts.map { it.first }) }
    var icons by remember(state.libraryIcons) { mutableStateOf(state.libraryIcons) }
    var activeId by remember { mutableStateOf(state.libraryChoices.firstOrNull()?.id) }
    val active = state.libraryChoices.firstOrNull { it.id == activeId } ?: state.libraryChoices.firstOrNull()
    val listFocus = remember { FocusRequester() }
    LaunchedEffect(state.libraryChoices.isNotEmpty()) {
        if (state.libraryChoices.isNotEmpty()) { withFrameNanos { }; listFocus.requestFocus() }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.92f).fillMaxHeight(.9f), shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.library_manage), style = MaterialTheme.typography.headlineSmall)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    LazyColumn(Modifier.weight(1.2f).fillMaxHeight().focusRequester(listFocus).focusGroup()
                        .testTag("tv-library-choices"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Text(stringResource(R.string.library_choice_intro),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SpoleSecondaryButton(onClick = { selected = state.libraryChoices.mapTo(mutableSetOf()) { it.id } }) {
                                    Text(stringResource(R.string.library_all))
                                }
                                SpoleSecondaryButton(onClick = { selected = emptySet() }) { Text(stringResource(R.string.library_none)) }
                            }
                        }
                        if (state.libraryChoicesLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        state.libraryChoicesError?.let { error -> item {
                            Text(error); SpoleSecondaryButton(onClick = onRetry) { Text(stringResource(R.string.library_retry)) }
                        } }
                        items(state.libraryChoices, key = { it.id }) { library ->
                            Box(Modifier.onFocusChanged { if (it.hasFocus) activeId = library.id }.focusGroup()) {
                                SettingsToggleRow(library.name, "", library.id in selected, "library-choice-${library.id}") {
                                    selected = if (it) selected + library.id else selected - library.id
                                }
                            }
                        }
                    }
                    Surface(Modifier.weight(1f).fillMaxHeight(), shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            active?.let { library ->
                                val icon = icons[library.id] ?: LibraryIcon.forCollection(library.collectionType)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(icon.vector(), null, Modifier.size(28.dp))
                                    Text(library.name, style = MaterialTheme.typography.titleLarge)
                                }
                                if (library.id in selected) {
                                    SettingsToggleRow(stringResource(R.string.tv_pin_library), "", library.id in pinned,
                                        "library-pin-${library.id}") {
                                        pinned = if (it) pinned + library.id else pinned - library.id
                                    }
                                    if (library.id in pinned) {
                                        LibraryIconPicker(icon) { icons = icons + (library.id to it) }
                                        if (pinned.size > 1) {
                                        val index = pinned.indexOf(library.id)
                                        Text(stringResource(R.string.library_menu_order), style = MaterialTheme.typography.titleSmall)
                                        fun move(to: Int) { pinned = pinned.toMutableList().apply { removeAt(index); add(to, library.id) } }
                                        SpoleSecondaryButton(onClick = { move(index - 1) }, enabled = index > 0,
                                            modifier = Modifier.fillMaxWidth().testTag("library-order-up-${library.id}")) {
                                            Icon(SpoleIcons.ChevronUp, null, Modifier.size(20.dp))
                                            Text(stringResource(R.string.tv_move_up, library.name), Modifier.padding(start = 8.dp))
                                        }
                                        SpoleSecondaryButton(onClick = { move(index + 1) }, enabled = index < pinned.lastIndex,
                                            modifier = Modifier.fillMaxWidth().testTag("library-order-down-${library.id}")) {
                                            Icon(SpoleIcons.ChevronDown, null, Modifier.size(20.dp))
                                            Text(stringResource(R.string.tv_move_down, library.name), Modifier.padding(start = 8.dp))
                                        }
                                        }
                                    }
                                } else Text(stringResource(R.string.library_none_help), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(androidx.compose.ui.res.pluralStringResource(R.plurals.library_selected_count, selected.size, selected.size),
                        Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    SpoleSecondaryButton(onClick = onDismiss) { Text(stringResource(R.string.library_cancel)) }
                    Button(onClick = { onSave(selected, pinned.filter { it in selected }, state.libraryChoices.associate {
                        it.id to (icons[it.id] ?: LibraryIcon.forCollection(it.collectionType)) }) },
                        enabled = !state.libraryChoicesLoading && state.libraryChoicesError == null,
                        modifier = Modifier.testTag("library-selection-save")) { Text(stringResource(R.string.library_save)) }
                }
            }
        }
    }
}
