package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.components.*

@Composable
internal fun HomeRowFormats(value: Personalization, onChange: (Personalization) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SettingsActionRow(stringResource(R.string.library_art), stringResource(R.string.refine_row_format_hint), "home-formats") { open = true }
    if (open) AlertDialog(onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.library_art)) },
        confirmButton = { SpoleSecondaryButton(onClick = { open = false }) { Text(stringResource(R.string.action_close)) } },
        text = { Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeRow.entries.filter { it in setOf(HomeRow.CONTINUE_WATCHING, HomeRow.NEXT_UP,
                HomeRow.FAVOURITES, HomeRow.JELLYFIN_MOVIES, HomeRow.JELLYFIN_SERIES, HomeRow.EMBY_MOVIES, HomeRow.EMBY_SERIES) }.forEach { row ->
                ThemeChoice(homeRowTitle(row, false), value.homeRowFormats[row.name] ?: "AUTO",
                    listOf("AUTO", "POSTER", "THUMB"), "row-format-$row", {
                        stringResource(when (it) { "POSTER" -> R.string.library_art_poster; "THUMB" -> R.string.library_art_thumb; else -> R.string.library_art_auto })
                    }) { onChange(value.copy(homeRowFormats = value.homeRowFormats + (row.name to it))) }
            }
        } })
}
