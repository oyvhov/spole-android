package app.reelstack.player

import app.reelstack.data.repository.RememberedSubtitle
import app.reelstack.data.repository.RememberedTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Finding "the same track" in the next episode.
 *
 * Track numbers cannot be trusted from one file to the next: a release with a commentary track
 * shifts everything after it by one, and a remux without the extra dub moves it back. So what is
 * remembered is a description, and what is looked for is the closest thing to it that speaks the
 * same language. A different language is never "close": English is no substitute for Japanese
 * however well the codec and the position fit.
 */
class TrackMatchingTest {

    private fun audio(
        index: Int, language: String?, title: String? = null, codec: String? = "eac3", channels: Int = 6,
    ) = PlaybackTrack(index, "Track $index", language, title = title, codec = codec, channels = channels)

    private fun sub(
        index: Int, language: String?, title: String? = null, forced: Boolean = false,
        sdh: Boolean = false, codec: String? = "subrip",
    ) = PlaybackTrack(index, "Text $index", language, isText = true, title = title, codec = codec,
        forced = forced, hearingImpaired = sdh)

    private fun remembered(
        language: String?, title: String? = null, forced: Boolean = false, sdh: Boolean = false,
        codec: String? = null, channels: Int = 0, position: Int = -1,
    ) = RememberedTrack(language, title, forced, sdh, codec, channels, position)

    private fun best(wanted: RememberedTrack, vararg candidates: PlaybackTrack) =
        bestMatchingTrack(wanted, candidates.toList())?.index

    // --- language ------------------------------------------------------------------------

    @Test fun theLanguageMustMatch() {
        val japanese = remembered("jpn", codec = "eac3", channels = 6, position = 0)
        assertEquals(2, best(japanese, audio(1, "eng"), audio(2, "jpn")))
        // Codec, channels and position all fit the English track; it is still the wrong language.
        assertNull(best(japanese, audio(1, "eng"), audio(2, "ger")))
    }

    @Test fun aTrackWithoutALanguageIsNeverASubstituteForOneWithALanguage() {
        assertNull(best(remembered("jpn"), audio(1, null), audio(2, "und")))
    }

    @Test fun languageCodesFromDifferentConventionsAreTheSameLanguage() {
        assertEquals(2, best(remembered("nob"), sub(1, "eng"), sub(2, "nor")))
        assertEquals(2, best(remembered("nn"), sub(1, "eng"), sub(2, "nb")))
        assertEquals(2, best(remembered("ger"), sub(1, "eng"), sub(2, "deu")))
        assertEquals(2, best(remembered("chi"), sub(1, "eng"), sub(2, "zho")))
        assertEquals(2, best(remembered("fre"), sub(1, "eng"), sub(2, "fra")))
        assertEquals(2, best(remembered("jpn"), sub(1, "eng"), sub(2, "JPN")))
    }

    @Test fun undeterminedLanguagesCountAsUnknown() {
        assertEquals(null, trackLanguageKey(null))
        assertEquals(null, trackLanguageKey(""))
        assertEquals(null, trackLanguageKey("und"))
        assertEquals(null, trackLanguageKey("Unknown"))
        assertEquals(trackLanguageKey("zho"), trackLanguageKey("chi"))
        assertEquals(trackLanguageKey("nor"), trackLanguageKey("nob"))
    }

    // --- what counts most -------------------------------------------------------------------

    @Test fun aFullTrackIsNotReplacedByAForcedOneOfTheSameLanguage() {
        val full = remembered("ger")
        assertEquals(2, best(full, sub(1, "ger", forced = true), sub(2, "ger")))
        assertEquals(1, best(full, sub(1, "ger"), sub(2, "ger", forced = true)))
        val forced = remembered("ger", forced = true)
        assertEquals(1, best(forced, sub(1, "ger", forced = true), sub(2, "ger")))
        assertEquals(2, best(forced, sub(1, "ger"), sub(2, "ger", forced = true)))
    }

    @Test fun anOrdinaryTrackIsNotReplacedByTheOneForTheHearingImpaired() {
        val plain = remembered("eng")
        assertEquals(2, best(plain, sub(1, "eng", sdh = true), sub(2, "eng")))
        val sdh = remembered("eng", sdh = true)
        assertEquals(1, best(sdh, sub(1, "eng", sdh = true), sub(2, "eng")))
    }

    @Test fun theTitleSeparatesTwoTracksOfTheSameKind() {
        val commentary = remembered("eng", title = "Director's commentary")
        assertEquals(2, best(commentary, audio(1, "eng", title = "Surround"), audio(2, "eng", title = "director's  commentary")))
    }

    @Test fun titleForcedAndSdhOutweighCodecPositionAndChannelsTogether() {
        // Everything weak points at track 1 (same codec, same channels, same position); the one
        // strong signal, the title, points at track 2.
        val wanted = remembered("eng", title = "Commentary", codec = "ac3", channels = 2, position = 0)
        assertEquals(2, best(wanted,
            audio(1, "eng", title = "Main", codec = "ac3", channels = 2),
            audio(2, "eng", title = "Commentary", codec = "dts", channels = 6)))
    }

