package app.reelstack.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reelstack.R
import app.reelstack.data.model.HOME_MEDIA_SOURCES
import app.reelstack.data.model.HomeLayout
import app.reelstack.data.model.HomeLibraryChoice
import app.reelstack.data.model.HomeRowKey
import app.reelstack.data.model.HomeRowKind
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.homeRowFormat
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.*
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelLayout

/** What the Home editor can change. Defaults do nothing, so a screen test can leave it out. */
class HomeEditorActions(
    val onLayoutChange: (HomeLayout) -> Unit = {},
    val onLibrariesChange: (ServiceKind, HomeLibraryChoice) -> Unit = { _, _ -> },
    val onLoadLibraries: () -> Unit = {},
)

/** The rows the editor lists: shared rows always, a server's rows once it has an address. */
internal fun editableHomeRows(state: ReelstackUiState): List<HomeRowKey> {
    val sources = state.homeMediaSources.ifEmpty { if (state.configuredCount == 0) HOME_MEDIA_SOURCES else emptyList() }
    return state.effectiveHomeLayout.order.filter { it.source == null || it.source in sources }
}

/** The single entry in settings for everything about which rows Home shows. */
@Composable
internal fun HomeLayoutSetting(state: ReelstackUiState, actions: HomeEditorActions) {
    var open by rememberSaveable { mutableStateOf(false) }
    val layout = state.effectiveHomeLayout
    val rows = editableHomeRows(state)
    SettingsActionRow(stringResource(R.string.home_layout_title),
        stringResource(R.string.home_layout_summary, rows.count(layout::isVisible), rows.size), "home-layout-open") {
        open = true
        actions.onLoadLibraries()
    }
    if (open) HomeLayoutEditor(state, actions) { open = false }
}

private const val FILTER_ALL = "ALL"
private const val FILTER_OTHER = "OTHER"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeLayoutEditor(state: ReelstackUiState, actions: HomeEditorActions, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { AppPreferencesRepository(context.applicationContext) }
    val options = LocalPersonalization.current
    val layout = state.effectiveHomeLayout
    val all = editableHomeRows(state)
    val sources = all.mapNotNull { it.source }.distinct()
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    val shown = all.filter { row ->
        when (filter) {
            FILTER_ALL -> true
            FILTER_OTHER -> row.source == null
            else -> row.source?.name == filter
        }
    }
    val selectedSource = sources.firstOrNull { it.name == filter }
    var detailRow by remember { mutableStateOf<HomeRowKey?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 720.dp).fillMaxWidth(.94f).fillMaxHeight(.92f).testTag("home-layout-editor"),
            shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.home_layout_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.home_layout_intro), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("home-layout-filters")) {
                    val chips = buildList {
                        add(FILTER_ALL to stringResource(R.string.home_layout_filter_all))
                        sources.forEach { add(it.name to it.displayName) }
                        if (all.any { it.source == null }) add(FILTER_OTHER to stringResource(R.string.home_layout_filter_other))
                    }
                    chips.forEach { (value, label) ->
                        FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(label) },
                            leadingIcon = sources.firstOrNull { it.name == value }?.let { kind ->
                                { ServiceLogo(kind, null, Modifier.size(16.dp)) }
                            },
                            modifier = Modifier.testTag("home-layout-filter-$value"))
                    }
                }
                if (selectedSource != null) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpoleSecondaryButton(onClick = { actions.onLayoutChange(layout.withSourceVisible(selectedSource, true)) },
                        modifier = Modifier.testTag("home-layout-show-source")) {
                        Text(stringResource(R.string.home_layout_show_all_from, selectedSource.displayName))
                    }
                    SpoleSecondaryButton(onClick = { actions.onLayoutChange(layout.withSourceVisible(selectedSource, false)) },
                        modifier = Modifier.testTag("home-layout-hide-source")) {
                        Text(stringResource(R.string.home_layout_hide_all_from, selectedSource.displayName))
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("home-layout-list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(shown, key = { _, row -> row.id }) { index, row ->
                        // Two arrows per row. Moving a row to an end disables the arrow that moved
                        // it; focus goes to the other arrow first, or Compose's own recovery would
                        // throw it somewhere else on a TV. Items are keyed, so these move with it.
                        val focus = remember(row.id) { listOf(FocusRequester(), FocusRequester()) }
                        val insideContinue = row.kind == HomeRowKind.NEXT_UP && options.combineContinueWatching &&
                            layout.isVisible(HomeRowKey(HomeRowKind.CONTINUE_WATCHING, row.source))
                        HomeLayoutRow(
                            row = row,
                            visible = layout.isVisible(row),
                            note = if (insideContinue) stringResource(R.string.home_layout_in_continue,
                                "«${stringResource(R.string.home_continue)}»") else null,
                            libraries = if (row.kind.usesLibraries && row.source != null)
                                librarySummary(row, state.homeLibraries[row.source], state.homeLibraryViews[row.source]) else null,
                            canMoveUp = index > 0,
                            canMoveDown = index < shown.lastIndex,
                            upFocus = focus[0],
                            downFocus = focus[1],
                            onVisibleChange = { actions.onLayoutChange(layout.withVisible(row, it)) },
                            onMove = { direction ->
                                val destination = index + direction
                                focus[if (destination <= 0) 1 else if (destination >= shown.lastIndex) 0 else if (direction < 0) 0 else 1]
                                    .requestFocus()
                                actions.onLayoutChange(layout.moved(row, direction, shown))
                            },
                            onOpen = { detailRow = row },
                        )
                    }
                    item(key = "home-layout-options") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            SettingsGroup(stringResource(R.string.home_layout_options))
                            SettingsToggleRow(stringResource(R.string.tv_combine_continue), stringResource(R.string.watching_order_hint),
                                options.combineContinueWatching, "home-layout-combine") {
                                preferences.personalization = options.copy(combineContinueWatching = it)
                            }
                        }
                    }
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { actions.onLayoutChange(HomeLayout.DEFAULT) }, modifier = Modifier.testTag("home-layout-reset")) {
                        Text(stringResource(R.string.home_layout_reset))
                    }
                    Button(onClick = onDismiss, modifier = Modifier.testTag("home-layout-done")) {
                        Text(stringResource(R.string.home_order_done))
                    }
                }
            }
        }
    }
    detailRow?.let { row ->
        HomeRowDetailDialog(row, state, actions, onFormat = { format ->
            preferences.personalization = options.copy(homeRowFormats = options.homeRowFormats + (row.id to format))
        }) { detailRow = null }
    }
}

