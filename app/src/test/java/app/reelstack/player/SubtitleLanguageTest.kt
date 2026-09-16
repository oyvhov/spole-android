package app.reelstack.player

import app.reelstack.data.model.*
import app.reelstack.data.model.SubtitleLanguage.*
import org.junit.Assert.*
import org.junit.Test

class SubtitleLanguageTest {
    @Test fun norwegianAliasesBelongToOneGroup() {
        listOf("no", "nor", "nb", "nob", "nn", "nno", "nb-NO", "nn_NO", "NORSK",
            "Bokm\u00e5l", "Norsk bokm\u00e5l", "Bokm\u00e5l norsk", "Nynorsk", "Norwegian (Bokmal)")
            .forEach { assertEquals(it, NORWEGIAN, subtitleLanguage(it)) }
    }

    @Test fun recognizesEnglishAndIgnoresUnrelatedNames() {
        listOf("en", "eng", "en-US", "EN_gb", "English SDH", "Engelsk").forEach {
            assertEquals(it, ENGLISH, subtitleLanguage(it))
        }
        listOf(null, "und", "normal", "Doctor No", "Commentary").forEach { assertNull(subtitleLanguage(it)) }
    }

    @Test fun choosesPreferredThenFallbackNeverArbitraryLanguage() {
        val english = MediaTrack(4, "English", "eng", true)
        val norwegian = MediaTrack(8, "Bokm\u00e5l norsk", "und")
        assertEquals(8, preferredSubtitleIndex(listOf(english, norwegian), NORWEGIAN, ENGLISH))
        assertEquals(4, preferredSubtitleIndex(listOf(english), NORWEGIAN, ENGLISH))
        assertEquals(-1, preferredSubtitleIndex(listOf(MediaTrack(2, "Deutsch", "deu", true)), NORWEGIAN, ENGLISH))
        assertEquals(-1, preferredSubtitleIndex(emptyList(), NORWEGIAN, ENGLISH))
    }

    @Test fun prefersFullTrackThenServerDefaultWithinSameLanguage() {
        val tracks = listOf(MediaTrack(2, "Norsk forced", "nor", true, true),
            MediaTrack(3, "Nynorsk", "nno"), MediaTrack(4, "Bokmal", "nob"))
        assertEquals(3, preferredSubtitleIndex(tracks, NORWEGIAN, ENGLISH))
        assertEquals(4, preferredSubtitleIndex(tracks, NORWEGIAN, ENGLISH, 4))
    }

    @Test fun offAndServerSettingsRemainAvailable() {
        val tracks = listOf(MediaTrack(7, "English", "eng", true))
        assertEquals(-1, preferredSubtitleIndex(tracks, NONE, ENGLISH))
        assertEquals(7, preferredSubtitleIndex(tracks, SERVER, ENGLISH))
        assertEquals(-1, preferredSubtitleIndex(tracks, NORWEGIAN, NONE))
        assertEquals(NORWEGIAN, SubtitleLanguage.decode("removed", NORWEGIAN))
    }
}
