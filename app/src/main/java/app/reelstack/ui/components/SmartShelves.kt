package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
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
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.SmartShelfIcon
import app.reelstack.data.model.SmartShelfKinds
import app.reelstack.data.model.SmartShelfPeriod
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

/**
 * What a tile says under its name: the tags and genres, without the words around them. With both,
 * «eller» or «og» stays, since that decides what is on the shelf.
 */
@Composable
internal fun smartShelfShortRule(shelf: SmartShelf): String {
    val genres = app.reelstack.data.model.distinctGenres(shelf.genres).map { genreLabel(it) }.distinct()
    if (!shelf.combinesBoth) return (shelf.tags + genres).take(3).joinToString(" · ")
    return stringResource(if (shelf.requiresBoth) R.string.smart_shelf_short_all else R.string.smart_shelf_short_any,
        shelf.tags.take(2).joinToString(" · "), genres.take(2).joinToString(" · "))
}

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
 * A tile shows a few of the shelf's own posters fanned out over a wash of the accent colour, so a
 * shelf reads as what it holds rather than as a folder icon with a name under it.
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
private fun tileWidth() = if (isTelevision()) 232.dp else 196.dp

@Composable
private fun SmartShelfTile(shelf: SmartShelf, items: List<RemoteLibraryItem>, source: ServiceKind, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    val name = smartShelfName(shelf)
    val accent = MaterialTheme.colorScheme.primary
    val scale = focusScale(focused, false)
    Box(Modifier.width(tileWidth()).aspectRatio(1.55f)
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape)
        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant)))
        .focusOutline(interaction, shape)
        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
        .semantics { contentDescription = name }
        .testTag("smart-shelf-${shelf.id}")) {
        // The fan: up to three posters leaning out from the right edge, the nearest one upright.
        val posters = items.mapNotNull { item ->
            app.reelstack.data.model.libraryArtworkUrl(item, app.reelstack.data.model.LibraryArtType.POSTER)
        }.take(3)
        Box(Modifier.fillMaxHeight().fillMaxWidth(.52f).align(Alignment.CenterEnd)) {
            posters.reversed().forEachIndexed { index, url ->
                val depth = posters.size - 1 - index
                Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight(.72f).aspectRatio(2f / 3f)
                    .offset(x = (-8 - depth * 22).dp, y = (depth * 4).dp)
                    .graphicsLayer { rotationZ = (depth - 1) * -7f + 4f }
                    .shadow(8.dp, RoundedCornerShape(8.dp), clip = false)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)) {
                    MediaArtwork(app.reelstack.data.model.librarySizedArtwork(url, 240), null, Modifier.fillMaxSize(), source = source)
                }
            }
        }
        // The name sits on a soft shade from the left, readable over any poster.
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .94f), .55f to Color.Transparent)))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp).fillMaxWidth(if (posters.isEmpty()) .9f else .5f),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(34.dp).background(accent.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(shelf.icon.vector(), null, Modifier.size(20.dp), tint = accent)
            }
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // Two lines, so «eller» or «og» and the genre after it are not lost to the ellipsis.
            Text(smartShelfShortRule(shelf), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
