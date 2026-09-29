package app.reelstack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.AccentPalette
import app.reelstack.ui.components.*

private val SettingsCategory.ink: Color
    get() = Color(when (this) {
        SettingsCategory.APPEARANCE -> AccentPalette.IRIS.softArgb
        SettingsCategory.HOME -> AccentPalette.OCEAN.softArgb
        SettingsCategory.PLAYBACK -> AccentPalette.CORAL.softArgb
        SettingsCategory.ACCOUNTS -> AccentPalette.MINT.softArgb
        SettingsCategory.UPDATES -> AccentPalette.GOLD.softArgb
        SettingsCategory.ABOUT -> AccentPalette.PEARL.softArgb
    })

/** The same category symbol follows the user from the menu into its settings pane. */
@Composable
internal fun SettingsCategoryIntro(category: SettingsCategory, showTitle: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsIconBadge(category.icon, category.ink, 52.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showTitle) Text(stringResource(category.title), style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(category.hint), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SettingsCategoryCard(category: SettingsCategory, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(modifier.fillMaxWidth().heightIn(min = 88.dp).settingsSurface(interaction)
        .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
            role = Role.Button, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsIconBadge(category.icon, category.ink, 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(category.title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(category.hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(SpoleIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SettingsCategoryDestination(category: SettingsCategory, selected: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp).settingsSurface(interaction, selected = selected)
        .selectable(selected, role = Role.Tab, interactionSource = interaction,
            indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsIconBadge(category.icon, category.ink, 34.dp)
        Text(stringResource(category.title), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
    }
}
