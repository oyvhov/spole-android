package app.reelstack.data.network

import org.junit.Assert.*
import org.junit.Test
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind

class HeroArtworkTest {
    private val media = LibraryMedia("film", "Film", "", artworkRes = 0, source = ServiceKind.JELLYFIN)

    @Test fun mobileChoosesSceneryEvenWhenTvHeroAndCardUseLetteredImages() {
        val backdrop = "https://media.example/Items/film/Images/Backdrop/0?tag=scenery"
        val thumb = "https://media.example/Items/film/Images/Thumb?tag=lettering"
        val item = media.copy(backdropUrl = backdrop, heroUrl = thumb, artworkUrl = thumb)
        assertEquals(backdrop, mobileHeroArtworkUrl(item))
        assertEquals(thumb, libraryHeroArtworkUrl(item))
    }

    @Test fun oldSnapshotsKeepRealBackdropsButDoNotEnlargeThumbsOrPosters() {
        val backdrop = "https://media.example/Items/film/Images/Backdrop/0?tag=scenery"
        assertEquals(backdrop, mobileHeroArtworkUrl(media.copy(heroUrl = backdrop)))
        assertEquals(backdrop, mobileHeroArtworkUrl(media.copy(artworkUrl = backdrop)))
        for (type in listOf("Thumb", "Primary")) {
            val image = "https://media.example/Items/film/Images/$type?tag=lettering"
            assertNull(mobileHeroArtworkUrl(media.copy(heroUrl = image, artworkUrl = image)))
        }
        assertNull(mobileHeroArtworkUrl(media.copy(artworkUrl = "https://image.example/poster.jpg")))
        assertEquals("https://image.example/backdrop.jpg", mobileHeroArtworkUrl(media.copy(heroUrl = "https://image.example/backdrop.jpg")))
    }

    @Test fun oldEpisodeBackdropMustBelongToItsSeries() {
        val episode = media.copy(mediaType = "Episode", seriesId = "series")
        val backdrop = "https://media.example/Items/series/Images/Backdrop/0?tag=scenery"
        assertEquals(backdrop, mobileHeroArtworkUrl(episode.copy(heroUrl = backdrop)))
        assertNull(mobileHeroArtworkUrl(episode.copy(heroUrl = "https://media.example/Items/episode/Images/Backdrop/0?tag=still")))
    }
    @Test fun heroHasOwnHighResolutionCacheUrlAndPreservesImageIdentity() {
        val card = "https://media.example/Items/series/Images/Thumb?maxWidth=480&quality=75&tag=abc"
        val hero = heroArtworkUrl(card)!!
        assertTrue(hero.contains("maxWidth=1920"))
        assertTrue(hero.contains("quality=90"))
        assertTrue(hero.contains("tag=abc"))
        assertFalse(hero.contains("maxWidth=480"))
        assertTrue(heroArtworkUrl(card, true)!!.contains("maxWidth=1280"))
    }
    @Test fun externalAndLocalArtworkStayUnchanged() {
        assertNull(heroArtworkUrl(null))
        assertEquals("https://image.tmdb.org/t/p/w780/a.jpg", heroArtworkUrl("https://image.tmdb.org/t/p/w780/a.jpg"))
    }
}
