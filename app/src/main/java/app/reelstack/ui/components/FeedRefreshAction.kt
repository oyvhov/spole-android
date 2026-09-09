package app.reelstack.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.ui.theme.Muted

/** A remote-accessible alternative to pulling the feed; keeps focus during refresh. */
@Composable
internal fun FeedRefreshAction(status: String, refreshing: Boolean, onRefresh: () -> Unit,
    modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier) {
        Text(status, color = Muted)
        TextButton(
            onClick = { if (!refreshing) onRefresh() },
            interactionSource = interaction,
            modifier = Modifier.heightIn(min = 48.dp).focusOutline(interaction, CircleShape)
                .testTag("feed-refresh").semantics { stateDescription = status },
        ) {
            Text(stringResource(if (refreshing) R.string.home_refreshing else R.string.home_refresh_action))
        }
    }
}
