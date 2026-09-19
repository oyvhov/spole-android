package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.SurfaceRaised

@Composable
internal fun TvMenuSettings(value: Personalization, onChange: (Personalization) -> Unit) {
    val order = (value.menuOrder + DEFAULT_MENU).distinct().filter { it in DEFAULT_MENU }
    val names = mapOf("HOME" to stringResource(R.string.nav_home), "LIBRARY" to stringResource(R.string.nav_library),
        "DISCOVER" to stringResource(R.string.nav_discover), "ACTIVITY" to stringResource(R.string.nav_activity),
        "SETTINGS" to stringResource(R.string.nav_settings))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isTelevision()) SettingsToggleRow(stringResource(R.string.tv_hide_sidebar), stringResource(R.string.tv_hide_sidebar_help),
            value.hideTvSidebar, "tv-hide-sidebar") { onChange(value.copy(hideTvSidebar = it)) }
        SettingsGroup("Di meny", "Vel kva du vil sjå, og flytt vala opp eller ned. Endringane gjeld med ein gong i både ståande og liggjande vising.")
        Text("Heim og Innstillingar er alltid tilgjengelege. På telefon i liggjande vising ligg Innstillingar fast nedst i sidemenyen.",
            style = MaterialTheme.typography.bodyMedium, color = Muted)
        TvMenuOrderEditor(order, names, value.hiddenMenuItems, value.requiredMenu(),
            { onChange(value.copy(menuOrder = it)) },
            { id, visible -> onChange(value.withMenuVisible(id, visible)) })
        SpoleSecondaryButton(onClick = { onChange(value.copy(menuOrder = DEFAULT_MENU, hiddenMenuItems = emptySet())) }) {
            Text(stringResource(R.string.tv_reset_menu))
        }
    }
}

@Composable
private fun TvMenuOrderEditor(order: List<String>, names: Map<String, String>, hidden: Set<String>, required: Set<String>,
    onOrder: (List<String>) -> Unit, onVisible: (String, Boolean) -> Unit) {
    val focus = remember(order.toSet()) { order.associateWith { listOf(FocusRequester(), FocusRequester()) } }
    order.forEachIndexed { index, id -> key(id) {
        val interaction = remember { MutableInteractionSource() }
        val shape = RoundedCornerShape(14.dp)
        val visible = id !in hidden || id in required
        Column(Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .focusOutline(interaction, shape)
            .testTag("menu-order-card-$id")
            .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(SurfaceRaised), contentAlignment = Alignment.Center) {
                Text("${index + 1}", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            Icon(menuIcon(id), null, Modifier.size(22.dp), tint = Muted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(names.getValue(id), style = MaterialTheme.typography.titleMedium,
                    color = if (visible) MaterialTheme.colorScheme.onSurface else Muted)
                Text(when {
                    id == "SETTINGS" -> "Alltid synleg · her kan du endre menyen"
                    id == "HOME" -> "Alltid synleg · tilbake til startsida"
                    id in required -> "Alltid synleg · vald som startside"
                    visible -> "Synleg i menyen"
                    else -> "Skjult frå menyen"
                },
                    style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End) {
            Switch(checked = visible, onCheckedChange = { onVisible(id, it) }, enabled = id !in required,
                modifier = Modifier.testTag("menu-visible-$id").semantics { contentDescription = names.getValue(id) })
            Spacer(Modifier.weight(1f))
            listOf(-1, 1).forEachIndexed { button, direction ->
                val target = index + direction
                IconButton(onClick = {
                    val destination = target.coerceIn(order.indices)
                    val next = order.toMutableList().apply { removeAt(index); add(destination, id) }
                    focus.getValue(id)[if (destination == 0) 1 else if (destination == order.lastIndex) 0 else button].requestFocus()
                    onOrder(next)
                }, enabled = target in order.indices,
                    modifier = Modifier.focusRequester(focus.getValue(id)[button])
                        .testTag("menu-${if (direction < 0) "up" else "down"}-$id")) {
                    Icon(if (direction < 0) SpoleIcons.ChevronUp else SpoleIcons.ChevronDown,
                        stringResource(if (direction < 0) R.string.home_order_up else R.string.home_order_down, names.getValue(id)))
                }
            }
            }
        }
    } }
}

private fun menuIcon(id: String): ImageVector = when (id) {
    "HOME" -> SpoleIcons.Home
    "LIBRARY" -> SpoleIcons.Library
    "DISCOVER" -> SpoleIcons.Search
    "ACTIVITY" -> SpoleIcons.Activity
    else -> SpoleIcons.Settings
}
