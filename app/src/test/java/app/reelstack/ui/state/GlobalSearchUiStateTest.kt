package app.reelstack.ui.state

import app.reelstack.R
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalSearchUiStateTest {
    private fun seerr(id: String, title: String, inLibrary: Boolean = false) = DiscoverMedia(
        id = id, title = title, metadata = "Film", artworkRes = R.drawable.media_placeholder, inLibrary = inLibrary,
    )

    private fun owned(id: String, title: String) = LibraryMedia(
        id = id, title = title, subtitle = "Film", artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN,
    )

    @Test fun aTitleYouOwnIsShownOnceInTheLibraryGroup() {
        val state = GlobalSearchUiState(search = SearchSlice(
            query = "silo",
            libraryResults = listOf(owned("jf-1", "Silo")),
            results = listOf(seerr("seerr-tv-1", "Silo", inLibrary = true), seerr("seerr-tv-2", "Silo Stories")),
        ))
        assertEquals(listOf("seerr-tv-2"), state.requestable.map { it.id })
    }

    @Test fun seerrKeepsAnAvailableTitleThatTheLibrarySearchDidNotReturn() {
        // A library that failed or is still indexing must not make the title vanish altogether.
        val state = GlobalSearchUiState(search = SearchSlice(
            query = "dune",
            results = listOf(seerr("seerr-movie-1", "Dune", inLibrary = true)),
        ))
        assertEquals(listOf("seerr-movie-1"), state.requestable.map { it.id })
    }

    @Test fun titlesMatchWithoutCaseOrPunctuation() {
        assertEquals(searchTitleKey("Spider-Man: No Way Home"), searchTitleKey("spider man no way home"))
    }
}
