package app.reelstack.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What runs up behind the television's menu line fades out instead of showing through the tabs.
 *
 * Only the strength is tested here: Robolectric does not draw the off-screen blend the fade is made of,
 * so how it looks is checked on a television.
 */
class MenuLineFadeTest {
    @Test fun nothingFadesWhileThePageStandsAtRest() = assertEquals(0f, menuLineFadeStrength(0f, 176f), 0f)

    @Test fun theFadeGrowsWithTheScroll() = assertEquals(.5f, menuLineFadeStrength(88f, 176f), 1e-6f)

    @Test fun whatHasScrolledTheFullLineIsGoneAndStaysGone() {
        assertEquals(1f, menuLineFadeStrength(176f, 176f), 0f)
        assertEquals(1f, menuLineFadeStrength(4000f, 176f), 0f)
    }

    @Test fun noLineMeansNoFade() = assertEquals(0f, menuLineFadeStrength(500f, 0f), 0f)
}
