package app.reelstack.ui.components

import androidx.compose.foundation.relocation.bringIntoViewRequester

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
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
internal const val HERO_FEATURE_COUNT = 5

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

/** Two title lines are reserved by the outer caption, outside the natural text stack. */
private val TITLE_SIZE = 32.sp
private val TITLE_LINE_HEIGHT = 36.sp
private val TV_TITLE_SIZE = 26.sp
private val TV_TITLE_LINE_HEIGHT = 28.sp

/** Native text includes font metrics that sp-to-dp line-height arithmetic does not capture. */
@Composable
internal fun measuredHeroTextHeight(style: TextStyle, lines: Int = 1): Dp {
    val measurer = rememberTextMeasurer(cacheSize = 1)
    val mergedStyle = LocalTextStyle.current.merge(style)
    val height = remember(measurer, mergedStyle, lines) {
        measurer.measure(text = List(lines) { "Ag" }.joinToString("\n"), style = mergedStyle,
            softWrap = false, maxLines = lines).size.height
    }
    return with(LocalDensity.current) { height.toDp() }
}

/** Keep late logos from moving actions; spare title space follows the complete caption. */
@Composable
internal fun HeroCaptionGroup(
    reservedTitleHeight: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    reservedBodyHeight: Dp = 0.dp,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = {
        Box { title() }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) { content() }
    }) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val heading = measurables[0].measure(loose)
        val body = measurables[1].measure(loose)
        val gap = if (body.height > 0 || reservedBodyHeight > 0.dp) spacing.roundToPx() else 0
        val height = maxOf(heading.height, reservedTitleHeight.roundToPx()) + gap +
            maxOf(body.height, reservedBodyHeight.roundToPx())
        layout(constraints.constrainWidth(maxOf(heading.width, body.width)), constraints.constrainHeight(height)) {
            heading.placeRelative(0, 0)
            body.placeRelative(0, heading.height + gap)
        }
    }
}

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
    val television = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val options = LocalPersonalization.current
    val allowRotation = if (television) !options.lightweightTv else motion
    var interaction by remember { mutableIntStateOf(0) }
    // The play button exists only for a title that can be played. When the hero turns to one that
    // cannot, the button goes; if the remote was on it, a television then hands focus to the first
    // thing on screen, which is the sidebar. Focus moves to «Sjå meir» before the turn instead.
    val playFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val openFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val playInteraction = remember { MutableInteractionSource() }
    val playFocused by playInteraction.collectIsFocusedAsState()
    val playFocusedNow by androidx.compose.runtime.rememberUpdatedState(playFocused)
    // A TV remote leaves focus on a button even when idle. Pause on actual key input instead
    // of freezing the carousel for as long as that focus remains (beta11). System animation scale
    // and calm transitions control the crossfade; only lightweight TV mode stops rotation.
    LaunchedEffect(identities, focused && !television, interaction, rotationEnabled, allowRotation, lifecycleOwner) {
        if (titles.size > 1 && (!focused || television) && rotationEnabled && allowRotation) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(8_000)
                    val current = shownId?.let(identities::indexOf)?.takeIf { it >= 0 } ?: position
                    val next = (current + 1) % identities.size
                    if (playFocusedNow && !titles[next].playableNow()) runCatching { openFocus.requestFocus() }
                    shownId = identities[next]
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
    val featureIntoView = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    // The focused button already participates in scrolling. A second request for the entire
    // hero fought that request whenever focus returned from the sidebar.
    val compactTelevision = television || shortWindow
    val featureInteraction = remember { MutableInteractionSource() }
    val actionInteraction = remember { MutableInteractionSource() }
    // The feature was the one artwork surface with a fixed height, so choosing Compact shrank every
    // rail under it and left the hero at full size.
    val heroScale = LocalPersonalization.current.artworkSize.scale
    val titleSize = if (compactTelevision) TV_TITLE_SIZE else TITLE_SIZE
    val titleLineHeight = if (compactTelevision) TV_TITLE_LINE_HEIGHT else TITLE_LINE_HEIGHT
    val titleSlot = measuredHeroTextHeight(TextStyle(fontSize = titleSize, lineHeight = titleLineHeight,
        fontWeight = FontWeight.SemiBold), 2)
    val overviewStyle = TextStyle(fontSize = if (compactTelevision) 13.sp else 14.sp,
        lineHeight = if (compactTelevision) 17.sp else 20.sp)
    // Window and text scale reserve the scene before its artwork has loaded.
    val reservedText = titleSlot +
        measuredHeroTextHeight(MaterialTheme.typography.bodyMedium, if (compactTelevision) 2 else 4) +
        measuredHeroTextHeight(overviewStyle, 2)
    // Reserve room for the first shelf's heading, artwork and captions at normal text size.
    // Large text can grow the scene instead of clipping its controls.
    val televisionHeight = with(density) { windowInfo.containerSize.height.toDp() } * .44f
    val sceneHeight = maxOf(if (television) televisionHeight else (if (compactTelevision) 220.dp else 330.dp) * heroScale,
        reservedText + if (compactTelevision) 80.dp else 130.dp)
    // The caption slot measures every carousel candidate. Its real height can exceed the text
    // estimate at large font sizes; a fixed touch height then squeezed the actions down to 16 dp.
    val featureSize = Modifier.heightIn(min = sceneHeight)
    val featureSpacing = if (television) 8.dp else if (compactTelevision) 4.dp else 10.dp
    val metadataReserve = heroMetadataReservedHeight()
    val numberLine = measuredHeroTextHeight(MaterialTheme.typography.labelLarge)
    val episodeReserve = measuredHeroTextHeight(MaterialTheme.typography.bodyMedium, if (compactTelevision) 1 else 2)
    val overviewLines = if (television && density.fontScale < 1.6f) 1 else 2
    val overviewReserve = measuredHeroTextHeight(overviewStyle, overviewLines)
    Box(modifier.fillMaxWidth().then(featureSize)
        .bringIntoViewRequester(featureIntoView)
        .onFocusChanged {
            if (television && it.hasFocus && !focused) interaction++
            focused = it.hasFocus
            onFocusWithin(it.hasFocus)
        }
        .onPreviewKeyEvent {
            if (television && it.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) interaction++
            false
        }
        .focusGroup()
        .then(if (television) Modifier else Modifier.clip(RoundedCornerShape(24.dp)).background(Ink))
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
        // On a television the picture runs on under the first row and fades into the page, instead of
        // ending in a line across the screen where the hero stops. It fades by its own transparency,
        // so a seasonal page shows through rather than a dark band in the hero's colour.
        Box(Modifier.matchParentSize().then(if (television) Modifier.heroOverflow(.6f) else Modifier).testTag("feature-artwork")) {
          Box(Modifier.matchParentSize()) {
            // Predecode the bounded carousel. Keep the outgoing image opaque beneath the incoming
            // layer until its fade finishes, avoiding a dark dip halfway through the transition.
            (if (motion) titles else listOf(selected)).forEach { title ->
              key(title.id) {
                val incoming = title.id == selected.id
                val duration = if (television) 1100 else 500
                val opacity by animateFloatAsState(if (incoming) 1f else 0f,
                    tween(durationMillis = if (motion && incoming) duration else 0,
                        delayMillis = if (motion && !incoming) duration else 0), label = "feature-artwork-${title.id}")
                Box(Modifier.matchParentSize().zIndex(if (incoming) 1f else 0f).graphicsLayer { alpha = opacity }) {
                    MediaArtwork(app.reelstack.data.network.heroArtworkUrl(app.reelstack.data.network.libraryHeroArtworkUrl(title), LocalPersonalization.current.lightweightTv), null, Modifier.align(Alignment.CenterEnd).fillMaxWidth(if (television) 1f else .72f).fillMaxHeight(), fallbackRes = title.artworkRes, contentScale = ContentScale.Crop, source = title.source, protectAspectRatio = false, alignment = Alignment.TopCenter, crossfadeDurationMillis = 0)
                }
              }
            }
          }
            if (!television) {
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                    0f to Ink, .27f to Ink, .52f to Ink.copy(alpha = .88f),
                    .72f to Ink.copy(alpha = .18f), 1f to Color.Transparent)))
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                    0f to Color.Transparent, .55f to Color.Transparent, 1f to Ink.copy(alpha = .6f))))
            }
            // Above the artwork and its scrims, under every word and control. Draws nothing at all
            // unless a season is running and the ornament is switched on.
            SeasonalOrnament(Modifier.matchParentSize())
        }
        Column(Modifier.align(if (television) Alignment.CenterStart else Alignment.TopStart)
            .fillMaxWidth(if (television && density.fontScale >= 1.5f) .82f else .54f).padding(
            vertical = if (television) 24.dp else if (compactTelevision) 10.dp else if (shortWindow) 20.dp else 32.dp,
            horizontal = if (television) 40.dp else 28.dp,
        ), verticalArrangement = Arrangement.spacedBy(featureSpacing)) {
          // Compose the bounded caption stack ahead of time: logos are ready and the actions
          // keep their position when a film replaces an episode with a taller caption.
          Box(Modifier.fillMaxWidth().testTag("feature-caption")) {
           titles.forEach { title -> key(title.id) {
            val incoming = title.id == selected.id
            val captionAlpha by animateFloatAsState(if (incoming) 1f else 0f,
                tween(if (!motion) 0 else if (incoming) 420 else 160,
                    delayMillis = if (motion && incoming) 160 else 0), label = "feature-caption-${title.id}")
           // Series and entries whose type is not yet known may receive episode fields later.
           val reserveEpisodeCaption = title.season != null || title.episode != null || !title.mediaType.equals("Movie", true)
           // The old per-field line reserves live after the complete, naturally stacked body.
           // Reserve even before late metadata arrives, including the single-candidate case.
           val bodyReserve = metadataReserve + featureSpacing + overviewReserve +
               (if (!compactTelevision) numberLine + featureSpacing else 0.dp) +
               (if (reserveEpisodeCaption) episodeReserve + featureSpacing else 0.dp)
           HeroCaptionGroup(titleSlot, featureSpacing, Modifier.fillMaxWidth().graphicsLayer { alpha = captionAlpha }
               .focusProperties { canFocus = incoming }
               .then(if (incoming) Modifier.testTag("feature-current-caption") else Modifier.clearAndSetSemantics {}),
               reservedBodyHeight = bodyReserve, title = {
            val logo = title.logoUrl
            var logoFailed by remember(title.id, logo) { mutableStateOf(false) }
            var logoRatio by remember(title.id, logo) { mutableStateOf<Float?>(null) }
            val logoWidth = if (television) 320.dp else 240.dp
                if (!logo.isNullOrBlank() && !logoFailed) {
                    MediaArtwork(
                        url = logo,
                        contentDescription = title.title,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomStart,
                        trimTransparent = true,
                        source = title.source,
                        onError = { logoFailed = true },
                        onAspectRatio = { logoRatio = it },
                        crossfadeDurationMillis = 0,
                        modifier = Modifier.height(logoRatio?.let { minOf(titleSlot, logoWidth / it) } ?: titleSlot)
                            .width(logoWidth).testTag("hero-clearlogo"),
                    )
                } else {
                    Text(title.title, color = Color.White, fontSize = titleSize, lineHeight = titleLineHeight,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("hero-title"))
                }
           }) {
            // "S06 E13 · Gjennom Ild Og Vann" reads as a filename. With the numbers carried as
            // numbers the hero writes them the way a person says them — and says it in exactly the
            // words the shelves below use, because the same episode appears in both and two
            // spellings of one fact on one screen is the kind of thing you cannot stop seeing.
            val numbers = episodeLine(title.season, title.episode, "")
            if (!compactTelevision && numbers.isNotBlank()) Text(
                numbers,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = .72f),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("hero-episode-number"),
            )
            // Films use the metadata row below for facts. Episodes keep their readable title here,
            // so season and episode numbers are shown once and in the same language everywhere.
            val name = readableMediaText(episodeTitle(title.subtitle, title.episode)).orEmpty()
            val episodeCaption = if (compactTelevision) listOf(numbers, name).filter(String::isNotBlank).joinToString(" · ") else name
            if (episodeCaption.isNotBlank()) Text(episodeCaption,
                color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodyMedium,
                maxLines = if (compactTelevision) 1 else 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("hero-episode-info"))
            HeroMetadataRow(title, interactive = incoming)
            readableMediaText(title.overview)?.let { overview -> Text(overview, color = Color.White.copy(alpha = .78f),
                fontSize = if (compactTelevision) 13.sp else 14.sp,
                lineHeight = if (compactTelevision) 17.sp else 20.sp,
                maxLines = if (television && density.fontScale < 1.6f) 1 else 2, overflow = TextOverflow.Ellipsis)
            }
           }
           } }
          }
            // The hero is usually showing something you are already part-way through. Starting or
            // resuming it is therefore the leading, accent-coloured action; details remain one
            // D-pad press to the right and keep their stable identity while the hero rotates.
            // On a television the buttons have no touch margin, so each ring lies on its own pill.
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalMinimumInteractiveComponentSize provides
                if (television) 0.dp else androidx.compose.material3.LocalMinimumInteractiveComponentSize.current) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selected.playableNow()) {
                    val resuming = (selected.progress ?: 0f) > 0f
                    Button(
                        onClick = {
                            app.reelstack.player.JellyfinPlayerActivity.open(
                                context, selected.remoteId.orEmpty(), source = selected.source,
                            )
                        },
                        interactionSource = playInteraction,
                        modifier = Modifier.heightIn(min = app.reelstack.ui.theme.ReelLayout.ControlMinHeight)
                            .focusRing(ButtonDefaults.shape)
                            .focusRequester(playFocus)
                            .testTag("tablet-feature-play"),
                    ) {
                        Icon(app.reelstack.ui.components.SpoleIcons.PlaySimple, null,
                            Modifier.size(18.dp).testTag("tablet-feature-play-icon"))
                        Text(stringResource(if (resuming) R.string.tv_resume else R.string.player_play),
                            Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }
                FilledTonalButton(onClick = { onOpen(selected.id) }, interactionSource = actionInteraction,
                    modifier = Modifier.heightIn(min = app.reelstack.ui.theme.ReelLayout.ControlMinHeight)
                    .focusRing(ButtonDefaults.filledTonalShape)
                    .focusRequester(openFocus)
                    .testTag("tablet-feature-open"), colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = .10f), contentColor = Color.White)) {
                    Text(stringResource(R.string.feature_more), style = MaterialTheme.typography.labelLarge)
                    Icon(app.reelstack.ui.components.SpoleIcons.ArrowForward, null,
                        Modifier.padding(start = 8.dp).size(16.dp).testTag("tablet-feature-open-icon"))
                }
            }
            }
        }
        if (account != null) {
            val playable = selected.playableNow()
            Box(Modifier.align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(16.dp)
                // Down from the account goes to the hero's own buttons. The sidebar is nearer by
                // distance, so without this the remote left the page from the top-right corner.
                .focusProperties { down = if (playable) playFocus else openFocus }
                .testTag("feature-account-overlay")) {
                account()
            }
        }
    }
}

