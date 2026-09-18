package app.reelstack.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.reelstack.ui.theme.*

/** A shared tablet destination: touch, keyboard and D-pad use the same focus/selection model. */
@Composable
internal fun WideDestination(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)

    val targetBackground = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val animatedBackground by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(durationMillis = 140),
        label = "wide-destination-bg",
    )

    val targetTint = if (selected || focused) Primary else Muted
    val animatedTint by animateColorAsState(
        targetValue = targetTint,
        animationSpec = tween(durationMillis = 140),
        label = "wide-destination-tint",
    )

    val targetTextColor = if (selected || focused) app.reelstack.ui.theme.Text else Muted
    val animatedTextColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(durationMillis = 140),
        label = "wide-destination-text",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(animatedBackground)
            .focusOutline(interaction, shape)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = animatedTint, modifier = Modifier.size(23.dp))
        Text(
            label,
            color = animatedTextColor,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
