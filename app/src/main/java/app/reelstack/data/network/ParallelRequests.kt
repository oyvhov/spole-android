package app.reelstack.data.network

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

/** How many requests one media-server feed may have open at once. Small servers stay responsive. */
internal const val FEED_PARALLEL_REQUESTS = 6

/**
 * Caps the requests in flight through [inner]. A transport that has not said it is safe to call
 * from several threads gets one call at a time, so the work around it can still be split up.
 */
internal class LimitedTransport(private val inner: JsonHttpTransport, permits: Int) : JsonHttpTransport {
    private val gate = Semaphore(if (inner.supportsConcurrentCalls) permits.coerceAtLeast(1) else 1)

    override val supportsConcurrentCalls: Boolean get() = inner.supportsConcurrentCalls

    private fun <T> limited(call: () -> T): T {
        gate.acquire()
        try { return call() } finally { gate.release() }
    }

    override fun get(url: String, headers: Map<String, String>) = limited { inner.get(url, headers) }
    override fun post(url: String, headers: Map<String, String>, jsonBody: String) = limited { inner.post(url, headers, jsonBody) }
    override fun delete(url: String, headers: Map<String, String>) = limited { inner.delete(url, headers) }
}

/**
 * [block] for every item at the same time, results in the original order. Each call has its own
 * short-lived threads, so a nested call can never wait for a thread its caller is holding.
 */
internal fun <T, R> parallelMap(items: List<T>, block: (T) -> R): List<R> {
    if (items.size <= 1) return items.map(block)
    val pool = Executors.newFixedThreadPool(minOf(items.size, FEED_PARALLEL_REQUESTS)) { task ->
        Thread(task, "spole-request").apply { isDaemon = true }
    }
    try {
        return items.map { item -> pool.submit(Callable { block(item) }) }.map { future ->
            try { future.get() } catch (failure: ExecutionException) { throw failure.cause ?: failure }
        }
    } finally {
        pool.shutdownNow()
    }
}

internal fun <A, B> parallelPair(first: () -> A, second: () -> B): Pair<A, B> {
    val results = parallelMap(listOf<() -> Any?>(first, second)) { it() }
    @Suppress("UNCHECKED_CAST")
    return results[0] as A to results[1] as B
}
