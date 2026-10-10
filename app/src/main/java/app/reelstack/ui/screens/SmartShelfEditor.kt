package app.reelstack.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reelstack.R
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.SmartShelfEditor
import app.reelstack.data.model.SmartShelfIcon
import app.reelstack.data.model.SmartShelfKinds
import app.reelstack.data.model.SmartShelfPeriod
import app.reelstack.data.model.SmartShelfPresets
import app.reelstack.ui.components.RemoteTextField
import app.reelstack.ui.components.SettingsToggleRow
import app.reelstack.ui.components.SmartShelfActions
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.SpoleSecondaryButton
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.genreLabel
import app.reelstack.ui.components.isTelevision
import app.reelstack.ui.components.smartShelfCounts
import app.reelstack.ui.components.smartShelfKindsLabel
import app.reelstack.ui.components.smartShelfName
import app.reelstack.ui.components.smartShelfPeriodLabel
import app.reelstack.ui.components.smartShelfPresetName
import app.reelstack.ui.components.vector
import app.reelstack.ui.components.windowLayoutPolicy

/**
 * Tags people tend to want a shelf for. Only the ones the server actually has are offered, and only
 * until something is typed into the search; then the whole list is searched.
 */
private val SUGGESTED_TAGS = listOf("halloween", "christmas", "christmas calendar", "holiday", "santa claus", "easter",
    "summer", "winter", "snow", "friendship", "dog", "cat", "horse", "dinosaur", "superhero", "space", "time travel",
    "road trip", "heist", "based on novel or book", "based on true story", "musical", "sports", "ghost", "witch",
    "vampire", "zombie", "pirate", "princess", "magic", "dragon", "robot", "norway", "scandinavia")

