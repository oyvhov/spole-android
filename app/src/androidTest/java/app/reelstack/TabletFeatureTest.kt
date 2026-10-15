package app.reelstack

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.*
import app.reelstack.ui.components.TabletLibraryFeature
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TabletFeatureTest {
    @get:Rule val rule = createComposeRule()
    private val media = LibraryMedia("fixture-episode", "A long series title", "S01 E02 · An episode",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN,
        overview = "A detailed description. ".repeat(30))

    @Test fun carouselCrossfadesWithoutMovingActionAndOpensDisplayedTitle() {
        var opened: String? = null
        val next = media.copy(id = "second", title = "Second series", overview = "Short description")
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelstackTheme { TabletLibraryFeature(media, { opened = it }, candidates = listOf(media, next)) } }
        rule.mainClock.advanceTimeBy(1_000)
        val before = rule.onNodeWithTag("tablet-feature-open").fetchSemanticsNode().boundsInRoot
        rule.mainClock.advanceTimeBy(9_000)
        rule.onNodeWithText(next.title).assertIsDisplayed()
        val after = rule.onNodeWithTag("tablet-feature-open").fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
        rule.onNodeWithText("Jellyfin").assertDoesNotExist()
        rule.onNodeWithText("Frå Jellyfin-biblioteket ditt").assertDoesNotExist()
        rule.onNodeWithTag("tablet-feature-open").performClick()
        assertEquals(next.id, opened)
    }

    @Test fun carouselPausesWhileRemoteFocusIsOnTheAction() {
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelstackTheme {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            androidx.compose.runtime.SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            TabletLibraryFeature(media, {}, candidates = listOf(media, media.copy(id = "second", title = "Second series")))
        } }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("tablet-feature-open").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
        rule.mainClock.advanceTimeBy(20_000)
        rule.onNodeWithText(media.title).assertIsDisplayed()
        rule.onNodeWithText("Second series").assertDoesNotExist()
    }

    @Test fun featureOpensExactLibraryItemWithoutPlayback() {
        var opened: String? = null
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1100.dp, 800.dp))) {
                ReelstackTheme { TabletLibraryFeature(media, { opened = it }) }
            }
        }
        rule.onNodeWithText(media.title).assertIsDisplayed()
        rule.onNodeWithTag("tablet-feature-open").assertIsDisplayed().performClick()
        assertEquals(media.id, opened)
    }
    @Test fun largeTextGrowsTheFeatureAndKeepsActionVisible() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1000.dp, 1200.dp))) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    ReelstackTheme { TabletLibraryFeature(media, {}) }
                }
            }
        }
        rule.onNodeWithTag("tablet-feature-open").assertIsDisplayed()
        rule.onNodeWithText(media.title).assertIsDisplayed()
    }
    @Test fun landscapeDoubleTextKeepsBothHeroActionsReadableAcrossMixedCaptions() {
        var opened: String? = null
        var playLabel = ""
        var moreLabel = ""
        val episode = media.copy(title = "Serie", remoteId = "episode", mediaType = "Episode", season = 1, episode = 2,
            facts = listOf("Episode", "2025", "74 min"), overview = "")
        val film = episode.copy(id = "movie", title = "A film", subtitle = "2025", mediaType = "Movie",
            season = null, episode = null, overview = "A detailed description. ".repeat(30))
        val fixtures = listOf(episode, film,
            episode.copy(id = "long-episode", title = "Ein lang serietittel som skal krevje begge tittellinjene",
                subtitle = "S01 E02 · A long episode title that occupies both caption lines",
                overview = "A detailed description. ".repeat(30)),
            film.copy(id = "short-movie", title = "Short", overview = "Short description"),
            episode.copy(id = "another-episode", subtitle = "S01 E02 · Another episode"))
        val candidates = androidx.compose.runtime.mutableStateOf(fixtures)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    ReelstackTheme {
                        playLabel = stringResource(R.string.player_play)
                        moreLabel = stringResource(R.string.feature_more)
                        Row {
                            Spacer(Modifier.width(200.dp)) // The expanded tablet navigation rail.
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                                TabletLibraryFeature(candidates.value.first(), { opened = it }, candidates = candidates.value)
                            }
                        }
                    }
                }
            }
        }
        rule.mainClock.advanceTimeBy(1_000)
        fun assertReadableAction(tag: String, label: String) {
            val bounds = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("The actual action must retain its minimum height: $bounds", bounds.bottom - bounds.top >= 40.dp)
            val text = rule.onNode(hasText(label) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)
            text.assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            text.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("Action text must have visible height", layouts.single().size.height > 0)
            assertFalse("Action text must not be clipped vertically", layouts.single().didOverflowHeight)
            val icon = rule.onNodeWithTag("$tag-icon", useUnmergedTree = true).assertIsDisplayed()
                .getUnclippedBoundsInRoot()
            assertTrue("Action icon must retain its visible height: $icon", icon.bottom - icon.top >= 16.dp)
        }
        assertReadableAction("tablet-feature-play", playLabel)
        assertReadableAction("tablet-feature-open", moreLabel)
        assertNaturalTextGap("hero-title", "hero-episode-number", 10.dp)
        assertNaturalTextGap("hero-episode-info", "hero-metadata", 10.dp)
        val before = rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot()
        // Other candidates already reserve the longer description, so late metadata is steady.
        rule.runOnIdle { candidates.value = fixtures.map { if (it.id == episode.id) it.copy(overview = film.overview) else it } }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(before, rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot())
        rule.mainClock.advanceTimeBy(8_240)
        assertReadableAction("tablet-feature-play", playLabel)
        assertReadableAction("tablet-feature-open", moreLabel)
        assertEquals(before, rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot())
        rule.onNodeWithTag("tablet-feature-open").performClick()
        assertEquals(film.id, opened)
        currentCaptionNode("hero-episode-number").assertDoesNotExist()
        currentCaptionNode("hero-episode-info").assertDoesNotExist()
        assertNaturalTextGap("hero-title", "hero-metadata", 10.dp)
        rule.mainClock.advanceTimeBy(8_240)
        assertNaturalTextGap("hero-title", "hero-episode-number", 10.dp)
        assertNaturalTextGap("hero-episode-info", "hero-metadata", 10.dp)
        val headingLayouts = mutableListOf<TextLayoutResult>()
        currentCaptionNode("hero-title")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(headingLayouts) }
        assertEquals("The long series name must exercise both title lines", 2, headingLayouts.single().lineCount)
        assertEquals(before, rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot())
    }

    @Test fun normalTextCaptionHasNoReservedBlankLines() {
        val episode = media.copy(title = "Serie", mediaType = "Episode", season = 1, episode = 2,
            subtitle = "S01 E02 · Svartisen", facts = listOf("Episode", "2005", "28 min"))
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))) {
                ReelstackTheme { TabletLibraryFeature(episode, {}, rotationEnabled = false) }
            }
        }
        assertNaturalTextGap("hero-title", "hero-episode-number", 10.dp)
        assertNaturalTextGap("hero-episode-info", "hero-metadata", 10.dp)
    }

    @Test fun singleDoubleTextCandidateKeepsActionsSteadyWhenEpisodeAndOverviewGrow() {
        val episode = androidx.compose.runtime.mutableStateOf(media.copy(title = "Serie", mediaType = "Episode",
            season = 1, episode = 2, subtitle = "S01 E02 · Svartisen", overview = "Ei kort omtale.", facts = emptyList()))
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    ReelstackTheme { TabletLibraryFeature(episode.value, {}, rotationEnabled = false) }
                }
            }
        }
        val before = rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot()
        rule.runOnIdle {
            episode.value = episode.value.copy(subtitle = "S01 E02 · Ein mykje lengre episodetittel som no treng to heile tekstlinjer",
                overview = "Ein lang omtale som blir lasta inn etter at heroen alt er synleg. ".repeat(10),
                facts = listOf("Episode", "2005", "28 min"), tmdbRating = 72f)
        }
        assertNaturalTextGap("hero-title", "hero-episode-number", 10.dp)
        assertNaturalTextGap("hero-episode-info", "hero-metadata", 10.dp)
        assertEquals(before, rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot())
    }

    @Test fun lateWideClearLogoAndItsErrorFallbackKeepCaptionAndActionsSteady() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val logo = java.io.File.createTempFile("hero-caption-", ".png", context.cacheDir)
        val bitmap = android.graphics.Bitmap.createBitmap(440, 40, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        logo.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        try {
            val episode = androidx.compose.runtime.mutableStateOf(media.copy(title = "Serie", mediaType = "Episode",
                season = 1, episode = 2, subtitle = "S01 E02 · Svartisen", facts = listOf("Episode", "2005", "28 min")))
            rule.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                        ReelstackTheme { TabletLibraryFeature(episode.value, {}, rotationEnabled = false) }
                    }
                }
            }
            val before = rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot()
            rule.runOnIdle { episode.value = episode.value.copy(logoUrl = logo.toURI().toString()) }
            rule.waitUntil(5_000) {
                runCatching {
                    val bounds = currentCaptionNode("hero-clearlogo").getUnclippedBoundsInRoot()
                    bounds.bottom - bounds.top < 24.dp
                }.getOrDefault(false)
            }
            val art = currentCaptionNode("hero-clearlogo").getUnclippedBoundsInRoot()
            val numberNode = currentCaptionNode("hero-episode-number")
            val number = numberNode.getUnclippedBoundsInRoot()
            val numberLayouts = mutableListOf<TextLayoutResult>()
            numberNode.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(numberLayouts) }
            with(numberLayouts.single().layoutInput.density) {
                // The forced window can have a fractional density: compare the exact rounded
                // pixel spacing used by HeroCaptionGroup instead of assuming 10 dp is integral.
                assertEquals(10.dp.roundToPx().toFloat(), (number.top - art.bottom).toPx(), .01f)
            }
            assertNaturalTextGap("hero-episode-info", "hero-metadata", 10.dp)
            assertEquals(before, rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot())
            rule.runOnIdle { episode.value = episode.value.copy(logoUrl = "file://${logo.absolutePath}.missing") }
            rule.waitUntil(5_000) { runCatching { currentCaptionNode("hero-title").fetchSemanticsNode() }.isSuccess }
            assertNaturalTextGap("hero-title", "hero-episode-number", 10.dp)
            assertEquals(before, rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot())
        } finally { logo.delete() }
    }

    private fun currentCaptionNode(tag: String) = rule.onNode(
        hasTestTag(tag) and hasAnyAncestor(hasTestTag("feature-current-caption")), useUnmergedTree = true,
    )

    private fun assertNaturalTextGap(fromTag: String, toTag: String, spacing: Dp) {
        val text = currentCaptionNode(fromTag)
        val bounds = text.getUnclippedBoundsInRoot()
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val lastLineBottom = with(layout.layoutInput.density) { layout.getLineBottom(layout.lineCount - 1).toDp() }
        val next = currentCaptionNode(toTag).getUnclippedBoundsInRoot()
        val gap = next.top - bounds.top - lastLineBottom
        assertTrue("No blank reserved line between $fromTag and $toTag: $gap", gap <= spacing + 1.dp)
        assertTrue("Caption lines must not overlap: $gap", gap >= 0.dp)
    }
    @Test fun accountOverlaysArtworkWithoutAddingAHeaderOrOpeningTitle() {
        var accountOpened = false
        var titleOpened = false
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(760.dp, 540.dp))) {
                ReelstackTheme { TabletLibraryFeature(media, { titleOpened = true }, account = {
                    Button({ accountOpened = true }, Modifier.size(48.dp).testTag("profile")) { Text("P") }
                }) }
            }
        }
        val feature = rule.onNodeWithTag("tablet-library-feature").fetchSemanticsNode().boundsInRoot
        val overlay = rule.onNodeWithTag("feature-account-overlay", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithText(media.title).fetchSemanticsNode().boundsInRoot
        assertTrue(overlay.top >= feature.top && overlay.bottom < feature.bottom)
        assertTrue(overlay.left >= title.left)
        rule.onNodeWithTag("profile").assertIsDisplayed().performClick()
        assertTrue(accountOpened)
        assertFalse(titleOpened)
    }
}
