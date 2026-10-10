package app.reelstack.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.ReelLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mobile-optimised cinematic spotlight hero.
 *
 * Provides a rich edge-to-edge backdrop, depth parallax during vertical scrolling,
 * subtle ambient glow, clear logo presentation, and accessible primary touch targets.
 */
@Composable
internal fun MobileLibraryFeature(
    media: LibraryMedia,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    candidates: List<LibraryMedia> = listOf(media),
    rotationEnabled: Boolean = true,
    parallaxOffset: Float = 0f,
) {
    val titles = candidates.ifEmpty { listOf(media) }.take(HERO_FEATURE_COUNT)
    val identities = titles.map { it.id }
    var shownId by rememberSaveable { mutableStateOf<String?>(null) }
    var position by rememberSaveable { mutableIntStateOf(0) }
    // Mirror the end pages so a swipe across either end remains a short, natural movement.
    val looping = titles.size > 1
    val pager = rememberPagerState(
        initialPage = if (looping) 1 else 0,
        pageCount = { if (looping) titles.size + 2 else 1 },
    )
    fun titleIndex(page: Int) = if (looping) Math.floorMod(page - 1, titles.size) else 0
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    val scrolling by remember { derivedStateOf { pager.isScrollInProgress } }
    val lifecycleOwner = LocalLifecycleOwner.current
    val motion = LocalMotionEnabled.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var headerHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(identities) {
        val index = shownId?.let(identities::indexOf)?.takeIf { it >= 0 } ?: position.coerceIn(titles.indices)
        pager.scrollToPage(index + if (looping) 1 else 0)
    }
    LaunchedEffect(pager.settledPage, scrolling, identities) {
        if (!scrolling) {
            val index = titleIndex(pager.settledPage)
            shownId = titles[index].id
            position = index
            if (looping && pager.settledPage == 0) pager.scrollToPage(titles.size)
            else if (looping && pager.settledPage == titles.size + 1) pager.scrollToPage(1)
        }
    }
    // A finger on the hero pauses rotation; a completed gesture starts a fresh eight seconds.
    LaunchedEffect(identities, rotationEnabled, motion, lifecycleOwner, dragged, scrolling, pager.settledPage) {
        if (looping && rotationEnabled && motion && !dragged && !scrolling) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                delay(8_000)
                // Starting a scroll cancels this timer effect. The movement must outlive it.
                scope.launch { pager.animateScrollToPage(pager.settledPage + 1, animationSpec = tween(500)) }
            }
        }
    }

    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).background(Ink)
        .testTag("mobile-library-feature")) {
        HorizontalPager(
            state = pager,
            userScrollEnabled = looping,
            beyondViewportPageCount = if (looping) 1 else 0,
            key = { page -> "${if (page == 0) "leading" else if (page == titles.size + 1) "trailing" else "title"}:${titles[titleIndex(page)].id}" },
            modifier = Modifier.fillMaxWidth().testTag("mobile-hero-pager"),
        ) { page ->
            MobileHeroPage(
                selected = titles[titleIndex(page)],
                titles = titles,
                index = titleIndex(page),
                onOpen = onOpen,
                onSelect = { index -> scope.launch {
                    if (motion) pager.animateScrollToPage(index + 1, animationSpec = tween(350))
                    else pager.scrollToPage(index + 1)
                } },
                topInset = with(density) { headerHeight.toDp() } + 72.dp,
                parallaxOffset = parallaxOffset,
                modifier = if (page == pager.currentPage) Modifier else Modifier.clearAndSetSemantics {},
            )
        }
        if (header != null) {
            Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .onSizeChanged { headerHeight = it.height }
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = ReelLayout.Gutter, vertical = 8.dp)
                .testTag("mobile-hero-header")) { header() }
        }
    }
}

