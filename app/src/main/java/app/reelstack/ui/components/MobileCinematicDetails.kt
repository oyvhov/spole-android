package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.ContentDetails
import app.reelstack.ui.theme.Surface

/** Phone identity lives on the artwork; facts and controls remain real, accessible content. */
@Composable
internal fun MobileCinematicDetails(
    opening: ContentDetails,
    details: ContentDetails,
    ready: Boolean,
    metadata: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 340.dp).testTag("mobile-detail-hero")) {
        MediaArtwork(
            url = app.reelstack.data.network.detailBackdropUrl(opening, details),
            contentDescription = null,
            fallbackRes = if (opening.remoteId == null && opening.artworkUrl == null) opening.artworkRes else 0,
            source = opening.source, contentScale = ContentScale.Crop, protectAspectRatio = false,
            alignment = Alignment.TopCenter, crossfadeDurationMillis = 180,
            modifier = Modifier.matchParentSize().testTag("mobile-detail-backdrop"),
        )
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Surface.copy(alpha = .12f), .35f to Surface.copy(alpha = .25f),
            .65f to Surface.copy(alpha = .88f), 1f to Surface)))
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 180.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            var failedLogo by remember(details.key, details.logoUrl) { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth().heightIn(min = 72.dp), contentAlignment = Alignment.Center) {
                if (!details.logoUrl.isNullOrBlank() && !failedLogo) {
                    MediaArtwork(details.logoUrl, opening.title,
                        Modifier.fillMaxWidth(.88f).height(72.dp).testTag("detail-title"),
                        contentScale = ContentScale.Fit, source = details.source,
                        trimTransparent = true, onError = { failedLogo = true })
                } else Text(opening.title, style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("detail-title"))
            }
            val subtitle = episodeLine(details.season, details.episode, opening.subtitle)
            if (subtitle.isNotBlank()) Text(subtitle,
                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("mobile-detail-subtitle"))
            metadata()
        }
    }
}
