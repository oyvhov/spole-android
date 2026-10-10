package app.reelstack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
class ShelfBringIntoViewTest {
    private fun scroll(offset: Float, size: Float) = ShelfBringIntoView.calculateScrollDistance(offset, size, 1000f)

    @Test fun aCardWellInViewStaysPut() {
        assertEquals(0f, scroll(300f, 300f), 0f)
        assertEquals(0f, scroll(200f, 600f), 0f)
    }

    /** Going down, the next row shows a fifth of the screen past the focused card; going up, the row above does. */
    @Test fun aCardNearAnEdgeLeavesAGlimpseOfTheNextRow() {
        assertEquals(300f, scroll(800f, 300f), 0f)
        assertEquals(100f, scroll(500f, 400f), 0f)
        assertEquals(-100f, scroll(100f, 300f), 0f)
        assertEquals(-500f, scroll(-300f, 300f), 0f)
    }

    /** Something too tall for a glimpse on both sides scrolls only until it is in view. */
    @Test fun aTallItemScrollsOnlyAsFarAsItNeeds() {
        assertEquals(0f, scroll(100f, 700f), 0f)
        assertEquals(600f, scroll(900f, 700f), 0f)
        assertEquals(-100f, scroll(-100f, 700f), 0f)
    }
}
