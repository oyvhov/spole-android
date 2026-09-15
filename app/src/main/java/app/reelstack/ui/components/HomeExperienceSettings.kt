package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.reelstack.R
import app.reelstack.data.model.*

@Composable
internal fun HomeExperienceSettings(value: Personalization, onChange: (Personalization) -> Unit) {
    SettingsGroup(stringResource(R.string.refine_group_start), stringResource(R.string.design_start_hint))
    ThemeChoice(stringResource(R.string.design_start_page), value.startInLibrary, listOf(false, true), "start-page",
        { stringResource(if (it) R.string.nav_library else R.string.nav_home) }) { onChange(value.copy(startInLibrary = it)) }
    SettingsToggleRow(stringResource(R.string.design_library_hub), stringResource(R.string.design_library_hub_hint),
        value.libraryHub, "library-hub") { onChange(value.copy(libraryHub = it)) }
}

@Composable
internal fun HeroSettings(value: Personalization, onChange: (Personalization) -> Unit) {
    SettingsGroup(stringResource(R.string.refine_group_hero))
    SettingsToggleRow(stringResource(R.string.tv_show_hero), stringResource(R.string.settings_tv_hero_hint), value.showHero, "show-hero") { onChange(value.copy(showHero = it)) }
    SettingsToggleRow(stringResource(R.string.design_hero_rotate), stringResource(R.string.refine_rotate_hint), value.heroRotate, "hero-rotate") { onChange(value.copy(heroRotate = it)) }
    SettingsToggleRow(stringResource(R.string.design_hero_logo), stringResource(R.string.refine_logo_hint), value.heroLogo, "hero-logo") { onChange(value.copy(heroLogo = it)) }
    SettingsToggleRow(stringResource(R.string.design_hero_compact), stringResource(R.string.refine_compact_hint), value.heroCompact, "hero-compact") { onChange(value.copy(heroCompact = it)) }
}

@Composable
internal fun LibraryAppearanceSettings(value: Personalization, onChange: (Personalization) -> Unit) {
    SettingsToggleRow(stringResource(R.string.design_upcoming_episodes), stringResource(R.string.design_upcoming_hint),
        value.showUpcomingEpisodes, "show-upcoming-episodes") { onChange(value.copy(showUpcomingEpisodes = it)) }
}
