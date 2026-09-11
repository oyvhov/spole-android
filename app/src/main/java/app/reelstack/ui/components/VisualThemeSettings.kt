package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.theme.LocalPersonalization

@Composable
internal fun SettingsActionRow(title: String, summary: String, tag: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape)
        .focusOutline(interaction, shape)
        .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
            role = Role.Button, onClick = onClick).testTag(tag).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
        }
        Icon(app.reelstack.ui.components.SpoleIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun SettingsToggleRow(title: String, summary: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape)
        .focusOutline(interaction, shape).toggleable(checked, role = Role.Switch, interactionSource = interaction,
            indication = androidx.compose.foundation.LocalIndication.current, onValueChange = onChange)
        .testTag(tag).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (summary.isNotBlank()) Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp))
        }
        Switch(checked, null)
    }
}

@Composable
private fun <T> ThemeChoice(title: String, selected: T, options: List<T>, prefix: String,
    label: @Composable (T) -> String, swatch: ((T) -> Color)? = null, onChange: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SettingsActionRow(title, label(selected), "theme-choice-$prefix") { open = true }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(title) },
        confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_close)) } },
        text = {
            val selectedFocus = remember { FocusRequester() }
            val tv = androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
                android.content.res.Configuration.UI_MODE_TYPE_MASK == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
            LaunchedEffect(Unit) { if(tv) { withFrameNanos { }; selectedFocus.requestFocus() } }
            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { option ->
                    val interaction = remember { MutableInteractionSource() }
                    val shape = RoundedCornerShape(12.dp)
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .then(if (option == selected) Modifier.focusRequester(selectedFocus) else Modifier).focusOutline(interaction, shape)
                        .selectable(option == selected, role = Role.RadioButton, interactionSource = interaction,
                            indication = androidx.compose.foundation.LocalIndication.current,
                            onClick = { onChange(option); open = false }).testTag("$prefix-$option")
                        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (swatch != null) Box(Modifier.size(26.dp).background(swatch(option), RoundedCornerShape(7.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(7.dp)))
                        Text(label(option), Modifier.weight(1f))
                        if (option == selected) Icon(app.reelstack.ui.components.SpoleIcons.Done, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        })
}

@Composable
internal fun VisualThemeSettings(value: Personalization, onChange: (Personalization) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemePreview(value)
        ThemeChoice(stringResource(R.string.theme_background), value.visualTheme, VisualTheme.entries, "mood",
            { stringResource(when(it) {
                VisualTheme.FOREST -> R.string.theme_forest; VisualTheme.MIDNIGHT -> R.string.theme_midnight
                VisualTheme.CINEMA -> R.string.theme_cinema; VisualTheme.PLUM -> R.string.theme_plum
            }) }, { Color(it.surface) }) { onChange(value.copy(visualTheme = it)) }
        ThemeChoice(stringResource(R.string.personal_accent), value.accent, AccentPalette.entries, "accent",
            { stringResource(when(it) {
                AccentPalette.LIME -> R.string.personal_lime; AccentPalette.OCEAN -> R.string.personal_ocean
                AccentPalette.IRIS -> R.string.personal_iris; AccentPalette.CORAL -> R.string.personal_coral
                AccentPalette.GOLD -> R.string.theme_gold; AccentPalette.MINT -> R.string.theme_mint
                AccentPalette.ROSE -> R.string.theme_rose; AccentPalette.PEARL -> R.string.theme_pearl
            }) }, { Color(it.argb) }) { onChange(value.copy(accent = it)) }
        ThemeChoice(stringResource(R.string.personal_artwork), value.artworkSize, ArtworkSize.entries, "artwork",
            { stringResource(when(it) { ArtworkSize.COMPACT -> R.string.personal_compact
                ArtworkSize.STANDARD -> R.string.personal_standard; ArtworkSize.LARGE -> R.string.personal_large }) }) {
            onChange(value.copy(artworkSize = it)) }
        ThemeChoice(stringResource(R.string.theme_corners), value.artworkCorners, ArtworkCorners.entries, "corners",
            { stringResource(when(it) { ArtworkCorners.CRISP -> R.string.theme_crisp
                ArtworkCorners.SOFT -> R.string.theme_soft; ArtworkCorners.ROUND -> R.string.theme_round }) }) {
            onChange(value.copy(artworkCorners = it)) }
        ThemeChoice(stringResource(R.string.theme_focus), value.focusStyle, FocusStyle.entries, "focus",
            { stringResource(when(it) { FocusStyle.WHITE -> R.string.theme_focus_white
                FocusStyle.ACCENT -> R.string.theme_focus_accent; FocusStyle.BOLD -> R.string.theme_focus_bold }) }) {
            onChange(value.copy(focusStyle = it)) }
        SettingsToggleRow(stringResource(R.string.theme_contrast), stringResource(R.string.theme_contrast_hint),
            value.highContrast, "theme-contrast") { onChange(value.copy(highContrast = it)) }
        TextButton(onClick = { onChange(value.copy(accent = AccentPalette.LIME, artworkSize = ArtworkSize.STANDARD,
            visualTheme = VisualTheme.FOREST, artworkCorners = ArtworkCorners.SOFT, focusStyle = FocusStyle.WHITE, highContrast = false)) },
            modifier = Modifier.testTag("appearance-reset")) { Text(stringResource(R.string.personal_reset)) }
    }
}

@Composable
internal fun ThemePreview(value: Personalization) {
    val description = stringResource(R.string.personal_preview)
    val accent = Color(value.accent.argb)
    val focus = if (value.focusStyle == FocusStyle.ACCENT) accent else app.reelstack.ui.theme.Text
    val shape = RoundedCornerShape(value.artworkCorners.radius.dp)
    Row(Modifier.fillMaxWidth().background(Color(value.visualTheme.background), RoundedCornerShape(16.dp))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        .padding(20.dp).clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val scale = value.artworkSize.scale
            Box(Modifier.size(40.dp * scale, 60.dp * scale).background(Color(value.visualTheme.raised), shape)
                .border(if (value.focusStyle == FocusStyle.BOLD) 4.dp else 2.dp, focus, shape))
            Box(Modifier.size(64.dp * scale, 40.dp * scale).background(accent.copy(alpha = .6f), shape))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Spole", color = app.reelstack.ui.theme.Text, style = MaterialTheme.typography.titleLarge)
            Box(Modifier.fillMaxWidth(.65f).height(5.dp).background(accent, RoundedCornerShape(3.dp)))
            Text(stringResource(R.string.theme_saved_live), color = Color(if (value.highContrast) value.visualTheme.mutedHigh else value.visualTheme.muted),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
