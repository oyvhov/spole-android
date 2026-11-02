package app.reelstack.player

import app.reelstack.data.model.subtitleLanguage
import app.reelstack.data.repository.RememberedSubtitle
import app.reelstack.data.repository.RememberedTrack
import java.util.Locale

/**
 * How much each thing about a track counts when the next episode is searched for "the same one".
 *
 * The language is a gate, not a score: a track in another language is never a stand-in, however
 * well everything else fits. Among the tracks in the right language the title, the forced flag and
 * the hearing-impaired flag decide, and they are worth more together than the three weak signals
 * (codec, channels, position) can ever add up to. That is what keeps "German, full" from landing
 * on "German, forced" because the forced track happens to sit at the same position.
 */
internal object TrackMatchWeights {
    const val LANGUAGE = 3
    const val TITLE = 4
    const val FORCED = 4
    const val HEARING_IMPAIRED = 4
    const val CODEC = 1
    const val CHANNELS = 1
    const val POSITION = 1

    /** Below this the closest track is still not close; the caller's own default applies instead. */
    const val MINIMUM = 3
}

/** Describes [track], the [position]th of its kind, in the terms [bestMatchingTrack] looks for. */
internal fun rememberedTrack(track: PlaybackTrack, position: Int) = RememberedTrack(
    language = track.language?.trim()?.lowercase(Locale.ROOT)?.takeIf { trackLanguageKey(it) != null },
    title = track.title?.trim()?.takeIf(String::isNotEmpty),
    forced = track.forced,
    hearingImpaired = track.hearingImpaired,
    codec = track.codec?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty),
    channels = track.channels,
    position = position,
)

/**
 * The track among [candidates] (all of one kind, in the server's order) that is closest to what
 * was [remembered], or null when nothing is close enough. Ties go to the earlier track.
 */
internal fun bestMatchingTrack(remembered: RememberedTrack, candidates: List<PlaybackTrack>): PlaybackTrack? {
    val language = trackLanguageKey(remembered.language)
    val title = normalisedTitle(remembered.title)
    val codec = normalisedCodec(remembered.codec)
    var best: PlaybackTrack? = null
    var bestScore = Int.MIN_VALUE
    candidates.forEachIndexed { position, candidate ->
        if (trackLanguageKey(candidate.language) != language) return@forEachIndexed
        var score = if (language != null) TrackMatchWeights.LANGUAGE else 0
        if (title != null && title == normalisedTitle(candidate.title)) score += TrackMatchWeights.TITLE
        if (remembered.forced == candidate.forced) score += TrackMatchWeights.FORCED
        if (remembered.hearingImpaired == candidate.hearingImpaired) score += TrackMatchWeights.HEARING_IMPAIRED
        if (codec != null && codec == normalisedCodec(candidate.codec)) score += TrackMatchWeights.CODEC
        if (remembered.channels > 0 && remembered.channels == candidate.channels) score += TrackMatchWeights.CHANNELS
        if (remembered.position == position) score += TrackMatchWeights.POSITION
        if (score > bestScore) { best = candidate; bestScore = score }
    }
    return best.takeIf { bestScore >= TrackMatchWeights.MINIMUM }
}

/** The server's index of the audio track the series was last watched with, or null for no opinion. */
internal fun rememberedAudioIndex(remembered: RememberedTrack?, audio: List<PlaybackTrack>): Int? =
    remembered?.let { bestMatchingTrack(it, audio)?.index }

/**
 * The server's index to show subtitles with: -1 for a series that was watched without them, the
 * matching track for one that was watched with them, and null when there is no opinion.
 *
 * A remembered language the file does not have is no opinion, not "off": the choice was "this
 * language", never "no subtitles", so the caller's own default has to decide.
 */
internal fun rememberedSubtitleIndex(remembered: RememberedSubtitle?, subtitles: List<PlaybackTrack>): Int? = when (remembered) {
    null -> null
    RememberedSubtitle.Off -> -1
    is RememberedSubtitle.Track -> bestMatchingTrack(remembered.track, subtitles)?.index
}

private fun normalisedTitle(title: String?): String? =
    title?.trim()?.lowercase(Locale.ROOT)?.replace(Regex("\\s+"), " ")?.takeIf(String::isNotEmpty)

/** `subrip` and `srt` are one format, as are `ssa` and `ass`; muxers do not agree on the name. */
private fun normalisedCodec(codec: String?): String? = when (val name = codec?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty)) {
    "subrip" -> "srt"
    "ssa" -> "ass"
    "webvtt" -> "vtt"
    else -> name
}

/**
 * One key for one language, whatever the file calls it: Norwegian under `nor`, `nob`, `nb` or
 * "Norsk", German under `ger` or `deu`, Chinese under `chi` or `zho`. Null for no language, which
 * includes "und", the tag muxers use for a language they could not identify.
 */
internal fun trackLanguageKey(raw: String?): String? {
    val code = raw?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty) ?: return null
    if (code in UNKNOWN_LANGUAGES) return null
    subtitleLanguage(code)?.let { return it.name }
    return BIBLIOGRAPHIC_TO_TERMINOLOGY[code] ?: TWO_LETTER_TO_THREE_LETTER[code] ?: code
}

private val UNKNOWN_LANGUAGES = setOf("und", "unknown", "mis", "zxx")

/** ISO 639-2 has two codes for twenty languages; Jellyfin hands out whichever the file carried. */
private val BIBLIOGRAPHIC_TO_TERMINOLOGY = mapOf(
    "alb" to "sqi", "arm" to "hye", "baq" to "eus", "bur" to "mya", "chi" to "zho", "cze" to "ces",
    "dut" to "nld", "geo" to "kat", "gre" to "ell", "ice" to "isl", "mac" to "mkd", "mao" to "mri",
    "may" to "msa", "per" to "fas", "rum" to "ron", "slo" to "slk", "tib" to "bod", "wel" to "cym",
)

/** The two-letter codes some muxers and external subtitle files use, for the languages people watch. */
private val TWO_LETTER_TO_THREE_LETTER = mapOf(
    "ja" to "jpn", "ko" to "kor", "zh" to "zho", "it" to "ita", "pt" to "por", "ru" to "rus", "nl" to "nld",
    "pl" to "pol", "tr" to "tur", "fi" to "fin", "ar" to "ara", "hi" to "hin", "cs" to "ces", "el" to "ell",
    "he" to "heb", "hu" to "hun", "th" to "tha", "uk" to "ukr", "is" to "isl", "ro" to "ron",
)
