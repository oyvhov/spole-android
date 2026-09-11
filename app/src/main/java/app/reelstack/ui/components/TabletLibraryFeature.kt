package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.platform.LocalDensity
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
import app.reelstack.ui.theme.LocalPersonalization

/** Only a title already permitted by the visible library rows; never an additional server query. */
internal fun tabletFeaturedTitle(series: List<LibraryMedia>, sections: Set<HomeSection>): LibraryMedia? =
    tabletFeaturedTitles(series, sections).firstOrNull()

/** Episode titles are mapped to SeriesName by the server parser. Deduplicate across servers too. */
internal fun tabletFeaturedTitles(series: List<LibraryMedia>, sections: Set<HomeSection>): List<LibraryMedia> =
    series.filter { media ->
        !media.artworkUrl.isNullOrBlank() && when (media.source) {
            ServiceKind.JELLYFIN -> HomeSection.JELLYFIN_SERIES in sections
            ServiceKind.EMBY -> HomeSection.EMBY_SERIES in sections
            else -> false
        }
    }.distinctBy { it.title.trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT) }.take(3)

/** The hero title reserves two of these lines whether a logo or a heading fills the slot. */
private val TITLE_SIZE = 32.sp
private val TITLE_LINE_HEIGHT = 36.sp

/** Stable action and profile targets; only the artwork and metadata crossfade. */
@Composable
internal fun TabletLibraryFeature(media: LibraryMedia, onOpen: (String) -> Unit, modifier: Modifier = Modifier,
    account: (@Composable () -> Unit)? = null,
    candidates: List<LibraryMedia> = listOf(media), rotationEnabled: Boolean = true) {
    val titles = candidates.ifEmpty { listOf(media) }.take(3)
    val identities = titles.map { it.id }
    var position by remember(identities) { mutableIntStateOf(0) }
    val selected = titles[position.coerceIn(titles.indices)]
    var focused by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val motion = android.provider.Settings.Global.getFloat(context.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    LaunchedEffect(identities, focused, rotationEnabled, lifecycleOwner, motion) {
        if (titles.size > 1 && !focused && rotationEnabled && motion) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(8_000)
                    position = (position + 1) % titles.size
                }
            }
        }
    }
    // The window, not the screen. Configuration.screenHeightDp reports the display, so in split
    // screen the feature kept its tall layout inside half a window. The threshold lives in
    // WindowLayoutPolicy with every other one.
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val shortWindow = app.reelstack.ui.layout.WindowLayoutPolicy(
        widthDp = with(density) { windowInfo.containerSize.width.toDp().value },
        heightDp = with(density) { windowInfo.containerSize.height.toDp().value },
    ).useCompactFeature
    val television = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val featureInteraction = remember { MutableInteractionSource() }
    val actionInteraction = remember { MutableInteractionSource() }
    // The feature was the one artwork surface with a fixed height, so choosing Compact shrank every
    // rail under it and left the hero at full size.
    val heroScale = LocalPersonalization.current.artworkSize.scale
    Box(modifier.fillMaxWidth().heightIn(min = (if (shortWindow) 250.dp else 330.dp) * heroScale)
        .onFocusChanged { focused = it.hasFocus }
        .focusGroup()
        .clip(RoundedCornerShape(24.dp))
        .background(Ink)
        // On television the whole hero used to take focus, and the only way to show that was a
        // ring round the entire picture — which is exactly what made it look like a selected cell
        // rather than a piece of artwork. The "Sjå meir" button inside is the focus target
        // instead: a remote reaches the same title through a ring the size of a button. A finger
        // still opens the hero anywhere on it, which is why the surface stays clickable elsewhere.
        .then(
            if (television) Modifier
            else Modifier
                .focusOutline(featureInteraction, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = featureInteraction,
                    indication = mediaCardIndication(),
                    onClick = { onOpen(selected.id) },
                )
        )
        .testTag("tablet-library-feature")) {
        Box(Modifier.matchParentSize()) {
            // Keep the three bounded image requests alive so the next slide is already decoded.
            titles.forEach { title ->
              key(title.id) {
                val opacity by animateFloatAsState(if (title.id == selected.id) 1f else 0f,
                    tween(800), label = "feature-artwork-${title.id}")
                Box(Modifier.matchParentSize().graphicsLayer { alpha = opacity }) {
                    MediaArtwork(title.artworkUrl, title.artworkRes, null,
                        Modifier.align(Alignment.CenterEnd).fillMaxWidth(.72f).fillMaxHeight(),
                        contentScale = ContentScale.Crop, source = title.source)
                }
              }
            }
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                0f to Ink, .27f to Ink, .52f to Ink.copy(alpha = .88f),
                .72f to Ink.copy(alpha = .18f), 1f to Color.Transparent)))
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                0f to Color.Transparent, .65f to Color.Transparent, 1f to Ink.copy(alpha = .6f))))
        }
        Column(Modifier.fillMaxWidth(.54f).padding(vertical = if (shortWindow) 20.dp else 32.dp, horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Crossfade(selected, animationSpec = tween(800), label = "feature-caption") { title ->
           Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceLogo(title.source, null, Modifier.size(22.dp))
                Text(title.source.displayName, color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelLarge)
            }
            val logo = title.logoUrl ?: if (title.source == ServiceKind.JELLYFIN) {
                title.artworkUrl?.replace(Regex("/Images/(Primary|Thumb)\\?.*"), "/Images/Logo?maxWidth=800&quality=90")
            } else null
            var logoFailed by remember(title.id) { mutableStateOf(false) }
            // One height for both branches, measured as the two text lines the fallback always
            // reserves. A clear logo sizes itself from its own aspect ratio, so without this the
            // block was ~24 dp for a wide logo and ~72 dp for a title — and a logo that failed
            // after layout swapped one for the other, which is the jump. Measured at the current
            // font scale so the two stay equal at 2x text as well.
            val titleSlot = with(LocalDensity.current) { (TITLE_LINE_HEIGHT * 2).toDp() }
            Box(
                Modifier.fillMaxWidth().heightIn(min = titleSlot),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (!logo.isNullOrBlank() && !logoFailed) {
                    MediaArtwork(
                        url = logo,
                        contentDescription = title.title,
                        contentScale = ContentScale.Fit,
                        source = title.source,
                        onError = { logoFailed = true },
                        modifier = Modifier.heightIn(max = titleSlot).widthIn(max = 240.dp)
                            .padding(vertical = 2.dp),
                    )
                } else {
                    Text(title.title, color = Color.White, fontSize = TITLE_SIZE, lineHeight = TITLE_LINE_HEIGHT,
                        fontWeight = FontWeight.SemiBold, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            // "S06 E13 · Gjennom Ild Og Vann" reads as a filename. With the numbers carried as
            // numbers, the hero can write them the way a person says them, on two lines: the
            // season, then the episode with its name.
            if (title.season != null) Text(
                stringResource(R.string.episode_season, title.season),
                color = Color.White.copy(alpha = .72f),
                style = MaterialTheme.typography.labelLarge,
            )
            val numbered = title.episode?.let { stringResource(R.string.episode_number, it) }
            // A series without episode titles answers with the name "Episode 2", and one with them
            // answers "Episode 2 - Getaway Sticks". Both repeat the number this line already states,
            // so the shared helper takes that prefix off and the rest is the name.
            val name = episodeTitle(title.subtitle, title.episode)
            val episodeLine = listOfNotNull(numbered, name.takeIf(String::isNotBlank)).joinToString(" – ")
            Text(episodeLine,
                color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodyMedium,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(title.overview.orEmpty(), color = Color.White.copy(alpha = .78f), fontSize = 14.sp, lineHeight = 20.sp,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
           }
          }
            FilledTonalButton(onClick = { onOpen(selected.id) }, interactionSource = actionInteraction,
                modifier = Modifier.heightIn(min = 48.dp).focusOutline(actionInteraction, RoundedCornerShape(24.dp))
                .testTag("tablet-feature-open"), colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = .10f), contentColor = Color.White)) {
                Text(stringResource(R.string.feature_more), style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 8.dp).size(16.dp))
            }
        }
        if (account != null) {
            Box(Modifier.align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(16.dp)
                .testTag("feature-account-overlay")) {
                account()
            }
        }
    }
}
