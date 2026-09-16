package app.reelstack.data.model

import java.text.Normalizer
import java.util.Locale

enum class SubtitleLanguage {
    NORWEGIAN, ENGLISH, SWEDISH, DANISH, GERMAN, FRENCH, SPANISH, SERVER, NONE;

    companion object {
        fun decode(value: String?, fallback: SubtitleLanguage) = entries.firstOrNull { it.name == value } ?: fallback
    }
}

/** Servers use both ISO codes and display names. Norwegian is intentionally one language group. */
fun subtitleLanguage(value: String?): SubtitleLanguage? {
    val normalized = Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT).trim()
    val words = normalized.split(Regex("[^a-z]+"))
    val code = normalized.takeIf { it.matches(Regex("[a-z]{2,3}([_-][a-z]{2,4})?")) }
        ?.substringBefore('-')?.substringBefore('_')
    return when {
        code in setOf("no", "nor", "nb", "nob", "nn", "nno") || words.any { it in setOf("norsk", "norwegian", "bokmal", "nynorsk") } -> SubtitleLanguage.NORWEGIAN
        code in setOf("en", "eng") || words.any { it in setOf("english", "engelsk") } -> SubtitleLanguage.ENGLISH
        code in setOf("sv", "swe") || words.any { it in setOf("svensk", "svenska", "swedish") } -> SubtitleLanguage.SWEDISH
        code in setOf("da", "dan") || words.any { it in setOf("dansk", "danish") } -> SubtitleLanguage.DANISH
        code in setOf("de", "deu", "ger") || words.any { it in setOf("german", "deutsch", "tysk") } -> SubtitleLanguage.GERMAN
        code in setOf("fr", "fra", "fre") || words.any { it in setOf("french", "francais", "fransk") } -> SubtitleLanguage.FRENCH
        code in setOf("es", "spa") || words.any { it in setOf("spanish", "espanol", "spansk") } -> SubtitleLanguage.SPANISH
        else -> null
    }
}

fun preferredSubtitleIndex(tracks: List<MediaTrack>, preferred: SubtitleLanguage, fallback: SubtitleLanguage,
    serverDefault: Int? = tracks.firstOrNull { it.isDefault }?.index): Int {
    if (preferred == SubtitleLanguage.NONE) return -1
    if (preferred == SubtitleLanguage.SERVER) return serverDefault ?: -1
    for (language in listOf(preferred, fallback).distinct()) {
        if (language == SubtitleLanguage.NONE || language == SubtitleLanguage.SERVER) continue
        tracks.filter { (subtitleLanguage(it.language) ?: subtitleLanguage(it.label)) == language }
            .sortedWith(compareBy<MediaTrack> { it.forced }.thenByDescending { it.index == serverDefault })
            .firstOrNull()?.let { return it.index }
    }
    return -1
}
