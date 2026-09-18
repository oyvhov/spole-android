package app.reelstack.update

internal data class ReleaseNoteBlock(val text: String, val heading: Boolean = false, val bullet: Boolean = false)

/** Read-only presentation of GitHub notes. No HTML, images or remote content is executed. */
internal fun releaseNoteBlocks(markdown: String): List<ReleaseNoteBlock> {
    val result = mutableListOf<ReleaseNoteBlock>()
    val paragraph = mutableListOf<String>()
    var fenced = false
    fun flush() {
        if (paragraph.isNotEmpty()) result += ReleaseNoteBlock(cleanNoteText(paragraph.joinToString(" ")))
        paragraph.clear()
    }
    for (raw in markdown.lineSequence()) {
        val line = raw.trim()
        when {
            line.startsWith("```") || line.startsWith("~~~") -> { flush(); fenced = !fenced }
            fenced -> { flush(); result += ReleaseNoteBlock(raw) }
            line.isBlank() || line.matches(Regex("[-*_]{3,}")) -> flush()
            line.matches(Regex("#{1,6}\\s+.*")) -> {
                flush(); result += ReleaseNoteBlock(cleanNoteText(line.replace(Regex("^#{1,6}\\s+"), "").trimEnd('#', ' ')), heading = true)
            }
            line.matches(Regex("(?:[-*+] |\\d+[.)] ).*")) -> {
                flush(); result += ReleaseNoteBlock(cleanNoteText(line.replace(Regex("^(?:[-*+] |\\d+[.)] )"), "")), bullet = true)
            }
            else -> paragraph += line.removePrefix("> ")
        }
    }
    flush()
    return result.filter { it.text.isNotBlank() }
}

private fun cleanNoteText(value: String): String = value
    .replace(Regex("!?\\[([^]]+)]\\(([^)]+)\\)")) { "${it.groupValues[1]} (${it.groupValues[2]})" }
    .replace(Regex("\\*\\*(.+?)\\*\\*|__(.+?)__|~~(.+?)~~|`([^`]+)`")) {
        it.groupValues.drop(1).first { group -> group.isNotEmpty() }
    }
    .replace(Regex("(?<!\\w)[*_]([^*_]+)[*_](?!\\w)"), "$1")
