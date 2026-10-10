package app.reelstack.data

import app.reelstack.data.model.*
import app.reelstack.data.network.RemoteLibraryItem
import org.junit.Assert.*
import org.junit.Test

class LibraryPresentationTest {
    @Test fun `two old library sections migrate to the first position of one Next section`() {
        assertEquals(listOf("FAVOURITES", "LIBRARY_NEXT", "FEATURE", "SMART_SHELVES", "LIBRARIES"),
            libraryHubOrder(listOf("FAVOURITES", "NEXT", "FEATURE", "CONTINUE", "LIBRARIES")))
        assertEquals(listOf("FEATURE", "LIBRARY_NEXT", "FAVOURITES", "SMART_SHELVES", "LIBRARIES"), libraryHubOrder(emptyList()))
    }

    /** A saved order from before smart shelves puts them above the library shelves; a moved row stays put. */
    @Test fun `smart shelves join an old order above the library shelves`() {
        assertEquals(listOf("SMART_SHELVES", "LIBRARIES", "FEATURE", "LIBRARY_NEXT", "FAVOURITES"),
            libraryHubOrder(listOf("LIBRARIES", "FEATURE", "LIBRARY_NEXT", "FAVOURITES")))
        assertEquals(listOf("SMART_SHELVES", "FEATURE", "LIBRARY_NEXT", "FAVOURITES", "LIBRARIES"),
            libraryHubOrder(listOf("SMART_SHELVES", "FEATURE", "LIBRARY_NEXT", "FAVOURITES", "LIBRARIES")))
        assertEquals(listOf("FEATURE", "SMART_SHELVES", "LIBRARIES", "LIBRARY_NEXT", "FAVOURITES"),
            libraryHubOrder(listOf("FEATURE", "LIBRARIES", "LIBRARY_NEXT", "FAVOURITES")))
    }

    @Test fun `Next stays visible if one old section was visible and remembers new hiding`() {
        assertEquals(emptySet<String>(), libraryHubHidden(setOf("CONTINUE")))
        assertEquals(emptySet<String>(), libraryHubHidden(setOf("NEXT")))
        assertEquals(setOf("LIBRARY_NEXT"), libraryHubHidden(setOf("CONTINUE", "NEXT")))
        assertEquals(setOf("LIBRARY_NEXT"), libraryHubHidden(setOf("LIBRARY_NEXT")))
    }
    private val show = RemoteLibraryItem("show", "Show", "", null, "Series", "show", artworkUrl = "thumb",
        thumbnailUrl = "thumb", posterUrl = "poster", heroUrl = "backdrop", logoUrl = "logo", bannerUrl = "banner")

    @Test fun `automatic frames follow the catalogue even while it is empty`() {
        assertEquals(LibraryArtType.THUMB, libraryArtType(LibraryDisplay(), "tvshows", emptyList()))
        assertEquals(LibraryArtType.POSTER, libraryArtType(LibraryDisplay(), "movies", emptyList()))
        assertEquals(LibraryArtType.THUMB, libraryArtType(LibraryDisplay(), null, listOf(show)))
        assertEquals(LibraryArtType.POSTER, libraryArtType(LibraryDisplay(artType = LibraryArtType.POSTER), "tvshows", listOf(show)))
    }

    @Test fun `cover uses real poster rather than rewriting the thumb and its tag`() {
        assertEquals("poster", libraryArtworkUrl(show, LibraryArtType.POSTER))
        assertEquals("thumb", libraryArtworkUrl(show, LibraryArtType.THUMB))
        assertEquals("banner", libraryArtworkUrl(show, LibraryArtType.BANNER))
        assertEquals("logo", libraryArtworkUrl(show, LibraryArtType.LOGO))
    }

    @Test fun `missing artwork falls back locally without inventing a request`() {
        val missing = show.copy(posterUrl = null, thumbnailUrl = null, logoUrl = null, bannerUrl = null, heroUrl = null, artworkUrl = "only-art")
        LibraryArtType.entries.forEach { assertEquals("only-art", libraryArtworkUrl(missing, it)) }
        assertNull(libraryArtworkUrl(missing.copy(artworkUrl = null), LibraryArtType.POSTER))
    }

    @Test fun `size buckets preserve image owners tags and server subpaths`() {
        val original = "https://media.example/jellyfin/Items/show/Images/Thumb?maxWidth=1920&quality=90&tag=thumb-tag"
        assertEquals("https://media.example/jellyfin/Items/show/Images/Thumb?maxWidth=480&quality=85&tag=thumb-tag",
            librarySizedArtwork(original, 421))
        assertEquals(librarySizedArtwork(original, 421), librarySizedArtwork(original, 470))
        assertEquals("https://cdn.example/poster.jpg", librarySizedArtwork("https://cdn.example/poster.jpg", 300))
    }

    private fun episode(id: String, series: String?, source: ServiceKind = ServiceKind.JELLYFIN) =
        LibraryMedia(id, id, "", artworkRes = 0, source = source, seriesId = series, mediaType = "Episode")

    @Test fun `next keeps latest resume per series and includes other series and films`() {
        val resume = listOf(episode("older", "show").copy(lastActivityEpochMillis = 1),
            episode("current", "show").copy(lastActivityEpochMillis = 3), episode("film", null))
        val next = listOf(episode("following", "show"), episode("other", "other-show"), episode("film", null))
        assertEquals(listOf("current", "film", "other"), libraryNextItems(resume, next).map { it.id })
    }

    @Test fun `next never drops an identically named id from another source`() {
        val resume = listOf(episode("current", "show"))
        val next = listOf(episode("following", "show", ServiceKind.EMBY))
        assertEquals(2, libraryNextItems(resume, next).size)
    }

    @Test fun `locally marked watched episodes leave Next immediately`() {
        assertEquals(listOf("fresh"), libraryNextItems(listOf(episode("done-resume", "show").copy(played = true)),
            listOf(episode("done-next", "other").copy(played = true), episode("fresh", "third"))).map { it.id })
    }
}
