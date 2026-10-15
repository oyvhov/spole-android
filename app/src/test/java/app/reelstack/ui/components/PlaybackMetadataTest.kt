package app.reelstack.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackMetadataTest {
    @Test fun `runtime chooses a localizable finish sentence and rolls over midnight`() {
        val text = formatMetadataFact("128 min", java.time.LocalTime.of(23, 30))
        assertEquals(app.reelstack.R.string.metadata_runtime_hours_minutes, text.resId)
        assertEquals(listOf(2, 8, "01:38"), text.args)
        assertEquals(app.reelstack.R.string.metadata_runtime_hours,
            formatMetadataFact("120 min", java.time.LocalTime.NOON).resId)
        assertEquals(app.reelstack.R.string.metadata_runtime_minutes,
            formatMetadataFact("28 min", java.time.LocalTime.NOON).resId)
    }

    @Test fun `non-runtime server facts and unavailable runtime stay untouched`() {
        listOf("2026", "PG-13", "Netflix", "0 min", "-1 min").forEach { fact ->
            assertEquals(fact, formatMetadataFact(fact, java.time.LocalTime.NOON).literal)
        }
    }


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
