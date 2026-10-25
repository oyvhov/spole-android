@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack.player

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubtitleBitmapCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    private val url = "https://media.example/Videos/1/Subtitles/2/Stream.mks"
    private fun client(bytes: ByteArray, received: (okhttp3.Request) -> Unit = {}) =
        OkHttpClient.Builder().addInterceptor { chain ->
            received(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(bytes.toResponseBody()).build()
        }.build()

    @Test fun largeTrackIsFileBackedAndRangeReadsKeepTheOriginalUri() {
        val bytes = ByteArray(17 * 1024 * 1024) { (it % 251).toByte() }
        val directory = temporary.newFolder()
        var requests = 0
        val http = client(bytes) { request ->
            requests++
            assertEquals("synthetic-credential", request.header("Authorization"))
        }
        SubtitleMemoryCache(http, mapOf("Authorization" to "synthetic-credential"), directory).use { cache ->
            cache.loadBitmap(url)
            cache.loadBitmap(url)
            assertEquals(1, requests)
            assertTrue(cache.contains(url))
            assertEquals(bytes.size.toLong(), directory.listFiles()!!.single().length())
            val source = cache.factory(DataSource.Factory { error("Must not reopen the network") }).createDataSource()
            try {
                val offset = 16L * 1024 * 1024 + 71
                assertEquals(128L, source.open(DataSpec.Builder().setUri(Uri.parse(url)).setPosition(offset).setLength(128).build()))
                val actual = ByteArray(128)
                assertEquals(128, source.read(actual, 0, actual.size))
                assertArrayEquals(bytes.copyOfRange(offset.toInt(), offset.toInt() + 128), actual)
                assertEquals(Uri.parse(url), source.uri)
            } finally { source.close() }
        }
        assertTrue(directory.listFiles()!!.isEmpty())
    }

    @Test fun oversizeOrInvalidRequestsLeaveNoFilesAndClosedCachesRejectWork() {
        val directory = temporary.newFolder()
        val cache = SubtitleMemoryCache(client(ByteArray(20)), emptyMap(), directory)
        assertThrows(IOException::class.java) { cache.loadBitmap(url, maximumBytes = 8) }
        assertFalse(cache.contains(url))
        assertTrue(directory.listFiles()!!.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { cache.loadBitmap("not a URL") }
        assertTrue(directory.listFiles()!!.isEmpty())
        cache.close()
        assertThrows(IOException::class.java) { cache.loadBitmap(url) }
    }

    @Test fun evictionAndOrphanCleanupPreserveOtherActivePlayersAndUnrelatedFiles() {
        val actualDirectory = temporary.newFolder()
        val orphan = actualDirectory.resolve("spole-bitmap-orphan.bin").apply { writeText("stale") }
        val directory = object : java.io.File(actualDirectory.absolutePath) {
            // Simulate a process-death leftover occupying the remaining disk budget.
            override fun getUsableSpace() = (if (orphan.exists()) 32L else 128L) * 1024 * 1024
        }
        val unrelated = directory.resolve("unrelated.bin").apply { writeText("keep") }
        val http = client(byteArrayOf(1, 2, 3))
        val first = SubtitleMemoryCache(http, emptyMap(), directory)
        val second = SubtitleMemoryCache(http, emptyMap(), directory)
        try {
            first.loadBitmap(url)
            assertFalse(orphan.exists())
            second.loadBitmap("$url?track=second")
            assertTrue(first.contains(url))
            assertTrue(second.contains("$url?track=second"))
            first.loadBitmap("$url?track=third")
            first.loadBitmap("$url?track=fourth")
            assertFalse(first.contains(url))
            assertTrue(second.contains("$url?track=second"))
            assertEquals(4, directory.listFiles()!!.size)
            first.close()
            assertTrue(second.contains("$url?track=second"))
            assertEquals(2, directory.listFiles()!!.size)
        } finally { first.close(); second.close() }
        assertEquals(listOf(unrelated), directory.listFiles()!!.toList())
    }
}
