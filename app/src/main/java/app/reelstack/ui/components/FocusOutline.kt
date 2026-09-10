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

/** TV artwork owns focus feedback; a Material state layer must never tint its caption. */
@Composable
internal fun mediaCardIndication(): androidx.compose.foundation.Indication? {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val television = configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    return if (television) null else androidx.compose.foundation.LocalIndication.current
}

/** Draw-only keyboard focus. No permanent poster border, scale, added focus target or layout shift. */
@Composable
internal fun Modifier.focusOutline(source: InteractionSource, shape: Shape): Modifier {
    val focused by source.collectIsFocusedAsState()
    val alpha by animateFloatAsState(if (focused) 1f else 0f, tween(110), label = "focus-outline")
    val style = app.reelstack.ui.theme.LocalPersonalization.current.focusStyle
    val color = if (style == app.reelstack.data.model.FocusStyle.ACCENT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    return border(if (style == app.reelstack.data.model.FocusStyle.BOLD) 5.dp else 3.dp, color.copy(alpha = alpha), shape)
}
