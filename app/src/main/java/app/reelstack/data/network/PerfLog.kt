package app.reelstack.data.network

import app.reelstack.BuildConfig

/**
 * Debug-only timings under the `SpolePerf` tag, so a slow screen can be read off logcat instead of
 * guessed at. Never a host, query, token or title: a path and a number of milliseconds.
 */
internal object PerfLog {
    private const val TAG = "SpolePerf"

    fun request(method: String, url: String, status: Int, startedNanos: Long) {
        if (!BuildConfig.DEBUG) return
        val path = runCatching { java.net.URI(url).rawPath }.getOrNull().orEmpty()
            // Ids are not secrets, but they are not needed to see what was slow either.
            .replace(Regex("/[0-9a-fA-F]{16,}"), "/{id}")
        log("http $method $path $status ${elapsed(startedNanos)}ms")
    }

    fun milestone(name: String, startedNanos: Long) {
        if (!BuildConfig.DEBUG) return
        log("$name ${elapsed(startedNanos)}ms")
    }

    private fun elapsed(startedNanos: Long) = (System.nanoTime() - startedNanos) / 1_000_000

    // android.util.Log is not available in JVM unit tests, where BuildConfig.DEBUG is true.
    private fun log(message: String) {
        runCatching { android.util.Log.i(TAG, message) }
    }
}
