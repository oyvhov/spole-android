package app.reelstack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.Season
import app.reelstack.data.model.SeasonalDecor
import app.reelstack.data.model.shelfId
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.AppNavigationRail
import app.reelstack.ui.AppTab
import app.reelstack.ui.components.LocalOpenSeasonShelf
import app.reelstack.ui.components.SeasonalIdleSpider
import app.reelstack.ui.components.SeasonalSpinner
import app.reelstack.ui.components.SeasonalThemeBanner
import app.reelstack.ui.components.SpoleEggs
import app.reelstack.ui.components.SpoleRewindOverlay
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** The two levels, the season's way into the library, and the things that are there to be found. */
class SeasonalTouchesTest {
    @get:Rule val rule = createComposeRule()
    private val repository get() = AppPreferencesRepository(InstrumentationRegistry.getInstrumentation().targetContext)
    private lateinit var original: Personalization

    @Before fun remember() { original = repository.personalization }
    @After fun restore() { repository.personalization = original }

    private fun halloween(decor: SeasonalDecor) {
        repository.personalization = Season.HALLOWEEN.applyTo(Personalization(seasonalDecor = decor))
    }

    @Test fun theMenuOffersTheSeasonsShelfAndOpensIt() {
        halloween(SeasonalDecor.CALM)
        var opened: String? = null
        rule.setContent { ReelstackTheme {
            AppNavigationRail(AppTab.HOME, {}, expanded = true, onLibrarySelect = { opened = it })
        } }
        rule.onNodeWithTag("wide-season-HALLOWEEN").assertHasClickAction().performClick()
        rule.runOnIdle { assertEquals(Season.HALLOWEEN.shelfId(), opened) }
    }

    @Test fun decorationsOffLeaveTheMenuAsItWas() {
        halloween(SeasonalDecor.OFF)
        rule.setContent { ReelstackTheme { AppNavigationRail(AppTab.HOME, {}, expanded = true) } }
        rule.onNodeWithTag("wide-season-HALLOWEEN").assertDoesNotExist()
        rule.onNodeWithTag("brand-season-HALLOWEEN").assertDoesNotExist()
    }

    /** On Home the greeting is a door; as the preview in Appearance it is not. */
    @Test fun theGreetingOpensTheShelfButItsPreviewDoesNot() {
        halloween(SeasonalDecor.CALM)
        var opened: Season? = null
        rule.setContent { ReelstackTheme { CompositionLocalProvider(LocalOpenSeasonShelf provides { opened = it }) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { SeasonalThemeBanner() }
        } } }
        rule.onNodeWithTag("season-banner-HALLOWEEN").assertHasClickAction().performClick()
        rule.runOnIdle { assertEquals(Season.HALLOWEEN, opened) }
    }

    @Test fun thePreviewInAppearanceIsNotADoor() {
        halloween(SeasonalDecor.OFF)
        rule.setContent { ReelstackTheme { CompositionLocalProvider(LocalOpenSeasonShelf provides { }) {
            Box(Modifier.fillMaxSize()) { SeasonalThemeBanner(preview = true) }
        } } }
        rule.onNodeWithTag("season-banner-HALLOWEEN").assertHasNoClickAction()
    }

    @Test fun theWheelCarriesThePumpkinAtCalmAndNothingWhenOff() {
        halloween(SeasonalDecor.CALM)
        rule.setContent { ReelstackTheme { SeasonalSpinner(Modifier.size(40.dp)) } }
        rule.onNodeWithTag("spinner-season-HALLOWEEN", useUnmergedTree = true).assertExists()
    }

    /** FULL only: the spider comes down once the room is quiet, and goes back up at the first press. */
    @Test fun theSpiderWaitsForQuietAndLeavesAtTheFirstPress() {
        halloween(SeasonalDecor.FULL)
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelstackTheme { Box(Modifier.size(80.dp, 400.dp)) { SeasonalIdleSpider(Modifier.fillMaxSize(), idleMillis = 1_000) } } }
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("season-idle-spider", useUnmergedTree = true).assertDoesNotExist()
        rule.mainClock.advanceTimeBy(2_500)
        rule.onNodeWithTag("season-idle-spider", useUnmergedTree = true).assertExists()
        SpoleEggs.touch()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("season-idle-spider", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun calmHasNoSpider() {
        halloween(SeasonalDecor.CALM)
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelstackTheme { Box(Modifier.size(80.dp, 400.dp)) { SeasonalIdleSpider(Modifier.fillMaxSize(), idleMillis = 100) } } }
        rule.mainClock.advanceTimeBy(6_000)
        rule.onNodeWithTag("season-idle-spider", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun theReelSpinsBackWhenAskedAndThenGoes() {
        repository.personalization = Personalization()
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelstackTheme { SpoleRewindOverlay() } }
        rule.onNodeWithTag("egg-rewind").assertDoesNotExist()
        SpoleEggs.rewind()
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithTag("egg-rewind").assertExists()
        rule.mainClock.advanceTimeBy(3_000)
        rule.onNodeWithTag("egg-rewind").assertDoesNotExist()
    }
}
