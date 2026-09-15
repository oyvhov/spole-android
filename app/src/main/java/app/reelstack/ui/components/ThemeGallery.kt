package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*

/** Preview is deliberately local: browsing a theme never changes the running app. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeGallery(value: Personalization, onChange: (Personalization) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(value.visualTheme) }
    SettingsActionRow(stringResource(R.string.phase_themes), stringResource(R.string.phase_theme_hint), "theme-gallery", SpoleIcons.Tune) {
        selected = value.visualTheme
        open = true
    }
    val wide = app.reelstack.ui.theme.LocalTabletCanvas.current ||
        (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val dialogHeight = with(androidx.compose.ui.platform.LocalDensity.current) {
        androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp() * .9f
    }
    if (open) androidx.compose.ui.window.Dialog(onDismissRequest = { open = false },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = if (wide) 820.dp else 440.dp).fillMaxWidth(.94f)
            .heightIn(max = dialogHeight)
            .testTag("theme-gallery-dialog"), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.phase_themes), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.phase_theme_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VisualTheme.entries.chunked(if (wide) 2 else 1).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { theme ->
                    val label = stringResource(when (theme) {
                        VisualTheme.FOREST -> R.string.theme_forest
                        VisualTheme.MIDNIGHT -> R.string.theme_midnight
                        VisualTheme.CINEMA -> R.string.theme_cinema
                        VisualTheme.PLUM -> R.string.theme_plum
                        VisualTheme.NOEL -> R.string.theme_noel
                        VisualTheme.HALLOWEEN -> R.string.theme_halloween
                    })
                    val accent = when (theme) {
                        VisualTheme.NOEL -> AccentPalette.HOLLY
                        VisualTheme.HALLOWEEN -> AccentPalette.PUMPKIN
                        else -> value.accent
                    }
                    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Surface(onClick = { selected = theme }, modifier = Modifier.weight(1f).focusOutline(interaction, RoundedCornerShape(12.dp)).testTag("theme-preview-${theme.name}"),
                        interactionSource = interaction,
                        shape = RoundedCornerShape(12.dp), color = Color(theme.background), contentColor = Color.White,
                        border = androidx.compose.foundation.BorderStroke(2.dp, if (selected == theme) Color(accent.argb) else Color(theme.outline))) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                if (selected == theme) Icon(SpoleIcons.Done, null, Modifier.size(20.dp), tint = Color(accent.argb))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.width(6.dp).height(48.dp).background(Color(accent.argb), RoundedCornerShape(3.dp)))
                                listOf(R.drawable.demo_coast, R.drawable.demo_winter, R.drawable.demo_manor).forEach { art ->
                                    androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(art), null,
                                        Modifier.weight(1f).height(68.dp).clip(RoundedCornerShape(value.artworkCorners.radius.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                }
                            }
                        }
                    }
                }
                }
                }
            }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_close)) }
        Spacer(Modifier.width(8.dp))
        SpoleSecondaryButton(onClick = {
            onChange(when (selected) {
                VisualTheme.NOEL -> Season.CHRISTMAS.applyTo(value)
                VisualTheme.HALLOWEEN -> Season.HALLOWEEN.applyTo(value)
                else -> value.copy(visualTheme = selected)
            })
            open = false
        }, modifier = Modifier.testTag("theme-apply")) { Text(stringResource(R.string.phase_apply)) }
        }
        }
        }
    }
}