@Composable
private fun MobileHeroPage(
    selected: LibraryMedia,
    titles: List<LibraryMedia>,
    index: Int,
    onOpen: (String) -> Unit,
    onSelect: (Int) -> Unit,
    topInset: androidx.compose.ui.unit.Dp,
    parallaxOffset: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val featureInteraction = remember { MutableInteractionSource() }
    val heroUrl = app.reelstack.data.network.mobileHeroArtworkUrl(selected)
    var artworkFailed by remember(selected.id, heroUrl) { mutableStateOf(false) }
    val hasArtwork = !artworkFailed && (!heroUrl.isNullOrBlank() ||
        selected.remoteId == null && selected.artworkRes != 0 && selected.artworkRes != R.drawable.media_placeholder)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (hasArtwork) 390.dp else 240.dp)
            .background(Ink)
            .clickable(
                interactionSource = featureInteraction,
                indication = mediaCardIndication(),
                onClick = { onOpen(selected.id) }
            )
    ) {
        // ── 1. Parallax artwork layer ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationY = parallaxOffset
                }
        ) {
            MediaArtwork(
                url = app.reelstack.data.network.heroArtworkUrl(
                    app.reelstack.data.network.mobileHeroArtworkUrl(selected),
                    false
                ),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                fallbackRes = if (selected.remoteId == null && selected.artworkUrl == null) selected.artworkRes else 0,
                contentScale = ContentScale.Crop,
                source = selected.source,
                protectAspectRatio = false,
                alignment = Alignment.TopCenter,
                crossfadeDurationMillis = 0,
                onError = { artworkFailed = true },
            )

            // ── 2. Cinematic ambient scrims ──────────────────────────────────────
            // Top scrim: guarantees contrast for status bar and header brand/actions
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Ink.copy(alpha = 0.90f),
                            0.20f to Ink.copy(alpha = 0.52f),
                            0.45f to Color.Transparent,
                        )
                    )
            )

            // Bottom scrim: deep, dark transition melting smoothly into Ink
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.35f to Color.Transparent,
                            0.65f to Ink.copy(alpha = 0.68f),
                            0.88f to Ink.copy(alpha = 0.94f),
                            1f to Ink,
                        )
                    )
            )

            // Ambient warmth/shadow at the lower edge
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Ink.copy(alpha = 0.85f),
                        )
                    )
            )

            SeasonalOrnament(Modifier.matchParentSize())
        }

        // ── 4. Hero details and primary actions ──────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(top = if (hasArtwork) topInset else (topInset - 72.dp).coerceAtLeast(64.dp))
                .padding(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = 18.dp,
                    bottom = if (titles.size > 1) 0.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val title = selected
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val logo = title.logoUrl
                var logoFailed by remember(title.id) { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    if (!logo.isNullOrBlank() && !logoFailed) {
                        MediaArtwork(
                            url = logo,
                            contentDescription = title.title,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.BottomStart,
                            trimTransparent = true,
                            source = title.source,
                            onError = { logoFailed = true },
                            modifier = Modifier
                                .height(56.dp)
                                .width(220.dp)
                                .testTag("mobile-hero-clearlogo")
                        )
                    } else {
                        Text(
                            text = title.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("mobile-hero-title")
                        )
                    }
                }

                val episodeInfo = episodeLine(title.season, title.episode, title.subtitle)
                if (episodeInfo.isNotBlank()) {
                    Text(
                        text = episodeInfo,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HeroMetadataRow(title)

                val progress = title.progress ?: 0f
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.48f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = Color.White.copy(alpha = 0.25f),
                    )
                }
            }

            // Touch action row
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selected.playableNow()) {
                    val resuming = (selected.progress ?: 0f) > 0f
                    Button(
                        onClick = {
                            app.reelstack.player.JellyfinPlayerActivity.open(
                                context, selected.remoteId.orEmpty(), source = selected.source
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(ReelLayout.ControlCorner),
                        modifier = Modifier
                            .heightIn(min = ReelLayout.ControlMinHeight)
                            .testTag("mobile-feature-play")
                    ) {
                        Icon(
                            imageVector = SpoleIcons.PlaySimple,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(if (resuming) R.string.tv_resume else R.string.player_play),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { onOpen(selected.id) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(ReelLayout.ControlCorner),
                    modifier = Modifier
                        .heightIn(min = ReelLayout.ControlMinHeight)
                        .testTag("mobile-feature-open")
                ) {
                    Text(
                        text = stringResource(R.string.feature_more),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = SpoleIcons.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }

            }
            if (titles.size > 1) Row(Modifier.align(Alignment.CenterHorizontally)) {
                titles.forEachIndexed { i, item ->
                    val current = i == index
                    Box(Modifier.size(48.dp)
                        .semantics { contentDescription = item.title; this.selected = current }
                        .clickable(role = Role.Button) { onSelect(i) }
                        .testTag("mobile-hero-dot-$i"), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(if (current) 8.dp else 5.dp).clip(CircleShape)
                            .background(if (current) Primary else Color.White.copy(alpha = 0.35f)))
                    }
                }
            }
        }
    }
}
