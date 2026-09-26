package app.reelstack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import app.reelstack.ui.theme.Divider
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted

/** A real navigation action, not a read-only text field that briefly opens an unwanted keyboard. */
@Composable
fun HomeSearchEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Ink,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("home-search"),
    ) {
        Column {
        Row(
            Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(app.reelstack.ui.components.SpoleIcons.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(21.dp))
            Text(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_search), color = Muted, fontSize = 14.sp,
                lineHeight = 20.sp, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = Divider, thickness = 1.dp)
        }
    }
}

/**
 * The way into search from the top of Home, beside the profile. It is always there on a phone or
 * tablet; the search line under the header is an extra the owner can turn on in Settings.
 */
@Composable
fun HomeSearchButton(onClick: () -> Unit, modifier: Modifier = Modifier, onArtwork: Boolean = false) {
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    androidx.compose.material3.IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.size(48.dp).focusOutline(interaction, androidx.compose.foundation.shape.CircleShape)
            .testTag("home-search-button"),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (onArtwork) androidx.compose.ui.graphics.Color.Black.copy(alpha = .38f) else app.reelstack.ui.theme.SurfaceRaised)
                .then(if (onArtwork) Modifier.border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = .18f),
                    androidx.compose.foundation.shape.CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(app.reelstack.ui.components.SpoleIcons.Search,
                contentDescription = androidx.compose.ui.res.stringResource(app.reelstack.R.string.search_open),
                tint = app.reelstack.ui.theme.Text, modifier = Modifier.size(22.dp))
        }
    }
}
