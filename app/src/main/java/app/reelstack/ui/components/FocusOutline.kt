package app.reelstack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Draw-only keyboard focus. No permanent poster border, scale, added focus target or layout shift. */
@Composable
internal fun Modifier.focusOutline(source: InteractionSource, shape: Shape): Modifier {
    val focused by source.collectIsFocusedAsState()
    val alpha by animateFloatAsState(if (focused) 1f else 0f, tween(110), label = "focus-outline")
    return border(3.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), shape)
}
