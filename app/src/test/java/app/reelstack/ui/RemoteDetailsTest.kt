package app.reelstack.ui

import android.app.Application
import androidx.lifecycle.ViewModelStore
import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.RemoteMediaDetails
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.launcher.MemoryTokens
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "television", application = Application::class)
class RemoteDetailsTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: ConnectionRepository
    private lateinit var container: AppContainer
    private val store = ViewModelStore()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        val c = RuntimeEnvironment.getApplication()
        repository = ConnectionRepository(c, c.getSharedPreferences("remote-test", 0).apply { edit().clear().commit() }, MemoryTokens())
        repository.save(ServiceConnection(ServiceKind.JELLYFIN, "Test", "http://127.0.0.1:9", "fake", userId = "adult"), "")
        repository.save(ServiceConnection(ServiceKind.EMBY, "Test", "http://127.0.0.1:9", "fake", userId = "adult-emby"), "")
        container = AppContainer(c, repository)
    }

    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun coldTitleShowsSkeletonThenRichServerDetailsAndPlaybackIdentity() = runTest(dispatcher) {
        val answer = CompletableDeferred<RemoteMediaDetails>()
        val vm = vm { _, _ -> answer.await() }
        vm.selectTab(AppTab.SETTINGS)
        vm.openRemoteDetails(ServiceKind.JELLYFIN, "abc")
        assertEquals(AppTab.HOME, vm.uiState.value.selectedTab)
        assertEquals(AppSheet.TitleDetails("abc"), vm.uiState.value.activeSheet)
        assertTrue(vm.uiState.value.contentDetails!!.loading)
        runCurrent()
        answer.complete(RemoteMediaDetails("Kystvakta", null, overview = "På havet", genres = listOf("Drama"), mediaType = "Movie"))
        runCurrent()
        val detail = vm.uiState.value.contentDetails!!
        assertEquals("Kystvakta", detail.title)
        assertEquals("På havet", detail.overview)
        assertEquals("abc", detail.remoteId)
        assertEquals(ServiceKind.JELLYFIN, detail.source)
        assertEquals("Movie", detail.mediaType)
        assertTrue(detail.libraryAvailable)
        assertFalse(detail.loading)
        assertEquals(ServiceKind.JELLYFIN to "abc", detailPlaybackTarget(vm.uiState.value, detail))
    }

    @Test fun cachedRemoteIdUsesActualUiIdWithoutColdFetch() = runTest(dispatcher) {
        var fetched = 0
        val vm = vm { _, _ -> fetched++; RemoteMediaDetails("Wrong", null) }
        vm.openPersonTitle(LibraryMedia("jellyfin-abc", "Cached", "", artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, remoteId = "abc"))
        vm.openRemoteDetails(ServiceKind.JELLYFIN, "abc")
        assertEquals(AppSheet.TitleDetails("jellyfin-abc"), vm.uiState.value.activeSheet)
        assertEquals("Cached", vm.uiState.value.contentDetails?.title)
        assertEquals(0, fetched)
    }

    @Test fun failureShowsExistingLocalizedError() = runTest(dispatcher) {
        val vm = vm { _, _ -> error("offline") }
        vm.openRemoteDetails(ServiceKind.JELLYFIN, "abc")
        runCurrent()
        assertFalse(vm.uiState.value.contentDetails!!.loading)
        assertEquals(app.reelstack.localization.AppLanguages.wrap(container.appContext).getString(R.string.error_content_details), vm.uiState.value.contentDetails!!.error)
    }

    @Test fun lateResponseCannotReplaceSameIdFromAnotherService() = runTest(dispatcher) {
        val answer = CompletableDeferred<RemoteMediaDetails>()
        val vm = vm { connection, _ ->
            if (connection.kind == ServiceKind.JELLYFIN) withContext(NonCancellable) { answer.await() }
            else RemoteMediaDetails("Emby title", null)
        }
        vm.openRemoteDetails(ServiceKind.JELLYFIN, "abc"); runCurrent()
        vm.openRemoteDetails(ServiceKind.EMBY, "abc"); runCurrent()
        answer.complete(RemoteMediaDetails("Stale adult title", null)); runCurrent()
        assertEquals("Emby title", vm.uiState.value.contentDetails!!.title)
        assertEquals(ServiceKind.EMBY, vm.uiState.value.contentDetails!!.source)
    }

    @Test fun closingSheetInvalidatesInFlightDetails() = runTest(dispatcher) {
        val answer = CompletableDeferred<RemoteMediaDetails>()
        val vm = vm { _, _ -> withContext(NonCancellable) { answer.await() } }
        vm.openRemoteDetails(ServiceKind.JELLYFIN, "abc"); runCurrent()
        vm.closeSheet()
        answer.complete(RemoteMediaDetails("Late title", null)); runCurrent()
        assertNull(vm.uiState.value.activeSheet)
        assertNull(vm.uiState.value.contentDetails)
    }

    @Test fun profileChangeInvalidatesAdultResponse() = runTest(dispatcher) {
        val answer = CompletableDeferred<RemoteMediaDetails>()
        val vm = vm { _, _ -> withContext(NonCancellable) { answer.await() } }
        vm.openRemoteDetails(ServiceKind.JELLYFIN, "abc"); runCurrent()
        repository.registerKidProfile("kid", "Barn", null)
        vm.selectProfile(repository.listProfiles().first { it.isKid })
        answer.complete(RemoteMediaDetails("Adult title", null)); runCurrent()
        assertEquals("kid", vm.uiState.value.activeProfileId)
        assertNull(vm.uiState.value.contentDetails)
    }

    @Test fun libraryParserPreservesSeriesTypeForColdLinks() {
        assertEquals("Series", app.reelstack.data.network.ServicePayloadParser.libraryDetails("""{"Name":"Kystvakta","Type":"Series"}""").mediaType)
    }

    @Test fun libraryParserProvidesColdMovieArtwork() {
        val remote = app.reelstack.data.network.ServicePayloadParser.libraryDetails(
            """{"Id":"abc","Name":"Kystvakta","Type":"Movie","ImageTags":{"Primary":"tag"}}""", "http://127.0.0.1:9")
        assertTrue(remote.artworkUrl?.contains("Items/abc/Images/Primary") == true)
    }

    @Test fun launcherHomeResetsChildNavigationWithoutLeavingChildProfile() = runTest(dispatcher) {
        repository.registerKidProfile("kid", "Barn", null)
        repository.save(ServiceConnection(ServiceKind.JELLYFIN, "Test", "http://127.0.0.1:9", "child-token", userId = "child"), "kid")
        repository.activeProfileId = "kid"
        val vm = vm { _, _ -> RemoteMediaDetails("Title", null) }
        vm.openKidsSeries(LibraryMedia("jellyfin-series", "Kystvakta", "", artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, remoteId = "series", mediaType = "Series"))
        assertTrue(vm.uiState.value.kidsBrowse.open)
        val reset = vm.uiState.value.launcherHomeRequest
        vm.openLauncherHome()
        assertFalse(vm.uiState.value.kidsBrowse.open)
        assertEquals(reset + 1, vm.uiState.value.launcherHomeRequest)
        assertEquals("kid", repository.activeProfileId)
        assertTrue(vm.uiState.value.isKidMode)
        assertEquals(AppTab.HOME, vm.uiState.value.selectedTab)
        assertNull(vm.uiState.value.activeSheet)
    }

    private fun vm(loader: suspend (ServiceConnection, String) -> RemoteMediaDetails) =
        ReelstackViewModel(container, loader).also { store.put("vm", it) }
}
