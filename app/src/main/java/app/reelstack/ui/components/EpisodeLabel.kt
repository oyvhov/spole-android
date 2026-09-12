package app.reelstack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.reelstack.R

/**
 * "S19 E09" written the way a person would say it.
 *
 * The server's own line is a filename — a padded season and episode number, sometimes followed by
 * an episode name that repeats the number. Only the screen knows which language to write it in, so
 * the numbers travel as numbers and the sentence is built here: "Sesong 19 · Episode 9 – Getaway
 * Sticks". Without numbers there is nothing to improve on, and the server's line is used unchanged.
 */
@Composable
internal fun episodeLine(season: Int?, episode: Int?, subtitle: String): String {
    if (season == null && episode == null) return subtitle
    // "Sesong 13 - Ep 2", then the name after a dot. The season and the episode belong to each
    // other, so a hyphen binds them; the name is a separate thing and gets a separate mark.
    val numbers = listOfNotNull(
        season?.let { stringResource(R.string.episode_season, it) },
        episode?.let { stringResource(R.string.episode_short, it) },
    ).joinToString(" - ")
    val name = episodeTitle(subtitle, episode)
    return listOfNotNull(numbers.takeIf(String::isNotBlank), name.takeIf(String::isNotBlank))
        .joinToString(" · ")
}

/**
 * The name out of a server's subtitle line, which carries the number first and the name after a
 * dot. The cleaning rules themselves live in [app.reelstack.data.model.episodeNameOf], because the
 * parser applies them to a bare name before any line is built.
 */
internal fun episodeTitle(subtitle: String, episode: Int?): String =
    app.reelstack.data.model.episodeNameOf(subtitle.substringAfter(" · ", ""), episode)
