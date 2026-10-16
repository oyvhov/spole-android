package app.reelstack.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvHeroEntryFocusTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun media(id: String, title: String) = LibraryMedia(id = id, title = title, subtitle = "2026", progress = .4f,
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, remoteId = id, mediaType = "Movie",
        facts = listOf("2026", "99 min"), tmdbRating = 7.1f)

    @Test fun comingBackFromTheMenuLandsOnAVisibleTarget() {
        lateinit var rail: FocusRequester
        lateinit var content: FocusRequester
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            rail = remember { FocusRequester() }
            content = remember { FocusRequester() }
            ReelstackTheme {
                Row {
                    Box(Modifier.size(80.dp).focusRequester(rail).focusable().testTag("rail"))
                    Box(Modifier.size(860.dp, 540.dp).focusRequester(content).focusRestorer().focusGroup()) {
                        androidx.compose.foundation.lazy.LazyColumn {
                            item(key = "tablet-feature") {
                                TabletLibraryFeature(media("1", "Ein"), onOpen = {}, candidates = listOf(media("1", "Ein"), media("2", "To")),
                                    account = { Box(Modifier.size(40.dp).focusable().testTag("account")) })
                            }
                            item(key = "row") {
                                androidx.compose.foundation.lazy.LazyRow {
                                    items(6) { Box(Modifier.size(120.dp, 180.dp).focusable().testTag("poster-$it")) }
                                }
                            }
                        }
                    }
                }
            }
        }
        // The page is entered the way the shell does it: focus requested on the content as a whole.
        rule.runOnIdle { content.requestFocus() }
        rule.waitForIdle()
        // It lands on something the reader can see. The line of facts can scroll for a finger, but
        // it must never hold the remote: it has no ring, and when the hero turns it stops being
        // focusable, which threw focus to the sidebar on a television.
        rule.onNode(isFocused()).assertExists()
        rule.mainClock.advanceTimeBy(8_500)
        rule.waitForIdle()
        rule.onNode(isFocused()).assertExists()
    }
}