/**
 * The smart shelf builder.
 *
 * Everything is chosen from what the server holds, so the remote never has to type: genres and the
 * most likely tags are chips, and the search is there for the rest. The line under the title says
 * what the shelf would hold right now, and changes as the rule does. Focus starts on a chip, never in
 * a text field, and the save button is never greyed out: a shelf without a rule says so when saved.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SmartShelfEditorDialog(editor: SmartShelfEditor, actions: SmartShelfActions) {
    val draft = editor.draft
    val television = isTelevision()
    val policy = windowLayoutPolicy()
    val menuShortcuts = television || (policy.useNavigationRail && !policy.useCompactTouchRail)
    val firstFocus = remember { FocusRequester() }
    var missingRule by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val tagField = rememberTextFieldState()
    val nameField = rememberTextFieldState(draft.name)
    LaunchedEffect(Unit) { if (television) { withFrameNanos { }; runCatching { firstFocus.requestFocus() } } }
    val update: (SmartShelf) -> Unit = { missingRule = false; confirmDelete = false; actions.update(it) }
    val currentDraft by rememberUpdatedState(draft)
    val currentUpdate by rememberUpdatedState(update)
    LaunchedEffect(nameField) {
        snapshotFlow { nameField.text.toString() }.collect { typed ->
            if (typed != currentDraft.name) currentUpdate(currentDraft.copy(name = typed))
        }
    }
    // A template names a shelf that has no name yet; a name being typed is never written over.
    LaunchedEffect(draft.name) {
        if (nameField.text.isEmpty() && draft.name.isNotEmpty()) nameField.setTextAndPlaceCursorAtEnd(draft.name)
    }
    Dialog(onDismissRequest = actions::close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 760.dp).fillMaxWidth(.94f).fillMaxHeight(.92f).testTag("smart-shelf-editor"),
            shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .16f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(draft.icon.vector(), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(if (editor.isNew) R.string.smart_shelf_new else R.string.smart_shelf_edit),
                            style = MaterialTheme.typography.headlineSmall)
                        Text(previewLine(editor, missingRule), style = MaterialTheme.typography.bodyMedium,
                            color = if (missingRule) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("smart-shelf-preview"))
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("smart-shelf-editor-list"),
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (editor.isNew) item(key = "templates") {
                        EditorSection(stringResource(R.string.smart_shelf_start_from)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmartShelfPresets.all.forEachIndexed { index, preset ->
                                    val applied = draft.tags == preset.tags && draft.genres == preset.genres &&
                                        draft.kinds == preset.kinds && draft.requiresBoth == preset.requiresBoth
                                    ShelfChip(stringResource(smartShelfPresetName(preset.id) ?: R.string.smart_shelf_untitled),
                                        selected = applied, role = Role.RadioButton, tag = "smart-template-${preset.id}", leading = preset.icon.vector(),
                                        modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier) {
                                        missingRule = false
                                        actions.applyTemplate(preset.id)
                                    }
                                }
                            }
                        }
                    }
                    item(key = "name") {
                        EditorSection(stringResource(R.string.smart_shelf_name)) {
                            RemoteTextField(nameField, television, ImeAction.Done, maxLength = 40,
                                placeholder = smartShelfName(draft.copy(name = "")),
                                modifier = Modifier.fillMaxWidth().testTag("smart-shelf-name"))
                            FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmartShelfIcon.entries.forEach { icon ->
                                    IconChip(icon.vector(), smartShelfIconName(icon), selected = draft.icon == icon, tag = "smart-icon-${icon.name}") {
                                        update(draft.copy(icon = icon))
                                    }
                                }
                            }
                        }
                    }
                    item(key = "kinds") {
                        EditorSection(stringResource(R.string.smart_shelf_kinds)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmartShelfKinds.entries.forEachIndexed { index, kinds ->
                                    ShelfChip(smartShelfKindsLabel(kinds), selected = draft.kinds == kinds, role = Role.RadioButton,
                                        tag = "smart-kinds-${kinds.name}",
                                        modifier = if (!editor.isNew && index == 0) Modifier.focusRequester(firstFocus) else Modifier) {
                                        update(draft.copy(kinds = kinds))
                                    }
                                }
                            }
                        }
                    }
                    item(key = "genres") {
                        EditorSection(stringResource(R.string.smart_shelf_genres), stringResource(R.string.smart_shelf_genres_hint)) {
                            // The server's own spelling first; a preset's other spelling of the same genre is not a second chip.
                            val genres = app.reelstack.data.model.distinctGenres(editor.facets.genres + draft.genres)
                            if (editor.facetsFailed) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // A server that did not answer is not a server with no genres: say so, and let it be asked again.
                                Text(stringResource(R.string.smart_shelf_facets_failed), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error)
                                editor.facetsError?.let { Text(stringResource(R.string.smart_shelf_facets_detail, it),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                SpoleSecondaryButton(onClick = actions::retryFacets, modifier = Modifier.testTag("smart-facets-retry")) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                            if (editor.facetsLoading && genres.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
                            else if (genres.isEmpty() && !editor.facetsFailed) Text(stringResource(R.string.smart_shelf_no_genres),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                genres.forEach { genre ->
                                    val chosen = draft.genres.any { app.reelstack.data.model.sameGenre(it, genre) }
                                    ShelfChip(genreLabel(genre), selected = chosen, tag = "smart-genre-$genre") {
                                        update(draft.copy(genres = if (chosen) draft.genres.filterNot { app.reelstack.data.model.sameGenre(it, genre) }
                                            else draft.genres + genre))
                                    }
                                }
                            }
                        }
                    }
                    item(key = "tags") {
                        EditorSection(stringResource(R.string.smart_shelf_tags), stringResource(R.string.smart_shelf_tags_hint)) {
                            if (draft.tags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                draft.tags.forEach { tag ->
                                    ShelfChip(tag, selected = true, trailing = SpoleIcons.Close, tag = "smart-tag-chosen-$tag") {
                                        update(draft.copy(tags = draft.tags - tag))
                                    }
                                }
                            }
                            RemoteTextField(tagField, television, ImeAction.Search, maxLength = 60,
                                label = stringResource(R.string.smart_shelf_tag_search), leadingIcon = SpoleIcons.Search,
                                modifier = Modifier.fillMaxWidth().testTag("smart-shelf-tag-search"))
                            val query = tagField.text.toString().trim()
                            val chosen = draft.tags.map { it.lowercase() }.toSet()
                            val available = editor.facets.tags
                            val matches = if (query.isBlank()) {
                                val known = available.associateBy { it.lowercase() }
                                SUGGESTED_TAGS.mapNotNull { known[it] }.filter { it.lowercase() !in chosen }
                            } else available.filter { it.contains(query, ignoreCase = true) && it.lowercase() !in chosen }
                                .sortedBy { if (it.startsWith(query, ignoreCase = true)) 0 else 1 }.take(30)
                            val exact = available.any { it.equals(query, ignoreCase = true) } || query.lowercase() in chosen
                            if (query.isBlank() && matches.isNotEmpty()) Text(stringResource(R.string.smart_shelf_tag_suggestions),
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                matches.forEach { tag ->
                                    ShelfChip(tag, selected = false, leading = SpoleIcons.Add, tag = "smart-tag-$tag") {
                                        update(draft.copy(tags = draft.tags + tag))
                                    }
                                }
                                // A server that keeps its tag list to itself still takes a tag typed in full.
                                if (query.isNotBlank() && !exact) ShelfChip(stringResource(R.string.smart_shelf_tag_add, query),
                                    selected = false, leading = SpoleIcons.Add, tag = "smart-tag-add") {
                                    update(draft.copy(tags = draft.tags + query.lowercase()))
                                    tagField.clearText()
                                }
                            }
                            if (query.isNotBlank() && matches.isEmpty() && exact) Text(stringResource(R.string.smart_shelf_no_tags),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // «Or» or «and» only means something once both sides have a choice in them.
                    if (draft.combinesBoth) item(key = "match") {
                        EditorSection(stringResource(R.string.smart_shelf_match), stringResource(R.string.smart_shelf_match_hint)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(false to R.string.smart_shelf_match_any, true to R.string.smart_shelf_match_all).forEach { (all, label) ->
                                    ShelfChip(stringResource(label), selected = draft.matchAll == all, role = Role.RadioButton,
                                        tag = if (all) "smart-match-all" else "smart-match-any") { update(draft.copy(matchAll = all)) }
                                }
                            }
                        }
                    }
                    item(key = "unwatched") {
                        SettingsToggleRow(stringResource(R.string.smart_shelf_unwatched), stringResource(R.string.smart_shelf_unwatched_hint),
                            draft.unwatched, "smart-shelf-unwatched") { update(draft.copy(unwatched = it)) }
                    }
                    item(key = "period") {
                        EditorSection(stringResource(R.string.smart_shelf_period), stringResource(R.string.smart_shelf_period_hint)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmartShelfPeriod.entries.forEach { period ->
                                    ShelfChip(smartShelfPeriodLabel(period), selected = draft.period == period, role = Role.RadioButton,
                                        tag = "smart-period-${period.name}") { update(draft.copy(period = period)) }
                                }
                            }
                        }
                    }
                    // Where the shelf shows up is the owner's choice, place by place and shelf by shelf.
                    item(key = "place") {
                        EditorSection(stringResource(R.string.smart_shelf_place)) {
                            SettingsToggleRow(stringResource(R.string.smart_shelf_in_library), stringResource(R.string.smart_shelf_in_library_hint),
                                draft.inLibrary, "smart-shelf-in-library") { update(draft.copy(inLibrary = it)) }
                            SettingsToggleRow(stringResource(R.string.smart_shelf_on_home), stringResource(R.string.smart_shelf_on_home_hint),
                                draft.onHome, "smart-shelf-on-home") { update(draft.copy(onHome = it)) }
                            if (menuShortcuts) SettingsToggleRow(stringResource(R.string.smart_shelf_in_menu), stringResource(R.string.smart_shelf_in_menu_hint),
                                draft.inMenu, "smart-shelf-in-menu") { update(draft.copy(inMenu = it)) }
                            // A period that has not begun keeps the shelf off Home and the menu; say until when.
                            val today = remember { java.time.LocalDate.now() }
                            val waitsFor = draft.period.takeIf { (draft.onHome || draft.inMenu) && !it.isActive(today) }?.nextStart(today)
                            if (waitsFor != null) {
                                val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                                val day = waitsFor.format(java.time.format.DateTimeFormatter.ofPattern(
                                    stringResource(R.string.smart_shelf_date_pattern), locale))
                                Text(stringResource(R.string.smart_shelf_period_waits, smartShelfPeriodLabel(draft.period), day),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("smart-shelf-period-waits"))
                            }
                            if (!draft.inLibrary && !draft.onHome && !(menuShortcuts && draft.inMenu)) Text(stringResource(R.string.smart_shelf_place_none),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("smart-shelf-place-none"))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!editor.isNew) SpoleSecondaryButton(onClick = {
                        if (confirmDelete || draft.preset) actions.delete(draft.id) else confirmDelete = true
                    }, modifier = Modifier.testTag("smart-shelf-delete")) {
                        Text(stringResource(when {
                            draft.preset -> R.string.smart_shelf_reset
                            confirmDelete -> R.string.smart_shelf_delete_confirm
                            else -> R.string.smart_shelf_delete
                        }), color = if (confirmDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.weight(1f))
                    SpoleSecondaryButton(onClick = actions::close, modifier = Modifier.testTag("smart-shelf-cancel")) {
                        Text(stringResource(R.string.library_cancel))
                    }
                    val saveInteraction = remember { MutableInteractionSource() }
                    Button(onClick = { if (draft.hasRule) actions.save() else missingRule = true },
                        interactionSource = saveInteraction,
                        modifier = Modifier.heightIn(min = 48.dp).focusOutline(saveInteraction, CircleShape, glow = false)
                            .testTag("smart-shelf-save")) {
                        Text(stringResource(R.string.library_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun previewLine(editor: SmartShelfEditor, missingRule: Boolean): String {
    val movies = editor.previewMovies
    val series = editor.previewSeries
    return when {
        missingRule -> stringResource(R.string.smart_shelf_needs_rule)
        !editor.draft.hasRule -> stringResource(R.string.smart_shelf_intro)
        editor.previewLoading -> stringResource(R.string.smart_shelf_preview_loading)
        movies == null || series == null -> stringResource(R.string.smart_shelf_intro)
        movies + series == 0 -> stringResource(R.string.smart_shelf_preview_none)
        editor.previewCapped -> stringResource(R.string.smart_shelf_preview_capped, smartShelfCounts(movies, series))
        else -> stringResource(R.string.smart_shelf_preview, smartShelfCounts(movies, series))
    }
}

@Composable
private fun EditorSection(title: String, hint: String = "", content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/** A choice in the builder: a pill that fills with the accent's container when chosen, with a ring on focus. */
@Composable
private fun ShelfChip(label: String, selected: Boolean, tag: String, modifier: Modifier = Modifier, role: Role = Role.Checkbox,
    leading: ImageVector? = null, trailing: ImageVector? = null, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    Row(modifier.heightIn(min = 40.dp).clip(CircleShape)
        .background(if (selected) colors.primaryContainer else colors.surfaceVariant, CircleShape)
        .focusOutline(interaction, CircleShape, glow = false)
        .selectable(selected, interactionSource = interaction, indication = LocalIndication.current, role = role, onClick = onClick)
        .testTag(tag).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val icon = if (selected && trailing == null) SpoleIcons.Done else leading
        icon?.let { Icon(it, null, Modifier.size(16.dp), tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant) }
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onPrimaryContainer else colors.onSurface)
        trailing?.let { Icon(it, null, Modifier.size(16.dp), tint = colors.onPrimaryContainer) }
    }
}

