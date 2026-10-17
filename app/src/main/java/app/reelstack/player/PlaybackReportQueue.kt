package app.reelstack.player

import kotlinx.coroutines.channels.Channel

/** Keep session boundaries, but replace queued ticks/seeks with the latest position. */
internal class PlaybackReportQueue<T>(
    private val session: (T) -> String,
    private val event: (T) -> String,
    private val capacity: Int = 32,
) {
    private val pending = ArrayDeque<T>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var closed = false

    @Synchronized fun offer(value: T) {
        if (closed) return
        if (event(value) in setOf("/Progress", "/Stopped")) {
            pending.removeAll { session(it) == session(value) && event(it) == "/Progress" }
        }
        if (pending.size >= capacity) {
            val expendable = pending.firstOrNull { event(it) == "/Progress" }
            if (expendable != null) pending.remove(expendable) else pending.removeFirst()
        }
        pending.addLast(value)
        wake.trySend(Unit)
    }

    suspend fun next(): T? {
        while (true) {
            synchronized(this) { if (pending.isNotEmpty()) return pending.removeFirst(); if (closed) return null }
            wake.receiveCatching()
        }
    }

    @Synchronized fun close() { closed = true; wake.close() }
}
