package app.reelstack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.ui.theme.Divider
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted

/** A real navigation action, not a read-only text field that briefly opens an unwanted keyboard. */
@Composable
fun HomeSearchEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Ink,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("home-search"),
    ) {
        Column {
        Row(
            Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(app.reelstack.ui.components.SpoleIcons.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(21.dp))
            Text(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_search), color = Muted, fontSize = 14.sp,
                lineHeight = 20.sp, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = Divider, thickness = 1.dp)
        }
    }
}
