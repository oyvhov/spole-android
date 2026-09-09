package app.reelstack.player

import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackStartPolicyTest {
    @Test fun newAndExistingInstallationsDefaultToResume() {
        assertTrue(Personalization().autoResume)
        assertEquals(45000L, playbackStartPosition(45000, 120000, false, true))
    }
    @Test fun askingIsOnlyNeededForARealResumePosition() {
        assertNull(playbackStartPosition(45000, 120000, false, false))
        assertEquals(0L, playbackStartPosition(0, 120000, false, false))
    }
    @Test fun completedAndInvalidPositionsStartAtTheBeginning() {
        assertEquals(0L, playbackStartPosition(45000, 120000, true, true))
        assertEquals(0L, playbackStartPosition(120000, 120000, false, true))
        assertEquals(0L, playbackStartPosition(-1, 120000, false, true))
    }
    @Test fun unknownDurationDoesNotDiscardSavedProgress() {
        assertEquals(45000L, playbackStartPosition(45000, 0, false, true))
    }
    @Test fun unknownSavedAppearanceValuesFallBackWithoutAReset() {
        assertEquals(AccentPalette.LIME, AccentPalette.decode("future-palette"))
        assertEquals(ArtworkSize.STANDARD, ArtworkSize.decode(null))
        AccentPalette.entries.forEach { assertEquals(it, AccentPalette.decode(it.name)) }
        ArtworkSize.entries.forEach { assertEquals(it, ArtworkSize.decode(it.name)) }
    }
}
