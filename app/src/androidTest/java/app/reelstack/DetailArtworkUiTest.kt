package app.reelstack

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.MobileCinematicDetails
import app.reelstack.ui.components.TvCinematicDetails
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Pixel checks distinguish the poster, opening backdrop and late replacement artwork. */
class DetailArtworkUiTest {
    @get:Rule val rule = createComposeRule()

    private fun image(name: String, color: Int): String {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "$name.png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file.toURI().toString()
    }

    private fun fixture(type: String = "Movie") = ContentDetails("fixture", "Ein tittel", "", "",
        artworkRes = 0, remoteId = "fixture", source = ServiceKind.JELLYFIN, mediaType = type,
        artworkUrl = image("detail-poster-red", android.graphics.Color.RED), loading = true)

    private fun pixel(tag: String) = rule.onNodeWithTag(tag, useUnmergedTree = true)
        .captureToImage().toPixelMap().let { it[it.width * 9 / 10, it.height / 10] }

    // Capture includes the cinematic scrim, so verify colour identity rather than full brightness.
    private fun greenBackdrop(tag: String): Boolean = pixel(tag).let {
        it.green > .5f && it.green > it.red * 2 && it.green > it.blue * 2
    }

    private fun waitForBackdrop(tag: String) {
        try { rule.waitUntil(10_000) { greenBackdrop(tag) } }
        catch (failure: ComposeTimeoutException) {
            throw AssertionError("Expected green artwork in $tag, got ${pixel(tag)}", failure)
        }
    }

    @Test fun mobileKeepsTheCorrectBackdropAcrossLateMetadata() {
        val opening = fixture().copy(backdropUrl = image("detail-opening-green", android.graphics.Color.GREEN))
        var details by mutableStateOf(opening)
        rule.setContent { ReelstackTheme { MobileCinematicDetails(opening, details, !details.loading) {} } }
        waitForBackdrop("mobile-detail-backdrop")
        val frame = rule.onNodeWithTag("mobile-detail-hero").getUnclippedBoundsInRoot()
        val late = image("detail-late-blue", android.graphics.Color.BLUE)
        rule.runOnIdle { details = opening.copy(loading = false, backdropUrl = late, overview = "Detaljar") }
        repeat(6) {
            rule.mainClock.advanceTimeBy(100)
            assertTrue("The opening backdrop must stay green", greenBackdrop("mobile-detail-backdrop"))
            assertEquals(frame, rule.onNodeWithTag("mobile-detail-hero").getUnclippedBoundsInRoot())
        }
    }

    @Test fun mobileSeriesWaitsForBackdropWithoutFlashingItsPoster() {
        val opening = fixture("Series")
        var details by mutableStateOf(opening)
        rule.setContent { ReelstackTheme { MobileCinematicDetails(opening, details, !details.loading) {} } }
        repeat(6) {
            rule.mainClock.advanceTimeBy(100)
            val color = pixel("mobile-detail-backdrop")
            assertFalse("The red poster must not appear: $color",
                color.red > .5f && color.red > color.green * 2 && color.red > color.blue * 2)
        }
        val frame = rule.onNodeWithTag("mobile-detail-hero").getUnclippedBoundsInRoot()
        val backdrop = image("detail-final-green", android.graphics.Color.GREEN)
        rule.runOnIdle { details = opening.copy(loading = false, backdropUrl = backdrop) }
        waitForBackdrop("mobile-detail-backdrop")
        assertEquals(frame, rule.onNodeWithTag("mobile-detail-hero").getUnclippedBoundsInRoot())
    }

    @Test fun tvBackdropAlsoStaysStableDuringMetadataLoading() {
        val opening = fixture("Series").copy(backdropUrl = image("detail-tv-green", android.graphics.Color.GREEN))
        var details by mutableStateOf(opening)
        rule.setContent { ReelstackTheme {
            Box(Modifier.fillMaxSize()) {
                TvCinematicDetails(details, rememberScrollState(), opening = opening, heading = {}) {}
            }
        } }
        waitForBackdrop("tv-detail-backdrop")
        val late = image("detail-tv-blue", android.graphics.Color.BLUE)
        rule.runOnIdle { details = opening.copy(loading = false, backdropUrl = late) }
        rule.mainClock.advanceTimeBy(600)
        assertTrue(greenBackdrop("tv-detail-backdrop"))
    }
}
