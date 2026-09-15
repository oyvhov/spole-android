package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.*

@Composable
internal fun LibraryCustomizationSetting(state: ReelstackUiState, value: Personalization, onChange: (Personalization) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SettingsActionRow(stringResource(R.string.refine_library_edit), stringResource(R.string.refine_library_edit_hint),
        "library-customize") { open = true }
    if (open) LibraryCustomizationDialog(state, value, onChange) { open = false }
}

@Composable
internal fun LibraryCustomizationDialog(state: ReelstackUiState, value: Personalization, onChange: (Personalization) -> Unit,
    onDismiss: () -> Unit) {
    val labels = mapOf("FEATURE" to stringResource(R.string.refine_group_hero),
        "CONTINUE" to stringResource(R.string.home_continue), "NEXT" to stringResource(R.string.tv_next_up),
        "FAVOURITES" to stringResource(R.string.home_favourites), "LIBRARIES" to stringResource(R.string.refine_library_shelves))
    val order = (value.libraryHubOrder + DEFAULT_LIBRARY_HUB).distinct().filter { it in labels }
    val libraries = state.libraryEntries.filter { it.collectionType != null }
    val ids = libraries.map { it.id }
    val ordered = (value.libraryOrder + ids).distinct().filter { it in ids }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 620.dp).fillMaxWidth(.94f).fillMaxHeight(.9f).testTag("library-customize-dialog"), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.refine_library_edit), style = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsToggleRow(stringResource(R.string.refine_library_title), stringResource(R.string.refine_library_title_hint),
                        value.showLibraryTitle, "library-title") { onChange(value.copy(showLibraryTitle = it)) }
                    SettingsToggleRow(stringResource(R.string.refine_library_wide), stringResource(R.string.refine_library_wide_hint),
                        value.libraryCardsWide, "library-wide") { onChange(value.copy(libraryCardsWide = it)) }
                    SettingsGroup(stringResource(R.string.refine_group_rows), stringResource(R.string.refine_order_hint))
                    SettingsToggleRow(stringResource(R.string.library_card_names), stringResource(R.string.library_card_names_hint),
                        value.showLibraryCardNames, "library-card-names") { onChange(value.copy(showLibraryCardNames = it)) }
                    OrderEditor(order, { labels.getValue(it) }, value.libraryHubHidden, prefix = "hub",
                        onOrder = { onChange(value.copy(libraryHubOrder = it)) },
                        onVisible = { id, show -> onChange(value.copy(libraryHubHidden = if (show) value.libraryHubHidden - id else value.libraryHubHidden + id)) })
                    if (ordered.size > 1) {
                        SettingsGroup(stringResource(R.string.refine_library_tiles))
                        OrderEditor(ordered, { id -> libraries.first { it.id == id }.title }, prefix = "library",
                            onOrder = { onChange(value.copy(libraryOrder = it)) })
                    }
                    HomeRowFormats(value, onChange)
                }
                SpoleSecondaryButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_close)) }
            }
        }
    }
}
