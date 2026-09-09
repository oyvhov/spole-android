package app.reelstack.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.ui.theme.Muted

/** The disclosure precedes the text, so expansion never moves the focused reading anchor. */
@Composable
internal fun ExpandableSynopsis(identity: String, title: String, overview: String?, loading: Boolean) {
    var expanded by rememberSaveable(identity) { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val measurer = rememberTextMeasurer()
    val style = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 25.sp)
    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = 174.dp)
        .padding(start = 6.dp, top = 19.dp, end = 6.dp)) {
        // Determine overflow before placement. No state-writing layout callback or size animation.
        val canExpand = overview != null && measurer.measure(overview, style = style, maxLines = 4,
            overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = constraints.maxWidth)).hasVisualOverflow
        val actionMaxWidth = maxWidth * .48f
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (canExpand || expanded) {
                    TextButton(onClick = { expanded = !expanded }, interactionSource = interaction,
                        modifier = Modifier.padding(start = 8.dp).widthIn(max = actionMaxWidth)
                            .focusOutline(interaction, CircleShape).testTag("overview-expand")) {
                        Text(stringResource(if (expanded) R.string.details_less else R.string.details_read_more))
                    }
                }
            }
            when {
                overview != null -> Text(overview, color = MaterialTheme.colorScheme.onSurface, style = style,
                    maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp).testTag("overview-text"))
                loading -> DetailTextSkeleton(Modifier.fillMaxWidth().padding(top = 14.dp))
                else -> Text(stringResource(R.string.details_no_overview), color = Muted, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
