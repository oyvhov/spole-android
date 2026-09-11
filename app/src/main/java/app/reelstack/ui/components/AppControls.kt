package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.*

@Composable
fun ServiceSymbol(kind: ServiceKind, modifier: Modifier = Modifier) {
    if (kind == ServiceKind.JELLYFIN || kind == ServiceKind.EMBY) {
        ServiceLogo(kind, null, modifier)
    } else {
        Icon(when (kind) {
            ServiceKind.SEERR -> app.reelstack.ui.components.SpoleIcons.Search
            ServiceKind.SONARR -> app.reelstack.ui.components.SpoleIcons.Screen
            else -> app.reelstack.ui.components.SpoleIcons.Movie
        }, null, modifier, tint = PrimarySoft)
    }
}

/**
 * Filter state is the value, not the label. Keying selection on the visible text meant a saved
 * filter silently reset the moment that copy was edited, and it blocked moving the copy out of
 * the composables at all.
 */
/**
 * A chip that leaves the page instead of filtering it.
 *
 * Same shape, height and family as the chips in [AppFilterRow], so a line of controls reads as one
 * line of controls rather than two unrelated kinds of button stacked on top of each other. The
 * chevron and the fixed colouring are the whole difference: a filter says what this page shows,
 * this says where the next page is.
 */
@Composable
fun AppNavigationChip(text: String, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val shape = RoundedCornerShape(10.dp)
    AssistChip(
        onClick = onClick,
        interactionSource = interaction,
        label = { Text(text, maxLines = 1) },
        trailingIcon = { Icon(SpoleIcons.ChevronRight, null, Modifier.size(18.dp), tint = Primary) },
        shape = shape,
        border = null,
        colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceRaised, labelColor = Primary),
        modifier = modifier.minimumInteractiveComponentSize().testTag(tag).focusOutline(interaction, shape),
    )
}

@Composable
fun <T> AppFilterRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionTag: ((T) -> String)? = null,
) {
    LazyRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it.toString() }) { option ->
            val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            FilterChip(
                interactionSource = interaction,
                selected = option == selected, onClick = { onSelect(option) },
                label = { Text(label(option), maxLines = 1) }, shape = RoundedCornerShape(10.dp), border = null,
                colors = FilterChipDefaults.filterChipColors(containerColor = SurfaceRaised, labelColor = Muted,
                    selectedContainerColor = Primary, selectedLabelColor = Ink),
                modifier = Modifier.minimumInteractiveComponentSize()
                    .then(optionTag?.let { Modifier.testTag(it(option)) } ?: Modifier)
                    .focusOutline(interaction, RoundedCornerShape(10.dp)),
            )
        }
    }
}
