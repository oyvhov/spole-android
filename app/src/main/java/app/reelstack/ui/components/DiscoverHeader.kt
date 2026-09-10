package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Search belongs beside the heading on spacious windows, not on a full-width extra row. */
@Composable
internal fun DiscoverHeader(heading: @Composable () -> Unit, account: @Composable () -> Unit,
    search: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("discover-header")) {
        val inline = app.reelstack.ui.layout.WindowLayoutPolicy(maxWidth.value, maxHeight.value).useInlineHeader && LocalDensity.current.fontScale < 1.6f
        if (inline) {
            Row(Modifier.fillMaxWidth().testTag("discover-header-wide"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.weight(.8f)) { heading() }
                Box(Modifier.weight(1.2f)) { search() }
                account()
            }
        } else {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(1f).padding(end = 12.dp)) { heading() }
                    account()
                }
                Box(Modifier.fillMaxWidth().padding(top = 18.dp)) { search() }
            }
        }
    }
}
