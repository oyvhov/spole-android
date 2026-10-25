package app.reelstack.ui.components

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvHeroFocusTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val film = LibraryMedia(id = "jf-1", title = "Film", subtitle = "2026", progress = .4f,
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, remoteId = "1", mediaType = "Movie")
    private val series = LibraryMedia(id = "jf-2", title = "Serie", subtitle = "2026",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, remoteId = "2", mediaType = "Series")

    private fun content(onOpen: (String) -> Unit = {}, motion: Boolean = true, fontScale: Float = 1f,
        options: app.reelstack.data.model.Personalization? = null) {
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme {
                val density = androidx.compose.ui.platform.LocalDensity.current
                CompositionLocalProvider(app.reelstack.ui.theme.LocalMotionEnabled provides motion,
                    app.reelstack.ui.theme.LocalPersonalization provides (options ?: app.reelstack.ui.theme.LocalPersonalization.current),
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale)) {
                    Row {
                        Box(Modifier.size(80.dp, 540.dp).focusable().testTag("outside-hero"))
                        TabletLibraryFeature(film, onOpen = onOpen, candidates = listOf(film, series))
                    }
                }
            }
        }
    }

    // beta11: a remote leaves focus on a hero button while the viewer only watches, so the hero
    // turns with focus on it. The play button goes for a title that cannot be played; focus has
    // to stay in the hero rather than fall to the sidebar.
    @Test fun focusStaysInTheHeroWhenTheNextTitleCannotBePlayed() {
        content()
        rule.onNodeWithTag("tablet-feature-play").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        rule.mainClock.advanceTimeBy(8_500)
        rule.waitForIdle()
        rule.onNodeWithTag("tablet-feature-play").assertDoesNotExist()
        rule.onNodeWithTag("tablet-feature-open").assertIsFocused()
    }

    @Test fun aRemotePressRestartsTheWaitBeforeTheNextTitle() {
        content()
        rule.mainClock.autoAdvance = false // performKeyInput would otherwise run the 8 s wait while idling
        rule.onNodeWithTag("tablet-feature-open").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        rule.mainClock.advanceTimeBy(7_000)
        rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionUp) }
        rule.waitForIdle() // let the press recompose before time moves on
        rule.mainClock.advanceTimeBy(2_000)
        rule.onNodeWithTag("tablet-feature-play").assertExists()
        rule.mainClock.advanceTimeBy(6_500)
        rule.waitForIdle()
        rule.onNodeWithTag("tablet-feature-play").assertDoesNotExist()
        rule.onNodeWithTag("tablet-feature-open").assertIsFocused()
    }

    @Test fun reducedMotionKeepsTheTelevisionHeroTurning() {
        content(motion = false)
        rule.onNodeWithTag("outside-hero").requestFocus()
        rule.mainClock.advanceTimeBy(8_500)
        rule.waitForIdle()
        rule.onNodeWithTag("tablet-feature-play").assertDoesNotExist()
    }

    @Test fun lightweightTelevisionModeStopsAutomaticRotation() {
        content(options = app.reelstack.data.model.Personalization(lightweightTv = true))
        rule.onNodeWithTag("tablet-feature-open").requestFocus()
        rule.mainClock.advanceTimeBy(17_000)
        rule.onNodeWithTag("tablet-feature-play").assertExists()
    }

    @Test fun theFocusedActionRemainsVisibleAndOpensTheTitleShownWithDoubleSizeText() {
        var opened: String? = null
        content(onOpen = { opened = it }, fontScale = 2f)
        rule.onNodeWithTag("tablet-feature-open").requestFocus()
        rule.mainClock.advanceTimeBy(8_500)
        rule.onNodeWithTag("tablet-feature-open").assertIsDisplayed().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(series.id, opened)
    }
}
