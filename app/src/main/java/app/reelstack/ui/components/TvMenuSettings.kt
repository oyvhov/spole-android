package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*

/** One focus target per page. Reordering controls live in a small, remote-friendly editor. */
@Composable
internal fun TvMenuSettings(value: Personalization, onChange: (Personalization) -> Unit) {
    var editing by remember { mutableStateOf<String?>(null) }
    val order = (value.menuOrder + DEFAULT_MENU).distinct().filter { it in DEFAULT_MENU }
    val names = mapOf("HOME" to stringResource(R.string.nav_home), "LIBRARY" to stringResource(R.string.nav_library),
        "DISCOVER" to stringResource(R.string.nav_discover), "ACTIVITY" to stringResource(R.string.nav_activity),
        "SETTINGS" to stringResource(R.string.nav_settings))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        order.forEachIndexed { index, name -> key(name) {
            val required = name in setOf("HOME", "SETTINGS")
            // "Synleg i menyen" is the row's value, not an explanation of what pressing it does, so
            // it belongs on the same line as the name — two more pages fit on a television screen.
            SettingsChoiceRow("${index + 1} · ${names.getValue(name)}", stringResource(when {
                required -> R.string.settings_menu_required
                name in value.hiddenMenuItems -> R.string.settings_menu_hidden
                else -> R.string.settings_menu_visible
            }), "menu-option-$name") { editing = name }
        } }
        app.reelstack.ui.components.SpoleSecondaryButton(onClick = { onChange(value.copy(menuOrder = DEFAULT_MENU, hiddenMenuItems = emptySet())) }) {
            Text(stringResource(R.string.tv_reset_menu))
        }
    }
    editing?.let { name ->
        val index = order.indexOf(name)
        val label = names.getValue(name)
        val required = name in setOf("HOME", "SETTINGS")
        fun move(target: Int) {
            onChange(value.copy(menuOrder = order.toMutableList().apply { removeAt(index); add(target, name) }))
        }
        AlertDialog(onDismissRequest = { editing = null }, title = { Text(label) },
            confirmButton = { app.reelstack.ui.components.SpoleSecondaryButton(onClick = { editing = null }) { Text(stringResource(R.string.action_close)) } },
            text = {
                val editorFocus = remember { FocusRequester() }
                val television = isTelevision()
                LaunchedEffect(name) { if (television) { withFrameNanos { }; editorFocus.requestFocus() } }
                Column(Modifier.heightIn(max = 300.dp).focusRequester(editorFocus).focusGroup().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (required) Text(stringResource(R.string.settings_menu_required), style = MaterialTheme.typography.bodyMedium)
                else SettingsToggleRow(stringResource(R.string.settings_menu_show), "", name !in value.hiddenMenuItems, "menu-visible-$name") { show ->
                    onChange(value.copy(hiddenMenuItems = if (show) value.hiddenMenuItems - name else value.hiddenMenuItems + name))
                }
                OutlinedButton(onClick = { move(index - 1) }, enabled = index > 0,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("menu-up-$name")) { Text(stringResource(R.string.tv_move_up, label)) }
                OutlinedButton(onClick = { move(index + 1) }, enabled = index < order.lastIndex,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("menu-down-$name")) { Text(stringResource(R.string.tv_move_down, label)) }
            } })
    }
}
