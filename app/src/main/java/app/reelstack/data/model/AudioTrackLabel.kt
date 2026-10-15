package app.reelstack.data.model

/** Display only: stream indices, keys and the server's original label stay unchanged. */
fun readableAudioTrackLabel(label: String, language: String? = null): String {
    val parts = label.split(Regex("\\s+[-–—·]\\s+"))
        .map(String::trim)
        .filter { it.isNotBlank() && !AUDIO_HANDLER.matches(it) }
    return parts.distinct().joinToString(" · ").ifBlank {
        language?.trim()?.takeIf(String::isNotBlank) ?: label.trim()
    }
}

// Match a whole container-generated field, never a real commentary title that contains the words.
private val AUDIO_HANDLER = Regex("(?:Audio\\s+)?Media\\s+Handler", RegexOption.IGNORE_CASE)