    @Test fun codecPositionAndChannelsDecideBetweenOtherwiseEqualTracks() {
        val sameCodec = remembered("eng", codec = "ac3")
        assertEquals(2, best(sameCodec, audio(1, "eng", codec = "dts"), audio(2, "eng", codec = "ac3")))
        val sameChannels = remembered("eng", channels = 2)
        assertEquals(2, best(sameChannels, audio(1, "eng", channels = 6), audio(2, "eng", channels = 2)))
        val samePosition = remembered("eng", position = 1)
        assertEquals(2, best(samePosition, audio(1, "eng"), audio(2, "eng")))
    }

    @Test fun codecsThatAreTheSameFormatUnderTwoNamesAreTheSameCodec() {
        assertEquals(2, best(remembered("eng", codec = "subrip"), sub(1, "eng", codec = "ass"), sub(2, "eng", codec = "srt")))
        assertEquals(2, best(remembered("eng", codec = "SSA"), sub(1, "eng", codec = "srt"), sub(2, "eng", codec = "ass")))
    }

    @Test fun aTieGoesToTheFirstTrack() {
        assertEquals(1, best(remembered("eng"), audio(1, "eng"), audio(2, "eng")))
    }

    // --- the minimum ---------------------------------------------------------------------------

    @Test fun aLanguageMatchAloneIsEnoughWhenTheLanguageIsKnown() {
        // Every flag disagrees; only the language is left, and that is what was chosen.
        val wanted = remembered("eng", title = "Commentary", forced = true, sdh = true)
        assertEquals(1, best(wanted, sub(1, "eng")))
    }

    @Test fun withoutALanguageAChanceCoincidenceIsNotEnough() {
        // Remembered: an untagged forced SDH track called "Commentary".
        val wanted = remembered(null, title = "Commentary", forced = true, sdh = true, codec = "ass", position = 3)
        // The only untagged candidate disagrees on everything that counts.
        assertNull(best(wanted, sub(1, null, codec = "subrip")))
        // Agreeing on the flags is enough: the same kind of track, with no language on either side.
        assertEquals(1, best(wanted, sub(1, null, forced = true, sdh = true)))
        // A candidate that does have a language is not the track that had none.
        assertNull(best(wanted, sub(1, "eng", forced = true, sdh = true)))
    }

    @Test fun nothingToChooseFromIsNothing() {
        assertNull(best(remembered("eng")))
    }

    // --- picking an index ---------------------------------------------------------------------

    @Test fun anAudioChoiceBecomesTheServersIndexOfTheMatch() {
        val tracks = listOf(audio(1, "eng"), audio(2, "jpn"), audio(5, "jpn", title = "Commentary"))
        assertEquals(2, rememberedAudioIndex(remembered("jpn"), tracks))
        assertEquals(5, rememberedAudioIndex(remembered("jpn", title = "Commentary"), tracks))
        assertNull(rememberedAudioIndex(remembered("fra"), tracks))
        assertNull(rememberedAudioIndex(null, tracks))
    }

    @Test fun switchedOffStaysSwitchedOffWhateverTheNextEpisodeOffers() {
        val tracks = listOf(sub(3, "nor"), sub(4, "eng"))
        assertEquals(-1, rememberedSubtitleIndex(RememberedSubtitle.Off, tracks))
        assertEquals(-1, rememberedSubtitleIndex(RememberedSubtitle.Off, emptyList()))
    }

    @Test fun aSubtitleChoiceBecomesTheServersIndexOfTheMatchOrNoOpinion() {
        val tracks = listOf(sub(3, "nor"), sub(4, "eng"), sub(6, "eng", forced = true))
        assertEquals(4, rememberedSubtitleIndex(RememberedSubtitle.Track(remembered("eng")), tracks))
        assertEquals(6, rememberedSubtitleIndex(RememberedSubtitle.Track(remembered("eng", forced = true)), tracks))
        // No such language here: the choice was "this language", not "none", so the caller's
        // default applies instead of switching the text off.
        assertNull(rememberedSubtitleIndex(RememberedSubtitle.Track(remembered("jpn")), tracks))
        assertNull(rememberedSubtitleIndex(null, tracks))
    }

    @Test fun aTrackAlwaysMatchesItselfWhenItIsRememberedAndLookedUpAgain() {
        val tracks = listOf(
            sub(3, "nor"), sub(4, "eng", sdh = true, codec = "ass"), sub(6, "eng", forced = true),
            sub(7, "eng", title = "Full"),
        )
        tracks.forEachIndexed { position, track ->
            val wanted = rememberedTrack(track, position)
            assertEquals(track.index, bestMatchingTrack(wanted, tracks)?.index)
        }
        val dub = listOf(audio(1, "eng", channels = 2), audio(2, "jpn", title = "Original", channels = 6))
        dub.forEachIndexed { position, track ->
            assertEquals(track.index, bestMatchingTrack(rememberedTrack(track, position), dub)?.index)
        }
    }

    @Test fun whatIsRememberedFromATrackIsOnlyItsDescription() {
        val track = sub(9, "nob", title = " Norsk ", forced = true, sdh = true, codec = "SubRip")
        val wanted = rememberedTrack(track, 4)
        assertEquals(RememberedTrack("nob", "Norsk", true, true, "subrip", 0, 4), wanted)
        // Blank titles and unknown languages are not worth remembering as such.
        assertEquals(RememberedTrack(null, null, false, false, null, 0, 0),
            rememberedTrack(sub(1, "und", title = "  ", codec = null), 0))
    }
}
