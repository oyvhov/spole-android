package app.reelstack.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackMetadataTest {

    @Test
    fun `server age numbers become a classification badge`() {
        assertEquals("6+", ageRatingLabel("6"))
        assertEquals("18+", ageRatingLabel(" 18 "))
    }

    @Test
    fun `years runtimes and free text are not mistaken for age ratings`() {
        assertNull(ageRatingLabel("2026"))
        assertNull(ageRatingLabel("94 min"))
        assertNull(ageRatingLabel("PG-13"))
    }
}
