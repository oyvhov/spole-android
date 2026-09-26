package app.reelstack.ui

import app.reelstack.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GenreNamesTest {
    @Test fun standardEnglishGenresResolveWithoutChangingCustomNames() {
        assertEquals(R.string.genre_reality_tv, standardGenreResource("  Reality   TV "))
        assertEquals(R.string.genre_comedy, standardGenreResource("comedy"))
        assertEquals(R.string.genre_scifi_fantasy, standardGenreResource("Sci-Fi & Fantasy"))
        assertNull(standardGenreResource("Nordisk krim"))
        assertNull(standardGenreResource("Komedie"))
    }
}
