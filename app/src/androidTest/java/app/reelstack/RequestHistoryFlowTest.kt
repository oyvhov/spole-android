package app.reelstack

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackViewModel
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.Assert.*
import org.junit.Test

/** Real ViewModel + HTTP + storage. Synthetic local server on isolated AVD 5562 only. */
class RequestHistoryFlowTest {
    private class Fixture : AutoCloseable {
        val server = ServerSocket(0)
        val historyReads = AtomicInteger()
        @Volatile var actor = 7
        @Volatile var failOlder = false
        @Volatile var blockOffset: Int? = null
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val responded = CountDownLatch(1)
        init {
            thread(isDaemon = true) {
                while (!server.isClosed) {
                    val socket = try { server.accept() } catch (_: SocketException) { break }
                    thread(isDaemon = true) {
                        runCatching { socket.use {
                            it.soTimeout = 5000
                            val reader = it.getInputStream().bufferedReader()
                            val path = reader.readLine().split(' ').getOrNull(1).orEmpty()
                            while (!reader.readLine().isNullOrEmpty()) { /* consume headers */ }
                            var status = 200
                            val isHistory = path.contains("sortDirection=desc")
                            val body = when {
                                path.endsWith("auth/me") -> """{"id":$actor,"displayName":"Fixture","permissions":32}"""
                                isHistory -> {
                                    historyReads.incrementAndGet()
                                    val offset = Regex("skip=(\\d+)").find(path)!!.groupValues[1].toInt()
                                    if (offset == blockOffset) { entered.countDown(); release.await(10, TimeUnit.SECONDS) }
                                    if (offset > 0 && failOlder) { status = 503; "{}" }
                                    else """{"pageInfo":{"results":21},"results":[${(offset until minOf(offset + 20,21)).joinToString(",") { id ->
                                        """{"id":${id+1},"status":2,"requestedBy":{"id":7},"media":{"tmdbId":${id+1},"mediaType":"movie","status":5,"title":"Fixture ${id+1}","posterPath":"/fixture.jpg"}}"""
                                    }}]}"""
                                }
                                else -> """{"results":[],"pageInfo":{"results":0},"totalPages":0}"""
                            }
                            val bytes = body.toByteArray()
                            it.getOutputStream().write("HTTP/1.1 $status OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray() + bytes)
                            if (isHistory && blockOffset != null) responded.countDown()
                        } }
                    }
                }
            }
        }
        override fun close() { release.countDown(); server.close() }
    }

    private fun exercise(block: (ReelstackViewModel, Fixture) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = AppContainer(instrumentation.targetContext)
        val sections = container.preferencesRepository.visibleHomeSections
        val store = ViewModelStore()
        Fixture().use { fixture ->
            ServiceKind.entries.forEach(container.connectionRepository::delete)
            container.preferencesRepository.visibleHomeSections = emptySet()
            container.connectionRepository.save(ServiceConnection(ServiceKind.SEERR, "Fixture",
                "http://127.0.0.1:${fixture.server.localPort}", "connect.sid=fixture", "7", sessionCookie = true))
            lateinit var model: ReelstackViewModel
            try {
                instrumentation.runOnMainSync { model = ReelstackViewModel(container); store.put("history", model) }
                await { model.uiState.value.accounts[ServiceKind.SEERR]?.id == "7" }
                block(model, fixture)
            } finally {
                fixture.release.countDown()
                instrumentation.runOnMainSync { store.clear() }
                ServiceKind.entries.forEach(container.connectionRepository::delete)
                container.preferencesRepository.visibleHomeSections = sections
                container.mediaSnapshotStore.clear()
            }
        }
    }
    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun await(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 15000
        while (!condition() && System.currentTimeMillis() < end) Thread.sleep(20)
        assertTrue("Asynchronous operation must finish", condition())
    }

    @Test fun duplicateTapsAndFailedOlderPageKeepTheCursorAndRetryCorrectly() = exercise { model, server ->
        main { model.openRequestHistory() }
        await { model.uiState.value.requestHistory.loaded }
        assertEquals(20, model.uiState.value.requestHistory.items.size)
        server.blockOffset = 20
        server.failOlder = true
        main { model.loadRequestHistory(true); model.loadRequestHistory(true) }
        assertTrue(server.entered.await(5, TimeUnit.SECONDS))
        assertEquals(2, server.historyReads.get())
        server.release.countDown()
        await { model.uiState.value.requestHistory.error != null }
        assertEquals(20, model.uiState.value.requestHistory.items.size)
        assertEquals(20, model.uiState.value.requestHistory.nextOffset)
        server.failOlder = false
        main { model.loadRequestHistory(true) }
        await { model.uiState.value.requestHistory.items.size == 21 }
        assertFalse(model.uiState.value.requestHistory.hasMore)
        assertEquals(3, server.historyReads.get())
    }
    @Test fun signOutDuringResponseCannotRepopulateOldHistory() = exercise { model, server ->
        server.blockOffset = 0
        main { model.openRequestHistory() }
        assertTrue(server.entered.await(5, TimeUnit.SECONDS))
        main { model.removeConnection(ServiceKind.SEERR) }
        server.release.countDown()
        assertTrue(server.responded.await(5, TimeUnit.SECONDS))
        main { /* drain main work */ }
        assertTrue(model.uiState.value.requestHistory.items.isEmpty())
        assertFalse(model.uiState.value.showRequestHistory)
        assertFalse(model.uiState.value.requestHistory.loading)
    }
    @Test fun profileRefreshDropsHistoryWhenTheServerActorChanges() = exercise { model, server ->
        main { model.openRequestHistory() }
        await { model.uiState.value.requestHistory.loaded }
        server.actor = 8
        main { model.refreshLiveData() }
        await { model.uiState.value.accounts[ServiceKind.SEERR]?.id == "8" }
        assertTrue(model.uiState.value.requestHistory.items.isEmpty())
        assertFalse(model.uiState.value.requestHistory.loaded)
    }
    @Test fun aDifferentServerActorStopsBeforeFetchingAnotherPage() = exercise { model, server ->
        main { model.openRequestHistory() }
        await { model.uiState.value.requestHistory.loaded }
        server.actor = 8
        main { model.loadRequestHistory(true) }
        await { model.uiState.value.requestHistory.error != null }
        assertTrue(model.uiState.value.requestHistory.items.isEmpty())
        assertEquals(1, server.historyReads.get())
    }
}
