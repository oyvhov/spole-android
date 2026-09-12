package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.LibraryIcon
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.mediaCardIndication
import app.reelstack.ui.components.vector
import app.reelstack.ui.theme.ReelLayout

/**
 * The page that lists your libraries.
 *
 * It used to be four folder tiles on a black field — on a television, three across the top and one
 * alone underneath, with the right-hand third of a two-metre screen showing nothing at all. Four
 * tiles is a menu, and a menu is what you put in a sidebar, not on a page.
 *
 * So the page shows what is *in* each library instead: a heading you can press to open it, and
 * under that heading the newest titles it holds. The libraries are still in the same order and one
 * press still opens any of them, so nothing that worked before stopped working — the difference is
 * that the reader can now see a reason to press.
 *
 * A library whose peek has not arrived, or failed, keeps its own artwork tile. That is the old page
 * for exactly the libraries the new one has nothing to say about.
 */
@Composable
internal fun LibraryLanding(
    libraries: List<app.reelstack.data.network.RemoteLibraryItem>,
    peeks: Map<String, List<LibraryMedia>>,
    /** What is half-watched anywhere in the app, so this page can find its own library's share. */
    inProgress: List<LibraryMedia>,
    icons: Map<String, LibraryIcon>,
    loading: Boolean,
    tv: Boolean,
    onOpenLibrary: (String) -> Unit,
    onOpenTitle: (String) -> Unit,
    cardActions: MediaCardActions? = null,
    connected: Boolean = true,
    error: String? = null,
) {
    val gutter = if (tv) 32.dp else 24.dp
    // A card here can be starred or marked watched, but not cleared from Continue watching: this
    // rail is not a resume shelf, it is a peek into a library.
    val shelfActions = cardActions.withoutResumeRemoval()
    LazyColumn(
        contentPadding = PaddingValues(start = gutter, end = 0.dp, top = gutter, bottom = gutter),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("library-landing"),
    ) {
        item(key = "heading") {
            Column(Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.nav_library),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    stringResource(R.string.tv_library_intro),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Not signed in, a failed listing, or a server that answered with no libraries at all. The
        // grid this page replaced said all three in its footer; a page with nothing but a heading
        // on it does not tell the reader which of the three happened.
        val emptyServer = connected && error == null && libraries.isEmpty() && !loading
        if (error != null || !connected || emptyServer) item(key = "state") {
            Text(
                error ?: stringResource(if (connected) R.string.tv_library_empty else R.string.library_connect),
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = ReelLayout.SectionTop),
            )
        }
        items(libraries, key = { it.id }) { library ->
            // What you are in the middle of here comes first. Home already knows it, so this costs
            // nothing, and "where I got to" is a better reason to open a library than "what is
            // new" — the progress bars on those cards are what tells the two apart.
            //
            // Capped, because the heading counts them: a series library with thirty episodes queued
            // said "30 på gang" above a rail that shows eight, and a number the reader cannot check
            // against what is on screen is worse than no number. Six is what a rail shows before
            // the edge of the screen, so the count and the cards agree.
            val started = inProgress.filter { it.libraryId == library.id }
                .sortedByDescending { it.progress ?: 0f }
                .take(LANDING_PROGRESS)
            val titles = (started + peeks[library.id].orEmpty()).distinctBy { it.id }
            Column(Modifier.padding(top = ReelLayout.SectionTop)) {
                LibraryHeading(
                    name = library.title,
                    started = started.size,
                    count = titles.size - started.size,
                    icon = icons[library.id] ?: LibraryIcon.forCollection(library.collectionType),
                    loading = loading && titles.isEmpty(),
                ) { onOpenLibrary(library.id) }
                if (titles.isNotEmpty()) {
                    Box(Modifier.padding(top = ReelLayout.SectionBottom)) {
                        ResumeRail(titles, onOpenTitle, shelfActions)
                    }
                } else {
                    // Nothing to show yet. The library's own artwork is still something to aim at,
                    // and it is the picture the reader recognises from every other client.
                    Box(Modifier.padding(top = ReelLayout.SectionBottom)) {
                        LibraryTile(library.artworkUrl, library.title) { onOpenLibrary(library.id) }
                    }
                }
            }
        }
    }
}

/**
 * How many half-finished titles a library's heading will name. A rail shows about eight cards
 * before the edge of the screen, and the heading has to stay true to what is under it.
 */
private const val LANDING_PROGRESS = 6

/**
 * One library's name, as the thing you press to open it.
 *
 * Wide and short rather than square: a heading is read along its line, and a remote reaching it
 * from the rail below should land on a shape the width of the words, not on a poster-sized button
 * that pushes the rail off the screen.
 */
@Composable
private fun LibraryHeading(
    name: String,
    started: Int,
    count: Int,
    icon: LibraryIcon,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .focusOutline(interaction, shape)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = mediaCardIndication(),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("library-heading-$name"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon.vector(), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOfNotNull(
                started.takeIf { it > 0 }?.let { stringResource(R.string.library_peek_progress, it) },
                when {
                    count > 0 -> pluralStringResource(R.plurals.library_peek_newest, count, count)
                    loading -> stringResource(R.string.library_peek_loading)
                    started > 0 -> null
                    else -> stringResource(R.string.library_peek_open)
                },
            ).joinToString(" · "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        Icon(
            app.reelstack.ui.components.SpoleIcons.ArrowForward,
            null,
            Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The library's own picture, at the size of one card in the rail it stands in for. */
@Composable
private fun LibraryTile(artworkUrl: String?, name: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    Box(
        Modifier
            .width(ReelLayout.EpisodeHeight * 16f / 9f)
            .height(ReelLayout.EpisodeHeight)
            .focusOutline(interaction, shape)
            .clip(shape)
            .background(Color.Black.copy(alpha = .35f))
            .clickable(
                interactionSource = interaction,
                indication = mediaCardIndication(),
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        MediaArtwork(artworkUrl, R.drawable.media_placeholder, name, Modifier.fillMaxSize(), source = ServiceKind.JELLYFIN)
    }
}
