package app.reelstack.ui.screens

/**
 * What is on the television's search keyboard, and what a press does to the typed term.
 *
 * The remote has no keyboard and the system one covers the results, so TV search draws its own grid
 * beside them (docs/SWIFTLY_LAN_2026-11-01.md, idea 1). The layout and the typing rules live here, once,
 * away from Compose, so they can be tested without a screen.
 *
 * The order is alphabetical, not QWERTY: with a directional pad you look for a letter, you do not
 * touch-type. The Norwegian letters follow Z, then the digits, then the hyphen and the space. With
 * delete that is 42 keys, which fill seven rows of six exactly; a microphone gets a row of its own.
 */
internal object SearchKeys {
    const val COLUMNS = 6

    /** The length history keeps (AppPreferencesRepository.rememberSearch). */
    const val MAX_LENGTH = 120

    enum class Kind { LETTER, SPACE, DELETE, SPEAK }

    /** One cell of the grid. [span] is how many columns it covers. */
    data class Key(val kind: Kind, val character: String = "", val span: Int = 1) {
        /** The character for a letter, otherwise the kind: what a test or a focus map names the key by. */
        val id: String get() = if (kind == Kind.LETTER) character else kind.name.lowercase()
    }

    private val characters: List<String> =
        (('A'..'Z').map { it.toString() } + listOf("Æ", "Ø", "Å") + ('0'..'9').map { it.toString() } + "-")

    /** The rows from top to bottom. The microphone is only a row when speech input exists. */
    fun rows(speech: Boolean): List<List<Key>> {
        val keys = characters.map { Key(Kind.LETTER, it) } + Key(Kind.SPACE) + Key(Kind.DELETE)
        val rows = keys.chunked(COLUMNS)
        return if (speech) rows + listOf(listOf(Key(Kind.SPEAK, span = COLUMNS))) else rows
    }

    /** A word starts at the very beginning, after a space and after a hyphen. */
    private fun startsWord(term: String): Boolean = term.isEmpty() || term.last() == ' ' || term.last() == '-'

    /**
     * What typing a key adds: a capital at the start of a word, otherwise lower case ("Game Of
     * Thrones", "Spider-Man"). Search ignores case, but history reads like titles this way.
     */
    fun typed(character: String, into: String): String {
        if (into.length + character.length > MAX_LENGTH) return into
        val capital = character.uppercase()
        // The sharp s has no one-letter capital; it stays as it is.
        val add = if (startsWord(into) && capital.length == character.length) capital else character.lowercase()
        return into + add
    }

    /** A space makes sense after a character that is not a space already. */
    fun spaceFits(term: String): Boolean = term.isNotEmpty() && term.last() != ' ' && term.length < MAX_LENGTH

    fun withSpace(term: String): String = if (spaceFits(term)) "$term " else term

    fun withoutLast(term: String): String {
        if (term.isEmpty()) return term
        val pair = term.length >= 2 && Character.isLowSurrogate(term.last()) && Character.isHighSurrogate(term[term.length - 2])
        return term.dropLast(if (pair) 2 else 1)
    }

    /** A character the keyboard can type: a letter, a digit, the hyphen or the space. */
    private fun accepts(codePoint: Int): Boolean =
        codePoint == ' '.code || codePoint == '-'.code || Character.isLetterOrDigit(codePoint)

    /** True when [text] is something the keys could have typed; the other characters of a hardware keyboard are not. */
    fun canType(text: String): Boolean = text.isNotEmpty() && text.codePoints().allMatch { accepts(it) }

    /**
     * Text from a hardware keyboard: letters, digits and the hyphen are typed like the keys, a space
     * like the space key. Anything else is left out, so a stray punctuation mark cannot get in.
     */
    fun hardware(text: String, into: String): String {
        var term = into
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            index += Character.charCount(codePoint)
            term = when {
                codePoint == ' '.code -> withSpace(term)
                accepts(codePoint) -> typed(String(Character.toChars(codePoint)), term)
                else -> term
            }
        }
        return term
    }

    /**
     * A capital at the start of every word, the rest as it was said: speech comes back in ordinary
     * case, and "NCIS" must stay "NCIS".
     */
    fun capitalizeWords(text: String): String {
        val trimmed = text.trim()
        val out = StringBuilder(trimmed.length)
        var atStart = true
        for (c in trimmed) {
            out.append(if (atStart) c.uppercaseChar() else c)
            atStart = c == ' ' || c == '-'
        }
        return out.toString().take(MAX_LENGTH)
    }
}
