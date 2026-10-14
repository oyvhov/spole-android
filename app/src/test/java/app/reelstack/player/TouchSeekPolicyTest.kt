package app.reelstack.player

import org.junit.Assert.*
import org.junit.Test

class TouchSeekPolicyTest {
    @Test fun bothSidesHaveLargeTargetsAndTheMiddleIsReserved() {
        for (width in listOf(412f, 840f, 1280f)) {
            assertEquals(-10_000L, touchSeekDelta(width * .1f, width))
            assertEquals(-10_000L, touchSeekDelta(width * .44f, width))
            assertNull(touchSeekDelta(width * .5f, width))
            assertEquals(10_000L, touchSeekDelta(width * .56f, width))
            assertEquals(10_000L, touchSeekDelta(width * .9f, width))
        }
        assertNull(touchSeekDelta(1f, 0f))
        assertNull(touchSeekDelta(-1f, 412f))
    }

    @Test fun seeksClampToTheVideoAndAccumulateFromTheLastTarget() {
        assertEquals(0L, touchSeekTarget(5_000, -10_000, 60_000))
        assertEquals(60_000L, touchSeekTarget(55_000, 10_000, 60_000))
        val first = touchSeekTarget(20_000, 10_000, 60_000)
        assertEquals(40_000L, touchSeekTarget(first, 10_000, 60_000))
        assertEquals(0L, touchSeekTarget(20_000, 10_000, 0))
    }
}
