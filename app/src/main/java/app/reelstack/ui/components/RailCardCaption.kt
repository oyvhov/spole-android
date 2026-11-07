package app.reelstack.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Text as TextColor

/** Keep the rail's caption reserve below the text, never between a title and its episode. */
@Composable
internal fun RailCardCaption(
    title: String,
    subtitle: String,
    titleMaxLines: Int,
    reserveTitleLines: Int = titleMaxLines,
    subtitleMaxLines: Int = 2,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    subtitleModifier: Modifier = Modifier,
) {
    @Suppress("NAME_SHADOWING") val subtitle = withoutPlaceholders(subtitle)
    val hasSubtitle = subtitle.isNotBlank()
    // On a television every card in a row takes the same room: one line for the episode, kept even when
    // there is none. A row is as tall as its tallest card in view, so a two-line episode name coming into view
    // grew the row and everything under it jumped (the Shield, 2026-11-07).
    val tv = isTelevision()
    @Suppress("NAME_SHADOWING") val subtitleMaxLines = if (tv) 1 else subtitleMaxLines
    // The reserves are measured, not composed. Two hidden texts that were never placed still sat in the semantics
    // tree, and Compose sorts every node there about every 100 ms while a page moves (the Shield, 2026-11-07).
    // A text measurer lays out the same line boxes as the visible title, which sp converted to dp can underestimate
    // at large text sizes.
    val measurer = androidx.compose.ui.text.rememberTextMeasurer(cacheSize = 4)
    val base = androidx.compose.material3.LocalTextStyle.current
    val titleStyle = base.merge(androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp))
    val subtitleStyle = base.merge(androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 17.sp))
    val subtitleGap = with(androidx.compose.ui.platform.LocalDensity.current) { 2.dp.roundToPx() }
    Layout(modifier = modifier, content = {
        Text(title, color = TextColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 19.sp, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis,
            modifier = titleModifier)
        if (hasSubtitle) Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
            maxLines = subtitleMaxLines, overflow = TextOverflow.Ellipsis,
            modifier = subtitleModifier.padding(top = 2.dp))
    }) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val heading = measurables[0].measure(loose)
        val subtitle = if (hasSubtitle) measurables[1].measure(loose) else null
        val width = androidx.compose.ui.unit.Constraints(maxWidth = loose.maxWidth)
        val titleReserve = if (reserveTitleLines > 0) measurer.measure(
            List(reserveTitleLines) { "Ag" }.joinToString(separator = Char(10).toString()),
            titleStyle, constraints = width).size.height else 0
        // The episode line's room, kept on a television even when there is no episode line.
        val subtitleReserve = if (tv) measurer.measure("Ag", subtitleStyle, maxLines = 1, constraints = width).size.height + subtitleGap else 0
        val height = maxOf(heading.height, titleReserve) + maxOf(subtitle?.height ?: 0, subtitleReserve)
        layout(constraints.constrainWidth(maxOf(heading.width, subtitle?.width ?: 0)),
            constraints.constrainHeight(height)) {
            heading.placeRelative(0, 0)
            subtitle?.placeRelative(0, heading.height)
        }
    }
}
