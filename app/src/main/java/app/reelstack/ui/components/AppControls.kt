package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.*

@Composable
fun ServiceSymbol(kind: ServiceKind, modifier: Modifier = Modifier) {
    if (kind == ServiceKind.JELLYFIN || kind == ServiceKind.EMBY) {
        ServiceLogo(kind, null, modifier)
    } else {
        Icon(when (kind) {
            ServiceKind.SEERR -> Icons.Rounded.Search
            ServiceKind.SONARR -> Icons.Rounded.Tv
            else -> Icons.Rounded.Movie
        }, null, modifier, tint = PrimarySoft)
    }
}

/**
 * Filter state is the value, not the label. Keying selection on the visible text meant a saved
 * filter silently reset the moment that copy was edited, and it blocked moving the copy out of
 * the composables at all.
 */
@Composable
fun <T> AppFilterRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
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
                modifier = Modifier.minimumInteractiveComponentSize().focusOutline(interaction, RoundedCornerShape(10.dp)),
            )
        }
    }
}
