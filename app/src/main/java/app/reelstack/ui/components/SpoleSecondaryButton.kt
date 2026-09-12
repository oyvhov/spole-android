package app.reelstack.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
internal fun isTelevision(): Boolean = LocalConfiguration.current.uiMode and
    Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

internal val LocalSettingsButtonStyle = staticCompositionLocalOf { false }

/** Secondary actions share a visible boundary and remote focus, including dialog actions. */
@Composable
internal fun SpoleSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = if (isTelevision() || LocalSettingsButtonStyle.current) RoundedCornerShape(14.dp) else ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (isTelevision() || LocalSettingsButtonStyle.current) {
        val interaction = interactionSource ?: remember { MutableInteractionSource() }
        OutlinedButton(onClick, modifier.heightIn(min = 48.dp).focusOutline(interaction, shape, glow = false),
            enabled = enabled, shape = shape, interactionSource = interaction,
            border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant),
            colors = colors, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp), content = content)
    } else {
        TextButton(onClick, modifier, enabled = enabled, shape = shape, colors = colors,
            contentPadding = contentPadding, interactionSource = interactionSource, content = content)
    }
}
