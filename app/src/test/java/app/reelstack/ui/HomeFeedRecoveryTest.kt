package app.reelstack.ui

import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

class HomeFeedRecoveryTest {
    @Test fun failedOrPartialMediaFeedRecoversAfterCooldown() {
        for (kind in listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
            assertTrue(shouldRetryHomeFeed(setOf(kind), emptySet(), false, 60_000))
            assertTrue(shouldRetryHomeFeed(emptySet(), setOf(kind), false, 60_000))
            assertFalse(shouldRetryHomeFeed(setOf(kind), emptySet(), false, 59_999))
            assertFalse(shouldRetryHomeFeed(setOf(kind), emptySet(), true, 120_000))
        }
    }
    @Test fun successfulEmptyFeedAndUnrelatedServiceFailuresDoNotPollLibrary() {
        assertFalse(shouldRetryHomeFeed(emptySet(), emptySet(), false, 120_000))
        assertFalse(shouldRetryHomeFeed(setOf(ServiceKind.SEERR, ServiceKind.SONARR), emptySet(), false, 120_000))
    }
}
