package app.reelstack.ui.kids

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.KidsWorld
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.isSeries
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.cinematicBleed
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Clean, charming and polished home screen for kids.
 *
 * Designed specifically for young children with large touch surfaces (64+ dp),
 * A quiet hero, clear media rows and large touch surfaces keep the child home easy to scan.
 */
@Composable
fun KidsHomeScreen(
    keepWatching: List<LibraryMedia>,
    yourShows: List<LibraryMedia>,
    favourites: List<LibraryMedia> = emptyList(),
    suggestions: List<LibraryMedia> = emptyList(),
    libraries: List<RemoteLibraryView> = emptyList(),
    source: ServiceKind = ServiceKind.JELLYFIN,
    world: KidsWorld = KidsWorld.SPACE,
    profileButton: (@Composable () -> Unit)? = null,
    onPlay: (LibraryMedia) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    loading: Boolean = false,
    error: Boolean = false,
    onRetry: () -> Unit = {},
) {
    val television = columns > 2
    var selectedLibraryId by rememberSaveable(libraries.map { it.id }) {
        mutableStateOf(libraries.firstOrNull()?.id)
    }
    val activeLibraryId = selectedLibraryId ?: libraries.firstOrNull()?.id

    val candidates = remember(keepWatching, favourites, yourShows) {
        (keepWatching + favourites + yourShows)
            .filter { !it.remoteId.isNullOrBlank() }
            .distinctBy { it.source to it.remoteId }
    }
    val featured = candidates.firstOrNull()

    if (keepWatching.isEmpty() && yourShows.isEmpty() && favourites.isEmpty() && suggestions.isEmpty()) {
        Box(modifier.fillMaxSize()) {
            if (profileButton != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .padding(if (television) 32.dp else 16.dp)
                ) {
                    profileButton()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (loading) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color(world.glow))
                    Text(
                        "Finn fram historiene dine …",
                        Modifier.padding(24.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                } else {
                    Text(
                        if (error) "Vi fekk ikkje henta historiene dine" else "Her kjem eventyra dine",
                        Modifier.padding(24.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                    Text(
                        if (error) "Prøv ein gong til, eller spør ein vaksen om hjelp."
                        else "Spør ein vaksen om å sjekke biblioteket ditt.",
                        Modifier.padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center,
                        color = Muted,
                    )
                    app.reelstack.ui.components.SpoleSecondaryButton(
                        onClick = onRetry,
                        modifier = Modifier.padding(24.dp).heightIn(min = 64.dp)
                    ) {
                        Text("Prøv igjen")
                    }
                }
            }
        }
        return
    }

    val yourShowsLabel = stringResource(R.string.kids_your_shows)
    val gutter = if (television) 48.dp else 24.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize().testTag("kids-home"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (!television) {
            fullWidthItem(columns) {
                KidsHomeHeader(profileButton = profileButton, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
            }
        }

        if (featured != null) {
            fullWidthItem(columns) {
                KidsHero(
                    featured = featured,
                    candidates = candidates,
                    resume = keepWatching.contains(featured),
                    television = television,
                    world = world,
                    profileButton = if (television) profileButton else null,
                    modifier = if (television) Modifier.cinematicBleed(gutter) else Modifier.padding(horizontal = 4.dp),
                    onPlay = onPlay,
                )
            }
        }

        if (libraries.isNotEmpty()) {
            fullWidthItem(columns) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                    modifier = Modifier.testTag("kids-libraries"),
                ) {
                    items(libraries, key = { it.id }) { library ->
                        KidsLibraryCard(
                            library = library,
                            selected = library.id == activeLibraryId,
                            source = source,
                            world = world,
                            television = television,
                        ) { selectedLibraryId = library.id }
                    }
                }
            }
        }

        // ── 1. Hald fram ────────────────────────────────────────────────────
        if (keepWatching.isNotEmpty()) {
            fullWidthItem(columns) {
                KidsSectionTitle(stringResource(R.string.kids_keep_watching), accent = Color(world.glow))
            }
            fullWidthItem(columns) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    modifier = Modifier.testTag("kids-keep-watching"),
                ) {
                    items(keepWatching, key = { it.id }) { media ->
                        KidsWideCard(
                            media = media,
                            modifier = Modifier.width(if (television) 240.dp else 190.dp),
                            onPlay = { onPlay(media) },
                        )
                    }
                }
            }
        }

        if (favourites.isNotEmpty()) {
            fullWidthItem(columns) {
                KidsSectionTitle("Favorittar", accent = Color(world.glow))
            }
            fullWidthItem(columns) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    modifier = Modifier.testTag("kids-favourites"),
                ) {
                    items(favourites, key = { it.id }) { media ->
                        KidsPosterCard(media = media, modifier = Modifier.width(if (television) 140.dp else 115.dp), onPlay = { onPlay(media) })
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            fullWidthItem(columns) {
                KidsSectionTitle("Forslag", accent = Color(world.glow))
            }
            fullWidthItem(columns) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    modifier = Modifier.testTag("kids-suggestions"),
                ) {
                    items(suggestions, key = { it.id }) { media ->
                        KidsPosterCard(media = media, modifier = Modifier.width(if (television) 140.dp else 115.dp), onPlay = { onPlay(media) })
                    }
                }
            }
        }

        val selectedView = libraries.firstOrNull { it.id == activeLibraryId }
        val displayShows = if (activeLibraryId != null) yourShows.filter { it.libraryId == activeLibraryId } else yourShows

        if (displayShows.isNotEmpty()) {
            fullWidthItem(columns) {
                KidsSectionTitle(selectedView?.name ?: yourShowsLabel, accent = Color(world.glow))
            }
            items(displayShows, key = { it.id }) { media ->
                KidsPosterCard(media = media, onPlay = { onPlay(media) })
            }
        } else if (activeLibraryId != null) {
            fullWidthItem(columns) {
                Text("Ingen historier i dette biblioteket enno.", color = Muted, modifier = Modifier.padding(vertical = 24.dp))
            }
        }
    }
}

