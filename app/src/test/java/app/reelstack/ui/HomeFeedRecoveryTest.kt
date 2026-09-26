package app.reelstack.ui

import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

class HomeFeedRecoveryTest {
    @Test fun failedOrPartialMediaFeedRecoversAfterCooldown() {
        for (kind in listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
            assertTrue(shouldRetryHomeFeed(setOf(kind), emptySet(), false, 60_000))
            assertFalse(shouldRetryHomeFeed(setOf(kind), emptySet(), false, 59_999))
            assertFalse(shouldRetryHomeFeed(setOf(kind), emptySet(), true, 120_000))
            // A partial answer is often a permission, not an outage: five minutes, not one.
            assertFalse(shouldRetryHomeFeed(emptySet(), setOf(kind), false, 60_000))
            assertTrue(shouldRetryHomeFeed(emptySet(), setOf(kind), false, 5 * 60_000))
        }
    }

    @Test fun successfulEmptyFeedAndUnrelatedServiceFailuresDoNotPollLibrary() {
        assertFalse(shouldRetryHomeFeed(emptySet(), emptySet(), false, 120_000))
        assertFalse(shouldRetryHomeFeed(setOf(ServiceKind.SEERR, ServiceKind.SONARR), emptySet(), false, 120_000))
    }

    @Test fun aServerThatStaysDownIsAskedLessOftenEachTime() {
        // It used to be a full Home refresh every minute for as long as Home was open.
        assertEquals(listOf(1L, 2L, 4L, 8L, 16L, 30L, 30L),
            (0..6).map { homeFeedRetryDelayMillis(it, failedOutright = true) / 60_000L })
        assertFalse(shouldRetryHomeFeed(setOf(ServiceKind.EMBY), emptySet(), false, 3 * 60_000L, attempts = 2))
        assertTrue(shouldRetryHomeFeed(setOf(ServiceKind.EMBY), emptySet(), false, 4 * 60_000L, attempts = 2))
        assertEquals(30 * 60_000L, homeFeedRetryDelayMillis(9, failedOutright = false))
    }
}
