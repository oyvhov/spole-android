package app.reelstack.ui

import app.reelstack.ui.components.orientationDiffers
import app.reelstack.ui.screens.railArtworkUrl
import app.reelstack.ui.screens.resumeRailIsWide
import org.junit.Assert.*
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * One shape per rail.
 *
 * A shelf that decided the card shape per title put a 2:3 poster next to a 16:9 still and made a row
 * whose cards differed by nearly three times in width. The shelf decides once; a picture that faces
 * the other way is fitted into the frame with a blurred copy of itself behind, which is what
 * [orientationDiffers] is asked about.
 */
class RailShapeTest {
    @Test fun aResumeShelfIsTheWideStillUnlessSomebodyAskedForPosters() {
        assertTrue(resumeRailIsWide(null))
        assertTrue(resumeRailIsWide("AUTO"))
        assertTrue(resumeRailIsWide("THUMB"))
        assertFalse(resumeRailIsWide("POSTER"))
    }

    @Test fun theShapeDoesNotDependOnWhatTheShelfHappensToHold() {
        // Checked on a real Emby shelf: letting the majority media type decide meant five films
        // outvoted four episodes, and every episode still was letterboxed into a poster frame.
        val everyFormat = listOf(null, "AUTO", "THUMB")
        assertEquals("one answer for every shelf", setOf(true), everyFormat.map(::resumeRailIsWide).toSet())
    }

    @Test fun onlyAnOrientationChangeCountsAsAMismatch() {
        val wideFrame = 16f / 9f
        val posterFrame = 2f / 3f
        // A poster handed to a wide frame, and a still handed to a poster frame.
        assertTrue(orientationDiffers(sourceRatio = 2f / 3f, frameRatio = wideFrame))
        assertTrue(orientationDiffers(sourceRatio = 16f / 9f, frameRatio = posterFrame))
        // The picture already fits.
        assertFalse(orientationDiffers(sourceRatio = 16f / 9f, frameRatio = wideFrame))
        assertFalse(orientationDiffers(sourceRatio = 2f / 3f, frameRatio = posterFrame))
    }

    @Test fun nearlySquareArtworkIsNotWorthABlurredBackplate() {
        assertFalse("a 4:3 still is close enough to a wide frame", orientationDiffers(4f / 3f, 16f / 9f))
        assertFalse("nothing to decide without a measurement", orientationDiffers(0f, 16f / 9f))
    }

    // --- and which picture fills it ---

    @Test fun aWideFrameAsksForTheWidePicture() {
        // The resume shelf is always wide. It used to ask for the wide frame and then fetch
        // `artworkUrl`, which for a film is the poster — so every film on the shelf you look at
        // most was a 2:3 poster with a blurred copy of itself filling the sides.
        assertEquals("hero", railArtworkUrl(wide = true, heroUrl = "hero", posterUrl = "poster", artworkUrl = "main"))
    }

    @Test fun aRailCardAsksForHeroArtAtCardSizeNotFeatureSize() {
        val hero = "https://media.example/Items/x/Images/Backdrop?maxWidth=1920&quality=90&tag=t"
        assertEquals(
            "https://media.example/Items/x/Images/Backdrop?maxWidth=1080&quality=85&tag=t",
            railArtworkUrl(wide = true, heroUrl = hero, posterUrl = "poster", artworkUrl = "main"),
        )
    }

    @Test fun aTallFrameAsksForThePoster() {
        assertEquals("poster", railArtworkUrl(wide = false, heroUrl = "hero", posterUrl = "poster", artworkUrl = "main"))
    }

    @Test fun aTitleWithNoBackdropStillShowsSomething() {
        // Not every title has a wide image on the server. A fitted poster beats an empty frame.
        assertEquals("main", railArtworkUrl(wide = true, heroUrl = null, posterUrl = "poster", artworkUrl = "main"))
        assertEquals("main", railArtworkUrl(wide = false, heroUrl = "hero", posterUrl = null, artworkUrl = "main"))
        assertNull(railArtworkUrl(wide = true, heroUrl = null, posterUrl = null, artworkUrl = null))
    }

    @Test fun episodeInWideFramePrefersSeriesThumbnailOverEpisodePrimary() {
        // The wide home rail must stay wide even when the episode also has a Primary still.
        assertEquals(
            "series_backdrop",
            railArtworkUrl(
                wide = true,
                heroUrl = "series_backdrop",
                posterUrl = "series_poster",
                artworkUrl = "still",
                isEpisode = true,
            ),
        )
    }

    @Test fun episodeInWideFrameFallsBackToSeriesBackdropWhenPrimaryMatchesPosterOrMissing() {
        // When the episode has no usable wide image, prefer series backdrop over portrait poster.
        assertEquals(
            "series_backdrop",
            railArtworkUrl(
                wide = true,
                heroUrl = "series_backdrop",
                posterUrl = "series_poster",
                artworkUrl = "series_poster",
                isEpisode = true,
            ),
        )
        assertEquals(
            "series_backdrop",
            railArtworkUrl(
                wide = true,
                heroUrl = "series_backdrop",
                posterUrl = "series_poster",
                artworkUrl = null,
                isEpisode = true,
            ),
        )
    }

    @Test fun episodeInWideFrameFallsBackToPosterIfNoBackdropExists() {
        assertEquals(
            "series_poster",
            railArtworkUrl(
                wide = true,
                heroUrl = null,
                posterUrl = "series_poster",
                artworkUrl = "series_poster",
                isEpisode = true,
            ),
        )
    }

    @Test fun episodeInTallFrameUsesPoster() {
        assertEquals(
            "series_poster",
            railArtworkUrl(
                wide = false,
                heroUrl = "series_backdrop",
                posterUrl = "series_poster",
                artworkUrl = "still",
                isEpisode = true,
            ),
        )
    }
}