@Composable
private fun KidsHomeHeader(
    profileButton: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.kids_greeting_plain),
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.kids_home_hint),
                color = Muted,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }
        profileButton?.invoke()
    }
}

/**
 * TV keeps the established cinematic hero. Mobile uses the same artwork with a calmer, shorter
 * presentation so it does not take over the whole phone screen.
 */
@Composable
private fun KidsHero(
    featured: LibraryMedia,
    candidates: List<LibraryMedia>,
    resume: Boolean,
    television: Boolean = false,
    world: KidsWorld,
    profileButton: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    onPlay: (LibraryMedia) -> Unit,
) {
    val motion = LocalMotionEnabled.current
    val titles = candidates.ifEmpty { listOf(featured) }.take(5)
    var position by rememberSaveable { mutableIntStateOf(0) }
    val selected = if (television) titles[position.coerceIn(titles.indices)] else featured

    LaunchedEffect(titles.map { it.id }, motion, television) {
        if (television && titles.size > 1 && motion) {
            while (true) {
                delay(8500)
                position = (position + 1) % titles.size
            }
        }
    }

    val glow = Color(world.glow)
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }
    val heroHeight = if (television) (screenHeight * 0.48f).coerceIn(300.dp, 360.dp) else 184.dp
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .then(if (television) Modifier else Modifier.clip(shape).background(Color(0xFF151522)))
            .testTag("kids-hero"),
    ) {
        if (television) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black,
                                0.25f to Color.Black,
                                0.65f to Color.Black.copy(alpha = 0.6f),
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.20f to Color.Black.copy(alpha = 0.5f),
                                0.45f to Color.Black,
                                1f to Color.Black,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
            ) {
                titles.forEach { title ->
                    key(title.id) {
                        val opacity by animateFloatAsState(
                            targetValue = if (title.id == selected.id) 1f else 0f,
                            animationSpec = tween(if (motion) 750 else 0),
                            label = "kids-hero-fade-${title.id}",
                        )
                        if (opacity > 0f) {
                            Box(Modifier.matchParentSize().graphicsLayer { alpha = opacity }) {
                                MediaArtwork(
                                    url = title.heroUrl ?: title.artworkUrl ?: title.posterUrl,
                                    contentDescription = null,
                                    source = title.source,
                                    fallbackRes = title.artworkRes,
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    protectAspectRatio = false,
                                    alignment = Alignment.TopCenter,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            MediaArtwork(
                url = selected.heroUrl ?: selected.artworkUrl ?: selected.posterUrl,
                contentDescription = null,
                source = selected.source,
                fallbackRes = selected.artworkRes,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                protectAspectRatio = false,
                alignment = Alignment.Center,
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(listOf(Color(0xF20B0C15), Color(0xA0000000), Color.Transparent))
                )
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(world.sky).copy(alpha = .95f)))
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(if (television) 0.58f else 0.82f)
                .padding(
                    start = if (television) 48.dp else 16.dp,
                    bottom = if (television) 20.dp else 12.dp,
                    end = if (television) 24.dp else 12.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val logo = selected.logoUrl
            var logoFailed by remember(selected.id) { mutableStateOf(false) }
            if (!logo.isNullOrBlank() && !logoFailed) {
                MediaArtwork(
                    url = logo,
                    contentDescription = selected.title,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    trimTransparent = true,
                    source = selected.source,
                    onError = { logoFailed = true },
                    modifier = Modifier
                        .height(if (television) 62.dp else 34.dp)
                        .width(if (television) 280.dp else 190.dp)
                        .padding(vertical = 2.dp),
                )
            } else {
                Text(
                    text = selected.title,
                    color = Color.White,
                    fontSize = if (television) 30.sp else 19.sp,
                    lineHeight = if (television) 36.sp else 23.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val subtitle = when {
                selected.isSeries && selected.season != null && selected.episode != null ->
                    "Sesong ${selected.season} · Episode ${selected.episode}"
                selected.subtitle.isNotBlank() -> selected.subtitle
                else -> null
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = if (television) 15.sp else 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (television && !selected.overview.isNullOrBlank()) {
                Text(
                    text = selected.overview,
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val buttonInteraction = remember { MutableInteractionSource() }
            val buttonFocused by buttonInteraction.collectIsFocusedAsState()
            val buttonShape = RoundedCornerShape(20.dp)

            Row(
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .clip(buttonShape)
                    .background(if (buttonFocused) glow else Primary)
                    .border(2.dp, if (buttonFocused) Color.White else Color.Transparent, buttonShape)
                    .focusOutline(buttonInteraction, buttonShape)
                    .clickable(
                        interactionSource = buttonInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = { onPlay(selected) },
                    )
                    .padding(
                        horizontal = if (television) 26.dp else 20.dp,
                        vertical = if (television) 14.dp else 10.dp,
                    )
                    .testTag("kids-hero-play"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    if (selected.isSeries) SpoleIcons.Screen else SpoleIcons.Play,
                    contentDescription = null,
                    tint = Color(0xFF101211),
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = if (selected.isSeries) "Vel episode" else if (resume) "Sjå vidare" else "Sjå no",
                    color = Color(0xFF101211),
                    fontSize = if (television) 19.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (television && profileButton != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(32.dp)
            ) {
                profileButton()
            }
        }

    }
}

/**
 * 16:9 artwork card for libraries directly from Emby/Jellyfin.
 * Displays the library's real cover image, dark bottom vignette, legible title,
 * and high-contrast selected status.
 */
@Composable
internal fun KidsLibraryCard(
    library: RemoteLibraryView,
    selected: Boolean,
    source: ServiceKind,
    world: KidsWorld,
    television: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    val glow = Color(world.glow)
    val scale = rememberKidsFocusScale(focused)

    Column(
        modifier = modifier
            .width(if (television) 184.dp else 156.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag("kids-library-${library.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .kidsFocusLift(focused, scale, glow)
                .clip(shape)
                .background(SurfaceRaised)
                .border(
                    width = if (selected) 3.5.dp else if (focused) 2.dp else 1.dp,
                    color = if (selected) glow else if (focused) Color.White else Color.White.copy(alpha = 0.15f),
                    shape = shape,
                ),
        ) {
            // Library artwork from Emby/Jellyfin
            MediaArtwork(
                url = library.artworkUrl,
                contentDescription = library.name,
                source = source,
                fallbackRes = R.drawable.media_placeholder,
                modifier = Modifier.fillMaxSize(),
            )

            // Bottom vignette
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    ),
            )

            // Title inside card over vignette
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = library.name,
                    color = Color.White,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(glow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            SpoleIcons.Done,
                            contentDescription = null,
                            tint = Color(0xFF101211),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullWidthItem(
    columns: Int,
    content: @Composable () -> Unit,
) {
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) { content() }
}

@Composable
private fun KidsSectionTitle(text: String, accent: Color = Primary) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accent),
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Wide 16:9 artwork card for partly-watched media with progress line and crisp title.
 */
@Composable
private fun KidsWideCard(
    media: LibraryMedia,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    val accent = Primary
    val scale = rememberKidsFocusScale(focused)

    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 180.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onPlay,
            )
            .testTag("kids-card-${media.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .kidsFocusLift(focused, scale, accent)
                .clip(shape)
                .background(SurfaceRaised),
        ) {
            MediaArtwork(
                url = media.heroUrl ?: media.artworkUrl ?: media.posterUrl,
                contentDescription = media.title,
                source = media.source,
                fallbackRes = R.drawable.media_placeholder,
                modifier = Modifier.fillMaxSize(),
            )

            // Bottom vignette
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    ),
            )

            val progress = media.progress
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(Primary),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = media.title,
            color = app.reelstack.ui.theme.Text,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )

        val sub = media.subtitle.takeIf { it.isNotBlank() }
        if (sub != null) {
            Text(
                text = sub,
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

/**
 * Poster tile for the grid with clean artwork loading and high-contrast typography.
 */
@Composable
private fun KidsPosterCard(
    media: LibraryMedia,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val accent = Primary
    val scale = rememberKidsFocusScale(focused)

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onPlay,
            )
            .testTag("kids-card-${media.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .kidsFocusLift(focused, scale, accent)
                .clip(shape)
                .background(SurfaceRaised),
        ) {
            MediaArtwork(
                url = media.posterUrl ?: media.artworkUrl ?: media.heroUrl,
                contentDescription = media.title,
                source = media.source,
                fallbackRes = R.drawable.media_placeholder,
                modifier = Modifier.fillMaxSize(),
            )

            // Bottom vignette
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                        )
                    ),
            )

            // Series indicator pill
            if (media.isSeries) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Icon(
                        SpoleIcons.Screen,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = media.title,
            color = app.reelstack.ui.theme.Text,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
