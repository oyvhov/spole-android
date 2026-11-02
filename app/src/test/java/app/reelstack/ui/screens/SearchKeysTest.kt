package app.reelstack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What is on the television's search keyboard, and what a press does to the typed term. */
class SearchKeysTest {
    private fun ids(rows: List<List<SearchKeys.Key>>) = rows.flatten().map { it.id }

    /** Presses the keys for [text] the way the on-screen keyboard does: letters, and the space key. */
    private fun press(text: String, from: String = "") = text.fold(from) { term, c ->
        if (c == ' ') SearchKeys.withSpace(term) else SearchKeys.typed(c.toString(), term)
    }

    @Test fun theGridIsSixColumnsWideAndEveryRowIsFull() {
        listOf(false, true).forEach { speech ->
            SearchKeys.rows(speech).forEach { row -> assertEquals(6, row.sumOf { it.span }) }
        }
        assertEquals(listOf("A", "B", "C", "D", "E", "F"), SearchKeys.rows(false).first().map { it.id })
    }

    @Test fun lettersComeFirstThenTheNorwegianOnesThenDigitsHyphenSpaceAndDelete() {
        val expected = ('A'..'Z').map { it.toString() } + listOf("Æ", "Ø", "Å") +
            ('0'..'9').map { it.toString() } + listOf("-", "space", "delete")
        assertEquals(expected, ids(SearchKeys.rows(speech = false)))
    }

    @Test fun withoutSpeechTheKeyboardHasNoMicrophoneAndSevenRows() {
        val rows = SearchKeys.rows(speech = false)
        assertEquals(7, rows.size)
        assertFalse("speak" in ids(rows))
    }

    @Test fun withSpeechTheMicrophoneHasARowOfItsOwnBelowDelete() {
        val rows = SearchKeys.rows(speech = true)
        assertEquals(8, rows.size)
        assertEquals(listOf(SearchKeys.Key(SearchKeys.Kind.SPEAK, span = 6)), rows.last())
        assertEquals("delete", rows[6].last().id)
    }

    @Test fun everyWordStartsWithACapitalAndTheRestOfItIsLowerCase() {
        assertEquals("Game Of Thrones", press("GAME OF THRONES"))
        assertEquals("Game Of Thrones", press("game of thrones"))
    }

    @Test fun aLetterAfterAHyphenStartsAWordToo() {
        assertEquals("Spider-Man", press("spider-man"))
    }

    @Test fun theNorwegianLettersFollowTheSameRule() {
        assertEquals("Ålesund", press("ÅLESUND"))
        assertEquals("Blå", SearchKeys.typed("Å", "Bl"))
        assertEquals("Ø", SearchKeys.typed("ø", ""))
        assertEquals("Mørke Æser", press("MØRKE ÆSER"))
    }

    @Test fun digitsAreTypedAsTheyAre() {
        assertEquals("Se7en 2", press("se7en 2"))
    }

    @Test fun aSpaceNeedsAWordBeforeItAndIsNotDoubled() {
        assertFalse(SearchKeys.spaceFits(""))
        assertEquals("", SearchKeys.withSpace(""))
        assertTrue(SearchKeys.spaceFits("Game"))
        assertEquals("Game ", SearchKeys.withSpace("Game"))
        assertFalse(SearchKeys.spaceFits("Game "))
        assertEquals("Game ", SearchKeys.withSpace("Game "))
    }

    @Test fun deleteTakesTheLastCharacterAndNothingFromAnEmptyTerm() {
        assertEquals("Gam", SearchKeys.withoutLast("Game"))
        assertEquals("Game", SearchKeys.withoutLast("Game "))
        assertEquals("", SearchKeys.withoutLast(""))
    }

    @Test fun aTermStopsGrowingAtTheLengthHistoryKeeps() {
        val full = "A".repeat(SearchKeys.MAX_LENGTH)
        assertEquals(full, SearchKeys.typed("b", full))
        assertEquals(full, SearchKeys.withSpace(full))
    }

    @Test fun aHardwareKeyboardTypesLettersDigitsAndTheHyphenLikeTheKeys() {
        assertEquals("Game Of 2", SearchKeys.hardware("game of 2", ""))
        assertEquals("Spider-Man", SearchKeys.hardware("spider-man", ""))
        assertEquals("Blåbær", SearchKeys.hardware("blåbær", ""))
    }

    @Test fun aHardwareKeyboardIgnoresWhatTheKeyboardCannotType() {
        assertEquals("Game", SearchKeys.hardware("g!a?m,e.", ""))
        assertEquals("", SearchKeys.hardware("  ", ""))
    }

    @Test fun speechGetsCapitalsAtTheStartOfEachWordAndKeepsTheRest() {
        assertEquals("Game Of Thrones", SearchKeys.capitalizeWords("game of thrones"))
        assertEquals("NCIS Los Angeles", SearchKeys.capitalizeWords("NCIS los angeles"))
        assertEquals("Spider-Man", SearchKeys.capitalizeWords("spider-man"))
        assertEquals("Ørnen", SearchKeys.capitalizeWords("  ørnen "))
        assertEquals("", SearchKeys.capitalizeWords("   "))
    }
}
