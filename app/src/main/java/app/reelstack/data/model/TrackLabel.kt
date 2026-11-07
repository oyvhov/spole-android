package app.reelstack.data.model

import java.util.Locale

/**
 * A track's name the way the rest of the page reads: the language in the app's own language, the parts the server
 * joins with " - " joined with " · ", and a subtitle format by its short name.
 *
 * On Loftet a film page offered «Norwegian - PGSSUB» under «Undertekster»: the server's English language name and its
 * codec's internal name (2026-11-07). Display only: stream indices, keys and the server's original label stay as they
 * are. [languageCode] is the stream's ISO 639 code; a code nobody knows leaves the server's word for the language.
 */
fun readableTrackLabel(label: String, languageCode: String?, locale: Locale = Locale.getDefault()): String {
    val language = languageName(languageCode, locale)
    val parts = label.split(Regex("""\s+[-–—·]\s+""")).map(String::trim)
        .filter { it.isNotBlank() && !TRACK_HANDLER.matches(it) }
        .map { part -> FORMATS[part.uppercase(Locale.ROOT)] ?: part }
        .mapIndexed { index, part ->
            // The server names the language first, in its own words or by the code itself.
            if (index == 0 && language != null && part.isLanguage(languageCode)) language else part
        }
    return parts.distinct().joinToString(" · ").ifBlank { language ?: languageCode?.takeIf(String::isNotBlank) ?: label.trim() }
}

private fun String.isLanguage(code: String?): Boolean {
    if (code.isNullOrBlank()) return false
    if (equals(code, ignoreCase = true)) return true
    val locale = Locale.forLanguageTag(twoLetter(code) ?: return false)
    return equals(locale.getDisplayLanguage(Locale.ENGLISH), ignoreCase = true) ||
        equals(locale.getDisplayLanguage(locale), ignoreCase = true)
}

private fun languageName(code: String?, locale: Locale): String? {
    val two = code?.let(::twoLetter) ?: return null
    val name = Locale.forLanguageTag(two).getDisplayLanguage(locale)
    if (name.isBlank() || name.equals(two, ignoreCase = true) || name.equals(code, ignoreCase = true)) return null
    return name.replaceFirstChar { it.titlecase(locale) }
}

/** ISO 639-1 for a two- or three-letter code, the older bibliographic codes (ger, fre, dut) included. */
private fun twoLetter(code: String): String? {
    val lower = code.trim().lowercase(Locale.ROOT)
    if (lower.length == 2) return lower.takeIf { it in TWO_LETTER }
    if (lower.length != 3) return null
    return THREE_TO_TWO[BIBLIOGRAPHIC[lower] ?: lower]
}

private val TWO_LETTER: Set<String> = Locale.getISOLanguages().toSet()
private val THREE_TO_TWO: Map<String, String> = Locale.getISOLanguages().mapNotNull { two ->
    runCatching { Locale.forLanguageTag(two).isO3Language }.getOrNull()?.takeIf(String::isNotBlank)?.let { it to two }
}.toMap() + mapOf("nob" to "nb", "nno" to "nn")
private val BIBLIOGRAPHIC = mapOf("ger" to "deu", "fre" to "fra", "dut" to "nld", "chi" to "zho", "cze" to "ces",
    "gre" to "ell", "per" to "fas", "rum" to "ron", "slo" to "slk", "ice" to "isl", "alb" to "sqi", "arm" to "hye",
    "baq" to "eus", "bur" to "mya", "geo" to "kat", "mac" to "mkd", "mao" to "mri", "may" to "msa", "tib" to "bod",
    "wel" to "cym")

/** Subtitle and container formats by the names a reader may know them by. */
private val FORMATS = mapOf("SUBRIP" to "SRT", "SRT" to "SRT", "PGSSUB" to "PGS", "PGS" to "PGS", "DVDSUB" to "VobSub",
    "DVB_SUBTITLE" to "DVB", "WEBVTT" to "WebVTT", "ASS" to "ASS", "SSA" to "SSA")

// Match a whole container-generated field, never a real commentary title that contains the words.
private val TRACK_HANDLER = Regex("""(?:(?:Audio|Subtitle|Video)\s+)?Media\s+Handler""", RegexOption.IGNORE_CASE)
