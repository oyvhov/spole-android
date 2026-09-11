package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.theme.*

@Composable
fun DevicePersonalizationSettings(showAppearance: Boolean = true, showPlayback: Boolean = true,
    expansionState: MutableState<Boolean>? = null) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { AppPreferencesRepository(context) }
    PersonalizationSettings(LocalPersonalization.current, { repository.personalization = it }, showAppearance, showPlayback, expansionState)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PersonalizationSettings(value: Personalization, onChange: (Personalization) -> Unit,
    showAppearance: Boolean = true, showPlayback: Boolean = true, expansionState: MutableState<Boolean>? = null) {
    val localExpansion = rememberSaveable { mutableStateOf(false) }
    var expanded by (expansionState ?: localExpansion)
    val expandedState = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    Column(Modifier.fillMaxWidth().padding(top = 26.dp)) {
        Text(stringResource(if (showAppearance) R.string.personal_title else R.string.personal_playback), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.personal_scope), color = Muted,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
        if (showAppearance) androidx.compose.material3.Surface(color = app.reelstack.ui.theme.Surface, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .clickable { expanded = !expanded }
                    .semantics { stateDescription = expandedState }
                    .testTag("appearance-expand"), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(26.dp).background(Color(value.accent.argb), CircleShape))
                    Text(stringResource(R.string.personal_appearance), modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium)
                    Icon(if (expanded) app.reelstack.ui.components.SpoleIcons.ChevronUp else app.reelstack.ui.components.SpoleIcons.ChevronDown, null)
                }
                // No animated remeasurement: stable controls remain under the user's finger.
                if (expanded) VisualThemeSettings(value, onChange)
            }
        }
        if (showPlayback) {
        if (showAppearance) Text(stringResource(R.string.personal_playback), color = Muted, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
        androidx.compose.material3.Surface(color = app.reelstack.ui.theme.Surface, shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.fillMaxWidth().toggleable(value.autoResume, role = Role.Switch,
                onValueChange = { onChange(value.copy(autoResume = it)) }).testTag("auto-resume")
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.personal_resume), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.personal_resume_note), color = Muted,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
                Switch(value.autoResume, onCheckedChange = null)
            }
        }
        }
    }
}
