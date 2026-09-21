package app.reelstack.data.model

import java.time.LocalTime

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

/** A local routine, not a server-side permission or a security boundary. */
data class KidsBedtime(
    val enabled: Boolean = false,
    val hour: Int = 19,
    val minute: Int = 30,
) {
    val time: LocalTime get() = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun isReached(now: LocalTime = LocalTime.now()): Boolean = enabled && !now.isBefore(time)

    fun nextHalfHour(): KidsBedtime {
        val next = (time.toSecondOfDay() / 60 + 30) % (24 * 60)
        return copy(hour = next / 60, minute = next % 60)
    }
}

data class KidsPreferences(
    val world: KidsWorld = KidsWorld.SPACE,
    val allowAppearance: Boolean = true,
    val decorations: Boolean = true,
    /** Keep library names below the artwork when the child-facing home is easier to scan that way. */
    val libraryTitlesBelow: Boolean = true,
    val reduceMotion: Boolean = false,
    val autoplay: Boolean = false,
    val episodeLimit: Int = 3,
    val subtitles: SubtitleLanguage = SubtitleLanguage.NORWEGIAN,
    val bedtime: KidsBedtime = KidsBedtime(),
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
