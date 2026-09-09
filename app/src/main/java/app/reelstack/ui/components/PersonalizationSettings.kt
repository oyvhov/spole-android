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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
                    Icon(if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null)
                }
                // No animated remeasurement: stable controls remain under the user's finger.
                if (expanded) {
                    Text(stringResource(R.string.personal_accent), color = Muted, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
                    FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentPalette.entries.forEach { palette ->
                            val label = stringResource(when (palette) {
                                AccentPalette.LIME -> R.string.personal_lime
                                AccentPalette.OCEAN -> R.string.personal_ocean
                                AccentPalette.IRIS -> R.string.personal_iris
                                AccentPalette.CORAL -> R.string.personal_coral
                            })
                            Row(Modifier.heightIn(min = 48.dp).background(SurfaceRaised, RoundedCornerShape(14.dp))
                                .selectable(value.accent == palette, role = Role.RadioButton,
                                    onClick = { onChange(value.copy(accent = palette)) })
                                .testTag("accent-${palette.name}").padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(24.dp).background(Color(palette.argb), CircleShape), contentAlignment = Alignment.Center) {
                                    if (value.accent == palette) Icon(Icons.Rounded.Check, null, tint = Ink, modifier = Modifier.size(18.dp))
                                }
                                Text(label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Text(stringResource(R.string.personal_artwork), color = Muted, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
                    FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArtworkSize.entries.forEach { size ->
                            FilterChip(selected = size == value.artworkSize,
                                onClick = { onChange(value.copy(artworkSize = size)) },
                                label = { Text(stringResource(when(size) {
                                    ArtworkSize.COMPACT -> R.string.personal_compact
                                    ArtworkSize.STANDARD -> R.string.personal_standard
                                    ArtworkSize.LARGE -> R.string.personal_large
                                })) }, modifier = Modifier.heightIn(min = 48.dp).testTag("artwork-${size.name}"))
                        }
                    }
                    val previewLabel = stringResource(R.string.personal_preview)
                    Row(Modifier.fillMaxWidth().height(100.dp).clearAndSetSemantics { contentDescription = previewLabel },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val scale = value.artworkSize.scale
                        Box(Modifier.size(40.dp * scale, 60.dp * scale).background(Primary, RoundedCornerShape(6.dp)))
                        Box(Modifier.size(80.dp * scale, 45.dp * scale).background(PrimarySoft, RoundedCornerShape(6.dp)))
                    }
                    Text(stringResource(R.string.personal_artwork_note), color = Muted, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onChange(value.copy(accent = AccentPalette.LIME, artworkSize = ArtworkSize.STANDARD)) },
                        modifier = Modifier.testTag("appearance-reset")) { Text(stringResource(R.string.personal_reset)) }
                }
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
