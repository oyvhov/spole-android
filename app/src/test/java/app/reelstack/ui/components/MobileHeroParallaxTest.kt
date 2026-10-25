package app.reelstack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.drawnContent
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.ReelstackTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-port-xhdpi", application = app.reelstack.SheetTestApplication::class)
class MobileHeroParallaxTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private var offset by mutableFloatStateOf(0f)
    private var motion by mutableStateOf(true)
    private var compositions = 0
    private val film = LibraryMedia(id = "fixture", title = "Testfilm", subtitle = "2026",
        artworkRes = R.drawable.session_still, mediaType = "Movie", source = ServiceKind.JELLYFIN)

    @Composable
    private fun Hero() {
        SideEffect { compositions++ }
        MobileLibraryFeature(film, {}, rotationEnabled = false, parallaxOffsetProvider = { offset })
    }

    private fun show() {
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelstackTheme {
            CompositionLocalProvider(LocalMotionEnabled provides motion) { Hero() }
        } }
        rule.mainClock.advanceTimeBy(300)
        rule.waitForIdle()
    }

    private fun artworkTop(): Float {
        // Robolectric applies graphics-layer transforms when its native Canvas draws the host.
        rule.runOnIdle { drawnContent(rule.activity).recycle() }
        return rule.onNodeWithTag("mobile-hero-artwork", useUnmergedTree = true)
            .getUnclippedBoundsInRoot().top.value
    }

    private fun change(block: () -> Unit) {
        rule.runOnIdle(block)
        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
    }

    private fun record(name: String): android.graphics.Bitmap {
        val image = rule.runOnIdle { drawnContent(rule.activity) }
        val file = File("build/outputs/motion-review", "$name.png")
        file.parentFile.mkdirs()
        file.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        println("motion-review: ${file.absolutePath}")
        return image
    }

    @Test fun scrollMovesArtworkWithoutRecomposingTheHeroCallerOrMovingActions() {
        show()
        val top = artworkTop()
        val action = rule.onNodeWithTag("mobile-feature-open").getUnclippedBoundsInRoot()
        val before = rule.runOnIdle { compositions }
        val imageBefore = record("hero-before")
        repeat(12) { frame ->
            rule.runOnIdle { offset = (frame + 1) * 8f }
            rule.mainClock.advanceTimeBy(32)
            rule.waitForIdle()
        }
        val count = rule.runOnIdle { compositions - before }
        println("hero-parallax: compositions=$count, updates=12, offsetPx=96")
        assertEquals(top + 96f / rule.density.density, artworkTop(), .1f)
        assertEquals(action, rule.onNodeWithTag("mobile-feature-open").getUnclippedBoundsInRoot())
        val imageAfter = record("hero-scrolled")
        assertFalse("The picture must actually move", imageBefore.sameAs(imageAfter))
        assertEquals("Scroll must only update the artwork layer", 0, count)
    }

    @Test fun reducedMotionStopsParallaxAndReenablingUsesTheLatestOffset() {
        show()
        val top = artworkTop()
        change { offset = 48f }
        assertEquals(top + 48f / rule.density.density, artworkTop(), .1f)
        change { motion = false }
        assertEquals(top, artworkTop(), .1f)
        change { offset = 96f }
        assertEquals(top, artworkTop(), .1f)
        record("hero-reduced-motion").recycle()
        change { motion = true }
        assertEquals(top + 96f / rule.density.density, artworkTop(), .1f)
    }
}
