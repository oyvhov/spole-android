package app.reelstack.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp

/** Anchored menus share the same surfaces as full choice dialogs. Material owns dismissal. */
@Composable
internal fun SpoleDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(expanded, onDismissRequest, modifier.widthIn(min = 240.dp, max = 360.dp),
        shape = RoundedCornerShape(24.dp), containerColor = containerColor,
        tonalElevation = 0.dp, shadowElevation = 8.dp) {
        Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

/** A whole row is one action. Text can grow, and disabled rows never receive a click. */
@Composable
internal fun SpoleDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp).settingsSurface(interaction, selected = selected == true)
        .semantics { if (selected != null) this.selected = selected }
        .clickable(enabled = enabled, interactionSource = interaction, indication = LocalIndication.current,
            role = if (selected == null) Role.Button else Role.RadioButton, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CompositionLocalProvider(LocalContentColor provides colors.onSurface.copy(alpha = if (enabled) 1f else .38f)) {
            if (leadingIcon != null) Box(Modifier.size(36.dp),
                contentAlignment = Alignment.Center) { leadingIcon() }
            Box(Modifier.weight(1f)) { ProvideTextStyle(MaterialTheme.typography.bodyLarge, text) }
            if (trailingIcon != null) trailingIcon()
        }
    }
}
