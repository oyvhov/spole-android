package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.text.Cue
import androidx.media3.ui.SubtitleView
import app.reelstack.R
import app.reelstack.data.model.SubtitleStyle
import app.reelstack.player.applyAppearance
import app.reelstack.ui.components.SettingsActionRow

@Composable
internal fun subtitleStyleName(style: SubtitleStyle): String = stringResource(when(style) {
    SubtitleStyle.CLEAN -> R.string.subtitle_clean
    SubtitleStyle.CINEMA -> R.string.subtitle_cinema
    SubtitleStyle.HIGH_CONTRAST -> R.string.subtitle_contrast
    SubtitleStyle.LARGE -> R.string.subtitle_large
})

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun SubtitleAppearanceSetting(value: SubtitleStyle, onChange: (SubtitleStyle) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SettingsActionRow(stringResource(R.string.subtitle_appearance), subtitleStyleName(value), "subtitle-style") { open = true }
    if (open) AlertDialog(onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.subtitle_choose)) },
        confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_close)) } },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val sample = stringResource(R.string.subtitle_sample)
                AndroidView(factory = { SubtitleView(it).apply { setBackgroundColor(0xFF263444.toInt()) } },
                    update = { it.applyAppearance(value); it.setCues(listOf(Cue.Builder().setText(sample).build())) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).testTag("subtitle-preview"))
                SubtitleStyle.entries.forEach { style ->
                    Surface(onClick = { onChange(style) }, shape = MaterialTheme.shapes.medium,
                        color = if (value == style) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().testTag("subtitle-style-${style.name}")) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            RadioButton(selected = value == style, onClick = null)
                            Text(subtitleStyleName(style), Modifier.weight(1f).padding(start = 8.dp, top = 12.dp))
                        }
                    }
                }
                Text(stringResource(R.string.subtitle_scope), style = MaterialTheme.typography.bodySmall)
            }
        })
}
