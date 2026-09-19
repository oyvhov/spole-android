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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import app.reelstack.data.model.LibraryIcon
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.vector
import app.reelstack.ui.components.focusOutline
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
    loading: Boolean = false,
    error: Boolean = false,
    onRetry: () -> Unit = {},
) {
    var selectedLibraryId by rememberSaveable(libraries.map { it.id }) {
        mutableStateOf<String?>(null)
    }
    val firstFocus = remember { FocusRequester() }
    val featured = keepWatching.firstOrNull() ?: yourShows.firstOrNull()
    LaunchedEffect(featured?.id) {
        if (featured != null && columns > 2) {
            withFrameNanos { }
            firstFocus.requestFocus()
        }
    }

    if (keepWatching.isEmpty() && yourShows.isEmpty()) {
        Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            if (loading) {
                androidx.compose.material3.CircularProgressIndicator()
                Text("Finn fram historiene dine …", Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
            } else {
                Text(if (error) "Vi fekk ikkje henta historiene dine" else "Her kjem eventyra dine",
                    Modifier.padding(24.dp), style = MaterialTheme.typography.headlineSmall)
                Text(if (error) "Prøv ein gong til, eller spør ein vaksen om hjelp." else "Spør ein vaksen om å sjekke biblioteket ditt.",
                    Modifier.padding(horizontal = 24.dp), textAlign = TextAlign.Center)
                app.reelstack.ui.components.SpoleSecondaryButton(onClick = onRetry,
                    modifier = Modifier.padding(24.dp).heightIn(min = 64.dp)) { Text("Prøv igjen") }
            }
        }
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
        if (featured != null) fullWidthItem(columns) {
            KidsFeaturedCard(featured, keepWatching.isNotEmpty(), columns > 2,
                Modifier.focusRequester(firstFocus)) { onPlay(featured) }
        }
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
                    item(key = "all-libraries") {
                        KidsLibraryChip("Alt", selectedLibraryId == null) { selectedLibraryId = null }
                    }
                    items(libraries, key = { it.id }) { library ->
                        KidsLibraryChip(library.name, library.id == selectedLibraryId) { selectedLibraryId = library.id }
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
        } else if (selectedLibraryId != null) {
            fullWidthItem(columns) {
                Text("Ingen historier her enno. Vel eit anna bibliotek.", color = Muted,
                    modifier = Modifier.padding(vertical = 24.dp))
            }
        }
    }
}

@Composable
private fun KidsLibraryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)
    Row(Modifier.heightIn(min = 64.dp).clip(shape)
        .background(if (selected) Primary else SurfaceRaised)
        .focusOutline(interaction, shape)
        .semantics { this.selected = selected }
        .clickable(interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (selected) Color(0xFF101211) else Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun KidsFeaturedCard(media: LibraryMedia, resume: Boolean, television: Boolean,
    modifier: Modifier, onPlay: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(28.dp)
    val scale = rememberKidsFocusScale(focused)
    Box(modifier.fillMaxWidth().padding(vertical = 10.dp)
        .kidsFocusLift(focused, scale, Primary).clip(shape).background(SurfaceRaised)
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onPlay)
        .testTag("kids-featured")) {
        MediaArtwork(url = media.heroUrl ?: media.artworkUrl ?: media.posterUrl,
            contentDescription = null, source = media.source, fallbackRes = R.drawable.media_placeholder,
            modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
            Color(0xF20A101D), Color(0xB80A101D), Color(0x150A101D)))))
        Column(Modifier.widthIn(max = if (television) 500.dp else 360.dp)
            .padding(if (television) 32.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (resume) "EVENTYRET HELD FRAM" else "KLAR FOR EI HISTORIE?", color = Color(0xFFDCE0EE),
                style = MaterialTheme.typography.labelLarge)
            Text(media.title, color = Color.White, fontSize = if (television) 34.sp else 28.sp,
                lineHeight = if (television) 40.sp else 34.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.heightIn(min = 64.dp).clip(RoundedCornerShape(18.dp)).background(Primary)
                .padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (media.isSeries) SpoleIcons.Screen else SpoleIcons.Play, null, tint = Color(0xFF101211))
                Text(if (media.isSeries) "Vel episode" else if (resume) "Sjå vidare" else "Sjå no",
                    color = Color(0xFF101211), fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
