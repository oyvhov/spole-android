package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * A setting whose summary *is* its value.
 *
 * [SettingsActionRow] stacks its summary under the title, which is right for a row whose second
 * line is a sentence explaining what pressing it does. For a choice it was wrong twice over: the
 * value is one or two words, so the row was two lines tall for no reason, and the whole right-hand
 * half of a television-width row sat empty while the answer hid under the question.
 *
 * The swatch comes with it. Reading "Korall" tells you which name you picked; the dot beside it
 * tells you what you actually chose, without opening the list again to look.
 */
@Composable
internal fun SettingsChoiceRow(
    title: String,
    value: String,
    tag: String,
    swatch: Color? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    val dot: @Composable () -> Unit = {
        // A circle, not a rounded square. A dark swatch — and half of these are dark, because half
        // of them are backgrounds — sat in a settings row looking exactly like an unticked checkbox.
        if (swatch != null) Box(Modifier.size(16.dp)
            .background(swatch, androidx.compose.foundation.shape.CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.CircleShape))
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // One line needs room for both halves. A phone is 360 dp wide and "Storleik på omslag og
        // bilete" alone fills it, so below this the value goes back under the title rather than
        // squeezing the question down to an ellipsis.
        val inline = maxWidth >= 420.dp
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .focusOutline(interaction, shape)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button, onClick = onClick).testTag(tag).padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (inline) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                dot()
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            } else {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 3.dp)) {
                        dot()
                        Text(value, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
            Icon(app.reelstack.ui.components.SpoleIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
        }
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
    SettingsChoiceRow(title, label(selected), "theme-choice-$prefix", swatch?.invoke(selected)) { open = true }
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
        // A season is a pairing, not a background: red on a red ground is not Christmas, it is a
        // warning. This row sets the mood and the accent together, and the two rows under it still
        // let anyone pull them apart again.
        ThemeChoice(stringResource(R.string.theme_season), Season.of(value), Season.entries, "season",
            { stringResource(when (it) {
                Season.NONE -> R.string.theme_season_none
                Season.CHRISTMAS -> R.string.theme_season_christmas
                Season.HALLOWEEN -> R.string.theme_season_halloween
            }) }, { Color(it.swatch) }) { onChange(it.applyTo(value)) }
        if (Season.of(value) != Season.NONE) SettingsToggleRow(stringResource(R.string.theme_ornament),
            stringResource(R.string.theme_ornament_hint), value.seasonalOrnament, "theme-ornament") {
            onChange(value.copy(seasonalOrnament = it))
        }
        ThemeChoice(stringResource(R.string.theme_background), value.visualTheme, VisualTheme.entries, "mood",
            { stringResource(when(it) {
                VisualTheme.FOREST -> R.string.theme_forest; VisualTheme.MIDNIGHT -> R.string.theme_midnight
                VisualTheme.CINEMA -> R.string.theme_cinema; VisualTheme.PLUM -> R.string.theme_plum
                VisualTheme.NOEL -> R.string.theme_noel; VisualTheme.HALLOWEEN -> R.string.theme_halloween
            }) }, { Color(it.surface) }) { onChange(value.copy(visualTheme = it)) }
        ThemeChoice(stringResource(R.string.personal_accent), value.accent, AccentPalette.entries, "accent",
            { stringResource(when(it) {
                AccentPalette.LIME -> R.string.personal_lime; AccentPalette.OCEAN -> R.string.personal_ocean
                AccentPalette.IRIS -> R.string.personal_iris; AccentPalette.CORAL -> R.string.personal_coral
                AccentPalette.GOLD -> R.string.theme_gold; AccentPalette.MINT -> R.string.theme_mint
                AccentPalette.ROSE -> R.string.theme_rose; AccentPalette.PEARL -> R.string.theme_pearl
                AccentPalette.HOLLY -> R.string.theme_holly; AccentPalette.PUMPKIN -> R.string.theme_pumpkin
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
            visualTheme = VisualTheme.FOREST, artworkCorners = ArtworkCorners.SOFT, focusStyle = FocusStyle.WHITE,
            highContrast = false, seasonalOrnament = true)) },
            modifier = Modifier.testTag("appearance-reset")) { Text(stringResource(R.string.personal_reset)) }
    }
}

@Composable
internal fun ThemePreview(value: Personalization) {
    val description = stringResource(R.string.personal_preview)
    val accent = Color(value.accent.argb)
    val focus = if (value.focusStyle == FocusStyle.ACCENT) accent else app.reelstack.ui.theme.Text
    val shape = RoundedCornerShape(value.artworkCorners.radius.dp)
    val muted = Color(if (value.highContrast) value.visualTheme.mutedHigh else value.visualTheme.muted)
    val scale = value.artworkSize.scale
    // Three cards on a shelf, one of them focused. Abstract blocks could not show what any of these
    // choices actually do \u2014 the corner radius, the card size and the focus ring only mean anything
    // on the shape they are applied to, and the accent only means anything where the app uses it.
    Row(Modifier.fillMaxWidth().background(Color(value.visualTheme.background), RoundedCornerShape(16.dp))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        .padding(18.dp).clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(3) { index ->
                Column(Modifier.width(42.dp * scale), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(63.dp * scale)
                            .background(Color(value.visualTheme.raised), shape)
                            // The progress bar is inside the card, so the card's corners have to
                            // hold it — without this it squared off the bottom-left of the artwork.
                            .clip(shape)
                            .then(if (index == 0) Modifier.border(
                                if (value.focusStyle == FocusStyle.BOLD) 3.dp else 2.dp, focus, shape) else Modifier),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        if (index == 0) Box(Modifier.fillMaxWidth(.55f).height(4.dp).background(accent))
                    }
                    Box(Modifier.fillMaxWidth().height(4.dp)
                        .background(app.reelstack.ui.theme.Text.copy(alpha = .8f), RoundedCornerShape(2.dp)))
                    Box(Modifier.fillMaxWidth(.6f).height(4.dp).background(muted, RoundedCornerShape(2.dp)))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Spole", color = app.reelstack.ui.theme.Text, style = MaterialTheme.typography.titleLarge)
            Box(Modifier.fillMaxWidth(.65f).height(5.dp).background(accent, RoundedCornerShape(3.dp)))
            Text(stringResource(R.string.theme_saved_live), color = muted,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
