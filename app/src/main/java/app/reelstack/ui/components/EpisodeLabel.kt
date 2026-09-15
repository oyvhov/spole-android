package app.reelstack.ui.components

/** Shared compact numbering on heroes, shelves, details and playback controls. */
internal fun episodeLine(season: Int?, episode: Int?, subtitle: String): String {
    if (season == null && episode == null) return subtitle
    val numbers = listOfNotNull(
        season?.let { "S$it" },
        episode?.let { "E$it" },
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
