package app.reelstack

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.ui.*
import java.net.ServerSocket
import java.net.SocketException
import kotlin.concurrent.thread
import org.junit.Assert.*
import org.junit.Test

class EpisodeSeriesNavigationTest {
    @Test fun episodeOpensItsSeriesOnTheSameSeasonAndBackRestoresTheEpisode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = AppContainer(instrumentation.targetContext)
        val store = ViewModelStore()
        val server = ServerSocket(0)
        val episode = """{"Id":"episode","Name":"Third episode","SeriesName":"Series title","Type":"Episode","SeriesId":"series","ParentIndexNumber":3,"IndexNumber":3,"Overview":"Episode text"}"""
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: SocketException) { break }
                thread(isDaemon = true) { runCatching { socket.use {
                    it.soTimeout = 3000
                    val reader = it.getInputStream().bufferedReader()
                    val path = reader.readLine().split(' ').getOrNull(1).orEmpty()
                    while (!reader.readLine().isNullOrEmpty()) { }
                    val body = when {
                        path.contains("Users/Me") -> """{"Id":"me","Name":"Fixture","Policy":{"IsAdministrator":false}}"""
                        path.contains("/Views") -> """{"Items":[{"Id":"shows","Name":"Series","CollectionType":"tvshows"}]}"""
                        path.contains("/Seasons") -> """{"Items":[{"Id":"season1","Name":"Season 1","Type":"Season","IndexNumber":1},{"Id":"season3","Name":"Season 3","Type":"Season","IndexNumber":3}]}"""
                        path.contains("/Episodes") -> """{"Items":[$episode]}"""
                        path.substringBefore('?').endsWith("/episode") -> episode
                        path.substringBefore('?').endsWith("/series") -> """{"Id":"series","Name":"Series title","Type":"Series","Overview":"Series text"}"""
                        path.contains("Sessions") -> "[]"
                        else -> """{"Items":[]}"""
                    }
                    val bytes = body.toByteArray()
                    it.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray() + bytes)
                } } }
            }
        }
        fun await(check: () -> Boolean) {
            val deadline = System.currentTimeMillis() + 10000
            while (!check() && System.currentTimeMillis() < deadline) Thread.sleep(25)
            assertTrue(check())
        }
        try {
            ServiceKind.entries.forEach(container.connectionRepository::delete)
            container.connectionRepository.save(ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "http://127.0.0.1:${server.localPort}", "fixture", userId = "me"))
            lateinit var model: ReelstackViewModel
            instrumentation.runOnMainSync {
                model = ReelstackViewModel(container); store.put("series-navigation", model)
                model.openPersonTitle(LibraryMedia("jellyfin-episode", "Series title", "Third episode", artworkRes = R.drawable.media_placeholder,
                    source = ServiceKind.JELLYFIN, remoteId = "episode", mediaType = "Episode", seriesId = "series", season = 3, episode = 3))
            }
            await { model.uiState.value.contentDetails?.loading == false && model.uiState.value.seriesBrowse.selectedSeasonId == "season3" && !model.uiState.value.seriesBrowse.loading }
            instrumentation.runOnMainSync { model.openEpisodeSeries() }
            await { model.uiState.value.contentDetails?.key == "jellyfin-series" && model.uiState.value.contentDetails?.loading == false &&
                model.uiState.value.seriesBrowse.selectedSeasonId == "season3" }
            assertEquals("Series", model.uiState.value.contentDetails?.mediaType)
            instrumentation.runOnMainSync { model.selectSeason("season1"); model.closeSheet() }
            assertEquals("jellyfin-episode", model.uiState.value.contentDetails?.key)
            assertEquals("season3", model.uiState.value.seriesBrowse.selectedSeasonId)
            assertEquals("Episode", model.uiState.value.contentDetails?.mediaType)
            instrumentation.runOnMainSync { model.closeSheet() }
            assertNull(model.uiState.value.activeSheet)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            server.close()
            ServiceKind.entries.forEach(container.connectionRepository::delete)
            container.mediaSnapshotStore.clear()
        }
    }
}
