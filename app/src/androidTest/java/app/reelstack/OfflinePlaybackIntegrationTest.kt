@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack

import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.media3.datasource.DataSpec
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.offline.*
import app.reelstack.player.OfflinePlayerActivity
import app.reelstack.ui.theme.ReelstackTheme
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test

/** A small, real download/cache/player round trip on the isolated phone AVD only. */
class OfflinePlaybackIntegrationTest {
    @get:Rule val rule = createComposeRule()

    private class Fixture(private val bytes: ByteArray) : AutoCloseable {
        val server = ServerSocket(0)
        val firstChunk = CountDownLatch(1)
        val continueFirst = CountDownLatch(1)
        val authorised = AtomicBoolean(false)
        val resumedRange = AtomicBoolean(false)
        val url = "http://127.0.0.1:${server.localPort}/video.mp4"

        init {
            thread(isDaemon = true) {
                while (!server.isClosed) {
                    val socket = try { server.accept() } catch (_: SocketException) { break }
                    thread(isDaemon = true) {
                        runCatching { socket.use {
                            it.soTimeout = 5_000
                            val reader = it.getInputStream().bufferedReader()
                            reader.readLine()
                            val headers = mutableMapOf<String, String>()
                            while (true) {
                                val line = reader.readLine() ?: break
                                if (line.isEmpty()) break
                                headers[line.substringBefore(':').lowercase()] = line.substringAfter(':').trim()
                            }
                            if (headers["authorization"]?.contains("fixture-offline-token") == true) authorised.set(true)
                            val start = headers["range"]?.substringAfter("bytes=")?.substringBefore('-')?.toIntOrNull() ?: 0
                            if (start > 0) resumedRange.set(true)
                            val range = if (start > 0) "Content-Range: bytes $start-${bytes.lastIndex}/${bytes.size}\r\n" else ""
                            val out = it.getOutputStream()
                            out.write(("HTTP/1.1 ${if (start > 0) 206 else 200} OK\r\n" +
                                "Content-Type: video/mp4\r\nContent-Length: ${bytes.size - start}\r\n" +
                                "Accept-Ranges: bytes\r\n${range}Connection: close\r\n\r\n").toByteArray())
                            if (firstChunk.count > 0) {
                                out.write(bytes, start, 32_768); out.flush()
                                firstChunk.countDown()
                                continueFirst.await(20, TimeUnit.SECONDS)
                                out.write(bytes, start + 32_768, bytes.size - start - 32_768)
                            } else out.write(bytes, start, bytes.size - start)
                            out.flush()
                        } }
                    }
                }
            }
        }

        override fun close() { continueFirst.countDown(); server.close() }
    }

    private fun await(condition: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 30_000
        while (!condition() && SystemClock.elapsedRealtime() < end) SystemClock.sleep(50)
        assertTrue("Offline operation must finish", condition())
    }

    @Test fun pausedDownloadResumesAndCachedPlaybackIsRevokedAfterProfileRoundTrip() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeFalse(context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION)
        val container = (context.applicationContext as ReelstackApplication).container
        val connections = container.connectionRepository
        val originalProfile = connections.activeProfileId
        val originalConnection = connections.get(ServiceKind.JELLYFIN, "")
        val originalWifi = container.preferencesRepository.wifiOnly
        val downloads = container.offlineDownloads
        val bytes = instrumentation.context.assets.open("player/video.mp4").use { it.readBytes() }
        var id: String? = null
        rule.setContent { ReelstackTheme { Text("Offline-prøve") } }

        Fixture(bytes).use { fixture ->
            try {
                instrumentation.runOnMainSync {
                    connections.activeProfileId = ""
                    container.preferencesRepository.wifiOnly = false
                    downloads.onlyWifi(false)
                    connections.save(ServiceConnection(ServiceKind.JELLYFIN, "Fixture",
                        fixture.url.substringBeforeLast('/'), "fixture-offline-token", "offline-user"))
                    val connection = connections.get(ServiceKind.JELLYFIN)
                    assertNull(downloads.enqueue(OfflineDownloadRequest("", connection,
                        OfflineMediaCandidate("offline-${System.nanoTime()}", ServiceKind.JELLYFIN,
                            directDownloadUrl = fixture.url), "Offline fixture clip", mediaType = "Movie", mimeType = "video/mp4")))
                }
                assertTrue(fixture.firstChunk.await(15, TimeUnit.SECONDS))
                await { downloads.snapshot("").items.any { it.title == "Offline fixture clip" } }
                id = downloads.snapshot("").items.first { it.title == "Offline fixture clip" }.id
                downloads.pause(id!!)
                await { downloads.snapshot("").items.any { it.id == id && it.state == OfflineDownloadState.PAUSED } }
                fixture.continueFirst.countDown()
                downloads.resume(id!!)
                await { downloads.snapshot("").items.any { it.id == id && it.state == OfflineDownloadState.COMPLETE } }
                assertTrue("Download uses the account header", fixture.authorised.get())
                assertTrue("Resume reuses the partial file", fixture.resumedRange.get())

                val source = downloads.playbackSource("", id!!)!!
                fixture.close() // Any accidental network fallback now fails.
                val dataSource = OfflineDownloadRuntime.offlineDataSourceFactory(context).createDataSource()
                val cached = ByteArray(bytes.size)
                try {
                    dataSource.open(DataSpec.Builder().setUri(source.uri).setKey(source.cacheKey).build())
                    var offset = 0
                    while (offset < cached.size) {
                        val count = dataSource.read(cached, offset, cached.size - offset)
                        assertTrue("Entire clip remains locally readable", count > 0)
                        offset += count
                    }
                } finally { dataSource.close() }
                assertArrayEquals(bytes, cached)

                ActivityScenario.launch<OfflinePlayerActivity>(Intent(context, OfflinePlayerActivity::class.java)
                    .putExtra("offline_download_id", id)).use { scenario ->
                    rule.waitUntil(20_000) {
                        rule.onAllNodesWithContentDescription(context.getString(R.string.player_pause)).fetchSemanticsNodes().isNotEmpty()
                    }
                    rule.waitUntil(10_000) {
                        rule.onAllNodesWithTag("player-time-label").fetchSemanticsNodes().any { node ->
                            node.config.getOrNull(SemanticsProperties.Text)?.any { Regex("^0:0[2-9]").containsMatchIn(it.text) } == true
                        }
                    }
                    scenario.moveToState(Lifecycle.State.CREATED)
                    instrumentation.runOnMainSync {
                        connections.activeProfileId = "synthetic-offline-child"
                        connections.activeProfileId = ""
                    }
                    scenario.moveToState(Lifecycle.State.RESUMED)
                    rule.onNodeWithText(context.getString(R.string.offline_playback_unavailable)).assertIsDisplayed()
                    rule.onNodeWithText("Offline fixture clip").assertDoesNotExist()
                }
                connections.save(connections.get(ServiceKind.JELLYFIN).copy(userId = "replacement-user"))
                assertNull("Account replacement cannot borrow the completed file", downloads.playbackSource("", id!!))
            } finally {
                id?.let { downloads.remove("", it) }
                instrumentation.runOnMainSync {
                    container.preferencesRepository.wifiOnly = originalWifi
                    downloads.onlyWifi(originalWifi)
                    if (originalConnection.baseUrl.isNotBlank()) connections.save(originalConnection, "")
                    else connections.delete(ServiceKind.JELLYFIN, "")
                    connections.activeProfileId = originalProfile
                }
            }
        }
    }
}
