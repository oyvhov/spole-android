package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*

@Composable
internal fun TvMenuSettings(value: Personalization, onChange: (Personalization) -> Unit) {
    val order = (value.menuOrder + DEFAULT_MENU).distinct().filter { it in DEFAULT_MENU }
    val names = mapOf("HOME" to stringResource(R.string.nav_home), "LIBRARY" to stringResource(R.string.nav_library),
        "DISCOVER" to stringResource(R.string.nav_discover), "ACTIVITY" to stringResource(R.string.nav_activity),
        "SETTINGS" to stringResource(R.string.nav_settings))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isTelevision()) SettingsToggleRow(stringResource(R.string.tv_hide_sidebar), stringResource(R.string.tv_hide_sidebar_help),
            value.hideTvSidebar, "tv-hide-sidebar") { onChange(value.copy(hideTvSidebar = it)) }
        SettingsGroup(stringResource(R.string.refine_menu_order), stringResource(R.string.refine_order_hint))
        OrderEditor(order, { names.getValue(it) }, value.hiddenMenuItems,
            setOf("HOME", "SETTINGS") + if (value.startInLibrary) setOf("LIBRARY") else emptySet(),
            "menu", { onChange(value.copy(menuOrder = it)) },
            { id, visible -> onChange(value.copy(hiddenMenuItems = if (visible) value.hiddenMenuItems - id else value.hiddenMenuItems + id)) })
        SpoleSecondaryButton(onClick = { onChange(value.copy(menuOrder = DEFAULT_MENU, hiddenMenuItems = emptySet())) }) {
            Text(stringResource(R.string.tv_reset_menu))
        }
    }
}
