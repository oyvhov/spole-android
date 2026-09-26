package app.reelstack.ui.components

import androidx.compose.foundation.relocation.bringIntoViewRequester

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.Muted

/** Only a title already permitted by the visible library rows; never an additional server query. */
internal fun tabletFeaturedTitle(series: List<LibraryMedia>, sections: Set<HomeSection>): LibraryMedia? =
    tabletFeaturedTitles(series, sections).firstOrNull()

/** Episode titles are mapped to SeriesName by the server parser. Deduplicate across servers too. */
private const val HERO_FEATURE_COUNT = 5

internal fun tabletFeaturedTitles(candidates: List<LibraryMedia>, sections: Set<HomeSection>, allowLocalArtwork: Boolean = false): List<LibraryMedia> =
    tabletFeaturedTitles(candidates, app.reelstack.data.model.HomeLayout.fromLegacy(
        app.reelstack.data.model.HomeRow.entries, sections, showNextUp = true), allowLocalArtwork)

/** The feature follows the new-films and new-episodes rows: a server whose row is off is not featured. */
internal fun tabletFeaturedTitles(candidates: List<LibraryMedia>, layout: app.reelstack.data.model.HomeLayout,
    allowLocalArtwork: Boolean = false): List<LibraryMedia> {
    fun shown(kind: app.reelstack.data.model.HomeRowKind, source: ServiceKind) =
        layout.isVisible(app.reelstack.data.model.HomeRowKey(kind, source))
    val sections = buildSet {
        if (shown(app.reelstack.data.model.HomeRowKind.NEW_MOVIES, ServiceKind.JELLYFIN)) add(HomeSection.JELLYFIN_MOVIES)
        if (shown(app.reelstack.data.model.HomeRowKind.NEW_SERIES, ServiceKind.JELLYFIN)) add(HomeSection.JELLYFIN_SERIES)
        if (shown(app.reelstack.data.model.HomeRowKind.NEW_MOVIES, ServiceKind.EMBY)) add(HomeSection.EMBY_MOVIES)
        if (shown(app.reelstack.data.model.HomeRowKind.NEW_SERIES, ServiceKind.EMBY)) add(HomeSection.EMBY_SERIES)
    }
    return featuredBySections(candidates, sections, allowLocalArtwork)
}

private fun featuredBySections(candidates: List<LibraryMedia>, sections: Set<HomeSection>, allowLocalArtwork: Boolean): List<LibraryMedia> =
    candidates.filter { media ->
        val hasArt = app.reelstack.data.network.libraryHeroArtworkUrl(media) != null || (allowLocalArtwork && media.artworkRes != 0)
        if (!hasArt) return@filter false
        val isSeries = media.mediaType.equals("Series", ignoreCase = true) ||
            media.mediaType.equals("Episode", ignoreCase = true) ||
            media.season != null || media.episode != null ||
            media.subtitle.matches(Regex("(?i)S\\d+\\s*E\\d+.*"))
        val isMovie = media.mediaType.equals("Movie", ignoreCase = true)
        when (media.source) {
            ServiceKind.JELLYFIN -> if (isMovie) HomeSection.JELLYFIN_MOVIES in sections
                else if (isSeries) HomeSection.JELLYFIN_SERIES in sections
                else (HomeSection.JELLYFIN_SERIES in sections || HomeSection.JELLYFIN_MOVIES in sections)
            ServiceKind.EMBY -> if (isMovie) HomeSection.EMBY_MOVIES in sections
                else if (isSeries) HomeSection.EMBY_SERIES in sections
                else (HomeSection.EMBY_SERIES in sections || HomeSection.EMBY_MOVIES in sections)
            else -> false
        }
    }.distinctBy { it.title.trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT) }
        .sortedByDescending { it.heroUrl != null }.take(HERO_FEATURE_COUNT)

/** The hero title reserves two of these lines whether a logo or a heading fills the slot. */
private val TITLE_SIZE = 32.sp
private val TITLE_LINE_HEIGHT = 36.sp
private val TV_TITLE_SIZE = 26.sp
private val TV_TITLE_LINE_HEIGHT = 28.sp

