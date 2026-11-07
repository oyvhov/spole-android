package app.reelstack.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * A row that runs on under the page's end [margin] to the edge of the screen, where its own fade ([rowEdgeFade])
 * lets it go, as Home's shelves do.
 *
 * Within the margin a row stopped 32 dp short of the edge and cut a card through there: «Neste» on the library pages,
 * «Medvirkende» and the episodes on the detail pages (Loftet, 2026-11-07). The row keeps its start where the page
 * puts it; only its end goes further, so right to left it runs on to the left edge instead.
 */
internal fun Modifier.bleedToEnd(margin: Dp): Modifier = layout { measurable, constraints ->
    val extra = margin.roundToPx()
    if (!constraints.hasBoundedWidth || extra <= 0) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
    val placeable = measurable.measure(constraints.copy(minWidth = constraints.minWidth + extra,
        maxWidth = constraints.maxWidth + extra))
    layout(constraints.maxWidth, placeable.height) { placeable.placeRelative(0, 0) }
}
