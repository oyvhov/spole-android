package app.reelstack.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackAdjustmentsTest {
    @Test fun invalidAndExtremeInputsCannotEscapeSupportedRanges() {
        assertEquals(PlaybackAdjustments(1f, 2_000, -10_000), PlaybackAdjustments(Float.NaN, Int.MAX_VALUE, Int.MIN_VALUE).bounded())
        assertEquals(.5f, PlaybackAdjustments(.1f).bounded().speed)
        assertEquals(2f, PlaybackAdjustments(4f).bounded().speed)
    }
    @Test fun positiveDelaysPutAudioAndSubtitlesLaterThanPicture() {
        assertEquals(1_200_000L, adjustedAudioClock(1_000_000, 200))
        assertEquals(800_000L, subtitleRenderPosition(1_000_000, 200))
        assertEquals(1_200_000L, subtitleRenderPosition(1_000_000, -200))
    }
}
