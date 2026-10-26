package app.reelstack

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.ReelstackSheets
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.AppSheet
import app.reelstack.ui.theme.ReelstackTheme
import app.reelstack.ui.theme.LocalMotionEnabled
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

/** The whole cast card must fit, including the name and role below its portrait. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "nn-rNO-w960dp-h540dp-television-xhdpi", application = SheetTestApplication::class)
@OptIn(coil3.annotation.DelicateCoilApi::class)
class TvDetailsRobolectricTest {
    @get:Rule val rule = createComposeRule()
    private val cast = listOf(
        CastMember("Alexandra Testperson", "Ei viktig rolle", remoteId = "first"),
        CastMember("Kristoffer Testperson", "Ein annan karakter", remoteId = "second"),
        CastMember("Anne Testperson", "Ein lang karaktertekst", remoteId = "third"),
    )

    @After fun resetFixtures() {
        coil3.SingletonImageLoader.reset()
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test fun movieCastFitsOnOpeningWithLogoRatingsAndTwoLineSynopsis() {
        openMovie()
        saveReview("movie-cast.png")
        rule.onNodeWithTag("play-in-spole").assertIsFocused()
        assertTrue(rule.onNodeWithTag("detail-title").getUnclippedBoundsInRoot().top >= 16.dp)
        cast.forEach { assertWholeCardVisible(it) }
    }

    @Test fun remoteRevealsWholeCastCardAtDoubleFontSizeAndReturnsToActions() {
        openMovie(fontScale = 2f)
        rule.onNodeWithTag("play-in-spole").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        val focusedCard = rule.onNode(isFocused() and hasText("Testperson", substring = true))
        val firstFocus = focusedCard.fetchSemanticsNode().id
        assertWholeCardVisible(focusedCard)
        focusedCard.performKeyInput { pressKey(Key.DirectionRight) }
        assertNotEquals(firstFocus, focusedCard.fetchSemanticsNode().id)
        assertWholeCardVisible(focusedCard)
        saveReview("movie-cast-font-2.png")
        cast.forEach { person ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            rule.onNodeWithText(person.name, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            for (line in 0 until layout.lineCount) {
                assertFalse("Names should fit at double font size: ${person.name}", layout.isLineEllipsized(line))
            }
            for (line in 1 until layout.lineCount) {
                val start = layout.getLineStart(line)
                assertTrue("Names should wrap between words: ${person.name}",
                    person.name[start - 1].isWhitespace() || person.name[start].isWhitespace())
            }
        }
        focusedCard.performKeyInput { pressKey(Key.DirectionUp) }
        rule.onNodeWithTag("play-in-spole").assertIsDisplayed()
        assertTrue(rule.onNodeWithTag("detail-title").getUnclippedBoundsInRoot().top >= 0.dp)
    }

    private fun openMovie(fontScale: Float = 1f) {
        RuntimeEnvironment.setFontScale(fontScale)
        val context = RuntimeEnvironment.getApplication()
        AppPreferencesRepository(context).personalization = Personalization()
        val bitmap = android.graphics.Bitmap.createBitmap(400, 100, android.graphics.Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(bitmap).drawText("EIN TESTFILM", 4f, 78f,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 58f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        // A bitmap mapper avoids Windows file: paths being interpreted as Android paths.
        coil3.SingletonImageLoader.setUnsafe(coil3.ImageLoader.Builder(context).components {
            add(coil3.map.Mapper<String, android.graphics.Bitmap> { data, _ ->
                bitmap.takeIf { data == "fixture-logo" }
            })
        }.build())
        val details = ContentDetails("jellyfin-cast-film", "Ein testfilm", "Jellyfin", "2025",
            artworkRes = R.drawable.session_still, logoUrl = "fixture-logo",
            source = ServiceKind.JELLYFIN, mediaType = "Movie", libraryAvailable = true,
            facts = listOf("2025", "162 min", "NO-15"), criticRating = 94,
            genres = listOf("Thriller", "Crime", "Comedy"), cast = cast,
            progress = .12f, remainingMinutes = 143,
            overview = "Ei gruppe gamle vener møtest igjen etter mange år og må samarbeide for å redde familien. ".repeat(3))
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://example.com", "fixture")),
            activeSheet = AppSheet.TitleDetails(details.key), contentDetails = details)
        rule.setContent { ReelstackTheme {
            val mode = LocalInputModeManager.current
            SideEffect { mode.requestInputMode(InputMode.Keyboard) }
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                ReelstackSheets(state, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        } }
        rule.onNodeWithTag("detail-title").assertHeightIsEqualTo(72.dp)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        rule.onNodeWithTag("overview-text", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(fontScale, layouts.single().layoutInput.density.fontScale, .001f)
    }

    private fun card(person: CastMember) = rule.onNode(hasClickAction() and hasText(person.name))

    private fun assertWholeCardVisible(person: CastMember) {
        assertWholeCardVisible(card(person))
    }

    private fun assertWholeCardVisible(card: SemanticsNodeInteraction) {
        val bounds = card.assertIsDisplayed().getUnclippedBoundsInRoot()
        val viewport = rule.onNodeWithTag("tv-cinematic-detail").getUnclippedBoundsInRoot()
        assertTrue("Whole portrait, name and role must fit: $bounds / $viewport",
            bounds.top >= viewport.top && bounds.bottom <= viewport.bottom)
    }

    private fun saveReview(name: String) {
        val view = ShadowDialog.getLatestDialog().window!!.decorView
        val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(android.graphics.Canvas(bitmap)) }
        val folder = File("build/tv-cast-review").apply { mkdirs() }
        File(folder, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
