package app.reelstack.player

import java.util.Locale

/** Media3's Matroska extractor writes a zero start and relative end, with colons for centiseconds. */
internal fun assMatroskaDialogue(sample: ByteArray, startMs: Long): ByteArray? {
    val text = sample.toString(Charsets.UTF_8).trimEnd('\u0000', '\n', '\r')
    if (!text.startsWith("Dialogue: ")) return null
    val fields = text.substringAfter("Dialogue: ").split(',', limit = 3)
    if (fields.size != 3) return null
    val duration = assTimeMs(fields[1]) ?: return null
    return "Dialogue: ${assTime(startMs)},${assTime(startMs + duration)},${fields[2]}\n".toByteArray()
}

internal fun assTimeMs(value: String): Long? {
    val fields = value.trim().split(':', '.')
    if (fields.size != 4) return null
    val parts = fields.map { it.toLongOrNull()?.takeIf { n -> n >= 0 } ?: return null }
    if (parts[0] > 24 || parts[1] > 59 || parts[2] > 59 || parts[3] > 99) return null
    return ((parts[0] * 60 + parts[1]) * 60 + parts[2]) * 1000 + parts[3] * 10
}

/** Read timing without constructing a JNI object/string for every event in a full document. */
internal fun assDocumentEndMs(bytes: ByteArray): Long {
    var inEvents = false
    var endIndex = 2
    var endMs = 0L
    bytes.toString(Charsets.UTF_8).lineSequence().forEach { raw ->
        val line = raw.trim()
        if (line.startsWith('[')) inEvents = line.equals("[Events]", true)
        else if (inEvents && line.startsWith("Format:", true)) {
            endIndex = line.substringAfter(':').split(',').indexOfFirst { it.trim().equals("End", true) }
        } else if (inEvents && endIndex in 0..16 && line.startsWith("Dialogue:", true)) {
            line.substringAfter(':').split(',', limit = endIndex + 2).getOrNull(endIndex)
                ?.let(::assTimeMs)?.let { endMs = maxOf(endMs, it) }
        }
    }
    return endMs
}

private fun assTime(ms: Long): String {
    val value = ms.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%d:%02d:%02d.%02d", value / 3_600_000, value / 60_000 % 60, value / 1000 % 60, value / 10 % 100)
}
