package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.localization.AppLanguage
import app.reelstack.localization.AppLanguages

@Composable
fun LanguagePreference() {
    val context = LocalContext.current
    val selected = AppLanguages.selected(context)
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(onClick = { expanded = !expanded }, shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().testTag("language-picker")) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Rounded.Language, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.language_title), style = MaterialTheme.typography.titleMedium)
                Text(languageLabel(selected), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (expanded) {
        AlertDialog(onDismissRequest = { expanded = false },
            title = { Text(stringResource(R.string.language_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AppLanguage.entries.forEach { language ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .selectable(selected == language, role = Role.RadioButton, onClick = {
                                expanded = false
                                AppLanguages.select(context, language)
                            }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected == language, onClick = null)
                            Text(languageLabel(language), Modifier.weight(1f).padding(start = 12.dp))
                        }
                    }
                    Text(stringResource(R.string.language_note), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                }
            },
            confirmButton = { TextButton(onClick = { expanded = false }) { Text(stringResource(R.string.action_close)) } })
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.language_preview)
    else -> language.nativeName
}
