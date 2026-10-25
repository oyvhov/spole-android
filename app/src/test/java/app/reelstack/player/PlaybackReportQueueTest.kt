package app.reelstack.player

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackReportQueueTest {
    private data class Report(val session: String, val event: String, val position: Int)
    private fun queue() = PlaybackReportQueue<Report>({ it.session }, { it.event })
    @Test fun slowNetworkCoalescesSeeksAndRetainsStartAndFinalStop() = runBlocking {
        val queue = queue()
        queue.offer(Report("one", "", 0))
        repeat(10_000) { queue.offer(Report("one", "/Progress", it)) }
        assertEquals("", queue.next()!!.event)
        assertEquals(9_999, queue.next()!!.position)
        queue.offer(Report("one", "/Progress", 20_000))
        queue.offer(Report("one", "/Stopped", 10_000))
        queue.close()
        assertEquals(Report("one", "/Stopped", 10_000), queue.next())
        assertNull(queue.next())
    }
    @Test fun closingWakesAWaitingReporter() = runBlocking {
        val queue = queue()
        val waiting = async { queue.next() }
        yield()
        queue.close()
        assertNull(withTimeout(1_000) { waiting.await() })
    }
    @Test fun manySessionChangesHaveBoundedMemoryAndKeepTheNewestStop() = runBlocking {
        val queue = queue()
        repeat(100) { queue.offer(Report("$it", "", 0)); queue.offer(Report("$it", "/Stopped", it)) }
        queue.close()
        val drained = mutableListOf<Report>()
        while (true) drained += queue.next() ?: break
        assertEquals(32, drained.size)
        assertEquals(Report("99", "/Stopped", 99), drained.last())
    }
}
