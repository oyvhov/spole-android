package app.reelstack.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.LocalPersonalization

/** One scrolling page keeps remote focus, artwork and the episode rail in the same coordinate space. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun TvCinematicDetails(
    details: ContentDetails,
    scroll: ScrollState,
    heading: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val options = LocalPersonalization.current
    val largeText = LocalDensity.current.fontScale >= 1.5f
    BoxWithConstraints(Modifier.fillMaxSize().background(Ink).testTag("tv-cinematic-detail")) {
        val heroHeight = maxHeight * .76f
        if (options.detailBackdrop) {
            Box(Modifier.fillMaxWidth().height(maxHeight)) {
                MediaArtwork(details.backdropUrl ?: details.artworkUrl, null, Modifier.matchParentSize(),
                    fallbackRes = details.artworkRes, contentScale = ContentScale.Crop,
                    source = details.source, protectAspectRatio = false)
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                    0f to Ink.copy(alpha = if (options.highContrast) 1f else .96f),
                    .55f to Ink.copy(alpha = if (options.highContrast) .98f else .76f),
                    1f to Color.Transparent)))
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                    0f to Color.Transparent, .55f to Ink.copy(alpha = .16f), .92f to Ink)))
            }
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides DetailBringIntoView) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).testTag("detail-scroll")) {
                Box(Modifier.fillMaxWidth().heightIn(min = heroHeight).testTag("tv-detail-hero")) {
                    Column(Modifier.fillMaxWidth(if (largeText) .92f else .62f)
                        .padding(start = 40.dp, top = 24.dp, end = 20.dp, bottom = 20.dp)) { heading() }
                }
                Column(Modifier.fillMaxWidth().padding(start = 40.dp, end = 32.dp, bottom = 40.dp), content = content)
            }
        }
    }
}

@Composable
internal fun DetailLogo(details: ContentDetails, title: String) {
    var failed by remember(details.key, details.logoUrl) { mutableStateOf(false) }
    if (!details.logoUrl.isNullOrBlank() && !failed) {
        MediaArtwork(details.logoUrl, title, Modifier.width(280.dp).height(72.dp).testTag("detail-title"),
            contentScale = ContentScale.Fit, source = details.source, onError = { failed = true })
    } else {
        Text(title, style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.testTag("detail-title"))
    }
}

@Composable
internal fun TvDetailSynopsis(details: ContentDetails) {
    val overview = details.overview?.takeIf(String::isNotBlank) ?: return
    var expanded by rememberSaveable(details.key) { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyMedium
    val label = stringResource(if (expanded) R.string.details_less else R.string.details_read_more)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        val iconSpace = with(LocalDensity.current) { 32.dp.roundToPx() }
        val overflows = measurer.measure(overview, style = style, maxLines = 2,
            overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = (constraints.maxWidth - iconSpace).coerceAtLeast(1)))
            .hasVisualOverflow
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .focusOutline(interaction, RoundedCornerShape(8.dp))
            .clickable(enabled = overflows || expanded, interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current, role = Role.Button,
                onClickLabel = label) { expanded = !expanded }.testTag("overview-expand"),
            verticalAlignment = Alignment.CenterVertically) {
            Text(overview, style = style, maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).testTag("overview-text"))
            if (overflows || expanded) Icon(if (expanded) SpoleIcons.ChevronUp else SpoleIcons.ChevronDown,
                label, Modifier.padding(start = 8.dp).size(24.dp))
        }
    }
}
