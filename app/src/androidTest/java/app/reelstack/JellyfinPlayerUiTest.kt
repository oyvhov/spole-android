package app.reelstack

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.toPixelMap
import app.reelstack.player.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class JellyfinPlayerUiTest {
    @get:Rule val rule = createComposeRule()
    private fun screen(state: PlayerScreenState, scale: Float = 1f, close: () -> Unit = {}, subtitle: (Int) -> Unit = {}, choose: (PlayableItem) -> Unit = {}) {
        rule.setContent { val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ReelstackTheme { PlayerScreen(state, null, close, {}, {}, {}, choose, {}, {}, subtitle, {}, {}) }
            }
        }
    }
    @Test fun pictureCanFillTheScreenAndReturnToUncroppedFit() {
        screen(PlayerScreenState(busy=false,durationMs=20000))
        rule.onNodeWithText("Fyll skjermen").performScrollTo().performClick()
        rule.onNodeWithText("Heile biletet").assertIsDisplayed().performClick()
        rule.onNodeWithText("Fyll skjermen").assertIsDisplayed()
        rule.onNodeWithTag("player-close").assertIsDisplayed()
    }
    @Test fun closeIsAvailableWhileVideoLoads() {
        var closed=false
        screen(PlayerScreenState(),close={closed=true})
        rule.onNodeWithTag("player-close").assertIsDisplayed().performClick()
        assertTrue(closed)
        rule.onNodeWithTag("player-toggle").assertIsNotEnabled()
    }
    @Test fun hiddenControlsAppearOnTouchDownWithoutWaitingForFingerRelease() {
        rule.mainClock.autoAdvance = false
        screen(PlayerScreenState(busy=false,playing=true,durationMs=20000))
        rule.mainClock.advanceTimeBy(4_000)
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
        rule.onNodeWithTag("player-close").assertDoesNotExist()
        rule.onNodeWithTag("player-touch-surface").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(width * .75f, height * .4f))
        }
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithTag("player-toggle").assertExists()
        rule.onNodeWithTag("player-touch-surface").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(120)
        rule.onNodeWithTag("player-toggle").assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithTag("player-close").assertIsDisplayed()
        rule.mainClock.autoAdvance = true
    }
    @Test fun backStaysPinnedAfterScrollingLargeTextControls() {
        var closed = false
        screen(PlayerScreenState(title="Ein lang filmtittel med fleire ord",busy=false,durationMs=20000),
            scale=2f, close={ closed=true })
        val before = rule.onNodeWithTag("player-close").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText("Kvalitet",substring=true).performScrollTo()
        val after = rule.onNodeWithTag("player-close").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(before,after)
        rule.onNodeWithTag("player-close").performTouchInput { click() }
        assertTrue(closed)
    }
    @Test fun textChoiceAndOffAreExplicit() {
        var selected=99
        screen(PlayerScreenState(busy=false,subtitles=listOf(PlaybackTrack(2,"Norsk", "nor",true)),subtitleIndex=2),subtitle={selected=it})
        rule.onNodeWithText("Tekst",substring=true).performScrollTo().performClick()
        rule.onNodeWithText("Undertekstar").assertIsDisplayed()
        rule.onNodeWithText("Av",substring=false).performClick()
        assertEquals(-1,selected)
    }
    @Test fun largeTextKeepsCloseAndQualityReachable() {
        screen(PlayerScreenState(title="Ein lang filmtittel som framleis skal vere lesbar",busy=false,durationMs=20000),scale=2f)
        rule.onNodeWithTag("player-close").assertIsDisplayed()
        rule.onNodeWithText("Kvalitet",substring=true).performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("Automatisk").assertIsDisplayed()
    }
    @Test fun episodesAreExplicitAndShowPersonalResume() {
        var selected=""
        val episode=PlayableItem("ep","Testserie","Episode","S01 E02 · Ny dag",resumeMs=5000)
        screen(PlayerScreenState(busy=false,browsing=true,choices=listOf(episode)),choose={selected=it.id})
        rule.onNodeWithText("Hald fram frå 0:05").assertIsDisplayed()
        rule.onNodeWithText(episode.subtitle).performClick()
        assertEquals("ep",selected)
    }
    @Test fun errorsHaveRetryAndExternalExit() {
        screen(PlayerScreenState(busy=false,error="Fekk ikkje starta videoen."))
        rule.onNodeWithText("Prøv igjen").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Opne i Jellyfin").performScrollTo().assertIsDisplayed()
    }
    @Test fun playerTitleIsLightOnTheBlackVideoSurface() {
        screen(PlayerScreenState(title="Spole testtittel",busy=false))
        val pixels=rule.onNodeWithText("Spole testtittel").captureToImage().toPixelMap()
        var brightPixels=0
        for(y in 0 until pixels.height) for(x in 0 until pixels.width) {
            val color=pixels[x,y]
            if(color.red>.8f && color.green>.8f && color.blue>.8f) brightPixels++
        }
        assertTrue("Player title must not inherit black text",brightPixels>20)
    }
}