/**
 * One row of the editor: what it is and where it comes from, its switch, its libraries and its
 * place. Two lines rather than one, so the controls never squeeze the title at large text sizes.
 */
@Composable
private fun HomeLayoutRow(
    row: HomeRowKey,
    visible: Boolean,
    note: String?,
    libraries: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    upFocus: FocusRequester,
    downFocus: FocusRequester,
    onVisibleChange: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
    onOpen: () -> Unit,
) {
    val title = homeRowKindTitle(row.kind)
    val shape = RoundedCornerShape(ReelLayout.ControlCorner)
    val subtitle = listOfNotNull(
        row.source?.displayName ?: stringResource(R.string.home_layout_shared),
        stringResource(R.string.home_layout_hidden).takeIf { !visible },
        note,
    ).joinToString(" · ")
    Column(Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceVariant, shape)
        .testTag("home-layout-row-${row.id}").padding(horizontal = 10.dp, vertical = 6.dp)) {
        val interaction = remember { MutableInteractionSource() }
        val inner = RoundedCornerShape(8.dp)
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(inner).focusOutline(interaction, inner)
            .toggleable(visible, role = Role.Switch, interactionSource = interaction, indication = LocalIndication.current,
                onValueChange = onVisibleChange)
            .testTag("home-layout-visible-${row.id}").padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.source?.let { ServiceLogo(it, null, Modifier.size(20.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = if (visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(visible, null)
        }
        // The label takes what it needs and the arrows keep to the right. Giving both halves a
        // weight split the line in two and broke "Alle bibliotek" over two lines.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            if (libraries != null) {
                val openInteraction = remember { MutableInteractionSource() }
                TextButton(onClick = onOpen, interactionSource = openInteraction,
                    modifier = Modifier.weight(1f, fill = false).focusOutline(openInteraction, CircleShape)
                        .testTag("home-layout-libraries-${row.id}")) {
                    Icon(SpoleIcons.Tune, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(libraries, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Spacer(Modifier.width(0.dp))
            }
            Row { listOf(-1 to canMoveUp, 1 to canMoveDown).forEach { (direction, enabled) ->
                val interaction = remember { MutableInteractionSource() }
                IconButton(onClick = { onMove(direction) }, enabled = enabled, interactionSource = interaction,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).focusOutline(interaction, CircleShape)
                        .focusRequester(if (direction < 0) upFocus else downFocus)
                        .testTag("home-layout-${if (direction < 0) "up" else "down"}-${row.id}")) {
                    Icon(if (direction < 0) SpoleIcons.ChevronUp else SpoleIcons.ChevronDown,
                        stringResource(if (direction < 0) R.string.home_order_up else R.string.home_order_down,
                            listOfNotNull(title, row.source?.displayName).joinToString(" · ")))
                }
            } }
        }
    }
}

/** The libraries one server offers for [kind]: those whose collection type can fill it. */
internal fun libraryOptions(kind: HomeRowKind, views: List<RemoteLibraryView>): List<RemoteLibraryView> =
    views.filter { kind.accepts(it.collectionType) }

@Composable
private fun librarySummary(row: HomeRowKey, choice: HomeLibraryChoice?, views: List<RemoteLibraryView>?): String {
    val included = choice?.included?.get(row.kind) ?: return stringResource(R.string.home_layout_libraries_all)
    if (views == null) return pluralStringResource(R.plurals.home_layout_libraries_chosen, included.size, included.size)
    val offered = libraryOptions(row.kind, views)
    val chosen = offered.count { it.id in included }
    return if (chosen == 0) stringResource(R.string.home_layout_libraries_none)
    else stringResource(R.string.home_layout_libraries_some, chosen, offered.size)
}

/** The libraries that feed one row, and the card format it uses. */
@Composable
private fun HomeRowDetailDialog(
    row: HomeRowKey,
    state: ReelstackUiState,
    actions: HomeEditorActions,
    onFormat: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val source = row.source ?: return
    val title = "${homeRowKindTitle(row.kind)} · ${source.displayName}"
    val options = LocalPersonalization.current
    val choice = state.homeLibraries[source] ?: HomeLibraryChoice()
    val included = choice.included[row.kind]
    val views = state.homeLibraryViews[source]
    SpoleChoiceDialog(stringResource(R.string.home_layout_row_settings, title), onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).testTag("home-layout-detail"),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_layout_libraries_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
            val offered = views?.let { libraryOptions(row.kind, it) }
            when {
                views == null && state.homeLibraryViewsLoading -> Column(Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.home_layout_libraries_loading), style = MaterialTheme.typography.bodyMedium)
                }
                views == null -> Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_layout_libraries_failed, source.displayName))
                    SpoleSecondaryButton(onClick = actions.onLoadLibraries, modifier = Modifier.testTag("home-layout-libraries-retry")) {
                        Text(stringResource(R.string.home_layout_retry))
                    }
                }
                offered.isNullOrEmpty() -> Text(stringResource(R.string.home_layout_libraries_empty, source.displayName),
                    modifier = Modifier.padding(12.dp))
                else -> {
                    val ids = offered.mapTo(linkedSetOf()) { it.id }
                    SettingsToggleRow(stringResource(R.string.home_layout_libraries_all), "", included == null,
                        "home-layout-library-all") { everything ->
                        actions.onLibrariesChange(source, choice.with(row.kind, if (everything) null else ids))
                    }
                    offered.forEach { view ->
                        SettingsToggleRow(view.name, "", included?.contains(view.id) ?: true, "home-layout-library-${view.id}") { on ->
                            val base = (included ?: ids).intersect(ids)
                            val next = if (on) base + view.id else base - view.id
                            // Every library ticked is the same as "all", which also takes in a
                            // library the server gets later.
                            actions.onLibrariesChange(source, choice.with(row.kind, next.takeUnless { it == ids }))
                        }
                    }
                }
            }
            if (row.kind in setOf(HomeRowKind.CONTINUE_WATCHING, HomeRowKind.NEXT_UP, HomeRowKind.FAVOURITES,
                    HomeRowKind.NEW_MOVIES, HomeRowKind.NEW_SERIES)) {
                ThemeChoice(stringResource(R.string.library_art), homeRowFormat(options.homeRowFormats, row.id) ?: "AUTO",
                    listOf("AUTO", "POSTER", "THUMB"), "row-format-${row.id}", {
                        stringResource(when (it) {
                            "POSTER" -> R.string.library_art_poster
                            "THUMB" -> R.string.library_art_thumb
                            else -> R.string.library_art_auto
                        })
                    }, onChange = onFormat)
            }
        }
    }
}

@Composable
internal fun homeRowKindTitle(kind: HomeRowKind): String = stringResource(when (kind) {
    HomeRowKind.CONTINUE_WATCHING -> R.string.home_continue
    HomeRowKind.NOW_PLAYING -> R.string.home_now_playing
    HomeRowKind.NEXT_UP -> R.string.tv_next_up
    HomeRowKind.FAVOURITES -> R.string.home_favourites
    HomeRowKind.NEW_MOVIES -> R.string.home_new_movies
    HomeRowKind.NEW_SERIES -> R.string.home_new_episodes
    HomeRowKind.RECOMMENDATIONS -> R.string.home_recommendations
    HomeRowKind.RECENT_RELEASES -> R.string.home_recent_releases
    HomeRowKind.UPCOMING -> R.string.home_upcoming
})
