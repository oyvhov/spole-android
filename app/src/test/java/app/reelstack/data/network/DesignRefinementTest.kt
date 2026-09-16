package app.reelstack.data.network

import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.Test

class DesignRefinementTest {
    @Test fun oldEpisodeSnapshotOnlyReusesVerifiedSeriesLandscape() {
        val episode = LibraryMedia("episode", "Series", "", artworkRes = 0,
            source = ServiceKind.JELLYFIN, mediaType = "Episode", seriesId = "series")
        fun image(owner: String, type: String) = "https://media.example/jellyfin/Items/$owner/Images/$type?tag=art"
        assertEquals(image("series", "Thumb"), libraryHeroArtworkUrl(episode.copy(artworkUrl = image("series", "Thumb"))))
        assertEquals(image("series", "Backdrop/0"), libraryHeroArtworkUrl(episode.copy(artworkUrl = image("series", "Backdrop/0"))))
        assertNull(libraryHeroArtworkUrl(episode.copy(artworkUrl = image("episode", "Thumb"))))
        assertNull(libraryHeroArtworkUrl(episode.copy(artworkUrl = image("series", "Primary"))))
        assertNull(libraryHeroArtworkUrl(episode.copy(seriesId = null, artworkUrl = image("series", "Thumb"))))
    }
    @Test fun newHeroMetadataWinsAndOldPostersAreNotStretched() {
        val movie = LibraryMedia("movie", "Movie", "", artworkRes = 0, source = ServiceKind.JELLYFIN,
            mediaType = "Movie", artworkUrl = "https://media.example/Items/movie/Images/Primary")
        assertNull(libraryHeroArtworkUrl(movie))
        assertEquals("https://art.example/backdrop.jpg", libraryHeroArtworkUrl(movie.copy(heroUrl = "https://art.example/backdrop.jpg")))
    }
    @Test fun episodeHeroUsesSeriesBackdropInsteadOfEpisodeImage() {
        val item = ServicePayloadParser.libraryItems("""{"Items":[{"Id":"episode","Name":"Episode","Type":"Episode","SeriesId":"series",
            "ImageTags":{"Primary":"still","Thumb":"episode-thumb"},"BackdropImageTags":["episode-backdrop"],
            "ParentBackdropItemId":"series","ParentBackdropImageTags":["series-backdrop"],"SeriesPrimaryImageTag":"series-poster"}]}""").single()
        assertTrue(item.heroImagePath!!.contains("Items/series/Images/Backdrop/0"))
        assertTrue(item.heroImagePath!!.contains("tag=series-backdrop"))
        assertTrue(item.posterImagePath!!.contains("Items/series/Images/Primary"))
    }
    @Test fun seriesCarriesBothPosterAndLandscapeArtwork() {
        val item = ServicePayloadParser.libraryItems("""{"Items":[{"Id":"series","Name":"Show","Type":"Series",
            "ImageTags":{"Primary":"poster","Thumb":"thumb"},"BackdropImageTags":["backdrop"]}]}""").single()
        assertEquals("Thumb", item.artworkImageType)
        assertTrue(item.posterImagePath!!.contains("tag=poster"))
        assertTrue(item.heroImagePath!!.contains("Backdrop/0"))
        assertTrue(item.heroImagePath!!.contains("tag=backdrop"))
    }
    @Test fun episodeWithOnlyItsOwnArtworkNeverSuppliesHeroArt() {
        val item = ServicePayloadParser.libraryItems("""{"Items":[{"Id":"episode","Name":"Episode","Type":"Episode","ImageTags":{"Primary":"still","Thumb":"thumb"},"BackdropImageTags":["own"]}]}""").single()
        assertNull(item.heroImagePath)
    }
}
