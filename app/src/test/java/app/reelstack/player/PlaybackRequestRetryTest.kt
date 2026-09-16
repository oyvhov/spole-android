package app.reelstack.player

import app.reelstack.data.network.HttpResponse
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class PlaybackRequestRetryTest {
    @Test fun temporaryServerFailureRecoversWithoutUserRetry() {
        var calls = 0
        val waits = mutableListOf<Long>()
        val result = playbackRequest(waits::add) { HttpResponse(if (++calls == 1) 503 else 200, "") }
        assertEquals(200, result.statusCode)
        assertEquals(listOf(500L), waits)
    }
    @Test fun networkTimeoutRecoversAndUsesBoundedBackoff() {
        var calls = 0
        val waits = mutableListOf<Long>()
        val result = playbackRequest(waits::add) {
            if (++calls < 3) throw IOException("fixture")
            HttpResponse(200, "")
        }
        assertEquals(200, result.statusCode)
        assertEquals(listOf(500L, 1000L), waits)
    }
    @Test fun permanentFailureIsNotAnEndlessLoop() {
        var calls = 0
        val result = playbackRequest({}) { calls++; HttpResponse(502, "") }
        assertEquals(502, result.statusCode)
        assertEquals(3, calls)
    }
    @Test fun permissionMissingTitleAndRedirectAreNeverRetried() {
        for (status in listOf(401, 403, 404, 302)) {
            var calls = 0
            playbackRequest({ fail("Must not wait") }) { calls++; HttpResponse(status, "") }
            assertEquals(1, calls)
        }
    }
    @Test fun retryAfterIsRespectedWithoutWaitingMinutesInThePlayer() {
        val waits = mutableListOf<Long>()
        var calls = 0
        playbackRequest(waits::add) { HttpResponse(if (++calls == 1) 429 else 200, "", retryAfterSeconds = 2) }
        assertEquals(listOf(2000L), waits)
        calls = 0
        playbackRequest({ fail("Leave a long rate limit to the user") }) {
            calls++; HttpResponse(429, "", retryAfterSeconds = 60)
        }
        assertEquals(1, calls)
    }
}
