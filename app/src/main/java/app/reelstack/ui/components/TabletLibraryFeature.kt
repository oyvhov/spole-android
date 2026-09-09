package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted

/** Only a title already permitted by the visible library rows; never an additional server query. */
internal fun tabletFeaturedTitle(series: List<LibraryMedia>, sections: Set<HomeSection>): LibraryMedia? =
    series.firstOrNull { media ->
        !media.artworkUrl.isNullOrBlank() && when (media.source) {
            ServiceKind.JELLYFIN -> HomeSection.JELLYFIN_SERIES in sections
            ServiceKind.EMBY -> HomeSection.EMBY_SERIES in sections
            else -> false
        }
    }

/** Static composition: one real library title, no timed carousel or invented recommendation. */
@Composable
internal fun TabletLibraryFeature(media: LibraryMedia, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val shortWindow = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 650
    val actionInteraction = remember { MutableInteractionSource() }
    Box(modifier.fillMaxWidth().heightIn(min = if (shortWindow) 250.dp else 330.dp)
        .clip(RoundedCornerShape(24.dp)).background(Ink).testTag("tablet-library-feature")) {
        Box(Modifier.matchParentSize()) {
            MediaArtwork(media.artworkUrl, media.artworkRes, null,
                Modifier.align(Alignment.CenterEnd).fillMaxWidth(.72f).fillMaxHeight(),
                contentScale = ContentScale.Crop, source = media.source)
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                0f to Ink, .27f to Ink, .52f to Ink.copy(alpha = .88f),
                .72f to Ink.copy(alpha = .18f), 1f to Color.Transparent)))
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                0f to Color.Transparent, .65f to Color.Transparent, 1f to Ink.copy(alpha = .6f))))
        }
        Column(Modifier.fillMaxWidth(.54f).padding(vertical = if (shortWindow) 20.dp else 32.dp, horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceLogo(media.source, null, Modifier.size(15.dp))
                Text(stringResource(R.string.tablet_library_feature, media.source.displayName),
                    color = Muted, style = MaterialTheme.typography.labelLarge)
            }
            Text(media.title, color = Color.White, fontSize = 38.sp, lineHeight = 43.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(media.subtitle, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodyMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            media.overview?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color.White.copy(alpha = .82f), fontSize = 15.sp, lineHeight = 23.sp,
                    maxLines = if (shortWindow) 2 else 3, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(onClick = { onOpen(media.id) }, interactionSource = actionInteraction,
                modifier = Modifier.heightIn(min = 48.dp).focusOutline(actionInteraction, RoundedCornerShape(24.dp))
                .testTag("tablet-feature-open"), colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White, contentColor = Ink)) {
                Text(stringResource(R.string.media_view_details))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 12.dp).size(18.dp))
            }
        }
    }
}
