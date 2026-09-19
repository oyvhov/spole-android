package app.reelstack.data.model

/** Cosmetic choices are local to each child; library access always belongs to the server. */
enum class KidsWorld(val title: String, val subtitle: String, val sky: Long, val glow: Long) {
    SPACE("Verdsrom", "Mellom stjerner og planetar", 0xFF101326, 0xFF9281DF),
    OCEAN("Havdjup", "Eit hav av historier", 0xFF071C29, 0xFF4AC9D1),
    FOREST("Eventyrskog", "Der nye eventyr veks", 0xFF101F1C, 0xFF75BDA2),
    AURORA("Nordlys", "Lys over ei stille verd", 0xFF17162A, 0xFFCE8DD5),
    SUNSET("Solnedgang", "Ein varm stad å lande", 0xFF26151F, 0xFFEFAD82),
    CINEMA("Kino", "Din eigen vesle kinosal", 0xFF171923, 0xFF8FA6CF);

    companion object {
        fun decode(value: String?) = entries.firstOrNull { it.name == value } ?: SPACE
    }
}

data class KidsPreferences(
    val world: KidsWorld = KidsWorld.SPACE,
    val allowAppearance: Boolean = true,
    val decorations: Boolean = true,
    val reduceMotion: Boolean = false,
    val autoplay: Boolean = false,
    val episodeLimit: Int = 3,
    val subtitles: SubtitleLanguage = SubtitleLanguage.NORWEGIAN,
) {
    fun playbackOptions(adult: Personalization): Personalization = adult.copy(
        autoResume = true,
        showNextEpisode = true,
        autoPlayNextEpisode = autoplay,
        preferredSubtitleLanguage = subtitles,
        fallbackSubtitleLanguage = SubtitleLanguage.NONE,
    )
    // chain counts automatic advances; the manually started episode also counts.
    fun canAutoplay(chain: Int) = autoplay && chain + 1 < episodeLimit.coerceIn(1, 3)
}
