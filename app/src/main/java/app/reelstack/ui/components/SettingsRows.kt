package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.ui.theme.ControlOutline
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.SwitchTrackOn
// `Text` is also the Compose composable, so the theme colour travels under its own name.
import app.reelstack.ui.theme.Text as TextColor

/**
 * The rows the phone's Settings page is built from.
 *
 * The page groups related settings into one rounded card and puts an icon beside each row. Rows
 * that carried their own background instead — the three app-update rows, which were written for the
 * television panel where the parent column spaces them out — landed on the phone with no gap at
 * all, so three cards' corners collided into one pinched shape. They live here now so that both
 * pages draw from the same set and a row cannot be borrowed into the wrong one by accident.
 */
@Composable
internal fun SettingsPreferenceRow(
    icon: ImageVector,
    label: String,
    description: String? = null,
    checked: Boolean,
    tag: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        // Top-aligned: centring an icon against a block that can wrap to three lines leaves the
        // icon floating in the middle of the text instead of next to its label.
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(vertical = 14.dp),
    ) {
        SettingsRowIcon(icon)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(label, color = TextColor, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
            description?.let { Text(it, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp)) }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            // Lime marks the thumb, not the whole track: a list of switches should not read as
            // the loudest surface in the app when none of them is an action.
            colors = SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = SwitchTrackOn,
                checkedBorderColor = SwitchTrackOn,
                uncheckedThumbColor = Muted,
                uncheckedTrackColor = SurfaceRaised,
                uncheckedBorderColor = ControlOutline,
            ),
        )
    }
}

/**
 * The same row, for a setting you open rather than switch. The value sits under the label where the
 * description would be, because on this page that second line is always what the row currently says.
 */
@Composable
internal fun SettingsPreferenceAction(
    icon: ImageVector,
    label: String,
    value: String? = null,
    tag: String? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(vertical = 14.dp),
    ) {
        SettingsRowIcon(icon)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(label, color = TextColor, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
            value?.let {
                Text(it, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Icon(SpoleIcons.ChevronRight, null, Modifier.padding(top = 2.dp).size(20.dp), tint = Primary)
    }
}

@Composable
private fun SettingsRowIcon(icon: ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceRaised),
    ) {
        Icon(icon, contentDescription = null, tint = Muted, modifier = Modifier.size(21.dp))
    }
}
