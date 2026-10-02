package app.reelstack.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    val index = shownId?.let(identities::indexOf)?.takeIf { it >= 0 } ?: position.coerceIn(titles.indices)
    val selected = titles[index]
    SideEffect { shownId = selected.id; position = index }

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val motion = LocalMotionEnabled.current

    LaunchedEffect(identities, rotationEnabled, motion, lifecycleOwner) {
        if (titles.size > 1 && rotationEnabled && motion) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(8_000)
                    val current = shownId?.let(identities::indexOf)?.takeIf { it >= 0 } ?: position
                    shownId = identities[(current + 1) % identities.size]
                }
            }
        }
    }

    val featureInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 450.dp, max = 520.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Ink)
            .clickable(
                interactionSource = featureInteraction,
                indication = mediaCardIndication(),
                onClick = { onOpen(selected.id) }
            )
            .testTag("mobile-library-feature")
    ) {
        // ── 1. Parallax artwork layer ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationY = parallaxOffset
                }
        ) {
            (if (motion) titles else listOf(selected)).forEach { title ->
                key(title.id) {
                    val incoming = title.id == selected.id
                    val opacity by animateFloatAsState(
                        targetValue = if (incoming) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = if (motion && incoming) 500 else 0,
                            delayMillis = if (motion && !incoming) 500 else 0
                        ),
                        label = "mobile-hero-art-${title.id}"
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .zIndex(if (incoming) 1f else 0f)
                            .graphicsLayer { alpha = opacity }
                    ) {
                        MediaArtwork(
                            url = app.reelstack.data.network.heroArtworkUrl(
                                app.reelstack.data.network.libraryHeroArtworkUrl(title),
                                false
                            ),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            fallbackRes = title.artworkRes,
                            contentScale = ContentScale.Crop,
                            source = title.source,
                            protectAspectRatio = false,
                            alignment = Alignment.TopCenter,
                            crossfadeDurationMillis = 0,
                        )
                    }
                }
            }

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

        // ── 3. Integrated Top Header ─────────────────────────────────────────
        if (header != null) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(horizontal = ReelLayout.Gutter, vertical = 8.dp)
                    .testTag("mobile-hero-header")
            ) {
                header()
            }
        }

        // ── 4. Hero details and primary actions ──────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = ReelLayout.Gutter, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Crossfade(
                targetState = selected,
                animationSpec = tween(if (motion) 300 else 0),
                label = "mobile-hero-caption"
            ) { title ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val logo = title.logoUrl
                    var logoFailed by remember(title.id) { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp, max = 64.dp),
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
                            color = Primary,
                            trackColor = Color.White.copy(alpha = 0.25f),
                        )
                    }
                }
            }

            // Touch action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
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

                Spacer(Modifier.weight(1f))

                // Carousel dot indicators
                if (titles.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        titles.forEachIndexed { i, t ->
                            val isCurrent = i == index
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 8.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) Primary else Color.White.copy(alpha = 0.35f)
                                    )
                                    .clickable {
                                        shownId = t.id
                                        position = i
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
