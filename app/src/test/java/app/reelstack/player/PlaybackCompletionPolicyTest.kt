package app.reelstack.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackCompletionPolicyTest {
    @Test fun matchesJellyfinDefaultAndItsStrictPercentageBoundary() {
        assertFalse(playbackNearEnd(540_000, 600_000))
        assertTrue(playbackNearEnd(540_001, 600_000))
        assertTrue(playbackNearEnd(595_000, 600_000))
        assertFalse(playbackNearEnd(300_000, 600_000))
    }
    @Test fun lastSecondCompletesButUnknownDurationDoesNot() {
        assertTrue(playbackNearEnd(9_000, 10_000))
        assertFalse(playbackNearEnd(0, 1_000))
        assertFalse(playbackNearEnd(100_000, 0))
    }
}
