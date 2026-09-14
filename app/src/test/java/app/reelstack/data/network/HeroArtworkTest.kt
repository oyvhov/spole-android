package app.reelstack.data.network

import org.junit.Assert.*
import org.junit.Test

class HeroArtworkTest {
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
