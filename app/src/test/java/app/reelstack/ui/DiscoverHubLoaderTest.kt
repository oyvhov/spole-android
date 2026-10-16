package app.reelstack.ui

import app.reelstack.data.model.CastMember
import app.reelstack.data.model.DiscoverGenre
import app.reelstack.data.model.DiscoverHubState
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.DiscoverRowKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverHubLoaderTest {
    private class Source(val failing: Set<DiscoverRowKind> = emptySet()) : DiscoverHubSource {
        override suspend fun row(kind: DiscoverRowKind, page: Int): DiscoverRowPage =
            if (kind in failing) error("down") else DiscoverRowPage(listOf(DiscoverMedia("seerr-movie-${kind.ordinal + 1}", "x", "", 0,
                false, remoteId = kind.ordinal + 1, mediaType = "movie")), page, false)
        override suspend fun genres() = listOf(DiscoverGenre(27, "Grøssar", "movie", null))
        override suspend fun cast(media: DiscoverMedia) = listOf(CastMember("P${media.remoteId}", seerrPersonId = media.remoteId))
    }

    private suspend fun loadWith(source: DiscoverHubSource, current: () -> String? = { "s" }): DiscoverHubState {
        var state = DiscoverHubState()
        DiscoverHubLoader(source).load("s", current) { change -> state = change(state) }
        return state
    }

    @Test fun aFailingRowDoesNotStopTheOthers() = runTest {
        val state = loadWith(Source(failing = setOf(DiscoverRowKind.UPCOMING_MOVIES)))
        assertTrue(state.rows.getValue(DiscoverRowKind.UPCOMING_MOVIES).failed)
        assertEquals(1, state.rows.getValue(DiscoverRowKind.TRENDING).items.size)
        assertEquals(1, state.rows.getValue(DiscoverRowKind.UPCOMING_SERIES).items.size)
        assertEquals(listOf("Grøssar"), state.genres.map { it.name })
        assertEquals(listOf(1), state.people.map { it.id })
        assertTrue(state.rows.values.none { it.loading })
    }

    @Test fun lateRowsFromAnotherScopeAreDropped() = runTest {
        val state = loadWith(Source(), current = { "another profile" })
        assertTrue(state.rows.values.all { it.items.isEmpty() })
        assertTrue(state.people.isEmpty())
    }

    @Test fun rowsLoadWithAnApiKey() = runTest {
        // The loader never asks who the user is: a connection with an API key gets the same rows.
        assertEquals(3, loadWith(Source()).rows.size)
    }
}
