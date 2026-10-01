package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import org.robolectric.RuntimeEnvironment
import java.io.File

/** Small-phone rendering and scrolling, independent of shared emulator availability. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "nn-rNO-w360dp-h780dp-xhdpi", application = SheetTestApplication::class)
class MobileDetailsRobolectricTest {
    @get:Rule val rule = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor = 1f
    })
    private val details = ContentDetails("fixture", "Ein film", "Seerr", "Film",
        artworkRes = R.drawable.media_placeholder, mediaType = "movie",
        overview = "Ei lang forteljing med plass til å rulle. ".repeat(100))

    @Composable
    private fun Sheets(state: ReelstackUiState) {
        ReelstackSheets(state, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
    private fun bounds(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun playableDetails(font: Float = 1f, loading: Boolean = false, mediaType: String = "Episode",
        title: String = "Ted Lasso"): MutableState<ReelstackUiState> {
        // Dialog installs its own Android density. A parent LocalDensity override never reaches
        // that window, so set the actual system resources before creating the content.
        RuntimeEnvironment.setFontScale(font)
        val episode = details.copy(key = "jellyfin-phone-episode", title = title,
            artworkRes = R.drawable.session_still, facts = listOf("2026", "42 min"),
            genres = listOf("Drama"), tmdbRating = 85f,
            subtitle = if (mediaType == "Episode") "S4 - E9 · Mae Rides the Bus" else "",
            mediaType = mediaType, source = ServiceKind.JELLYFIN,
            season = if (mediaType == "Episode") 4 else null,
            episode = if (mediaType == "Episode") 9 else null,
            progress = .2f, remainingMinutes = 42, loading = loading)
        val state = mutableStateOf(ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://example.com", "fixture")),
            activeSheet = AppSheet.TitleDetails(episode.key), contentDetails = episode))
        rule.setContent {
            ReelstackTheme { Sheets(state.value) }
        }
        return state
    }

    @Test fun mobilePlayRemainsAboveTheSystemInsetWhileSynopsisScrolls() {
        checkPinnedPlay(1f)
    }

    @Test fun mobilePlayRemainsReadableAndPinnedAtDoubleFontSize() {
        checkPinnedPlay(2f)
    }

    private fun checkPinnedPlay(font: Float) {
        playableDetails(font)
        val navigationInset = 48
        val decor = ShadowDialog.getLatestDialog().window!!.decorView
        rule.runOnUiThread {
            decor.dispatchApplyWindowInsets(android.view.WindowInsets.Builder()
                .setInsets(android.view.WindowInsets.Type.navigationBars(), android.graphics.Insets.of(0, 0, 0, navigationInset))
                .setVisible(android.view.WindowInsets.Type.navigationBars(), true).build())
        }
        rule.onNodeWithText("Detaljar", substring = false).assertDoesNotExist()
        val play = rule.onNodeWithTag("play-in-spole").assertIsDisplayed().assertIsEnabled()
        play.assertTextContains("Hald fram · 42 min att")
        val layouts = mutableListOf<TextLayoutResult>()
        play.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals("The dialog must really use the requested font scale", font, layouts.single().layoutInput.density.fontScale, .001f)
        val ratingLayouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText("TMDB").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(ratingLayouts) }
        val rating = ratingLayouts.single()
        // A one-line paragraph can keep the incoming maximum width even when Text wraps to
        // its actual content. Check the drawn line, not that unused paragraph width.
        assertEquals("The whole rating brand must fit on one line", 4, rating.getLineEnd(0))
        assertTrue("The rating brand must fit horizontally", rating.getLineRight(0) <= rating.size.width + 1f)
        assertTrue("The rating brand must fit vertically", rating.getLineBottom(0) <= rating.size.height + 1f)
        val originalPlay = bounds("play-in-spole")
        val frame = bounds("sheet-viewport")
        val close = bounds("sheet-close")
        val art = bounds("mobile-detail-backdrop")
        val title = bounds("detail-title")
        assertEquals("The cinematic art must reach both screen edges", frame.width, art.width, 1f)
        assertEquals("The title is centered on the artwork", art.center.x, title.center.x, 1f)
        rule.onNodeWithTag("mobile-detail-subtitle").assertTextEquals("S4 - E9 · Mae Rides the Bus")
        assertTrue("Play must fit wholly inside the safe viewport", originalPlay.bottom < frame.bottom)
        assertTrue("The gesture bar must have its own space", frame.bottom <= decor.height - navigationInset)
        assertTrue("Scrolling content must stop above the action", bounds("detail-scroll").bottom <= originalPlay.top)
        captureReview("mobile-detail-font-$font.png")
        rule.onNodeWithTag("overview-expand").performScrollTo().performClick()
        rule.onNodeWithTag("detail-scroll").performTouchInput { swipeUp(durationMillis = 180) }
        rule.waitForIdle()
        assertTrue(rule.onNodeWithTag("detail-scroll").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value() > 0)
        assertEquals("Reading must never move Play", originalPlay, bounds("play-in-spole"))
        assertEquals("Scrolling the hero must never move Close", close, bounds("sheet-close"))
        play.assertIsDisplayed().assertIsEnabled()
    }

    @Test fun loadingKeepsMobilePlayDisabledWithoutMovingItsFooter() {
        val state = playableDetails(loading = true)
        rule.onNodeWithTag("play-in-spole").assertIsDisplayed().assertIsNotEnabled()
        val frame = bounds("sheet-viewport")
        val footer = bounds("detail-playback-footer")
        val close = bounds("sheet-close")
        rule.runOnIdle { state.value = state.value.copy(contentDetails = state.value.contentDetails!!.copy(loading = false)) }
        rule.onNodeWithTag("play-in-spole").assertIsDisplayed().assertIsEnabled()
        assertEquals(frame, bounds("sheet-viewport"))
        assertEquals(footer, bounds("detail-playback-footer"))
        assertEquals(close, bounds("sheet-close"))
    }

    @Test fun repeatedPhoneOpeningsStayStableWithEarlyAndLateMetadata() {
        RuntimeEnvironment.setFontScale(1f)
        val episode = details.copy(key = "jellyfin-phone-episode", title = "Ted Lasso",
            source = ServiceKind.JELLYFIN, mediaType = "Episode",
            subtitle = "S4 - E9 · Mae Rides the Bus", season = 4, episode = 9)
        val state = mutableStateOf(ReelstackUiState(activeSheet = null))
        rule.setContent { ReelstackTheme { Sheets(state.value) } }
        rule.mainClock.autoAdvance = false
        for (arrivalFrame in listOf(0, 8, 36)) {
            val previousDialog = ShadowDialog.getLatestDialog()
            rule.runOnUiThread { state.value = state.value.copy(
                activeSheet = AppSheet.TitleDetails(episode.key),
                contentDetails = episode.copy(loading = true, overview = null)) }
            var attachingFrames = 0
            do {
                rule.mainClock.advanceTimeByFrame()
                rule.waitForIdle()
                attachingFrames++
            } while ((ShadowDialog.getLatestDialog() === previousDialog ||
                ShadowDialog.getLatestDialog()?.isShowing != true) && attachingFrames < 20)
            assertTrue("Opening with metadata at frame $arrivalFrame must attach a new dialog before sampling its animation", attachingFrames < 20)
            val decor = ShadowDialog.getLatestDialog().window!!.decorView
            val bitmap = android.graphics.Bitmap.createBitmap(decor.width, decor.height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val positions = mutableListOf<Float>()
            val heights = mutableListOf<Float>()
            repeat(65) { frame ->
                if (frame == arrivalFrame) rule.runOnUiThread {
                    state.value = state.value.copy(contentDetails = episode.copy(
                        facts = listOf("2026", "42 min"), genres = listOf("Drama"), tmdbRating = 85f))
                }
                rule.mainClock.advanceTimeByFrame()
                // Android's draw clock is separate from Compose's animation clock. Inspect
                // the drawn surface: Robolectric semantics omit render-layer translations.
                rule.runOnUiThread {
                    bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
                    decor.draw(canvas)
                }
                positions += (0 until bitmap.height).firstOrNull { y ->
                    android.graphics.Color.alpha(bitmap.getPixel(bitmap.width / 2, y)) == 255
                }?.toFloat() ?: bitmap.height.toFloat()
                if (System.getenv("SPOLE_CAPTURE_REVIEW") == "1" && frame in listOf(0, 10, 64)) {
                    val folder = File("build/mobile-details-review").apply { mkdirs() }
                    File(folder, "opening-$arrivalFrame-frame-$frame.png").outputStream().use {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
                heights += rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().size.height.toFloat()
                if (frame < 5) rule.onNodeWithTag("detail-loading").assertExists()
            }
            val finalTop = positions.last()
            assertTrue("Phone opening must animate rather than snap: $positions", positions.first() > finalTop + 10f)
            assertTrue("Metadata at frame $arrivalFrame caused overshoot: $positions", positions.all { it >= finalTop - 1f })
            assertTrue("Phone entrance reversed direction: $positions", positions.zipWithNext().all { (a, b) -> b <= a + 1f })
            assertTrue("Metadata changed the phone surface height: $heights", heights.all { kotlin.math.abs(it - heights.first()) < 1f })
            rule.onNodeWithTag("detail-loading").assertDoesNotExist()
            rule.onNodeWithTag("mobile-detail-backdrop").assertIsDisplayed()
            val viewport = bounds("sheet-viewport")
            val close = bounds("sheet-close")
            repeat(6) {
                rule.mainClock.advanceTimeByFrame()
                assertEquals(viewport, bounds("sheet-viewport"))
                assertEquals(close, bounds("sheet-close"))
            }
            rule.runOnUiThread { state.value = state.value.copy(activeSheet = null, contentDetails = null) }
            var detachingFrames = 0
            do {
                rule.mainClock.advanceTimeByFrame()
                rule.waitForIdle()
                detachingFrames++
            } while (ShadowDialog.getLatestDialog()?.isShowing == true && detachingFrames < 20)
            assertTrue("The old dialog must be detached between openings", detachingFrames < 20)
            bitmap.recycle()
        }
        rule.mainClock.autoAdvance = true
    }

    @Test fun disconnectedTitlesHaveNoEmptyPlaybackFooter() {
        rule.setContent { ReelstackTheme { Sheets(ReelstackUiState(
            activeSheet = AppSheet.TitleDetails(details.key), contentDetails = details)) } }
        rule.onNodeWithTag("detail-playback-footer").assertDoesNotExist()
        rule.onNodeWithTag("play-in-spole").assertDoesNotExist()
        rule.onNodeWithTag("sheet-close").assertIsDisplayed()
    }

    @Test fun moviePlayStaysVisibleBesideALongPosterTitleAtDoubleFontSize() {
        playableDetails(font = 2f, mediaType = "Movie", title = "Ein lang filmtittel med fleire linjer")
        rule.onNodeWithTag("mobile-detail-backdrop").assertExists()
        val play = bounds("play-in-spole")
        rule.onNodeWithTag("overview-expand").performScrollTo().performClick()
        rule.onNodeWithTag("play-in-spole").assertIsDisplayed().assertIsEnabled()
        assertEquals(play, bounds("play-in-spole"))
    }

    /** Opt-in local design review; normal CI runs write no images. */
    private fun captureReview(name: String) {
        if (System.getenv("SPOLE_CAPTURE_REVIEW") != "1") return
        val folder = File("build/mobile-details-review").apply { mkdirs() }
        val view = ShadowDialog.getLatestDialog().window!!.decorView
        val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(android.graphics.Canvas(bitmap)) }
        File(folder, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun seriesWithoutAConcreteEpisodeDoesNotOfferPlayback() {
        playableDetails(mediaType = "Series")
        rule.onNodeWithTag("detail-playback-footer").assertDoesNotExist()
        rule.onNodeWithTag("play-in-spole").assertDoesNotExist()
        rule.onNodeWithTag("sheet-close").assertIsDisplayed()
    }

}
