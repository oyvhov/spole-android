package app.reelstack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.Personalization

@Composable
internal fun NextEpisodeSettings(value: Personalization, onChange: (Personalization) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsToggleRow(stringResource(R.string.player_next_episode), stringResource(R.string.next_episode_offer_hint),
            value.showNextEpisode, "next-episode-enabled") { onChange(value.copy(showNextEpisode = it)) }
        if (value.showNextEpisode) {
            ThemeChoice(stringResource(R.string.next_episode_lead), value.nextEpisodeLeadSeconds,
                listOf(0, 15, 30, 60, 90, 120, 180, 300), "next-episode-lead",
                { if (it == 0) stringResource(R.string.next_episode_at_end) else stringResource(R.string.next_episode_seconds, it) }) {
                onChange(value.copy(nextEpisodeLeadSeconds = it))
            }
        }
        SettingsToggleRow(stringResource(R.string.next_episode_auto), stringResource(R.string.next_episode_auto_hint),
            value.autoPlayNextEpisode, "next-episode-auto") { onChange(value.copy(autoPlayNextEpisode = it)) }
        if (value.autoPlayNextEpisode) {
            ThemeChoice(stringResource(R.string.next_episode_delay), value.nextEpisodeDelaySeconds,
                listOf(5, 10, 12, 15, 20, 30, 60), "next-episode-delay",
                { stringResource(R.string.next_episode_seconds, it) }) { onChange(value.copy(nextEpisodeDelaySeconds = it)) }
        }
    }
}