/** Includes both plain facts and a late rating mark without changing the caption's outer frame. */
@Composable
internal fun heroMetadataReservedHeight(): Dp = maxOf(
    measuredHeroTextHeight(MaterialTheme.typography.labelMedium), measuredHeroTextHeight(MaterialTheme.typography.bodyMedium),
    measuredHeroTextHeight(MaterialTheme.typography.labelSmall) + 4.dp, 22.dp)

/** One restrained line keeps the hero informative without turning it into a details sheet. */
@Composable
internal fun HeroMetadataRow(title: LibraryMedia, interactive: Boolean = true) {
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
    val hasRatings = (title.criticRating != null || title.tmdbRating != null || title.mdblistRating != null)
    if (facts.isEmpty() && !hasRatings) return

    Row(
        // A finger can slide a long line of facts; the remote never stops on it. A scrolling row is
        // focusable by itself, with no ring to show it, and the incoming caption's row was where
        // a television landed on arrival and on the way back from the menu. When the hero then
        // turned, that row stopped being focusable and Android handed focus to the sidebar.
        Modifier.fillMaxWidth()
            .then(if (isTelevision()) Modifier else Modifier.horizontalScroll(rememberScrollState(), enabled = interactive))
            .testTag("hero-metadata"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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

/**
 * The hero's picture on a television: [extra] of the hero's height taller, overflowing below it, and
 * faded by its own transparency. Clear of the words on the left, whole in the upper right, and gone
 * by the time it reaches the first row's posters.
 */
internal fun Modifier.heroOverflow(extra: Float): Modifier = this
    .layout { measurable, constraints ->
        val height = (constraints.maxHeight * (1f + extra)).toInt()
        val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(constraints.maxWidth, height))
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
    }
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(Brush.horizontalGradient(0f to Color.Transparent, .27f to Color.Transparent,
            .52f to Color.Black.copy(alpha = .14f), .72f to Color.Black.copy(alpha = .8f), 1f to Color.Black),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
        drawRect(Brush.verticalGradient(0f to Color.Black, .42f to Color.Black, .7f to Color.Black.copy(alpha = .45f), 1f to Color.Transparent),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
    }
