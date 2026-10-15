package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.MobileLibraryFeature
import app.reelstack.ui.appScaffoldPadding
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MobileHeroGestureTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
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

    @Test fun headerUsesTheActualSystemSafeAreaOnlyOnce() {
        rule.runOnUiThread { WindowCompat.setDecorFitsSystemWindows(rule.activity.window, false) }
        var safeTop = 0.dp
        rule.setContent { ReelstackTheme {
            val density = LocalDensity.current
            safeTop = with(density) { WindowInsets.safeDrawing.getTop(this).toDp() }
            androidx.compose.material3.Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                Box(Modifier.fillMaxSize().appScaffoldPadding(padding)) {
                    LazyColumn {
                        item { MobileLibraryFeature(first, {}, rotationEnabled = false,
                            header = { Text("Spole", Modifier.testTag("fixture-header")) }) }
                    }
                }
            }
        } }
        val header = rule.onNodeWithTag("fixture-header", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Must test a real status bar", safeTop > 0.dp)
        assertEquals((safeTop + 8.dp).value, header.top.value, 1f)
        rule.onNodeWithTag("mobile-feature-open").assertIsDisplayed()
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
        val heading = currentCaptionNode(hasTestTag("mobile-hero-title") and hasText(first.title))
        heading.assertIsDisplayed()
        rule.onNodeWithTag("mobile-feature-play").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("mobile-feature-open").performScrollTo().assertIsDisplayed()
        val header = rule.onNodeWithTag("fixture-header").getUnclippedBoundsInRoot()
        val title = heading.getUnclippedBoundsInRoot()
        assertTrue(title.top > header.bottom)
    }

    @Test fun compactCaptionHasNaturalGapsAndSteadyActions() = checkCaption(1f)
    @Test fun doubleTextCaptionHasNaturalGapsAndSteadyActions() = checkCaption(2f)

    private fun checkCaption(fontScale: Float) {
        val facts = listOf("Episode", "2005", "28 min", "A deliberately long certification label that overflows the metadata row")
        val episode = first.copy(title = "Serie", subtitle = "S01 E02 · Svartisen", mediaType = "Episode", season = 1,
            episode = 2, facts = emptyList())
        val long = episode.copy(id = "long", title = "Ein lang serietittel som treng meir enn ei tekstlinje", facts = facts)
        val film = episode.copy(id = "film", title = "Film", subtitle = "2005", mediaType = "Movie", season = null,
            episode = null, facts = facts.map { if (it == "Episode") "Film" else it })
        val candidates = androidx.compose.runtime.mutableStateOf(listOf(episode, long, film))
        var opened: String? = null
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(androidx.compose.ui.unit.DpSize(400.dp, 900.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                    ReelstackTheme { CompositionLocalProvider(LocalMotionEnabled provides false) {
                        LazyColumn { item {
                            MobileLibraryFeature(candidates.value.first(), { opened = it }, candidates = candidates.value,
                                rotationEnabled = false)
                        } }
                    } }
                }
            }
        }
        val before = rule.onNodeWithTag("mobile-feature-open").getUnclippedBoundsInRoot()
        rule.runOnIdle { candidates.value = candidates.value.map { if (it.id == episode.id) it.copy(facts = facts) else it } }
        assertEquals(before, rule.onNodeWithTag("mobile-feature-open").getUnclippedBoundsInRoot())
        rule.onAllNodesWithText(episode.title).assertCountEquals(1)
        rule.onAllNodesWithText(long.title).assertCountEquals(0)
        rule.onAllNodes(inCurrentCaption(hasTestTag("hero-metadata")), useUnmergedTree = true).assertCountEquals(1)
        assertMobileTextGap("mobile-hero-title", "mobile-hero-episode-info")
        assertMobileTextGap("mobile-hero-episode-info", "hero-metadata")
        val metadata = currentCaptionNode(hasTestTag("hero-metadata"))
        val scrollKey = androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange
        val scrollRange = metadata.fetchSemanticsNode().config[scrollKey]
        val initialScroll = scrollRange.value()
        val maxScroll = scrollRange.maxValue()
        assertTrue("The visible metadata row has horizontal overflow", maxScroll > 0f)
        val availableScroll = maxScroll - initialScroll
        assertTrue("The visible metadata row has unused horizontal overflow", availableScroll > 0f)
        metadata.performTouchInput {
            // Stay within the row's overflow so the gesture cannot hand off to the pager.
            val distance = minOf(availableScroll / 5f, width / 4f)
            swipe(start = center, end = center - Offset(distance, 0f), durationMillis = 1_200)
        }
        assertTrue("The visible metadata row receives the drag", metadata.fetchSemanticsNode().config[scrollKey].value() > initialScroll)
        rule.onAllNodesWithText(episode.title).assertCountEquals(1)
        rule.onNodeWithTag("mobile-hero-dot-1").performClick()
        assertMobileTextGap("mobile-hero-title", "mobile-hero-episode-info")
        assertMobileTextGap("mobile-hero-episode-info", "hero-metadata")
        assertEquals(before, rule.onNodeWithTag("mobile-feature-open").getUnclippedBoundsInRoot())
        rule.onNodeWithTag("mobile-hero-dot-2").performClick()
        currentCaptionNode(hasTestTag("mobile-hero-episode-info")).assertDoesNotExist()
        assertMobileTextGap("mobile-hero-title", "hero-metadata")
        assertEquals(before, rule.onNodeWithTag("mobile-feature-open").getUnclippedBoundsInRoot())
        rule.onNodeWithTag("mobile-feature-open").performClick()
        assertEquals(film.id, opened)
    }

    private fun inCurrentCaption(matcher: SemanticsMatcher) = matcher and
        hasAnyAncestor(hasTestTag("mobile-feature-current-page")) and
        hasAnyAncestor(hasTestTag("mobile-feature-current-caption"))

    private fun currentCaptionNode(matcher: SemanticsMatcher) =
        rule.onNode(inCurrentCaption(matcher), useUnmergedTree = true)

    private fun assertMobileTextGap(fromTag: String, toTag: String) {
        val text = currentCaptionNode(hasTestTag(fromTag))
        val bounds = text.getUnclippedBoundsInRoot()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        text.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val lastLineBottom = with(layout.layoutInput.density) { layout.getLineBottom(layout.lineCount - 1).toDp() }
        val next = currentCaptionNode(hasTestTag(toTag)).getUnclippedBoundsInRoot()
        val gap = next.top - bounds.top - lastLineBottom
        assertTrue("Caption has no reserved blank line between $fromTag and $toTag: $gap", gap <= 7.dp)
        assertTrue("Caption lines do not overlap: $gap", gap >= 0.dp)
    }
}
