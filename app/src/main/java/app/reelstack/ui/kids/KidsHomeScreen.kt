package app.reelstack.ui.kids

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.LibraryIcon
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.vector
import app.reelstack.ui.theme.*

/**
 * Clean, charming and polished home screen for kids.
 *
 * Designed specifically for young children with large touch surfaces (64+ dp),
 * distinct library artwork tiles ("Barnefilmer", "Barne-TV"), smooth TV focus navigation,
 * and high-contrast readable typography.
 */
@Composable
fun KidsHomeScreen(
    keepWatching: List<LibraryMedia>,
    yourShows: List<LibraryMedia>,
    libraries: List<RemoteLibraryView> = emptyList(),
    source: ServiceKind = ServiceKind.JELLYFIN,
    onPlay: (LibraryMedia) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var selectedLibraryId by remember(libraries.map { it.id }) {
        mutableStateOf<String?>(libraries.firstOrNull()?.id)
    }

    if (keepWatching.isEmpty() && yourShows.isEmpty()) {
        KidsEmptyState(modifier = modifier)
        return
    }

    val allLabel = stringResource(R.string.library_all)
    val yourShowsLabel = stringResource(R.string.kids_your_shows)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize().testTag("kids-home"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── 1. Biblioteksbilete (Library Pictures) ──────────────────────────
        if (libraries.isNotEmpty()) {
            fullWidthItem(columns) {
                KidsSectionTitle(stringResource(R.string.nav_library))
            }
            fullWidthItem(columns) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                    modifier = Modifier.testTag("kids-libraries"),
                ) {
                    items(libraries, key = { it.id }) { library ->
                        KidsLibraryCard(
                            library = library,
                            selected = library.id == selectedLibraryId,
                            source = source,
                            onSelect = { selectedLibraryId = library.id },
                        )
                    }

                    if (libraries.size > 1) {
                        item(key = "all-libraries") {
                            KidsAllLibraryCard(
                                selected = selectedLibraryId == null,
                                onSelect = { selectedLibraryId = null },
                            )
                        }
                    }
                }
            }
        }

        // ── 2. Hald fram (Continue Watching) ────────────────────────────────
        if (keepWatching.isNotEmpty()) {
            fullWidthItem(columns) {
                KidsSectionTitle(stringResource(R.string.kids_keep_watching))
            }
            fullWidthItem(columns) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
                    modifier = Modifier.testTag("kids-keep-watching"),
                ) {
                    items(keepWatching, key = { it.id }) { media ->
                        KidsWideCard(media = media, onPlay = { onPlay(media) })
                    }
                }
            }
        }

        // ── 3. Rutenett for valt bibliotek / innhald ─────────────────────────
        val selectedView = libraries.firstOrNull { it.id == selectedLibraryId }
        val displayShows = if (selectedLibraryId == null) {
            yourShows
        } else {
            yourShows.filter { it.libraryId == selectedLibraryId }
        }

        if (displayShows.isNotEmpty()) {
            val sectionHeading = when {
                selectedView != null -> selectedView.name
                selectedLibraryId == null && libraries.isNotEmpty() -> allLabel
                else -> yourShowsLabel
            }

            fullWidthItem(columns) {
                KidsSectionTitle(sectionHeading)
            }
            items(displayShows, key = { it.id }) { media ->
                KidsPosterCard(media = media, onPlay = { onPlay(media) })
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
private fun KidsSectionTitle(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Primary),
        )
        Text(
            text = text,
            color = app.reelstack.ui.theme.Text,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Prominent 16:9 library picture card displaying server artwork (e.g. Barnefilmer, Barne-TV).
 */
@Composable
private fun KidsLibraryCard(
    library: RemoteLibraryView,
    selected: Boolean,
    source: ServiceKind,
    onSelect: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(20.dp)
    val accent = Primary
    val scale = rememberKidsFocusScale(focused)
    val libraryIcon = LibraryIcon.forCollection(library.collectionType).vector()

    Column(
        modifier = Modifier
            .width(220.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onSelect,
            )
            .testTag("kids-library-${library.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .kidsFocusLift(focused, scale, accent)
                .clip(shape)
                .background(SurfaceRaised)
                .then(
                    if (selected) Modifier.border(3.dp, accent, shape)
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                ),
        ) {
            if (!library.artworkUrl.isNullOrBlank()) {
                MediaArtwork(
                    url = library.artworkUrl,
                    contentDescription = library.name,
                    source = source,
                    fallbackRes = R.drawable.media_placeholder,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(SurfaceRaised, Color(0xFF1F2833))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = libraryIcon,
                        contentDescription = null,
                        tint = if (selected) accent else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            // Bottom vignette scrim for high-contrast legible text
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.88f),
                            )
                        )
                    ),
            )

            // Title and Icon at bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = libraryIcon,
                    contentDescription = null,
                    tint = if (selected) accent else Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = library.name,
                    color = if (selected) accent else Color.White,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Selected pill badge in top corner
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
    }
}

/**
 * "All" library card to view combined content without filtering.
 */
@Composable
private fun KidsAllLibraryCard(
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(20.dp)
    val accent = Primary
    val scale = rememberKidsFocusScale(focused)

    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onSelect,
            )
            .testTag("kids-library-all"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .kidsFocusLift(focused, scale, accent)
                .clip(shape)
                .background(
                    if (selected) Brush.linearGradient(listOf(SurfaceRaised, Color(0xFF263238)))
                    else Brush.linearGradient(listOf(SurfaceRaised, Color(0xFF192026)))
                )
                .then(
                    if (selected) Modifier.border(3.dp, accent, shape)
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                Icon(
                    imageVector = SpoleIcons.Library,
                    contentDescription = null,
                    tint = if (selected) accent else Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.library_all),
                    color = if (selected) accent else Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Wide 16:9 artwork card for partly-watched media with progress line and crisp title.
 */
@Composable
private fun KidsWideCard(media: LibraryMedia, onPlay: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(20.dp)
    val accent = Primary
    val scale = rememberKidsFocusScale(focused)

    Column(
        modifier = Modifier
            .width(280.dp)
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
private fun KidsPosterCard(media: LibraryMedia, onPlay: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(20.dp)
    val accent = Primary
    val scale = rememberKidsFocusScale(focused)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
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

@Composable
private fun KidsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(
                    Brush.radialGradient(
                        listOf(Primary.copy(alpha = 0.25f), SurfaceRaised)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                SpoleIcons.Kids,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.kids_empty_title),
            color = app.reelstack.ui.theme.Text,
            fontSize = 26.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.kids_empty_body),
            color = Muted,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
    }
}
