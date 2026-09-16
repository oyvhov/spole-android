package app.reelstack.player

import app.reelstack.data.network.HttpResponse
import java.io.IOException

/** Bounded retries for reads/negotiation only, never playback reports or permission failures. */
internal fun playbackRequest(wait: (Long) -> Unit = Thread::sleep, request: () -> HttpResponse): HttpResponse {
    for (attempt in 0..2) {
        var pause = (attempt + 1) * 500L
        try {
            val response = request()
            if (response.statusCode !in setOf(408, 429, 500, 502, 503, 504) || attempt == 2) return response
            response.retryAfterSeconds?.let { seconds ->
                if (seconds > 5) return response
                pause = maxOf(pause, seconds * 1000L)
            }
        } catch (error: IOException) {
            if (attempt == 2) throw error
        }
        wait(pause)
    }
    error("Unreachable")
}
