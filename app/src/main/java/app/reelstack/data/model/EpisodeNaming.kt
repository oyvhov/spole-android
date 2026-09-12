package app.reelstack.data.model

/**
 * The part of a server's episode name that is actually a name.
 *
 * Two things arrive here pretending to be titles. An episode Jellyfin never matched keeps the
 * file's name — "71.Grader.Nord.Kjendis.S17E01.NORWEGiAN" — which tells a reader nothing the
 * episode number beside it has not already said. And an episode that *was* matched is often called
 * "Episode 9 - Getaway Sticks", repeating a number the line already states.
 *
 * This lives in the data layer rather than beside the screens because the parser needs it too: when
 * the server answers without a series name, the card falls back to the episode's own name, and that
 * fallback has to be as clean as the line underneath it.
 */
internal fun episodeNameOf(name: String, episode: Int?): String {
    // Normalise the spacing first. `\s` does not match a non-breaking space, and a server that
    // writes "Episode\u00a01" looks identical on screen to one that writes "Episode 1" — so the
    // prefix survived the strip and the row printed "1 · Episode 1", the number twice. Padded and
    // doubled spaces go the same way.
    val trimmed = name.replace('\u00a0', ' ').replace(Regex("""\s+"""), " ").trim()
    if (trimmed.isBlank()) return ""
    // All three marks have to be present — no spaces at all, three or more dots, and a
    // season-episode marker — so a real title like "S.W.A.T." is left alone.
    if (trimmed.none { it == ' ' } && trimmed.count { it == '.' } >= 3 &&
        RELEASE_MARK.containsMatchIn(trimmed)
    ) return ""
    if (episode == null) return trimmed
    // Only that exact number, or "Episode 9" would eat the start of "Episode 90" — and only when
    // the number is the whole of it or a separator follows. Without that last condition the strip
    // was greedy enough to turn the real title "Episode 1 and a half" into "and a half".
    val numbered = Regex(
        """^Episode\s*0*$episode(?![0-9])(?:\s*[-–—:.]+\s*|\s*$)""",
        RegexOption.IGNORE_CASE,
    )
    return numbered.replace(trimmed, "").trim()
}

private val RELEASE_MARK = Regex("""[Ss]\d{1,2}[Ee]\d{1,3}""")
