package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearancePresets(value: Personalization, onChange: (Personalization) -> Unit) {
    Text(stringResource(R.string.design_looks), style = MaterialTheme.typography.titleLarge)
    ThemeGallery(value, onChange)
    SavedLooks(value, onChange)
    Text(stringResource(R.string.design_look_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SpoleSecondaryButton(onClick = { onChange(value.copy(visualTheme = VisualTheme.CINEMA,
            artworkCorners = ArtworkCorners.SOFT, artworkSize = ArtworkSize.STANDARD, heroCompact = false)) }) {
            Text(stringResource(R.string.design_look_cinema))
        }
        SpoleSecondaryButton(onClick = { onChange(value.copy(visualTheme = VisualTheme.MIDNIGHT,
            artworkCorners = ArtworkCorners.CRISP, artworkSize = ArtworkSize.COMPACT, heroCompact = true)) }) {
            Text(stringResource(R.string.design_look_compact))
        }
        SpoleSecondaryButton(onClick = { onChange(value.copy(visualTheme = VisualTheme.FOREST,
            artworkCorners = ArtworkCorners.SOFT, artworkSize = ArtworkSize.LARGE, highContrast = true, reduceMotion = true)) }) {
            Text(stringResource(R.string.design_look_comfort))
        }
    }
}
@Composable
private fun SavedLooks(value: Personalization, onChange: (Personalization) -> Unit) {
    val context = LocalContext.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val preferences = remember(context) { app.reelstack.data.repository.AppPreferencesRepository(context) }
    var saved by remember { mutableStateOf(preferences.savedAppearances) }
    var open by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    SpoleSecondaryButton(onClick = { open = true }, modifier = Modifier.testTag("saved-looks")) {
        Text(stringResource(R.string.refine_my_looks))
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(stringResource(R.string.refine_my_looks)) },
        confirmButton = { SpoleSecondaryButton(onClick = { open = false }) { Text(stringResource(R.string.action_close)) } },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.refine_save_look_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it.take(32) }, label = { Text(stringResource(R.string.refine_look_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("look-name"))
            SpoleSecondaryButton(onClick = {
                val entry = SavedAppearance.capture(name, value)
                saved = (saved.filterNot { it.name.equals(entry.name, true) } + entry).takeLast(12)
                preferences.savedAppearances = saved
                name = ""
                keyboard?.hide()
                focusManager.clearFocus()
            }, enabled = name.isNotBlank(), modifier = Modifier.testTag("look-save")) { Text(stringResource(R.string.refine_save_look)) }
            saved.forEach { entry ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    SpoleSecondaryButton(onClick = { onChange(entry.applyTo(value)); open = false }, modifier = Modifier.weight(1f).testTag("look-load-${entry.name}")) { Text(entry.name) }
                    IconButton(onClick = { saved = saved - entry; preferences.savedAppearances = saved }) {
                        Icon(SpoleIcons.Close, stringResource(R.string.refine_delete_named, entry.name))
                    }
                }
            }
        } })
}
