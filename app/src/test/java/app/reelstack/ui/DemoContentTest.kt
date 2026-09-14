package app.reelstack.ui

import app.reelstack.data.model.*
import app.reelstack.ui.components.tabletFeaturedTitles
import org.junit.Assert.*
import org.junit.Test

class DemoContentTest {
    @Test fun populatedOfflineLibraryHasRichDistinctTitlesForBothServices() {
        val movies = demoRecentMovies()
        val series = demoRecentSeries()
        for (source in listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
            assertTrue(movies.count { it.source == source } >= 5)
            assertTrue(series.count { it.source == source } >= 4)
        }
        val all = movies + series
        assertEquals(all.size, all.map { it.id }.distinct().size)
        assertTrue(all.all { it.artworkRes != 0 && it.artworkUrl == null && it.remoteId == null })
        assertTrue(all.all { !it.overview.isNullOrBlank() && it.genres.isNotEmpty() && it.facts.any { fact -> fact.startsWith("★ ") } })
        assertTrue(demoDiscover().size >= 12)
        assertTrue(demoFavourites().size >= 6)
        assertEquals(2, demoSessions().map(::demoSessionArtwork).distinct().size)
    }
    @Test fun localHeroRequiresExplicitDemoAndHonoursHiddenLibraryRows() {
        val pool = demoRecentSeries()
        assertTrue(tabletFeaturedTitles(pool, HomeSection.entries.toSet()).isEmpty())
        assertEquals(5, tabletFeaturedTitles(pool, HomeSection.entries.toSet(), allowLocalArtwork = true).size)
        assertTrue(tabletFeaturedTitles(pool, emptySet(), allowLocalArtwork = true).isEmpty())
        assertTrue(tabletFeaturedTitles(pool, setOf(HomeSection.EMBY_SERIES), true).all { it.source == ServiceKind.EMBY })
    }
    @Test fun progressAndNextEpisodesRemainCoherentAndHaveNoRemoteActions() {
        assertTrue(demoResume().all { it.progress != null && it.progress!! in 0f..1f })
        assertTrue(demoNextUp().all { it.mediaType == "Episode" && it.season != null && it.episode != null && it.seriesId != null })
        assertTrue(demoNextUp().map { it.seriesId }.intersect(demoResume().map { it.seriesId }.toSet()).isEmpty())
        assertTrue(demoDiscover().all { it.remoteId == null && it.artworkUrl == null })
        assertEquals(setOf("Episode", "Movie"), demoUpcoming().map { it.mediaType }.toSet())
    }
}
