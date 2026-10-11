package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.LibraryArtType
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.SmartShelfIcon
import app.reelstack.data.model.SmartShelfKinds
import app.reelstack.data.model.SmartShelfPeriod
import app.reelstack.data.model.libraryArtworkUrl
import app.reelstack.data.model.librarySizedArtwork
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.ui.standardGenreResource

/**
 * What a screen can ask of the smart shelves. Provided once beside the view model, like
 * [LocalOpenSeasonShelf], so the library page and its builder need no callbacks threaded through.
 */
internal interface SmartShelfActions {
    fun open(id: String)
    fun edit(id: String? = null, template: String? = null)
    fun update(draft: SmartShelf)
    fun applyTemplate(template: String)
    fun save()
    fun delete(id: String)
    fun close()
    /** Asks the server for its genres and tags again, after it did not answer. */
    fun retryFacets()
    /** Saves the order a shelf's page shows. */
    fun sort(id: String, sort: app.reelstack.data.model.SmartShelfSort) {}
}

internal val LocalSmartShelfActions = staticCompositionLocalOf<SmartShelfActions?> { null }

/** The language's own name for a preset; null for the owner's shelves, which carry their own. */
internal fun smartShelfPresetName(id: String): Int? = when (id) {
    "halloween" -> R.string.theme_season_halloween
    "christmas" -> R.string.theme_season_christmas
    "christmas-calendar" -> R.string.smart_preset_christmas_calendar
    "easter-crime" -> R.string.smart_preset_easter_crime
    else -> null
}

@Composable
internal fun smartShelfName(shelf: SmartShelf): String =
    shelf.name.ifBlank { smartShelfPresetName(shelf.id)?.let { stringResource(it) } ?: stringResource(R.string.smart_shelf_untitled) }

internal fun SmartShelfIcon.vector(): ImageVector = when (this) {
    SmartShelfIcon.STAR -> SpoleIcons.Star
    SmartShelfIcon.HEART -> SpoleIcons.Heart
    SmartShelfIcon.PUMPKIN -> SpoleIcons.Pumpkin
    SmartShelfIcon.SNOWFLAKE -> SpoleIcons.Snowflake
    SmartShelfIcon.CALENDAR -> SpoleIcons.Calendar
    SmartShelfIcon.MOVIE -> SpoleIcons.Movie
    SmartShelfIcon.SCREEN -> SpoleIcons.Screen
    SmartShelfIcon.KIDS -> SpoleIcons.Kids
    SmartShelfIcon.ANIMATION -> SpoleIcons.Animation
    SmartShelfIcon.DOCUMENTARY -> SpoleIcons.Documentary
    SmartShelfIcon.MUSIC -> SpoleIcons.Music
    SmartShelfIcon.SPORT -> SpoleIcons.Sport
}

/** A genre the way Spole names it elsewhere; the server's own word when it is not a standard one. */
@Composable
internal fun genreLabel(genre: String): String = standardGenreResource(genre)?.let { stringResource(it) } ?: genre

@Composable
internal fun smartShelfKindsLabel(kinds: SmartShelfKinds): String = stringResource(when (kinds) {
    SmartShelfKinds.BOTH -> R.string.smart_shelf_kinds_both
    SmartShelfKinds.MOVIES -> R.string.smart_shelf_kinds_movies
    SmartShelfKinds.SERIES -> R.string.smart_shelf_kinds_series
})

@Composable
internal fun smartShelfPeriodLabel(period: SmartShelfPeriod): String = stringResource(when (period) {
    SmartShelfPeriod.ALWAYS -> R.string.smart_shelf_period_always
    SmartShelfPeriod.HALLOWEEN -> R.string.smart_shelf_period_halloween
    SmartShelfPeriod.CHRISTMAS -> R.string.smart_shelf_period_christmas
    SmartShelfPeriod.ADVENT -> R.string.smart_shelf_period_advent
    SmartShelfPeriod.EASTER -> R.string.smart_shelf_period_easter
})

/** The rule in one line: what it looks for, and what narrows it. */
@Composable
internal fun smartShelfRule(shelf: SmartShelf): String {
    val tags = shelf.tags.joinToString(", ")
    val genres = app.reelstack.data.model.distinctGenres(shelf.genres).map { genreLabel(it) }.distinct().joinToString(", ")
    val asks = when {
        shelf.combinesBoth -> stringResource(
            if (shelf.requiresBoth) R.string.smart_shelf_rule_all else R.string.smart_shelf_rule_any, tags, genres)
        shelf.tags.isNotEmpty() -> stringResource(R.string.smart_shelf_rule_tags, tags)
        shelf.genres.isNotEmpty() -> stringResource(R.string.smart_shelf_rule_genres, genres)
        else -> null
    }
    return listOfNotNull(
        asks,
        if (shelf.kinds != SmartShelfKinds.BOTH) smartShelfKindsLabel(shelf.kinds) else null,
        if (shelf.unwatched) stringResource(R.string.smart_shelf_rule_unwatched) else null,
    ).joinToString("  ·  ")
}