/** Extend artwork into the list gutters without changing the width of the rows below it. */
internal fun Modifier.cinematicBleed(inset: Dp): Modifier = layout { measurable, constraints ->
    val margin = inset.roundToPx()
    val child = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + margin * 2,
        maxWidth = constraints.maxWidth + margin * 2))
    layout(constraints.maxWidth, child.height) { child.placeRelative(-margin, 0) }
}

/** Stable action and profile targets; only the artwork and metadata crossfade. */
@Composable
internal fun TabletLibraryFeature(media: LibraryMedia, onOpen: (String) -> Unit, modifier: Modifier = Modifier,
    account: (@Composable () -> Unit)? = null,
    candidates: List<LibraryMedia> = listOf(media), rotationEnabled: Boolean = true,
    onFocusWithin: (Boolean) -> Unit = {}) {
    val options = LocalPersonalization.current
    val titles = candidates.ifEmpty { listOf(media) }.take(HERO_FEATURE_COUNT)
    val identities = titles.map { it.id }
    // The hero follows a title, not a slot. A refresh reorders the candidates, and the old index
    // then pointed at another film while the first was still on screen. Only a title that has
    // left the list gives way, to whatever now stands in its place.
    var shownId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var position by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    val index = shownId?.let(identities::indexOf)?.takeIf { it >= 0 } ?: position.coerceIn(titles.indices)
    val selected = titles[index]
    androidx.compose.runtime.SideEffect { shownId = selected.id; position = index }
    var focused by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val motion = app.reelstack.ui.theme.LocalMotionEnabled.current
    LaunchedEffect(identities, focused, rotationEnabled, options.heroRotate, lifecycleOwner) {
        if (titles.size > 1 && !focused && rotationEnabled && options.heroRotate) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(8_000)
                    val current = shownId?.let(identities::indexOf)?.takeIf { it >= 0 } ?: position
                    shownId = identities[(current + 1) % identities.size]
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
    val featureIntoView = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    // The focused button already participates in scrolling. A second request for the entire
    // hero fought that request whenever focus returned from the sidebar.
    val compactTelevision = television || options.heroCompact || shortWindow
    val featureInteraction = remember { MutableInteractionSource() }
    val actionInteraction = remember { MutableInteractionSource() }
    // The feature was the one artwork surface with a fixed height, so choosing Compact shrank every
    // rail under it and left the hero at full size.
    val heroScale = LocalPersonalization.current.artworkSize.scale
    // Window and text scale determine the scene once. Media metadata never changes its height.
    val reservedText = with(density) {
        (if (compactTelevision) 28.sp else 36.sp).toDp() * 2 +
            20.sp.toDp() * (if (compactTelevision) 2 else 4) +
            (if (compactTelevision) 17.sp else 20.sp).toDp() * 2
    }
    // Leave enough of the first shelf in view to show that the page continues below the hero.
    // The old 76% scene pushed even the library names below a 1080p television's lower edge.
    val televisionHeight = with(density) { windowInfo.containerSize.height.toDp() } *
        if (options.heroCompact) .52f else .64f
    val sceneHeight = maxOf(if (television) televisionHeight else (if (compactTelevision) 220.dp else 330.dp) * heroScale,
        reservedText + if (compactTelevision) 80.dp else 130.dp)
    val featureSize = Modifier.height(sceneHeight)
    val featureSpacing = if (compactTelevision) 4.dp else 10.dp
    val titleSize = if (compactTelevision) TV_TITLE_SIZE else TITLE_SIZE
    val titleLineHeight = if (compactTelevision) TV_TITLE_LINE_HEIGHT else TITLE_LINE_HEIGHT
    Box(modifier.fillMaxWidth().then(featureSize)
        .bringIntoViewRequester(featureIntoView)
        .onFocusChanged {
            focused = it.hasFocus
            onFocusWithin(it.hasFocus)
        }
        .focusGroup()
        .then(if (television) Modifier else Modifier.clip(RoundedCornerShape(24.dp)))
        .background(Ink)
        // On television the whole hero used to take focus, and the only way to show that was a
        // ring round the entire picture — which is exactly what made it look like a selected cell
        // rather than a piece of artwork. The "Sjå meir" button inside is the focus target
        // instead: its neutral treatment keeps the artwork prominent. A finger
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
            (if (motion) titles else listOf(selected)).forEach { title ->
              key(title.id) {
                val opacity by animateFloatAsState(if (title.id == selected.id) 1f else 0f,
                    tween(800), label = "feature-artwork-${title.id}")
                Box(Modifier.matchParentSize().graphicsLayer { alpha = opacity }) {
                    MediaArtwork(app.reelstack.data.network.heroArtworkUrl(app.reelstack.data.network.libraryHeroArtworkUrl(title), LocalPersonalization.current.lightweightTv), null, Modifier.align(Alignment.CenterEnd).fillMaxWidth(if (television) 1f else .72f).fillMaxHeight(), fallbackRes = title.artworkRes, contentScale = ContentScale.Crop, source = title.source, protectAspectRatio = false, alignment = Alignment.TopCenter)
                }
              }
            }
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                0f to Ink, .27f to Ink, .52f to Ink.copy(alpha = .88f),
                .72f to Ink.copy(alpha = .18f), 1f to Color.Transparent)))
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                0f to Color.Transparent, .55f to Color.Transparent, 1f to Ink.copy(alpha = if (television) 1f else .6f))))
            // Above the artwork and its scrims, under every word and control. Draws nothing at all
            // unless a season is running and the ornament is switched on.
            SeasonalOrnament(Modifier.matchParentSize())
        }
        Column(Modifier.align(if (television) Alignment.CenterStart else Alignment.TopStart)
            .fillMaxWidth(if (television && density.fontScale >= 1.5f) .82f else .54f).padding(
            vertical = if (television) 32.dp else if (compactTelevision) 10.dp else if (shortWindow) 20.dp else 32.dp,
            horizontal = if (television) 40.dp else 28.dp,
        ), verticalArrangement = Arrangement.spacedBy(featureSpacing)) {
          Crossfade(selected, animationSpec = tween(if (motion) 800 else 0), label = "feature-caption") { title ->
           Column(verticalArrangement = Arrangement.spacedBy(featureSpacing)) {
            val logo = title.logoUrl.takeIf { options.heroLogo }
            var logoFailed by remember(title.id) { mutableStateOf(false) }
            // One height for both branches, measured as the two text lines the fallback always
            // reserves. A clear logo sizes itself from its own aspect ratio, so without this the
            // block was ~24 dp for a wide logo and ~72 dp for a title — and a logo that failed
            // after layout swapped one for the other, which is the jump. Measured at the current
            // font scale so the two stay equal at 2x text as well.
            val titleSlot = with(LocalDensity.current) { titleLineHeight.toDp() * 2 }
            Box(
                Modifier.fillMaxWidth().height(titleSlot),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (!logo.isNullOrBlank() && !logoFailed) {
                    MediaArtwork(
                        url = logo,
                        contentDescription = title.title,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        trimTransparent = true,
                        source = title.source,
                        onError = { logoFailed = true },
                        modifier = Modifier.height(titleSlot).width(if (television) 320.dp else 240.dp).testTag("hero-clearlogo")
                            .padding(vertical = 2.dp),
                    )
                } else {
                    Text(title.title, color = Color.White, fontSize = titleSize, lineHeight = titleLineHeight,
                        fontWeight = FontWeight.SemiBold, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            // "S06 E13 · Gjennom Ild Og Vann" reads as a filename. With the numbers carried as
            // numbers the hero writes them the way a person says them — and says it in exactly the
            // words the shelves below use, because the same episode appears in both and two
            // spellings of one fact on one screen is the kind of thing you cannot stop seeing.
            val numbers = episodeLine(title.season, title.episode, "")
            if (!compactTelevision) Text(
                numbers,
                minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = .72f),
                style = MaterialTheme.typography.labelLarge,
            )
            // Films use the metadata row below for facts. Episodes keep their readable title here,
            // so season and episode numbers are shown once and in the same language everywhere.
            val name = episodeTitle(title.subtitle, title.episode)
            val episodeCaption = if (compactTelevision) listOf(numbers, name).filter(String::isNotBlank).joinToString(" · ") else name
            if (episodeCaption.isNotBlank()) Text(episodeCaption,
                color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodyMedium,
                minLines = if (compactTelevision) 1 else 2,
                maxLines = if (compactTelevision) 1 else 2,
                overflow = TextOverflow.Ellipsis)
            HeroMetadataRow(title)
            Text(title.overview.orEmpty(), color = Color.White.copy(alpha = .78f),
                fontSize = if (compactTelevision) 13.sp else 14.sp,
                lineHeight = if (compactTelevision) 17.sp else 20.sp,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
           }
          }
            // The hero is usually showing something you are already part-way through. Starting or
            // resuming it is therefore the leading, accent-coloured action; details remain one
            // D-pad press to the right and keep their stable identity while the hero rotates.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selected.playableNow()) {
                    val resuming = (selected.progress ?: 0f) > 0f
                    Button(
                        onClick = {
                            app.reelstack.player.JellyfinPlayerActivity.open(
                                context, selected.remoteId.orEmpty(), source = selected.source,
                            )
                        },
                        modifier = Modifier.heightIn(min = app.reelstack.ui.theme.ReelLayout.ControlMinHeight)
                            .testTag("tablet-feature-play"),
                    ) {
                        Icon(app.reelstack.ui.components.SpoleIcons.PlaySimple, null, Modifier.size(18.dp))
                        Text(stringResource(if (resuming) R.string.tv_resume else R.string.player_play),
                            Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }
                FilledTonalButton(onClick = { onOpen(selected.id) }, interactionSource = actionInteraction,
                    modifier = Modifier.heightIn(min = app.reelstack.ui.theme.ReelLayout.ControlMinHeight)
                    .testTag("tablet-feature-open"), colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = .10f), contentColor = Color.White)) {
                    Text(stringResource(R.string.feature_more), style = MaterialTheme.typography.labelLarge)
                    Icon(app.reelstack.ui.components.SpoleIcons.ArrowForward, null, Modifier.padding(start = 8.dp).size(16.dp))
                }
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

/** One restrained line keeps the hero informative without turning it into a details sheet. */
@Composable
private fun HeroMetadataRow(title: LibraryMedia) {
    val preferences = LocalPersonalization.current
    val type = title.facts.firstOrNull()?.takeUnless { it.startsWith("S") }
    val year = title.facts.firstOrNull { it.matches(Regex("\\d{4}")) }
    val runtime = title.runtimeMinutes?.takeIf { it > 0 } ?: title.facts.firstNotNullOfOrNull {
        Regex("^(\\d+) min$").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull()
    }
    val certification = title.facts.firstOrNull { fact ->
        fact != type && fact != year && !fact.matches(Regex("^\\d+ min$")) &&
            !fact.startsWith("★") && !fact.matches(Regex("(?i)^S\\d+\\s+E\\d+.*"))
    }
    val facts = listOfNotNull(
        type,
        year,
        runtime?.let(::formatHeroRuntime),
        certification,
    )
    val hasRatings = preferences.showRatings &&
        (title.criticRating != null || title.tmdbRating != null || title.mdblistRating != null)
    if (facts.isEmpty() && !hasRatings) return

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("hero-metadata"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ServiceLogo(title.source, title.source.displayName, Modifier.size(18.dp))
        facts.forEachIndexed { index, fact ->
            if (index > 0) HeroMetadataDot()
            Text(fact, color = Color.White.copy(alpha = .68f), style = MaterialTheme.typography.labelMedium,
                maxLines = 1)
        }
        if (hasRatings && facts.isNotEmpty()) HeroMetadataDot()
        if (hasRatings) {
            title.tmdbRating?.let { TmdbRating(it, Modifier.testTag("hero-tmdb-rating")) }
            title.criticRating?.let { RottenTomatoesRating(it, Modifier.testTag("hero-critic-rating")) }
            title.mdblistRating?.let {
                if (title.tmdbRating != null || title.criticRating != null) HeroMetadataDot()
                Text("MDBList ${"%.1f".format(java.util.Locale.ROOT, it / 10f)}", color = Muted,
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.testTag("hero-mdblist-rating"))
            }
        }
    }
}

@Composable
private fun HeroMetadataDot() {
    Text("·", color = Color.White.copy(alpha = .35f), style = MaterialTheme.typography.labelMedium)
}

private fun formatHeroRuntime(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0 -> "${minutes}m"
        remainder == 0 -> "${hours}t"
        else -> "${hours}t ${remainder}m"
    }
}
