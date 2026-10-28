package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
@OptIn(ExperimentalTestApi::class)
class HomeTvShelfNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val media = List(10) { index -> LibraryMedia(
            id = "episode-$index", title = "Serie $index", subtitle = "Episode", progress = .4f,
            artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode",
        ) }

    private fun showHome(
        nextUp: Boolean = false,
        movies: Boolean = false,
        fontScale: Float = 1f,
        motion: Boolean = true,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://example.com", "fixture", userId = "u")),
            resume = if (!nextUp && !movies) media else emptyList(),
            nextUp = if (nextUp) media else emptyList(),
            recentMovies = if (movies) media else emptyList(),
            homeSections = setOf(if (movies) HomeSection.JELLYFIN_MOVIES else HomeSection.CONTINUE_WATCHING),
        )
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                    CompositionLocalProvider(
                        LocalPersonalization provides Personalization(showHero = false),
                        LocalMotionEnabled provides motion,
                        LocalLayoutDirection provides direction,
                    ) {
                        HomeScreen(state, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}, showSearch = false)
                    }
                }
            }
        }
    }

    private fun assertReturnToFirstCard(movies: Boolean = false, direction: LayoutDirection = LayoutDirection.Ltr) {
        fun card(index: Int) = if (movies) rule.onNode(hasClickAction() and hasText("Serie $index"))
            else rule.onNodeWithTag("resume-card-episode-$index")
        fun start(bounds: androidx.compose.ui.unit.DpRect) =
            if (direction == LayoutDirection.Ltr) bounds.left.value else bounds.right.value
        val first = card(0)
        first.requestFocus()
        first.assertIsFocused()
        val originalStart = start(first.getUnclippedBoundsInRoot())

        val forward = if (direction == LayoutDirection.Ltr) Key.DirectionRight else Key.DirectionLeft
        val backward = if (direction == LayoutDirection.Ltr) Key.DirectionLeft else Key.DirectionRight
        repeat(7) { index ->
            rule.onNode(isFocused()).performKeyInput { pressKey(forward) }
            card(index + 1).assertIsFocused()
        }
        repeat(7) { index ->
            rule.onNode(isFocused()).performKeyInput { pressKey(backward) }
            card(6 - index).assertIsFocused()
        }

        val returnedStart = start(first.getUnclippedBoundsInRoot())
        // The test API combines the scaled position with the unscaled width, so RTL's right edge
        // can differ by half the focused card's growth while the inset itself remains intact.
        val tolerance = if (direction == LayoutDirection.Rtl) 6f else 1f
        assertEquals("The first card should return to its original inset", originalStart, returnedStart, tolerance)
    }

    @Test fun returningToTheFirstResumeCardRestoresItsMargin() {
        showHome()
        assertReturnToFirstCard()
    }

    @Test fun nextUpRestoresItsMarginWithLargeTextAndReducedMotion() {
        showHome(nextUp = true, fontScale = 2f, motion = false)
        assertReturnToFirstCard()
    }

    @Test fun otherHomeShelvesRestoreTheSameMargin() {
        showHome(movies = true)
        assertReturnToFirstCard(movies = true)
    }

    @Test fun returningToTheFirstCardRespectsRightToLeftLayout() {
        showHome(direction = LayoutDirection.Rtl)
        assertReturnToFirstCard(direction = LayoutDirection.Rtl)
    }

    @Test fun rightToLeftWithReducedMotion() {
        showHome(motion = false, direction = LayoutDirection.Rtl)
        assertReturnToFirstCard(direction = LayoutDirection.Rtl)
    }
}
