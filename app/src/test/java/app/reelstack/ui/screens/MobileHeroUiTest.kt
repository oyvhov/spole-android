package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import app.reelstack.ui.components.MobileLibraryFeature
import app.reelstack.ui.theme.LocalMotionEnabled
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-port-xhdpi", application = app.reelstack.SheetTestApplication::class)
class MobileHeroUiTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val sampleItem = LibraryMedia(
        id = "test-movie-1",
        title = "Inception",
        subtitle = "A dream within a dream",
        artworkRes = 0,
        source = ServiceKind.JELLYFIN,
        artworkUrl = "https://example.test/poster.jpg",
        heroUrl = "https://example.test/hero.jpg",
        mediaType = "Movie",
        remoteId = "rem-1",
    )

    private fun show(state: ReelstackUiState, onLibraryClick: (String) -> Unit = {}, fontScale: Float = 1.0f) = rule.setContent {
        ReelstackTheme {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale)) {
                HomeScreen(
                    state = state,
                    contentPadding = PaddingValues(0.dp),
                    onSessionClick = {},
                    onPlaybackToggle = {},
                    onMediaClick = {},
                    onLibraryClick = onLibraryClick,
                    onUpcomingClick = {},
                    onRefresh = {},
                )
            }
        }
    }

    private val second = sampleItem.copy(id = "second", title = "Arrival", remoteId = "second-remote")

    private fun showCarousel(onOpen: (String) -> Unit = {}, motion: Boolean = true, rotation: Boolean = false) = rule.setContent {
        ReelstackTheme {
            androidx.compose.runtime.CompositionLocalProvider(LocalMotionEnabled provides motion) {
                MobileLibraryFeature(sampleItem, onOpen, candidates = listOf(sampleItem, second), rotationEnabled = rotation)
            }
        }
    }

    @Test fun touchSwipesInBothDirectionsAndOpensTheVisibleTitle() {
        var opened: String? = null
        showCarousel(onOpen = { opened = it })
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeLeft() }
        rule.onNodeWithText("Arrival").assertIsDisplayed()
        rule.onNodeWithTag("mobile-feature-open").performClick()
        assertEquals("second", opened)
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeRight() }
        rule.onNodeWithText("Inception").assertIsDisplayed()
    }

    @Test fun touchCarouselLoopsAcrossBothEnds() {
        showCarousel()
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeRight() }
        rule.onNodeWithText("Arrival").assertIsDisplayed()
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeLeft() }
        rule.onNodeWithText("Inception").assertIsDisplayed()
        repeat(2) { rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeLeft() } }
        rule.onNodeWithText("Inception").assertIsDisplayed()
    }

    @Test fun manualSelectionWorksWithReducedMotionAndLargeDotTargets() {
        showCarousel(motion = false)
        rule.onNodeWithTag("mobile-hero-dot-1").performClick()
        rule.onNodeWithText("Arrival").assertIsDisplayed()
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeRight() }
        rule.onNodeWithText("Inception").assertIsDisplayed()
    }

    @Test fun verticalSwipeOnHeroStillScrollsTheHomeFeed() {
        lateinit var feed: LazyListState
        rule.setContent { ReelstackTheme {
            feed = rememberLazyListState()
            LazyColumn(state = feed) {
                item { MobileLibraryFeature(sampleItem, {}, candidates = listOf(sampleItem, second), rotationEnabled = false) }
                item { Box(Modifier.height(1_200.dp)) }
            }
        } }
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeUp() }
        rule.runOnIdle { org.junit.Assert.assertTrue(feed.firstVisibleItemScrollOffset > 0 || feed.firstVisibleItemIndex > 0) }
    }

    @Test fun refreshingTheCandidateListKeepsTheChosenTitle() {
        var current by mutableStateOf(listOf(sampleItem, second))
        var opened: String? = null
        rule.setContent { ReelstackTheme {
            MobileLibraryFeature(sampleItem, { opened = it }, candidates = current, rotationEnabled = false)
        } }
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeLeft() }
        rule.runOnIdle { current = listOf(second, sampleItem) }
        rule.onNodeWithText("Arrival").assertIsDisplayed()
        rule.onNodeWithTag("mobile-feature-open").performClick()
        assertEquals(second.id, opened)
        rule.runOnIdle { current = listOf(sampleItem) }
        rule.onNodeWithText("Inception").assertIsDisplayed()
    }

    @Test fun automaticRotationFinishesAndRestartsItsDelayAfterTouch() {
        rule.mainClock.autoAdvance = false
        showCarousel(rotation = true)
        rule.mainClock.advanceTimeBy(7_000)
        rule.onNodeWithText("Inception").assertIsDisplayed()
        rule.onNodeWithTag("mobile-hero-pager").performTouchInput { swipeLeft() }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("Arrival").assertIsDisplayed()
        rule.mainClock.advanceTimeBy(1_500)
        rule.onNodeWithText("Arrival").assertIsDisplayed()
        rule.mainClock.advanceTimeBy(7_200)
        rule.onNodeWithText("Inception").assertIsDisplayed()
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun mobileHeroRendersOnPhoneWhenMediaAvailable() {
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://jellyfin.example", "token")),
            recentMovies = listOf(sampleItem),
        )
        show(state)
        rule.waitForIdle()

        rule.onNodeWithTag("mobile-library-feature").assertIsDisplayed()
        rule.onNodeWithTag("mobile-hero-title", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("mobile-feature-open").assertIsDisplayed()
    }

    @Test
    fun mobileHeroInvokesOpenCallback() {
        var openedId: String? = null
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://jellyfin.example", "token")),
            recentMovies = listOf(sampleItem),
        )
        show(state, onLibraryClick = { openedId = it })

        rule.onNodeWithTag("mobile-feature-open").performClick()
        assertEquals("test-movie-1", openedId)
    }

    @Test
    fun mobileHeroRendersCleanlyAtFontScaleTwo() {
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://jellyfin.example", "token")),
            recentMovies = listOf(sampleItem),
        )
        show(state, fontScale = 2.0f)
        rule.waitForIdle()

        rule.onNodeWithTag("mobile-library-feature").assertIsDisplayed()
        rule.onNodeWithTag("mobile-hero-title", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("mobile-feature-open").assertIsDisplayed()
    }
}
