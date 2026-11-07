package app.reelstack.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
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
    Layout(modifier = modifier, content = {
        Text(title, color = TextColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 19.sp, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis,
            modifier = titleModifier)
        if (hasSubtitle) Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
            maxLines = subtitleMaxLines, overflow = TextOverflow.Ellipsis,
            modifier = subtitleModifier.padding(top = 2.dp))
        // Measure the same line boxes as the visible title. Converting sp to dp alone can
        // underestimate their real height at large text sizes. This child is never placed.
        Text(title, color = TextColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 19.sp, minLines = reserveTitleLines, maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics {})
        // The episode line's room, measured like the title's reserve and never placed.
        if (tv) Text("", fontSize = 12.sp, lineHeight = 17.sp, minLines = 1, maxLines = 1,
            modifier = Modifier.padding(top = 2.dp).clearAndSetSemantics {})
    }) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val heading = measurables[0].measure(loose)
        val subtitle = if (hasSubtitle) measurables[1].measure(loose) else null
        val titleReserve = measurables[if (hasSubtitle) 2 else 1].measure(loose)
        val subtitleReserve = if (tv) measurables.last().measure(loose).height else 0
        val height = maxOf(heading.height, titleReserve.height) + maxOf(subtitle?.height ?: 0, subtitleReserve)
        layout(constraints.constrainWidth(maxOf(heading.width, subtitle?.width ?: 0)),
            constraints.constrainHeight(height)) {
            heading.placeRelative(0, 0)
            subtitle?.placeRelative(0, heading.height)
        }
    }
}
