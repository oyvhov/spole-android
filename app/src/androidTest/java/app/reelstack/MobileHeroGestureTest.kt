package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.MobileLibraryFeature
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MobileHeroGestureTest {
    @get:Rule val rule = createComposeRule()
    private val first = LibraryMedia("first", "Den første filmen med eit langt namn", "", artworkRes = 0,
        source = ServiceKind.JELLYFIN, mediaType = "Movie", remoteId = "first")
    private val second = first.copy(id = "second", title = "Den andre filmen", remoteId = "second")

    private fun show(onOpen: (String) -> Unit = {}, motion: Boolean = true, rotation: Boolean = false) {
        rule.setContent { ReelstackTheme { CompositionLocalProvider(LocalMotionEnabled provides motion) {
            LazyColumn {
                item { MobileLibraryFeature(first, onOpen, candidates = listOf(first, second), rotationEnabled = rotation,
                    header = { Text("Spole", modifier = Modifier.testTag("fixture-header")) }) }
                item { Box(Modifier.height(1_200.dp)) }
            }
        } } }
    }

    @Test fun swipeUpdatesTheTitleAndDetailsAction() {
        var opened: String? = null
        show({ opened = it })
        val header = rule.onNodeWithTag("fixture-header").getUnclippedBoundsInRoot()
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeLeft() }
        rule.onNodeWithText(second.title).assertIsDisplayed()
        rule.onNodeWithTag("mobile-feature-open").performClick()
        rule.runOnIdle { assertEquals(second.id, opened) }
        assertEquals(header, rule.onNodeWithTag("fixture-header").getUnclippedBoundsInRoot())
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeRight() }
        rule.onNodeWithText(first.title).assertIsDisplayed()
    }

    @Test fun swipesLoopAcrossBothEnds() {
        show()
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeRight() }
        rule.onNodeWithText(second.title).assertIsDisplayed()
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeLeft() }
        rule.onNodeWithText(first.title).assertIsDisplayed()
        repeat(2) { rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeLeft() } }
        rule.onNodeWithText(first.title).assertIsDisplayed()
    }

    @Test fun manualTouchWorksWithReducedMotion() {
        show(motion = false)
        rule.onNodeWithTag("mobile-hero-dot-1").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithText(second.title).assertIsDisplayed()
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeRight() }
        rule.onNodeWithText(first.title).assertIsDisplayed()
    }

    @Test fun manualSwipeRestartsAutomaticRotationDelay() {
        rule.mainClock.autoAdvance = false
        show(rotation = true)
        rule.mainClock.advanceTimeBy(7_000)
        rule.onNodeWithText(first.title).assertIsDisplayed()
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeLeft() }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText(second.title).assertIsDisplayed()
        rule.mainClock.advanceTimeBy(1_500)
        rule.onNodeWithText(second.title).assertIsDisplayed()
        rule.mainClock.advanceTimeBy(7_200)
        rule.onNodeWithText(first.title).assertIsDisplayed()
        rule.mainClock.autoAdvance = true
    }

    @Test fun verticalGestureStillScrollsTheFeed() {
        lateinit var feed: LazyListState
        rule.setContent { ReelstackTheme {
            feed = rememberLazyListState()
            LazyColumn(state = feed) {
                item { MobileLibraryFeature(first, {}, candidates = listOf(first, second), rotationEnabled = false) }
                item { Box(Modifier.height(1_200.dp)) }
            }
        } }
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeUp() }
        rule.runOnIdle { assertTrue(feed.firstVisibleItemIndex > 0 || feed.firstVisibleItemScrollOffset > 0) }
    }

    @Test fun headingAndActionsRemainReachableAtSystemFontScale() {
        var actualFontScale = 0f
        rule.setContent { ReelstackTheme {
            actualFontScale = LocalDensity.current.fontScale
            LazyColumn {
                item { MobileLibraryFeature(first, {}, candidates = listOf(first, second), rotationEnabled = false,
                    header = { Text("Spole", modifier = Modifier.testTag("fixture-header")) }) }
            }
        } }
        val expected = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.configuration.fontScale
        rule.runOnIdle { assertEquals(expected, actualFontScale, .001f) }
        val heading = rule.onNode(hasTestTag("mobile-hero-title") and hasText(first.title), useUnmergedTree = true)
        heading.assertIsDisplayed()
        rule.onNodeWithTag("mobile-feature-play").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("mobile-feature-open").performScrollTo().assertIsDisplayed()
        val header = rule.onNodeWithTag("fixture-header").getUnclippedBoundsInRoot()
        val title = heading.getUnclippedBoundsInRoot()
        assertTrue(title.top > header.bottom)
    }
}
