package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.theme.LocalPersonalization

@Composable
fun NavigationPersonalizationSettings() {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { AppPreferencesRepository(context) }
    NavigationOptions(LocalPersonalization.current) { repository.personalization = it }
}

@Composable
internal fun NavigationOptions(value: Personalization, showHeader: Boolean = true, menuOnly: Boolean = false,
    onChange: (Personalization) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showHeader) OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("navigation-options")) {
            Text(stringResource(R.string.tv_customize))
        }
        if (expanded || !showHeader) {
            Text(stringResource(R.string.settings_tv_menu_hint), style = MaterialTheme.typography.bodySmall)
            val order = (value.menuOrder + DEFAULT_MENU).distinct().filter { it in DEFAULT_MENU }
            order.forEachIndexed { index, name -> key(name) {
                val label = stringResource(when (name) {
                    "HOME" -> R.string.nav_home
                    "LIBRARY" -> R.string.nav_library
                    "DISCOVER" -> R.string.nav_discover
                    "ACTIVITY" -> R.string.nav_activity
                    else -> R.string.nav_settings
                })
                Row(Modifier.fillMaxWidth().testTag("menu-option-$name"), verticalAlignment = Alignment.CenterVertically) {
                    val required = name in setOf("HOME", "SETTINGS")
                    Row(Modifier.weight(1f).heightIn(min = 52.dp).toggleable(name !in value.hiddenMenuItems,
                        enabled = !required, role = Role.Checkbox,
                        onValueChange = { show -> onChange(value.copy(hiddenMenuItems = if (show) value.hiddenMenuItems - name else value.hiddenMenuItems + name)) }),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(required || name !in value.hiddenMenuItems, null, enabled = !required)
                        Text(label, Modifier.weight(1f).padding(start = 6.dp))
                    }
                    fun move(target: Int) {
                        val moved = order.toMutableList().apply { removeAt(index); add(target, name) }
                        onChange(value.copy(menuOrder = moved))
                    }
                    IconButton(onClick = { move(index - 1) }, enabled = index > 0, modifier = Modifier.testTag("menu-up-$name")) {
                        Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.tv_move_up, label))
                    }
                    IconButton(onClick = { move(index + 1) }, enabled = index < order.lastIndex, modifier = Modifier.testTag("menu-down-$name")) {
                        Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.tv_move_down, label))
                    }
                }
            } }
            if (!menuOnly) {
            PreferenceToggle(R.string.tv_next_up, value.showNextUp, "show-next-up") { onChange(value.copy(showNextUp = it)) }
            PreferenceToggle(R.string.tv_combine_continue, value.combineContinueWatching, "combine-continue") { onChange(value.copy(combineContinueWatching = it)) }
            if (value.combineContinueWatching) Text(stringResource(R.string.watching_order_hint), style = MaterialTheme.typography.bodySmall)
            PreferenceToggle(R.string.tv_show_hero, value.showHero, "show-hero") { onChange(value.copy(showHero = it)) }
            PreferenceToggle(R.string.tv_show_ratings, value.showRatings, "show-ratings") { onChange(value.copy(showRatings = it)) }
            PreferenceToggle(R.string.tv_show_quality, value.showQuality, "show-quality") { onChange(value.copy(showQuality = it)) }
            PreferenceToggle(R.string.tv_slow_startup, value.slowStartup, "slow-startup") { onChange(value.copy(slowStartup = it)) }
            }
            TextButton(onClick = { onChange(value.copy(menuOrder = DEFAULT_MENU, hiddenMenuItems = emptySet())) }) {
                Text(stringResource(R.string.tv_reset_menu))
            }
        }
    }
}

@Composable
private fun PreferenceToggle(label: Int, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag).toggleable(checked, role = Role.Switch,
        onValueChange = onChange).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked, null)
    }
}
