package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.DiscoverHubState
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.DiscoverPerson
import app.reelstack.data.model.DiscoverRowKind
import app.reelstack.data.model.DiscoverRowState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
import app.reelstack.ui.state.DiscoverUiState
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h900dp-xhdpi", application = app.reelstack.SheetTestApplication::class)
class DiscoverHubScreenTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun media(id: Int) = DiscoverMedia("seerr-movie-$id", "Tittel $id", "2026", R.drawable.media_placeholder, false,
        remoteId = id, mediaType = "movie", artworkUrl = "https://image.tmdb.org/t/p/w500/$id.jpg")

    private fun hubState(failed: Boolean = false) = DiscoverUiState(
        connections = listOf(ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "c", userId = "7", sessionCookie = true)),
        hub = DiscoverHubState(rows = mapOf(
            DiscoverRowKind.TRENDING to DiscoverRowState(listOf(media(1), media(2))),
            DiscoverRowKind.UPCOMING_MOVIES to DiscoverRowState(failed = failed),
            DiscoverRowKind.UPCOMING_SERIES to DiscoverRowState(listOf(media(3))),
        ), people = listOf(DiscoverPerson(9, "Margaret Qualley", null))),
        trackedRequests = listOf(TrackedRequest("r", 4, "movie", "Coraline", "https://image.tmdb.org/t/p/w500/r.jpg", emptySet())),
    )

    @Test fun postersCarryNoTextOrButtons() {
        rule.setContent { ReelstackTheme { DiscoverHubContent(hubState(), PaddingValues(), actions()) } }
        rule.onNodeWithTag("discover-poster-seerr-movie-1").assertIsDisplayed()
        rule.onAllNodes(hasAnyAncestor(hasTestTag("discover-poster-seerr-movie-1")) and hasText("Tittel 1")).assertCountEquals(0)
        rule.onAllNodes(hasAnyAncestor(hasTestTag("discover-poster-seerr-movie-1")) and hasClickAction()).assertCountEquals(0)
    }

    @Test fun aFailedRowIsGoneAndSaysSoOnce() {
        rule.setContent { ReelstackTheme { DiscoverHubContent(hubState(failed = true), PaddingValues(), actions()) } }
        rule.onNodeWithTag("discover-row-UPCOMING_MOVIES").assertDoesNotExist()
        rule.onNodeWithTag("discover-rows-retry").performScrollTo().assertIsDisplayed()
    }

    @Test fun yourRequestsComeFirstAsLines() {
        val opened = mutableListOf<String>()
        rule.setContent { ReelstackTheme { DiscoverHubContent(hubState(), PaddingValues(), actions(onLine = { opened += it })) } }
        rule.onNodeWithTag("my-request-r").performClick()
        assertEquals(listOf("r"), opened)
    }

    @Test fun withoutSeerrThePageSaysSo() {
        var message = ""
        rule.setContent {
            ReelstackTheme {
                message = androidx.compose.ui.res.stringResource(R.string.search_not_connected)
                DiscoverHubContent(DiscoverUiState(), PaddingValues(), actions())
            }
        }
        rule.onNodeWithText(message).assertIsDisplayed()
    }

    private fun actions(onLine: (String) -> Unit = {}) = DiscoverHubActions({}, {}, {}, onLine, {}, {}, {})
}
