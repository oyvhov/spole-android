package app.reelstack.ui.kids

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class KidsLauncherDetailsTest {
    @get:Rule val rule = createComposeRule()
    private val details = ContentDetails("abc", "Kystvakta", "", "", artworkRes = R.drawable.media_placeholder,
        remoteId = "abc", source = ServiceKind.JELLYFIN, mediaType = "Movie", overview = "På havet", libraryAvailable = true)

    @Test fun childMovieHasOnlyPlayAndCloseActions() {
        var plays = 0
        rule.setContent { ReelstackTheme { KidsLauncherDetailsContent(details, { plays++ }, {}, {}) } }
        rule.onNodeWithText("Kystvakta").assertIsDisplayed()
        rule.onNodeWithText("På havet").assertIsDisplayed()
        rule.onNodeWithText("Play").performClick()
        rule.onAllNodes(hasClickAction()).assertCountEquals(2)
        assertEquals(1, plays)
    }

    @Test fun childSeriesOpensExistingEpisodeBrowser() {
        var opens = 0
        rule.setContent { ReelstackTheme { KidsLauncherDetailsContent(details.copy(mediaType = "Series"), {}, { opens++ }, {}) } }
        rule.onNodeWithText("Show episodes").performClick()
        rule.onNodeWithText("Play").assertDoesNotExist()
        assertEquals(1, opens)
    }

    @Test fun skeletonDoesNotOfferPlayback() {
        rule.setContent { ReelstackTheme { KidsLauncherDetailsContent(details.copy(title = "", loading = true), {}, {}, {}) } }
        rule.onNodeWithContentDescription("Loading more details").assertExists()
        rule.onNodeWithText("Play").assertDoesNotExist()
    }

    @Test fun failureHasNoPlaybackAction() {
        rule.setContent { ReelstackTheme { KidsLauncherDetailsContent(details.copy(error = "Kunne ikkje hente"), {}, {}, {}) } }
        rule.onNodeWithText("Kunne ikkje hente").assertIsDisplayed()
        rule.onNodeWithText("Play").assertDoesNotExist()
    }

    @Test fun childControlsMeet64DpMinimum() {
        rule.setContent { ReelstackTheme { KidsLauncherDetailsContent(details, {}, {}, {}) } }
        rule.onNodeWithText("Play").assertHeightIsAtLeast(64.dp)
        rule.onNodeWithText("Close").assertHeightIsAtLeast(64.dp)
    }

    @Test fun largeTextKeepsActionsReachable() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                ReelstackTheme {
                    Box(Modifier.size(320.dp, 420.dp)) {
                        KidsLauncherDetailsContent(details.copy(title = "Kystvakta og livet på havet", overview = "På havet. ".repeat(20)), {}, {}, {})
                    }
                }
            }
        }
        rule.onNodeWithText("Kystvakta og livet på havet").assertIsDisplayed()
        rule.onNodeWithText("Play").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Close").performScrollTo().assertIsDisplayed()
    }
}
