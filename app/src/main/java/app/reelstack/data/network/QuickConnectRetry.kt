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
