package app.reelstack

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.ServiceKind
import app.reelstack.player.*
import app.reelstack.ui.theme.ReelstackTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PlaybackPreviewUiTest {
    @get:Rule val rule = createComposeRule()
    private fun sprite(): TrickplayGrid {
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "preview-${System.nanoTime()}").apply { mkdirs() }
        val image = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
        Canvas(image).apply {
            val paint = Paint()
            listOf(0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt(), 0xffffff00.toInt()).forEachIndexed { i, color ->
                paint.color = color
                drawRect((i % 2 * 160).toFloat(), (i / 2 * 90).toFloat(), (i % 2 * 160 + 160).toFloat(), (i / 2 * 90 + 90).toFloat(), paint)
            }
        }
        File(folder, "0.jpg").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
        return TrickplayGrid(160, 90, 2, 2, 10_000, 4, folder.toURI().toString().trimEnd('/'), "fixture")
    }
    @Test fun spriteCropsEachCellWithoutChangingThePreviewFrame() {
        val grid = sprite()
        var position by mutableLongStateOf(0)
        rule.setContent { ReelstackTheme { PlaybackSeekPreview(position, grid, emptyList(), ServiceKind.JELLYFIN) } }
        val node = rule.onNodeWithTag("player-thumbnail-preview")
        val bounds = node.fetchSemanticsNode().boundsInRoot
        listOf(0, 1, 2, 3).forEach { frame ->
            rule.runOnIdle { position = frame * 10_000L }
            rule.waitUntil(5_000) {
                val pixels = node.captureToImage().toPixelMap()
                val color = pixels[pixels.width / 2, pixels.height / 4]
                when (frame) { 0 -> color.red > .9f && color.green < .1f; 1 -> color.green > .9f && color.red < .1f
                    2 -> color.blue > .9f && color.red < .1f; else -> color.red > .9f && color.green > .9f }
            }
            assertEquals(bounds, node.fetchSemanticsNode().boundsInRoot)
        }
        saveTvReview("player-trickplay-crop", node.captureToImage())
    }
    @Test fun remotePreviewAppearsAboveTheUnmovedTimeline() {
        val grid = sprite()
        lateinit var input: androidx.compose.ui.input.InputModeManager
        rule.setContent { input = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, durationMs = 60_000, trickplay = grid), null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        rule.runOnIdle { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        val timeline = rule.onNodeWithTag("player-timeline")
        val bounds = timeline.fetchSemanticsNode().boundsInRoot
        timeline.performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("player-thumbnail-preview").assertIsDisplayed()
        assertEquals(bounds, timeline.fetchSemanticsNode().boundsInRoot)
        assertTrue(rule.onNodeWithTag("player-thumbnail-preview").fetchSemanticsNode().boundsInRoot.bottom <= bounds.top)
        saveTvReview("player-trickplay-tv", rule.onNodeWithTag("jellyfin-player").captureToImage())
    }
}
