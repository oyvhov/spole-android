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

    @Test fun aTranslatedTitleMatchesTheSameProviderIdAndType() {
        val library = owned("jf-42", "Den lokale tittelen").copy(tmdbId = 42, mediaType = "Series")
        val same = seerr("seerr-tv-42", "Original title", true).copy(remoteId = 42, mediaType = "tv")
        val state = GlobalSearchUiState(search = SearchSlice(libraryResults = listOf(library), results = listOf(same)))
        assertEquals(emptyList<DiscoverMedia>(), state.requestable)
    }

    @Test fun aRemakeAndAFilmSharingANameWithAnOwnedSeriesAreStillFindable() {
        val library = owned("jf-42", "The Bridge").copy(tmdbId = 42, mediaType = "Series")
        val remake = seerr("seerr-tv-43", "The Bridge", true).copy(remoteId = 43, mediaType = "tv")
        val film = seerr("seerr-movie-42", "The Bridge", true).copy(remoteId = 42, mediaType = "movie")
        val state = GlobalSearchUiState(search = SearchSlice(libraryResults = listOf(library), results = listOf(remake, film)))
        assertEquals(listOf(remake, film), state.requestable)
    }

    @Test fun anEpisodeWithTheSeriesNameDoesNotReplaceTheSeriesDetailPage() {
        val episode = owned("jf-episode", "Silo").copy(mediaType = "Episode")
        val series = seerr("seerr-tv-4", "Silo", true).copy(mediaType = "tv")
        val state = GlobalSearchUiState(search = SearchSlice(libraryResults = listOf(episode), results = listOf(series)))
        assertEquals(listOf(series), state.requestable)
    }

    @Test fun aKnownReleaseYearDistinguishesRemakesWhenTheLibraryHasNoProviderId() {
        val library = owned("original", "Dune").copy(mediaType = "Movie", premiereDate = "1984-12-14")
        val original = seerr("film1", "Dune", true).copy(mediaType = "movie", metadata = "1984")
        val remake = seerr("film2", "Dune", true).copy(mediaType = "movie", metadata = "2021")
        val state = GlobalSearchUiState(search = SearchSlice(libraryResults = listOf(library), results = listOf(original, remake)))
        assertEquals(listOf(remake), state.requestable)
    }
}
