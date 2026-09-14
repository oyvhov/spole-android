package app.reelstack.data.network

import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Retry only reads interrupted by transport loss; expiry and rejected credentials stay terminal. */
internal suspend fun <T> retryQuickConnectRead(
    pause: suspend (Long) -> Unit = { delay(it) },
    read: suspend () -> T,
): T {
    repeat(3) { attempt ->
        coroutineContext.ensureActive()
        try { return read() } catch (error: Exception) {
            val transport = error is IOException || error.cause is IOException
            if (!transport || attempt == 2) throw error
            pause(1_000L * (attempt + 1))
        }
    }
    error("Uventa Quick Connect-tilstand")
}

/**
 * Wait for approval without throwing away a valid answer from the final poll.
 *
 * Keeping this outside the UI also gives the direct Jellyfin flow and the automatic Seerr step
 * identical boundary behaviour. The old UI loop checked approval before each read, so approval on
 * its sixtieth and final read was incorrectly reported as an expired code.
 */
internal suspend fun awaitQuickConnectApproval(
    initial: QuickConnectChallenge,
    maxPolls: Int,
    pollIntervalMillis: Long,
    pause: suspend (Long) -> Unit = { delay(it) },
    onUpdate: suspend (QuickConnectChallenge) -> Unit = {},
    read: suspend (QuickConnectChallenge) -> QuickConnectChallenge,
): QuickConnectChallenge {
    require(maxPolls > 0 && pollIntervalMillis >= 0)
    var challenge = initial
    repeat(maxPolls) {
        coroutineContext.ensureActive()
        if (challenge.authenticated) return challenge
        pause(pollIntervalMillis)
        challenge = retryQuickConnectRead(read = { read(challenge) })
        onUpdate(challenge)
    }
    return challenge
}
