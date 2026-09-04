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

@Composable
fun AppFilterRow(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it }) { option ->
            FilterChip(
                selected = option == selected, onClick = { onSelect(option) },
                label = { Text(option) }, shape = RoundedCornerShape(10.dp), border = null,
                colors = FilterChipDefaults.filterChipColors(containerColor = SurfaceRaised, labelColor = Muted,
                    selectedContainerColor = Primary, selectedLabelColor = Ink),
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
        }
    }
}
