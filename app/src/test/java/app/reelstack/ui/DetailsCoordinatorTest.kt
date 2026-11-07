package app.reelstack.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.data.network.*
import app.reelstack.data.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class DetailsCoordinatorTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://media.example", "fixture", "adult")
    private val owner = SessionScope("", "fixture", 1)
    private fun media(id: String) = LibraryMedia("jellyfin-$id", "Title $id", "", artworkRes = 0, source = ServiceKind.JELLYFIN,
        remoteId = id, mediaType = "Movie")
    private fun state() = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection),
        recentMovies = listOf(media("one"), media("two"))))

    private fun TestScope.feature(state: MutableStateFlow<ReelstackUiState>, writer: SessionUiState = SessionUiState(state),
        read: (String) -> HttpResponse): DetailsCoordinator {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>) = read(url)
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("read only")
        }
        return DetailsCoordinator(writer, backgroundScope, context, MediaSyncRepository(mediaServerClient = MediaServerClient(transport)),
            isTelevision = { false }, captureSession = { owner },
            remoteDetails = { remote, id -> MediaServerClient(transport).details(remote, id) },
            ioDispatcher = StandardTestDispatcher(testScheduler))
    }

    @Test fun `opening another title cancels queued detail work before it reads the server`() = runTest {
        val state = state()
        val reads = mutableListOf<String>()
        val feature = feature(state) { url -> reads += url; HttpResponse(200, """{"Id":"two","Name":"Second title","Type":"Movie"}""") }
        feature.openLibraryDetails("jellyfin-one")
        feature.openLibraryDetails("jellyfin-two")
        runCurrent()
        assertFalse(reads.any { it.substringBefore('?').endsWith("/one") })
        assertEquals("jellyfin-two", state.value.contentDetails?.key)
        assertFalse(state.value.contentDetails!!.loading)
    }

    @Test fun `closing details stops queued reads while other feature state survives`() = runTest {
        val state = state()
        var reads = 0
        val feature = feature(state) { reads++; HttpResponse(200, "{}") }
        feature.openLibraryDetails("jellyfin-one")
        feature.closeDetails()
        state.value = state.value.copy(activeSheet = null, contentDetails = null, searchQuery = "kept")
        runCurrent()
        assertEquals(0, reads)
        assertNull(state.value.contentDetails)
        assertEquals("kept", state.value.searchQuery)
    }

    // A series with two seasons: season one has two episodes, season two has three.
    private val series = LibraryMedia("jellyfin-series", "Serie", "", artworkRes = 0, source = ServiceKind.JELLYFIN,
        remoteId = "series", mediaType = "Series")
    private fun seriesState() = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection),
        recentSeries = listOf(series)))
    private fun items(vararg entries: String) = HttpResponse(200, """{"Items":[${entries.joinToString(",")}]}""")
    private fun season(id: String, number: Int) = """{"Id":"$id","Name":"Season $number","Type":"Season","IndexNumber":$number}"""
    private fun episode(id: String, season: Int, number: Int) =
        """{"Id":"$id","Name":"Episode $number","SeriesName":"Serie","Type":"Episode","ParentIndexNumber":$season,"IndexNumber":$number}"""
    private fun seriesServer(url: String, season2: () -> HttpResponse = {
        items(episode("f1", 2, 1), episode("f2", 2, 2), episode("f3", 2, 3))
    }): HttpResponse = when {
        url.contains("Shows/series/Seasons") -> items(season("s1", 1), season("s2", 2))
        url.contains("Shows/series/Episodes") && url.contains("seasonId=s1") -> items(episode("e1", 1, 1), episode("e2", 1, 2))
        url.contains("Shows/series/Episodes") && url.contains("seasonId=s2") -> season2()
        else -> HttpResponse(404, "")
    }
    private fun SeriesBrowse.episodeIds() = episodes.map { it.remoteId }

    @Test fun `a season switch keeps the previous episodes, marked stale, until the new season arrives`() = runTest {
        val state = seriesState()
        val feature = feature(state, read = ::seriesServer)
        feature.openLibraryDetails("jellyfin-series")
        runCurrent()
        val first = state.value.seriesBrowse
        assertEquals("s1", first.selectedSeasonId)
        assertEquals(listOf("e1", "e2"), first.episodeIds())
        assertFalse(first.episodesStale)
        assertFalse(first.loading)

        feature.selectSeason("s2")

        // Nothing has been answered yet: the list the reader was looking at is still there, but it
        // is flagged as the old season's, and the playable target never reads from it.
        val waiting = state.value.seriesBrowse
        assertEquals("s2", waiting.selectedSeasonId)
        assertEquals(listOf("e1", "e2"), waiting.episodeIds())
        assertTrue(waiting.episodesStale)
        assertTrue(waiting.loading)
        assertTrue(waiting.currentEpisodes.isEmpty())

        runCurrent()
        val arrived = state.value.seriesBrowse
        assertEquals(listOf("f1", "f2", "f3"), arrived.episodeIds())
        assertFalse(arrived.episodesStale)
        assertFalse(arrived.loading)
        assertEquals(arrived.episodes, arrived.currentEpisodes)
    }

    @Test fun `a season that fails to load does not leave the old episodes standing in for it`() = runTest {
        val state = seriesState()
        val feature = feature(state, read = { seriesServer(it, season2 = { HttpResponse(500, "") }) })
        feature.openLibraryDetails("jellyfin-series")
        runCurrent()
        feature.selectSeason("s2")
        runCurrent()
        val failed = state.value.seriesBrowse
        assertEquals("s2", failed.selectedSeasonId)
        assertTrue(failed.episodes.isEmpty())
        assertFalse(failed.episodesStale)
        assertFalse(failed.loading)
        assertNotNull(failed.error)
    }

    @Test fun `going back to the first season before the second has answered ends with the first season's episodes`() = runTest {
        val state = seriesState()
        val feature = feature(state, read = ::seriesServer)
        feature.openLibraryDetails("jellyfin-series")
        runCurrent()
        feature.selectSeason("s2")
        feature.selectSeason("s1")
        assertTrue(state.value.seriesBrowse.episodesStale)
        runCurrent()
        val settled = state.value.seriesBrowse
        assertEquals("s1", settled.selectedSeasonId)
        assertEquals(listOf("e1", "e2"), settled.episodeIds())
        assertFalse(settled.episodesStale)
        assertFalse(settled.loading)
    }

    @Test fun `choosing the season that is already open reloads nothing and never marks it stale`() = runTest {
        val state = seriesState()
        var episodeReads = 0
        val feature = feature(state, read = { url -> if (url.contains("Shows/series/Episodes")) episodeReads++; seriesServer(url) })
        feature.openLibraryDetails("jellyfin-series")
        runCurrent()
        val reads = episodeReads
        val before = state.value.seriesBrowse
        feature.selectSeason("s1")
        runCurrent()
        assertEquals(reads, episodeReads)
        assertEquals(before, state.value.seriesBrowse)
    }

    @Test fun `same title response from a retired account cannot overwrite new account details`() = runTest {
        val state = state()
        val writer = SessionUiState(state)
        val feature = feature(state, writer) {
            writer.revoke()
            state.value = state.value.copy(sessionScope = owner.copy(generation = 2),
                contentDetails = state.value.contentDetails!!.copy(title = "New account", loading = false))
            HttpResponse(200, """{"Id":"one","Name":"Old account","Type":"Movie"}""")
        }
        feature.openLibraryDetails("jellyfin-one")
        runCurrent()
        assertEquals("New account", state.value.contentDetails?.title)
    }

    @Test fun `a library title opens with the artwork and the logo its card had`() = runTest {
        val card = media("one").copy(logoUrl = "https://media.example/Items/one/Images/Logo", heroUrl = "https://media.example/Items/one/Images/Thumb",
            artworkUrl = "https://media.example/Items/one/Images/Primary?maxWidth=480", mediaType = "Movie",
            backdropUrl = "https://media.example/Items/one/Images/Backdrop/0?maxWidth=480")
        val state = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection), recentMovies = listOf(card)))
        val feature = feature(state) { HttpResponse(200, "{}") }
        feature.openLibraryDetails(card.id)
        // Before the server has answered anything: the first frame of the page.
        val first = state.value.contentDetails!!
        assertTrue(first.loading)
        assertEquals(card.logoUrl, first.logoUrl)
        assertEquals(card.artworkUrl, first.artworkUrl)
        assertNotNull(detailBackdropUrl(first, first))
        assertEquals(card.title, first.title)
    }

    @Test fun `a card without a true backdrop still opens over its own picture`() = runTest {
        val card = media("one").copy(artworkUrl = "https://media.example/Items/one/Images/Primary?maxWidth=480")
        val state = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection), recentMovies = listOf(card)))
        val feature = feature(state) { HttpResponse(200, "{}") }
        feature.openLibraryDetails(card.id)
        val first = state.value.contentDetails!!
        assertNull(first.backdropUrl)
        assertEquals(card.artworkUrl, detailBackdropPreviewUrl(first, first))
    }

    @Test fun `a Seerr title opens with the backdrop the Oppdag header had`() = runTest {
        val title = DiscoverMedia("seerr-tv-9", "Eşref Tek", "2026", artworkRes = 0, inLibrary = false, artworkUrl = "https://image.tmdb.org/t/p/w342/poster.jpg",
            remoteId = 9, mediaType = "tv")
        val shown = "https://image.tmdb.org/t/p/w1280/header.jpg"
        val state = MutableStateFlow(ReelstackUiState(sessionScope = owner, discover = listOf(title),
            discoverHub = DiscoverHubState(focusDetails = mapOf(title.id to FocusDetails(emptyList(), null, shown)))))
        val feature = feature(state) { HttpResponse(200, "{}") }
        feature.openDiscoverDetails(title.id)
        val first = state.value.contentDetails!!
        assertEquals(shown, first.backdropUrl)
        assertEquals(title.artworkUrl, first.artworkUrl)
    }

    @Test fun `the list's own backdrop wins over the header's and a bare title keeps its poster as the fallback`() = runTest {
        val listed = DiscoverMedia("seerr-movie-1", "Med", "", artworkRes = 0, inLibrary = false, artworkUrl = "https://image.tmdb.org/p.jpg",
            backdropUrl = "https://image.tmdb.org/listed.jpg", remoteId = 1, mediaType = "movie")
        val bare = DiscoverMedia("seerr-movie-2", "Utan", "", artworkRes = 0, inLibrary = false, artworkUrl = "https://image.tmdb.org/bare.jpg",
            remoteId = 2, mediaType = "movie")
        val state = MutableStateFlow(ReelstackUiState(sessionScope = owner, discover = listOf(listed, bare),
            discoverHub = DiscoverHubState(focusDetails = mapOf(listed.id to FocusDetails(emptyList(), null, "https://image.tmdb.org/header.jpg")))))
        val feature = feature(state) { HttpResponse(200, "{}") }
        feature.openDiscoverDetails(listed.id)
        assertEquals(listed.backdropUrl, state.value.contentDetails!!.backdropUrl)
        feature.openDiscoverDetails(bare.id)
        val first = state.value.contentDetails!!
        assertNull(first.backdropUrl)
        assertEquals(bare.artworkUrl, detailBackdropPreviewUrl(first, first))
    }
}
