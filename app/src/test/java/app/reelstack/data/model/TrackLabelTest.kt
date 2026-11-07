package app.reelstack.data.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A track reads the way the rest of the page does. On Loftet a film page offered «Norwegian - PGSSUB» and an episode
 * «Norwegian - SUBRIP» under «Undertekster»: the server's language name in English and its codec's internal name
 * (2026-11-07).
 */
class TrackLabelTest {
    private val bokmal = Locale.forLanguageTag("nb")
    private val nynorsk = Locale.forLanguageTag("nn")

    @Test fun aSubtitleTrackNamesItsLanguageInTheAppsLanguageAndItsFormatShortly() {
        assertEquals("Norsk · PGS", readableTrackLabel("Norwegian - PGSSUB", "nor", bokmal))
        assertEquals("Norsk · SRT", readableTrackLabel("Norwegian - SUBRIP", "nor", bokmal))
    }

    @Test fun anAudioTrackKeepsWhatTheServerSaysAboutTheSound() {
        assertEquals("Engelsk · Dolby TrueHD + Dolby Atmos · 7.1 · Standard",
            readableTrackLabel("English - Dolby TrueHD + Dolby Atmos - 7.1 - Standard", "eng", nynorsk))
    }

    @Test fun anOlderLanguageCodeIsUnderstoodToo() {
        assertEquals("Tysk · SRT", readableTrackLabel("German - SUBRIP", "ger", bokmal))
    }

    @Test fun aTrackWithoutAKnownLanguageOnlyLosesTheServersSeparators() {
        assertEquals("Director commentary · AAC", readableTrackLabel("Director commentary - AAC", null, bokmal))
        assertEquals("und · SRT", readableTrackLabel("und - SUBRIP", "und", bokmal))
    }

    @Test fun aLabelThatIsOnlyTheContainersHandlerFallsBackToTheLanguage() {
        assertEquals("Norsk", readableTrackLabel("Audio Media Handler", "nor", bokmal))
    }
}
