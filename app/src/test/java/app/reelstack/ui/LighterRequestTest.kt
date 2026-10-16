package app.reelstack.ui

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.RequestDraft
import app.reelstack.data.model.RequestFolder
import app.reelstack.data.model.RequestOptions
import app.reelstack.data.model.RequestProfile
import app.reelstack.data.model.RequestRules
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "c", userId = "7", sessionCookie = true)
private val account = ServiceAccount(ServiceKind.SEERR, "7", "Øyvind", permissions = 8224)
private val film = DiscoverMedia("seerr-movie-11", "Masterplan", "2026", R.drawable.media_placeholder, false, remoteId = 11,
    mediaType = "movie", seerrStatus = 1)
private val options = RequestOptions(0, false, listOf(RequestProfile(4, "HD-1080p"), RequestProfile(5, "Ultra-HD")),
    listOf(RequestFolder("/mnt/w/Filmar 10", 4_815_000_000_000)), defaultProfileId = 4, defaultFolder = "/mnt/w/Filmar 10")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h900dp-xhdpi", application = app.reelstack.SheetTestApplication::class)
class PhoneTitleRequestFooterTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun askingForAFilmStaysAtTheBottomLikePlay() {
        val asked = mutableListOf<String>()
        rule.setContent {
            ReelstackTheme {
                ReelstackSheets(ReelstackUiState(connections = listOf(seerr), accounts = mapOf(ServiceKind.SEERR to account),
                    discover = listOf(film), activeSheet = AppSheet.TitleDetails(film.id),
                    contentDetails = ContentDetails(film.id, film.title, "Seerr", "Film · 2026", artworkRes = R.drawable.media_placeholder,
                        source = ServiceKind.SEERR, mediaType = "Movie", overview = "Lang omtale. ".repeat(80))),
                    null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { asked += it }, {})
            }
        }
        val footer = rule.onNodeWithTag("title-request-footer").assertIsDisplayed()
        val sheet = rule.onNodeWithTag("adaptive-dialog").fetchSemanticsNode().boundsInRoot
        assertTrue("footer is not at the bottom", footer.fetchSemanticsNode().boundsInRoot.bottom > sheet.bottom - 200f)
        // The same action is not also somewhere in the page above it.
        rule.onNodeWithTag("title-request").assertDoesNotExist()
        footer.performClick()
        rule.runOnIdle { assertEquals(listOf(film.id), asked) }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvLightFilmRequestTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun aFilmRequestFitsWithoutScrollingAndStartsOnSend() {
        // The size the request dialog has on a 960 × 540 television: 600 wide, 420 high.
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.size(600.dp, 420.dp).testTag("sheet")) {
                    RequestComposer(ReelstackUiState(connections = listOf(seerr), accounts = mapOf(ServiceKind.SEERR to account),
                        requestDraft = RequestDraft(film, loading = false, options = options, profileId = 4, folder = "/mnt/w/Filmar 10",
                            rules = RequestRules(canRequest = true, approvalRequired = false, quota = null))),
                        { _, _ -> }, {}, {}, {}, {}, {})
                }
            }
        }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("confirm-request").assertIsFocused() }.isSuccess }
        val sheet = rule.onNodeWithTag("sheet").fetchSemanticsNode().boundsInRoot
        listOf("request-title", "request-quality", "request-folder", "request-notification", "confirm-request").forEach { tag ->
            val bounds = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("$tag is outside the sheet: $bounds in $sheet", bounds.top >= sheet.top && bounds.bottom <= sheet.bottom + 0.5f)
        }
        // No explanation of the steps and no long introduction: a film is one decision.
        rule.onNodeWithTag("request-journey").assertDoesNotExist()
    }
}
