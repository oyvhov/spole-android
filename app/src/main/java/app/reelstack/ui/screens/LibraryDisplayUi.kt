package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.LibraryArtType
import app.reelstack.data.model.LibraryCardSize
import app.reelstack.data.model.LibraryDisplay
import app.reelstack.data.model.LibraryView
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Text as TextColor

/**
 * How a library is drawn, kept per library and read straight from preferences.
 *
 * The choice belongs to the library, not to the app: a film wall wants posters, a recordings
 * library wants thumbs. Storing it by id is what makes both true at once.
 */
@Composable
fun rememberLibraryDisplay(libraryId: String): Pair<LibraryDisplay, (LibraryDisplay) -> Unit> {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { AppPreferencesRepository(context) }
    var value by remember(libraryId) { mutableStateOf(repository.libraryDisplay(libraryId)) }
    return value to { updated: LibraryDisplay ->
        repository.setLibraryDisplay(libraryId, updated)
        value = updated
    }
}

/**
 * The display controls.
 *
 * One wrapping row, not a settings page: these live above the grid the whole time they are open, so
 * every row they take is a row of covers the reader cannot see. Chips are as wide as their label
 * rather than stretched to equal widths, the whole set stays visible so a D-pad reaches any of it
 * in one move, and each change saves immediately — there is no Save button, because the result is
 * on screen behind it.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LibraryDisplayPanel(
    display: LibraryDisplay,
    onChange: (LibraryDisplay) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier.fillMaxWidth().testTag("library-display-panel"),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Segments(
            label = stringResource(R.string.library_view),
            options = LibraryView.entries,
            selected = display.view,
            tag = "view",
            name = { stringResource(if (it == LibraryView.GRID) R.string.library_view_grid else R.string.library_view_list) },
        ) { onChange(display.copy(view = it)) }

        Segments(
            label = stringResource(R.string.library_size),
            options = LibraryCardSize.entries,
            selected = display.size,
            tag = "size",
            name = {
                stringResource(
                    when (it) {
                        LibraryCardSize.SMALL -> R.string.library_size_small
                        LibraryCardSize.MEDIUM -> R.string.library_size_medium
                        LibraryCardSize.LARGE -> R.string.library_size_large
                    },
                )
            },
        ) { onChange(display.copy(size = it)) }

        Segments(
            label = stringResource(R.string.library_art),
            options = LibraryArtType.entries,
            selected = display.artType,
            tag = "art",
            name = {
                stringResource(
                    when (it) {
                        LibraryArtType.AUTO -> R.string.library_art_auto
                        LibraryArtType.POSTER -> R.string.library_art_poster
                        LibraryArtType.THUMB -> R.string.library_art_thumb
                        LibraryArtType.BANNER -> R.string.library_art_banner
                        LibraryArtType.LOGO -> R.string.library_art_logo
                    },
                )
            },
        ) { onChange(display.copy(artType = it)) }

        // Titles are one binary choice, so it is a chip like the rest rather than a switch row.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.library_titles), style = MaterialTheme.typography.labelSmall, color = Muted)
            Chip(
                text = stringResource(if (display.showTitles) R.string.library_titles_on else R.string.library_titles_off),
                chosen = display.showTitles,
                tag = "library-titles-toggle",
            ) { onChange(display.copy(showTitles = !display.showTitles)) }
        }
    }
}

/** One labelled group of mutually exclusive chips. Shared with the filter bar. */
@Composable
internal fun <T> Segments(
    label: String,
    options: List<T>,
    selected: T,
    tag: String,
    name: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
        Row(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            options.forEach { option ->
                Chip(
                    text = name(option),
                    chosen = option == selected,
                    tag = "library-$tag-${(option as Enum<*>).name}",
                    role = Role.RadioButton,
                ) { onSelect(option) }
            }
        }
    }
}

@Composable
fun Chip(
    text: String,
    chosen: Boolean,
    tag: String,
    role: Role = Role.Button,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(9.dp)
    Box(
        Modifier.heightIn(min = 36.dp)
            .clip(shape)
            .background(if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .focusOutline(interaction, shape)
            .selectable(
                selected = chosen,
                interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = role,
                onClick = onClick,
            )
            .testTag(tag)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            // The selected chip sits on the accent, so its label takes the accent's own contrast
            // colour rather than the page's warm white.
            color = if (chosen) MaterialTheme.colorScheme.onPrimary else TextColor,
            maxLines = 1,
        )
    }
}

/**
 * Swaps the image type in an already-built Jellyfin artwork address.
 *
 * The feed asks for `Primary`; changing the type client-side keeps the whole thing to one rewritten
 * query rather than a new round of requests through the data layer. `AUTO` leaves the address as the
 * server built it, which is why it is the default.
 */
fun LibraryArtType.applyTo(url: String?): String? {
    val address = url ?: return null
    val wanted = api ?: return address
    return address.replace(Regex("/Images/(Primary|Thumb|Banner|Logo|Backdrop)"), "/Images/$wanted")
}
