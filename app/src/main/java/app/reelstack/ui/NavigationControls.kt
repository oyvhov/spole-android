package app.reelstack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.reelstack.ui.theme.Primary

/** Shared row for the rail and compact touch navigation. */
@Composable
internal fun NavigationControl(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    labelAlpha: Float,
    role: Role,
    modifier: Modifier,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val television = app.reelstack.ui.components.isTelevision()
    val collapsed = labelAlpha < .05f
    val windowActive = LocalWindowInfo.current.isWindowFocused
    var showFocusLabel by remember { mutableStateOf(false) }
    LaunchedEffect(focused, collapsed, television, windowActive) {
        showFocusLabel = false
        if (focused && collapsed && television && windowActive) {
            kotlinx.coroutines.delay(220)
            showFocusLabel = true
        }
    }
    val shape = RoundedCornerShape(16.dp)
    Box {
    Row(modifier.fillMaxWidth().heightIn(min = 58.dp).clip(shape)
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        .border(if (focused) 2.dp else 0.dp, if (focused) Primary else Color.Transparent, shape)
        .semantics { contentDescription = label }
        .selectable(selected, role = role, interactionSource = interaction,
            indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (selected || focused) Primary else app.reelstack.ui.theme.Muted, modifier = Modifier.size(24.dp))
        Text(label, color = if (selected || focused) MaterialTheme.colorScheme.onSurface else app.reelstack.ui.theme.Muted,
            style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 14.dp).wrapContentWidth(Alignment.Start, unbounded = true)
                .requiredWidth(106.dp).graphicsLayer { alpha = labelAlpha }
                .clearAndSetSemantics {})
    }
    if (focused && collapsed && windowActive && showFocusLabel) {
        val offset = with(LocalDensity.current) { 64.dp.roundToPx() }
        Popup(alignment = Alignment.CenterStart, offset = IntOffset(offset, 0),
            properties = PopupProperties(focusable = false)) {
            androidx.compose.material3.Surface(shape = RoundedCornerShape(10.dp),
                color = app.reelstack.ui.theme.SurfaceRaised, shadowElevation = 4.dp) {
                Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 240.dp).padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("navigation-focus-label"))
            }
        }
    }
    }
}
