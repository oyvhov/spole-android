package app.reelstack.data.network

import org.junit.Assert.*
import org.junit.Test
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.ContentDetails

class HeroArtworkTest {
    private val media = LibraryMedia("film", "Film", "", artworkRes = 0, source = ServiceKind.JELLYFIN)

    private val detail = ContentDetails("film", "Film", "", "", artworkRes = 0,
        artworkUrl = "https://image.example/poster.jpg", source = ServiceKind.JELLYFIN)

    @Test fun detailKeepsTheOpeningBackdropWhenMetadataArrives() {
        val opening = detail.copy(backdropUrl = "https://image.example/scenery.jpg", loading = true)
        val loaded = opening.copy(backdropUrl = "https://image.example/other.jpg", loading = false)
        assertEquals(opening.backdropUrl, detailBackdropUrl(opening, opening))
        assertEquals(opening.backdropUrl, detailBackdropUrl(opening, loaded))
    }

    @Test fun detailNeverShowsAPosterBeforeTheLateBackdrop() {
        assertNull(detailBackdropUrl(detail, detail))
        val loaded = detail.copy(backdropUrl = "https://image.example/scenery.jpg", loading = false)
        assertEquals(loaded.backdropUrl, detailBackdropUrl(detail, loaded))
        assertEquals(loaded.backdropUrl, detailBackdropUrl(detail.copy(backdropUrl = " "), loaded))
    }

    @Test fun detailAndHeroShareTheSameHighResolutionImageRequest() {
        val url = "https://media.example/Items/film/Images/Backdrop/0?maxWidth=480&quality=75&tag=scenery"
        val item = media.copy(backdropUrl = url, heroUrl = "https://media.example/Items/film/Images/Thumb")
        val opening = detail.copy(backdropUrl = heroArtworkUrl(mobileHeroArtworkUrl(item)))
        assertEquals(opening.backdropUrl, detailBackdropUrl(opening,
            opening.copy(backdropUrl = url.replace("480", "1280"))))
        assertTrue(opening.backdropUrl!!.contains("maxWidth=1920"))
    }

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
