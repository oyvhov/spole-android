package app.reelstack.ui

import androidx.test.core.app.ApplicationProvider
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.data.repository.MediaSyncRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DiscoverSearchCoordinatorTest {
    private fun owned(title: String) = LibraryMedia(title, title, "", artworkRes = 0, source = ServiceKind.JELLYFIN)
    private fun found(title: String) = DiscoverMedia(title, title, "", 0, false)
    private fun page(title: String) = MediaSyncRepository.SearchPage(listOf(found(title)), 1, false)

    private inner class Fixture(
        scope: TestScope,
        library: suspend (String) -> List<LibraryMedia> = { delay(100); listOf(owned(it)) },
        discover: suspend (String) -> MediaSyncRepository.SearchPage = { delay(500); page(it) },
        libraryByServer: suspend (ServiceConnection, String) -> List<LibraryMedia> = { _, query -> library(query) },
        discoverPage: suspend (String, Int) -> MediaSyncRepository.SearchPage = { query, _ -> discover(query) },
    ) {
        val preferences = AppPreferencesRepository(ApplicationProvider.getApplicationContext())
        var state = ReelstackUiState(
            connections = listOf(
                ServiceConnection(ServiceKind.JELLYFIN, "Local", "https://local.example", "test", userId = "u"),
                ServiceConnection(ServiceKind.SEERR, "Discover", "https://seerr.example", "test"),
            ),
            searchQuery = "previous", searchResults = listOf(found("previous")),
            librarySearchResults = listOf(owned("previous")), searchHasMore = true, searchPage = 3,
        )
        val coordinator = DiscoverSearchCoordinator(
            ApplicationProvider.getApplicationContext(), MediaSyncRepository(), preferences, scope,
            { state }, { state = it(state) }, ioDispatcher = StandardTestDispatcher(scope.testScheduler),
            searchLibrary = libraryByServer, searchDiscover = { _, query, number -> discoverPage(query, number) },
        )
    }

    @Test fun oldCardsStayWhileTypingAndTheLibraryArrivesBeforeSlowSeerr() = runTest {
        val fixture = Fixture(this)
        fixture.coordinator.setQuery("Silo")
        assertEquals("previous", fixture.state.searchResults.single().title)
        assertEquals("previous", fixture.state.librarySearchResults.single().title)
        assertTrue(fixture.state.isSearching)
        assertFalse(fixture.state.searchHasMore)
        advanceTimeBy(451)
        runCurrent()
        assertEquals("Silo", fixture.state.librarySearchResults.single().title)
        assertEquals("previous", fixture.state.searchResults.single().title)
        assertTrue(fixture.state.isSearching)
        advanceUntilIdle()
        assertEquals("Silo", fixture.state.searchResults.single().title)
        assertFalse(fixture.state.isSearching)
    }

    @Test fun aSupersededResponseCannotReplaceTheNewQuery() = runTest {
        val fixture = Fixture(this, discover = { query ->
            withContext(NonCancellable) { delay(if (query == "old") 1000 else 10) }
            page(query)
        })
        fixture.coordinator.setQuery("old")
        advanceTimeBy(351)
        fixture.coordinator.setQuery("new")
        advanceUntilIdle()
        assertEquals("new", fixture.state.searchQuery)
        assertEquals("new", fixture.state.searchResults.single().title)
        assertEquals("new", fixture.state.librarySearchResults.single().title)
        assertFalse(fixture.state.isSearching)
    }

    @Test fun fastSeerrTitlesAndPeopleAppearWhileTheLibraryIsStillSearching() = runTest {
        val person = DiscoverPerson(12, "Person", null)
        val fixture = Fixture(this,
            library = { delay(1000); listOf(owned(it)) },
            discover = { page(it).copy(people = listOf(person)) },
        )
        fixture.coordinator.setQuery("Silo")
        advanceTimeBy(351)
        assertEquals(listOf(found("Silo")), fixture.state.searchResults)
        assertEquals(listOf(person), fixture.state.searchPeople)
        assertTrue(fixture.state.isSearching)
        advanceUntilIdle()
        assertEquals(listOf(owned("Silo")), fixture.state.librarySearchResults)
        assertFalse(fixture.state.isSearching)
    }

    @Test fun localSearchDoesNotKeepPeopleOrPaginationFromThePreviousServerQuery() = runTest {
        val fixture = Fixture(this)
        fixture.state = fixture.state.copy(connections = emptyList(), discover = listOf(found("Silo")),
            searchPeople = listOf(DiscoverPerson(1, "Previous", null)))
        fixture.coordinator.setQuery("sil")
        assertEquals(listOf(found("Silo")), fixture.state.searchResults)
        assertTrue(fixture.state.searchPeople.isEmpty())
        assertFalse(fixture.state.searchHasMore)
        assertEquals(1, fixture.state.searchPage)
    }

    @Test fun failedSeerrDoesNotRemovePlayableLibraryAnswers() = runTest {
        val fixture = Fixture(this, discover = { delay(500); error("offline") })
        fixture.coordinator.setQuery("Silo")
        advanceUntilIdle()
        assertEquals("Silo", fixture.state.librarySearchResults.single().title)
        assertTrue(fixture.state.searchResults.isEmpty())
        assertNotNull(fixture.state.searchError)
        assertFalse(fixture.state.isSearching)
    }

    @Test fun aSlowUnavailableEmbyDoesNotHideJellyfinOrLookLikeAnEmptyAnswer() = runTest {
        val fixture = Fixture(this, libraryByServer = { server, query ->
            if (server.kind == ServiceKind.EMBY) { delay(2000); error("offline") }
            listOf(owned(query))
        })
        fixture.state = fixture.state.copy(connections = fixture.state.connections +
            ServiceConnection(ServiceKind.EMBY, "Cabin", "https://emby.example", "test", userId = "u"))
        fixture.coordinator.setQuery("Silo")
        advanceTimeBy(351)
        assertEquals(listOf(owned("Silo")), fixture.state.librarySearchResults)
        assertTrue(fixture.state.isSearching)
        advanceUntilIdle()
        assertTrue(fixture.state.searchError.orEmpty().contains("Emby"))
        assertFalse(fixture.state.searchError.orEmpty().contains("Jellyfin"))
        assertEquals(listOf(owned("Silo")), fixture.state.librarySearchResults)
        assertEquals(listOf(found("Silo")), fixture.state.searchResults)
    }

    @Test fun aFailedNextPageCanBeRetriedOnceAndKeepsTheLibraryError() = runTest {
        val pages = mutableListOf<Int>()
        val fixture = Fixture(this, library = { error("offline") }, discoverPage = { _, number ->
            pages += number
            if (pages == listOf(1, 2)) error("temporary")
            MediaSyncRepository.SearchPage(listOf(found("First"), found("Page $number")), number, number == 1)
        })
        fixture.coordinator.setQuery("Silo")
        advanceUntilIdle()
        val libraryError = fixture.state.searchError
        assertTrue(libraryError.orEmpty().contains("Jellyfin"))
        fixture.coordinator.loadMore()
        advanceUntilIdle()
        assertTrue(fixture.state.searchHasMore)
        assertTrue(fixture.state.searchMoreFailed)
        assertEquals(1, fixture.state.searchPage)
        assertEquals(listOf("First", "Page 1"), fixture.state.searchResults.map { it.title })
        fixture.coordinator.loadMore()
        fixture.coordinator.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 2), pages)
        assertEquals(listOf("First", "Page 1", "Page 2"), fixture.state.searchResults.map { it.title })
        assertFalse(fixture.state.searchMoreFailed)
        assertFalse(fixture.state.searchHasMore)
        assertEquals(libraryError, fixture.state.searchError)
    }

    @Test fun seerrCanPageWhileWaitingForASlowLibrary() = runTest {
        val fixture = Fixture(this, library = { delay(3000); listOf(owned(it)) },
            discoverPage = { _, number -> MediaSyncRepository.SearchPage(listOf(found("Page $number")), number, number == 1) })
        fixture.coordinator.setQuery("Silo")
        advanceTimeBy(351)
        assertTrue(fixture.state.isSearching)
        assertTrue(fixture.state.searchHasMore)
        fixture.coordinator.loadMore()
        runCurrent()
        assertEquals(2, fixture.state.searchPage)
        advanceUntilIdle()
        assertEquals(listOf("Page 1", "Page 2"), fixture.state.searchResults.map { it.title })
        assertFalse(fixture.state.searchHasMore)
        assertEquals(listOf(owned("Silo")), fixture.state.librarySearchResults)
    }

    @Test fun anOldPageCannotAppendAfterChangingAwayAndBackToTheSameTerm() = runTest {
        val fixture = Fixture(this, discoverPage = { query, number ->
            if (number > 1) withContext(NonCancellable) { delay(3000) }
            MediaSyncRepository.SearchPage(listOf(found("$query $number")), number, true)
        })
        fixture.coordinator.setQuery("Silo")
        advanceUntilIdle()
        fixture.coordinator.loadMore()
        runCurrent()
        fixture.coordinator.setQuery("Dune")
        fixture.coordinator.setQuery("Silo")
        advanceUntilIdle()
        assertEquals(listOf(found("Silo 1")), fixture.state.searchResults)
        assertEquals(1, fixture.state.searchPage)
        assertFalse(fixture.state.loadingMoreSearch)
    }

    @Test fun clearingTheFieldImmediatelyClearsOldCardsAndCancelsPendingAnswers() = runTest {
        val fixture = Fixture(this)
        fixture.coordinator.setQuery("Silo")
        advanceTimeBy(451)
        fixture.coordinator.setQuery("")
        assertTrue(fixture.state.searchResults.isEmpty())
        assertTrue(fixture.state.librarySearchResults.isEmpty())
        advanceUntilIdle()
        assertEquals("", fixture.state.searchQuery)
        assertTrue(fixture.state.searchResults.isEmpty())
        assertFalse(fixture.state.isSearching)
    }

    // History is what you looked at, not what you passed on the way. On a television the keyboard
    // searches after every key, so "Ga" and "Gam" are only the road to "Game".

    @Test fun typingNeverWritesTheTermsOnTheWayToHistory() = runTest {
        val fixture = Fixture(this)
        fixture.coordinator.setQuery("Ga")
        advanceUntilIdle()
        fixture.coordinator.setQuery("Gam")
        advanceUntilIdle()
        fixture.coordinator.setQuery("Game")
        advanceUntilIdle()
        assertTrue(fixture.preferences.searchHistory(fixture.state.activeProfileId).isEmpty())
        assertTrue(fixture.state.searchHistory.isEmpty())
    }

    @Test fun committingRemembersTheTermOnceAndShowsItAtOnce() = runTest {
        val fixture = Fixture(this)
        fixture.coordinator.setQuery("Game")
        advanceUntilIdle()
        fixture.coordinator.commitSearchToHistory()
        fixture.coordinator.commitSearchToHistory()
        assertEquals(listOf("Game"), fixture.preferences.searchHistory(fixture.state.activeProfileId))
        assertEquals(listOf("Game"), fixture.state.searchHistory)
    }

    @Test fun theNewestCommittedTermComesFirst() = runTest {
        val fixture = Fixture(this)
        fixture.coordinator.setQuery("Game")
        advanceUntilIdle()
        fixture.coordinator.commitSearchToHistory()
        fixture.coordinator.setQuery("Game Of Thrones")
        advanceUntilIdle()
        fixture.coordinator.commitSearchToHistory()
        fixture.coordinator.setQuery("game")
        fixture.coordinator.commitSearchToHistory()
        assertEquals(listOf("game", "Game Of Thrones"), fixture.state.searchHistory)
    }

    @Test fun clearingHistoryIsImmediateAndOnlyClearsTheActiveProfile() = runTest {
        val fixture = Fixture(this)
        fixture.preferences.rememberSearch("other", "Dune")
        fixture.coordinator.setQuery("Silo")
        fixture.coordinator.commitSearchToHistory()
        fixture.coordinator.clearSearchHistory()
        assertTrue(fixture.state.searchHistory.isEmpty())
        assertTrue(fixture.preferences.searchHistory(fixture.state.activeProfileId).isEmpty())
        assertEquals(listOf("Dune"), fixture.preferences.searchHistory("other"))
        assertEquals("Silo", fixture.state.searchQuery)
    }

    @Test fun aTermTooShortToSearchIsNotCommitted() = runTest {
        val fixture = Fixture(this)
        fixture.coordinator.setQuery("G")
        fixture.coordinator.commitSearchToHistory()
        fixture.coordinator.setQuery("")
        fixture.coordinator.commitSearchToHistory()
        assertTrue(fixture.state.searchHistory.isEmpty())
        assertTrue(fixture.preferences.searchHistory(fixture.state.activeProfileId).isEmpty())
    }

    @Test fun theCommittedTermIsTheTypedOneEvenBeforeItsAnswerArrives() = runTest {
        val fixture = Fixture(this)
        fixture.coordinator.setQuery("  Silo ")
        assertTrue(fixture.state.isSearching)
        fixture.coordinator.commitSearchToHistory()
        assertEquals(listOf("Silo"), fixture.state.searchHistory)
        advanceUntilIdle()
        assertEquals(listOf("Silo"), fixture.state.searchHistory)
    }
}
