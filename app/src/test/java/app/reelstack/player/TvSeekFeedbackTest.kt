package app.reelstack.player

import org.junit.Assert.*
import org.junit.Test

class TvSeekFeedbackTest {
    @Test fun reportsActualMovementAtBeginningAndEnd() {
        assertEquals(-5_000L, tvSeekFeedback(null, 5_000, 0, 100)!!.deltaMs)
        assertEquals(2_000L, tvSeekFeedback(null, 58_000, 60_000, 100)!!.deltaMs)
        assertNull(tvSeekFeedback(null, 0, 0, 100))
    }

    @Test fun successivePressesAccumulateInTheSameDirection() {
        val first = tvSeekFeedback(null, 30_000, 40_000, 100)
        val second = tvSeekFeedback(first, 40_000, 50_000, 300)
        assertEquals(20_000L, second!!.deltaMs)
        assertEquals(22_000L, tvSeekFeedback(second, 50_000, 52_000, 400)!!.deltaMs)
    }

    @Test fun reversingDirectionStartsANewMessage() {
        val forward = tvSeekFeedback(null, 20_000, 30_000, 100)
        assertEquals(-10_000L, tvSeekFeedback(forward, 30_000, 20_000, 200)!!.deltaMs)
    }

    @Test fun aPauseStartsANewMessage() {
        val previous = tvSeekFeedback(null, 20_000, 30_000, 100)
        assertEquals(10_000L, tvSeekFeedback(previous, 30_000, 40_000, 1_100)!!.deltaMs)
    }

    @Test fun boundaryPressDoesNotReplaceTheLastMessageWithZeroOrRestartItsTimeout() {
        val previous = tvSeekFeedback(null, 5_000, 0, 100)
        assertSame(previous, tvSeekFeedback(previous, 0, 0, 200))
    }
}
