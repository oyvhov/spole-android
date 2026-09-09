package app.reelstack.localization

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import app.reelstack.R
import app.reelstack.data.model.RequestSeason
import app.reelstack.data.model.RequestStage
import app.reelstack.data.model.SeriesNextEpisode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Presentation only. Request eligibility and stored stage identities remain in the domain model. */
@Composable
fun requestStageLabel(stage: RequestStage): String = stringResource(when (stage) {
    RequestStage.REQUESTED -> R.string.flow_stage_requested
    RequestStage.DOWNLOADING -> R.string.flow_stage_downloading
    RequestStage.IMPORTING -> R.string.flow_stage_importing
    RequestStage.AVAILABLE -> R.string.flow_stage_available
    RequestStage.DECLINED -> R.string.flow_stage_declined
    RequestStage.FAILED -> R.string.flow_stage_failed
    RequestStage.UNKNOWN -> R.string.flow_stage_unknown
    RequestStage.WATCHING -> R.string.flow_stage_watching
})

@Composable
fun requestStageExplanation(stage: RequestStage): String = stringResource(when (stage) {
    RequestStage.REQUESTED -> R.string.flow_explain_requested
    RequestStage.DOWNLOADING -> R.string.flow_explain_downloading
    RequestStage.IMPORTING -> R.string.flow_explain_importing
    RequestStage.AVAILABLE -> R.string.flow_explain_available
    RequestStage.DECLINED -> R.string.flow_explain_declined
    RequestStage.FAILED -> R.string.flow_explain_failed
    RequestStage.UNKNOWN -> R.string.flow_explain_unknown
    RequestStage.WATCHING -> R.string.flow_explain_watching
})

@Composable
fun seasonDisplayName(season: RequestSeason): String =
    when (season.name) {
        "Sesong ${season.number}", "Season ${season.number}" -> stringResource(R.string.flow_season_name, season.number)
        "Spesialepisodar" -> stringResource(R.string.flow_specials)
        else -> season.name
    }

@Composable
private fun requestDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(LocalConfiguration.current.locales[0]))

@Composable
fun seasonDescription(season: RequestSeason, today: LocalDate = LocalDate.now()): String = when {
    season.canRequest && season.airDate == null -> stringResource(R.string.flow_season_undated)
    season.canRequest && season.airDate!! > today -> stringResource(R.string.flow_season_coming, requestDate(season.airDate))
    else -> stringResource(when (season.status) {
        5 -> R.string.flow_stage_available
        4 -> R.string.flow_season_partial
        2 -> R.string.flow_season_pending
        3 -> R.string.flow_stage_requested
        6 -> R.string.flow_season_blocked
        1, 7 -> R.string.flow_season_missing
        else -> R.string.flow_stage_unknown
    })
}

@Composable
fun nextEpisodeDescription(episode: SeriesNextEpisode, today: LocalDate = LocalDate.now()): String? =
    if (episode.airDate < today) null else stringResource(R.string.flow_next_episode,
        "S${episode.season.toString().padStart(2, '0')} E${episode.episode.toString().padStart(2, '0')}",
        requestDate(episode.airDate))
