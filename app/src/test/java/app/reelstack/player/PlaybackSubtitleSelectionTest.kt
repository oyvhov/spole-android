package app.reelstack.player

import androidx.media3.common.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class PlaybackSubtitleSelectionTest {
    private fun group(id: String, mime: String, codecs: String? = null, supported: Boolean = true) = Tracks.Group(
        TrackGroup(id, Format.Builder().setId(id).setSampleMimeType(mime).setCodecs(codecs).build()),
        false, intArrayOf(if (supported) C.FORMAT_HANDLED else C.FORMAT_UNSUPPORTED_TYPE), booleanArrayOf(false))
    private val first = PlaybackTrack(2, "Norsk", "nor", embeddedPgs = true)
    private val second = PlaybackTrack(7, "Engelsk", "eng", embeddedPgs = true)
    private val text = PlaybackTrack(3, "Tekst", "nor", isText = true)
    private fun plan(index: Int) = PlaybackPlan(PlayableItem("item", "Film", "Movie"), "source", "session", "",
        PlaybackMode.DIRECT_PLAY, emptyList(), listOf(first, text, second), null, index, null)

    @Test fun serverIndicesMapToPgsOrdinalsWithoutCountingOtherTextOrSidecars() {
        val pgs1 = group("2", MimeTypes.APPLICATION_PGS)
        val pgs2 = group("7", MimeTypes.APPLICATION_MEDIA3_CUES, MimeTypes.APPLICATION_PGS)
        val tracks = Tracks(listOf(pgs1, group("3", MimeTypes.APPLICATION_SUBRIP),
            group("source:spole-subtitle-3", MimeTypes.TEXT_VTT), pgs2))
        assertEquals(pgs1.mediaTrackGroup, playbackSubtitleOverride(plan(2), tracks, emptySet())?.mediaTrackGroup)
        assertEquals(pgs2.mediaTrackGroup, playbackSubtitleOverride(plan(7), tracks, emptySet())?.mediaTrackGroup)
        assertEquals(pgs2.mediaTrackGroup, playbackSubtitleOverride(plan(7).copy(subtitles = listOf(second, text, first)),
            tracks, emptySet())?.mediaTrackGroup)
        assertNull(playbackSubtitleOverride(plan(-1), tracks, emptySet()))
        assertNull(playbackSubtitleOverride(plan(3), tracks, emptySet()))
        assertEquals("source:spole-subtitle-3", playbackSubtitleOverride(plan(3), tracks, setOf(3))?.mediaTrackGroup?.id)
    }

    @Test fun missingUnsupportedOrTranscodedPgsNeverSelectsTheWrongLanguage() {
        assertNull(playbackSubtitleOverride(plan(2), Tracks(listOf(group("7", MimeTypes.APPLICATION_PGS))), emptySet()))
        val tracks = Tracks(listOf(group("2", MimeTypes.APPLICATION_PGS, supported = false), group("7", MimeTypes.APPLICATION_PGS)))
        assertNull(playbackSubtitleOverride(plan(2), tracks, emptySet()))
        assertNull(playbackSubtitleOverride(plan(7).copy(mode = PlaybackMode.FULL_TRANSCODE), tracks, emptySet()))
    }

    @Test fun mixedBitmapDeliveryCountsEachOriginalLanguageOnceAndSelectsTheAdvertisedSidecar() {
        for (mime in listOf(MimeTypes.APPLICATION_PGS, MimeTypes.APPLICATION_VOBSUB)) {
            fun bitmap(track: PlaybackTrack) = track.copy(embeddedPgs = mime == MimeTypes.APPLICATION_PGS,
                embeddedBitmapMime = mime.takeIf { it == MimeTypes.APPLICATION_VOBSUB })
            val withSidecar = bitmap(first).copy(externalBitmapUrl = "https://media.example/captions.mks")
            val mixed = plan(7).copy(subtitles = listOf(withSidecar, bitmap(second)))
            val original1 = group("2", MimeTypes.APPLICATION_MEDIA3_CUES, mime)
            val original2 = group("7", MimeTypes.APPLICATION_MEDIA3_CUES, mime)
            val sidecar = group("source:spole-subtitle-2", MimeTypes.APPLICATION_MEDIA3_CUES, mime)
            val tracks = Tracks(listOf(original1, sidecar, original2))
            assertEquals(original2.mediaTrackGroup, playbackSubtitleOverride(mixed, tracks, setOf(2))?.mediaTrackGroup)
            assertEquals(sidecar.mediaTrackGroup, playbackSubtitleOverride(mixed.copy(subtitleIndex = 2),
                tracks, setOf(2))?.mediaTrackGroup)
            assertNull(playbackSubtitleOverride(mixed.copy(subtitleIndex = 2), tracks, emptySet()))
        }
    }

    @Test fun anOversizedAssUsesTheServersVttForTheSameStream() {
        val ass = PlaybackTrack(4, "Styled", "jpn", isText = true, nativeAss = true)
        val styled = plan(4).copy(subtitles = listOf(ass))
        val original = group("source:spole-subtitle-4", MimeTypes.TEXT_SSA)
        val converted = group("source:spole-subtitle-4-vtt", MimeTypes.APPLICATION_MEDIA3_CUES, MimeTypes.TEXT_VTT)
        val tracks = Tracks(listOf(original, converted))
        assertEquals(original.mediaTrackGroup, playbackSubtitleOverride(styled, tracks, setOf(4))?.mediaTrackGroup)
        assertEquals(converted.mediaTrackGroup,
            playbackSubtitleOverride(styled, tracks, setOf(4), vttFallback = setOf(4))?.mediaTrackGroup)
        assertNull(playbackSubtitleOverride(styled, tracks, emptySet(), vttFallback = setOf(4)))
    }

    @Test fun textEndsWithThePictureNotWithItsLastLine() {
        val duration = 20_000_000L; val offset = 1_000_000_000_000L
        // Media3's own answer still counts: an ended text renderer is ended.
        assertTrue(textRendererEnded(delegateEnded = true, readToEnd = false, positionUs = 0, offsetUs = offset, periodDurationUs = duration))
        // A line until 9:59:59 keeps Media3 waiting; the period has ended, so the text has too.
        assertTrue(textRendererEnded(false, readToEnd = true, positionUs = offset + duration, offsetUs = offset, periodDurationUs = duration))
        assertFalse(textRendererEnded(false, readToEnd = true, positionUs = offset + duration - 1, offsetUs = offset, periodDurationUs = duration))
        // Unread text or an unknown duration (live) keep Media3's behaviour.
        assertFalse(textRendererEnded(false, readToEnd = false, positionUs = offset + duration, offsetUs = offset, periodDurationUs = duration))
        assertFalse(textRendererEnded(false, readToEnd = true, positionUs = offset + duration, offsetUs = offset, periodDurationUs = C.TIME_UNSET))
    }
}
