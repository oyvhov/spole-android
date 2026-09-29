package app.reelstack.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.*
import app.reelstack.ui.theme.LocalPersonalization

/** The single entry in settings for everything about the library page, as «Tilpass framsida» is for Home. */
@Composable
internal fun LibraryCustomizationSetting(onOpen: () -> Unit) {
    SettingsActionRow(stringResource(R.string.refine_library_edit), stringResource(R.string.refine_library_edit_hint),
        "library-customize", SpoleIcons.Library, onOpen)
}

/**
 * «Tilpass biblioteksida»: which libraries the page shows, in what order, which of them also sit in
 * the menu, then the page's own rows and look. It used to be three places — «Vel bibliotek», a
 * dialog for rows and tiles, and a switch for the whole overview — and two of them could hide a
 * library, each in its own way.
 *
 * Rows, order and look are saved at once, like every other choice in Spole. The library selection
 * reloads the library, so it is saved once, when the editor closes — however it is closed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibraryEditorDialog(state: ReelstackUiState, onRetry: () -> Unit, onDismiss: () -> Unit,
    onSave: (Set<String>, List<String>, Map<String, LibraryIcon>) -> Unit,
    options: Personalization = LocalPersonalization.current, onChange: ((Personalization) -> Unit)? = null) {
    val context = LocalContext.current
    val preferences = remember(context) { AppPreferencesRepository(context.applicationContext) }
    val change: (Personalization) -> Unit = onChange ?: { preferences.personalization = it }
    val choices = state.libraryChoices
    val choiceIds = choices.map { it.id }
    val ordered = (options.libraryOrder + choiceIds).distinct().filter { it in choiceIds }
    val shortcuts = state.libraryShortcuts.map { it.first }
    // A library hidden from this page alone, before the two were one choice, starts out switched
    // off here. Closing the editor then saves it that way and the old list is emptied.
    var selected by remember(choices, state.selectedLibraryIds) {
        mutableStateOf(state.selectedLibraryIds - options.libraryHidden)
    }
    var pinned by remember(state.libraryShortcuts) { mutableStateOf(shortcuts.toSet()) }
    var icons by remember(state.libraryIcons) { mutableStateOf(state.libraryIcons) }
    var iconsChanged by remember { mutableStateOf(false) }
    var reordered by remember { mutableStateOf(false) }
    // Shortcuts are a menu the reader may have arranged before the order was shared with the page.
    // Until a library is moved here, that arrangement stands and a new shortcut joins at the end.
    val menu = (if (reordered) ordered else shortcuts + ordered).distinct().filter { it in pinned && it in selected }
    val policy = windowLayoutPolicy()
    // Only the full sidebar on a television or a tablet carries library shortcuts.
    val menuShortcuts = isTelevision() || (policy.useNavigationRail && !policy.useCompactTouchRail)
    val close = {
        val ready = choices.isNotEmpty() && !state.libraryChoicesLoading && state.libraryChoicesError == null
        val changed = selected != state.selectedLibraryIds || menu != shortcuts || iconsChanged
        if (ready && changed) {
            if (options.libraryHidden.isNotEmpty()) change(options.copy(libraryHidden = emptySet()))
            onSave(selected.intersect(choiceIds.toSet()), menu,
                choices.associate { it.id to (icons[it.id] ?: LibraryIcon.forCollection(it.collectionType)) })
        } else onDismiss()
    }
    val hubLabels = mapOf("FEATURE" to stringResource(R.string.refine_group_hero),
        "CONTINUE" to stringResource(R.string.home_continue), "NEXT" to stringResource(R.string.tv_next_up),
        "FAVOURITES" to stringResource(R.string.home_favourites), "LIBRARIES" to stringResource(R.string.refine_library_shelves))
    val hubOrder = (options.libraryHubOrder + DEFAULT_LIBRARY_HUB).distinct().filter { it in hubLabels }
    val connection = state.libraryConnection
    val hidden = stringResource(R.string.home_layout_hidden)
    val inMenu = stringResource(R.string.library_editor_in_menu)
    val addToMenu = stringResource(R.string.library_editor_add_to_menu)
    // A remote has nothing to start from. Without a first focus the first press found «Ferdig»
    // under the list, and every press after it stayed there.
    val firstFocus = remember { FocusRequester() }
    val television = isTelevision()
    // The first row the list shows: a library once they have arrived, else the page's first row —
    // but not while the libraries are still on their way, or focus would jump when they land.
    val firstRow = ordered.firstOrNull()?.let { "library-$it" }
        ?: hubOrder.firstOrNull()?.takeIf { !state.libraryChoicesLoading }?.let { "hub-$it" }
    LaunchedEffect(firstRow) {
        if (television && firstRow != null) { withFrameNanos { }; runCatching { firstFocus.requestFocus() } }
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 720.dp).fillMaxWidth(.94f).fillMaxHeight(.92f).testTag("library-customize-dialog"),
            shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.refine_library_edit), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.library_editor_intro), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("library-editor-list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (connection != null) {
                        item(key = "libraries-heading") {
                            SettingsGroup(stringResource(R.string.refine_library_tiles), connection.kind.displayName)
                        }
                        if (state.libraryChoicesLoading) item(key = "libraries-loading") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                                Text(stringResource(R.string.home_layout_libraries_loading), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        state.libraryChoicesError?.let { error -> item(key = "libraries-error") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(error)
                                SpoleSecondaryButton(onClick = onRetry, modifier = Modifier.testTag("library-editor-retry")) {
                                    Text(stringResource(R.string.home_layout_retry))
                                }
                            }
                        } }
                        itemsIndexed(ordered, key = { _, id -> "library-$id" }) { index, id ->
                            val view = choices.first { it.id == id }
                            val shown = id in selected
                            val icon = icons[id] ?: LibraryIcon.forCollection(view.collectionType)
                            val focus = remember(id) { listOf(FocusRequester(), FocusRequester()) }
                            val menuOptions: (@Composable RowScope.() -> Unit)? = if (!shown || !menuShortcuts) null else { {
                                FlowRow(Modifier.weight(1f, fill = false), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // The words say the state: a tick and «I sidemenyen», or a plus and the offer.
                                    val inSidebar = id in pinned
                                    val pinInteraction = remember { MutableInteractionSource() }
                                    TextButton(onClick = { pinned = if (inSidebar) pinned - id else pinned + id },
                                        interactionSource = pinInteraction,
                                        modifier = Modifier.focusOutline(pinInteraction, CircleShape).testTag("library-pin-$id")) {
                                        Icon(if (inSidebar) SpoleIcons.Done else SpoleIcons.Add, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (inSidebar) inMenu else addToMenu, style = MaterialTheme.typography.labelLarge)
                                    }
                                    if (inSidebar) LibraryIconPicker(icon) { icons = icons + (id to it); iconsChanged = true }
                                }
                            } }
                            LayoutEditorRow(
                                prefix = "library", id = id, title = view.name,
                                subtitle = if (shown) "" else hidden,
                                visible = shown, canMoveUp = index > 0, canMoveDown = index < ordered.lastIndex,
                                upFocus = focus[0], downFocus = focus[1],
                                onVisibleChange = { selected = if (it) selected + id else selected - id },
                                onMove = { direction ->
                                    val destination = index + direction
                                    focus[arrowAfterMove(destination, ordered.lastIndex, direction)].requestFocus()
                                    reordered = true
                                    change(options.copy(libraryOrder = ordered.toMutableList().apply { removeAt(index); add(destination, id) }))
                                },
                                leading = { Icon(icon.vector(), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                options = menuOptions,
                                switchFocus = firstFocus.takeIf { firstRow == "library-$id" },
                            )
                        }
                    }
                    item(key = "rows-heading") { SettingsGroup(stringResource(R.string.refine_group_rows)) }
                    itemsIndexed(hubOrder, key = { _, id -> "hub-$id" }) { index, id ->
                        val shown = id !in options.libraryHubHidden
                        val focus = remember(id) { listOf(FocusRequester(), FocusRequester()) }
                        LayoutEditorRow(
                            prefix = "hub", id = id, title = hubLabels.getValue(id), subtitle = if (shown) "" else hidden,
                            visible = shown, canMoveUp = index > 0, canMoveDown = index < hubOrder.lastIndex,
                            upFocus = focus[0], downFocus = focus[1],
                            switchFocus = firstFocus.takeIf { firstRow == "hub-$id" },
                            onVisibleChange = { show ->
                                change(options.copy(libraryHubHidden = if (show) options.libraryHubHidden - id else options.libraryHubHidden + id))
                            },
                            onMove = { direction ->
                                val destination = index + direction
                                focus[arrowAfterMove(destination, hubOrder.lastIndex, direction)].requestFocus()
                                change(options.copy(libraryHubOrder = hubOrder.toMutableList().apply { removeAt(index); add(destination, id) }))
                            },
                        )
                    }
                    item(key = "display") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            SettingsGroup(stringResource(R.string.library_editor_display))
                            SettingsToggleRow(stringResource(R.string.refine_library_wide), stringResource(R.string.refine_library_wide_hint),
                                options.libraryCardsWide, "library-wide") { change(options.copy(libraryCardsWide = it)) }
                            SettingsToggleRow(stringResource(R.string.library_card_names), stringResource(R.string.library_card_names_hint),
                                options.showLibraryCardNames, "library-card-names") { change(options.copy(showLibraryCardNames = it)) }
                            SettingsToggleRow(stringResource(R.string.refine_library_title), stringResource(R.string.refine_library_title_hint),
                                options.showLibraryTitle, "library-title") { change(options.copy(showLibraryTitle = it)) }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    val doneInteraction = remember { MutableInteractionSource() }
                    Button(onClick = close, interactionSource = doneInteraction,
                        modifier = Modifier.focusOutline(doneInteraction, CircleShape).testTag("library-editor-done")) {
                        Text(stringResource(R.string.home_order_done))
                    }
                }
            }
        }
    }
}

@Composable
internal fun LibraryIconPicker(selected: LibraryIcon, onSelect: (LibraryIcon) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val names = stringArrayResource(R.array.library_icons)
    SpoleSecondaryButton(onClick = { open = true }, modifier = Modifier.testTag("library-icon")) {
        Icon(selected.vector(), null, Modifier.size(20.dp))
        Text(names[selected.ordinal], Modifier.padding(start = 8.dp))
    }
    if (open) SpoleChoiceDialog(stringResource(R.string.library_icon), { open = false }, icon = selected.vector()) {
        LazyColumn { items(LibraryIcon.entries) { icon ->
            SpoleChoiceRow(names[icon.ordinal], selected == icon, icon = icon.vector()) {
                onSelect(icon); open = false
            }
        } }
    }
}