@Composable
private fun IconChip(icon: ImageVector, name: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(44.dp).clip(CircleShape)
        .background(if (selected) colors.primaryContainer else colors.surfaceVariant, CircleShape)
        .focusOutline(interaction, CircleShape, glow = false)
        .selectable(selected, interactionSource = interaction, indication = LocalIndication.current, role = Role.RadioButton, onClick = onClick)
        .semantics { contentDescription = name }
        .testTag(tag), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = if (selected) colors.primary else colors.onSurfaceVariant)
    }
}

@Composable
private fun smartShelfIconName(icon: SmartShelfIcon): String = stringResource(when (icon) {
    SmartShelfIcon.STAR -> R.string.smart_icon_star
    SmartShelfIcon.HEART -> R.string.smart_icon_heart
    SmartShelfIcon.PUMPKIN -> R.string.smart_icon_pumpkin
    SmartShelfIcon.SNOWFLAKE -> R.string.smart_icon_snowflake
    SmartShelfIcon.CALENDAR -> R.string.smart_icon_calendar
    SmartShelfIcon.MOVIE -> R.string.smart_icon_movie
    SmartShelfIcon.SCREEN -> R.string.smart_icon_screen
    SmartShelfIcon.KIDS -> R.string.smart_icon_kids
    SmartShelfIcon.ANIMATION -> R.string.smart_icon_animation
    SmartShelfIcon.DOCUMENTARY -> R.string.smart_icon_documentary
    SmartShelfIcon.MUSIC -> R.string.smart_icon_music
    SmartShelfIcon.SPORT -> R.string.smart_icon_sport
})
