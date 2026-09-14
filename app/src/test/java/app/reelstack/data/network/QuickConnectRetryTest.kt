package app.reelstack.data.network

import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class QuickConnectRetryTest {
    @Test fun approvalFromTheFinalAllowedPollIsKept() = runBlocking {
        var reads = 0
        val result = awaitQuickConnectApproval(
            initial = QuickConnectChallenge("secret", "123456", false),
            maxPolls = 3,
            pollIntervalMillis = 1_000,
            pause = {},
            read = { current -> current.copy(authenticated = ++reads == 3) },
        )

        assertTrue(result.authenticated)
        assertEquals(3, reads)
    }

    @Test fun transientContactFailureKeepsSameChallengeAndRecovers() = runBlocking {
        var calls = 0
        val waits = mutableListOf<Long>()
        val result = retryQuickConnectRead(pause = { waits += it }) {
            if (++calls < 3) contacting("Jellyfin") { throw IOException("offline") }
            "same-challenge"
        }
        assertEquals("same-challenge", result)
        assertEquals(listOf(1000L, 2000L), waits)
    }
    @Test fun expiredCodeIsNotRetried() = runBlocking {
        var calls = 0
        try {
            retryQuickConnectRead(pause = { fail("No pause") }) { calls++; throw ServiceMessage("Expired") }
        } catch (_: ServiceMessage) { }
        assertEquals(1, calls)
    }
    @Test fun cancellationIsNeverRetried() = runBlocking {
        var calls = 0
        try {
            retryQuickConnectRead(pause = { fail("No pause") }) { calls++; throw CancellationException() }
        } catch (_: CancellationException) { }
        assertEquals(1, calls)
    }
}