/**
 * The library page's row of smart shelves, with a tile to make a new one at the end.
 *
 * A tile is made of the shelf's own posters, so a shelf reads as what it holds rather than as a
 * folder icon with a name under it.
 */
@Composable
internal fun SmartShelfRow(shelves: List<SmartShelf>, peeks: Map<String, List<RemoteLibraryItem>>, source: ServiceKind,
    onOpen: (String) -> Unit, onNew: () -> Unit, modifier: Modifier = Modifier) {
    // The row reaches into the page margin and pads itself back, so a focused tile can grow
    // without its own row cutting it off; the first tile still lines up with the heading.
    LazyRow(modifier.bleedStart(16.dp).testTag("smart-shelf-row"), horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
        items(shelves, key = { it.id }) { shelf ->
            SmartShelfTile(shelf, peeks[shelf.id].orEmpty(), source) { onOpen(shelf.id) }
        }
        item(key = "new") { NewSmartShelfButton(onNew) }
    }
}

@Composable
private fun tileWidth() = if (isTelevision()) 240.dp else 220.dp

/** How many posters stand side by side on a tile. Three portrait posters fill a two-to-one tile exactly. */
private const val TILE_POSTERS = 3

/**
 * A tile is the shelf's own posters: three of them standing side by side fill it edge to edge, and
 * the name stands on a shade along the bottom. Nothing else is written on it; the rule is in the
 * builder. A slot without a poster keeps the accent's wash, so a shelf with no art still has a tile.
 * The tile is two to one, and only grows for a name that needs two lines at a large text size.
 */
@Composable
private fun SmartShelfTile(shelf: SmartShelf, items: List<RemoteLibraryItem>, source: ServiceKind, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val name = smartShelfName(shelf)
    val colors = MaterialTheme.colorScheme
    val scale = focusScale(focused, false)
    val width = tileWidth()
    val posters = items.mapNotNull { libraryArtworkUrl(it, LibraryArtType.POSTER) }.distinct().take(TILE_POSTERS)
    Box(Modifier.width(width).heightIn(min = width / 2)
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape)
        .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.surfaceVariant)))
        .focusOutline(interaction, shape)
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .semantics { contentDescription = name }
        .testTag("smart-shelf-${shelf.id}")) {
        Row(Modifier.matchParentSize()) {
            posters.forEach { url ->
                MediaArtwork(librarySizedArtwork(url, 240), null, Modifier.weight(1f).fillMaxHeight(), source = source)
            }
            if (posters.size < TILE_POSTERS) Spacer(Modifier.weight((TILE_POSTERS - posters.size).toFloat()))
        }
        if (posters.isEmpty()) Icon(shelf.icon.vector(), null, Modifier.align(Alignment.Center).size(36.dp),
            tint = colors.onSurfaceVariant.copy(alpha = .5f))
        // The shade rises from the bottom, so the name reads over any poster and the tops stay clear.
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color.Transparent, .4f to Color.Transparent, 1f to colors.background.copy(alpha = .94f))))
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(shelf.icon.vector(), null, Modifier.size(18.dp), tint = colors.onSurface)
            Text(name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).testTag("smart-shelf-name-${shelf.id}"))
        }
    }
}

/**
 * The way to another shelf: a quiet plus after the last tile, not a tile of its own. A row of
 * shelves stays a row of what they hold; the words come out only while the remote is on it.
 */
@Composable
private fun NewSmartShelfButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val label = stringResource(R.string.smart_shelf_new)
    Row(Modifier.heightIn(min = 48.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (focused) 1f else .45f))
        .focusOutline(interaction, CircleShape)
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }
        .testTag("smart-shelf-new")
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(SpoleIcons.Add, null, Modifier.size(24.dp),
            tint = if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        if (focused) Text(label, Modifier.padding(start = 8.dp, end = 6.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** «38 filmar og 4 seriar», or as much of it as there is. */
@Composable
internal fun smartShelfCounts(movies: Int, series: Int): String = listOfNotNull(
    if (movies > 0) pluralStringResource(R.plurals.smart_shelf_movies_count, movies, movies) else null,
    if (series > 0) pluralStringResource(R.plurals.smart_shelf_series_count, series, series) else null,
).joinToString(stringResource(R.string.smart_shelf_and))

/** Lay a horizontal row out [inset] further left than its slot, keeping its right edge where it was. */
internal fun Modifier.bleedStart(inset: androidx.compose.ui.unit.Dp): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth) {
        val child = measurable.measure(constraints)
        return@layout layout(child.width, child.height) { child.placeRelative(0, 0) }
    }
    val margin = inset.roundToPx()
    val width = constraints.maxWidth + margin
    val child = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, child.height) { child.placeRelative(-margin, 0) }
}
