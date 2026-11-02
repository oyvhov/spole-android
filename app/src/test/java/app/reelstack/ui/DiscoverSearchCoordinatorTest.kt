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
            searchLibraries = { _, query -> library(query) }, searchDiscover = { _, query -> discover(query) },
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

    @Test fun failedSeerrDoesNotRemovePlayableLibraryAnswers() = runTest {
        val fixture = Fixture(this, discover = { delay(500); error("offline") })
        fixture.coordinator.setQuery("Silo")
        advanceUntilIdle()
        assertEquals("Silo", fixture.state.librarySearchResults.single().title)
        assertTrue(fixture.state.searchResults.isEmpty())
        assertNotNull(fixture.state.searchError)
        assertFalse(fixture.state.isSearching)
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
