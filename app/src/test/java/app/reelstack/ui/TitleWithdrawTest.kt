package app.reelstack.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.RequestStage
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
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
class TitleWithdrawTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun withdrawingAsksFirstAndKeepingSendsNothing() {
        val media = DiscoverMedia("seerr-movie-11", "Coraline", "2009", R.drawable.media_placeholder, false, remoteId = 11,
            mediaType = "movie", seerrStatus = 2, requested = true)
        val withdrawn = mutableListOf<String>()
        rule.setContent {
            ReelstackTheme {
                ReelstackSheets(ReelstackUiState(discover = listOf(media), activeSheet = AppSheet.TitleDetails(media.id),
                    trackedRequests = listOf(TrackedRequest("movie:11:", 11, "movie", "Coraline", null, emptySet(), requestId = 5,
                        stage = RequestStage.REQUESTED)),
                    contentDetails = ContentDetails(media.id, media.title, "Seerr", "Film · 2009", artworkRes = R.drawable.media_placeholder,
                        source = ServiceKind.SEERR, mediaType = "Movie")),
                    null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, onWithdraw = { withdrawn += it })
            }
        }
        rule.onNodeWithTag("title-withdraw").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(emptyList<String>(), withdrawn) }
        rule.onNodeWithTag("withdraw-keep").performClick()
        rule.runOnIdle { assertEquals(emptyList<String>(), withdrawn) }
        rule.onNodeWithTag("title-withdraw").performScrollTo().performClick()
        rule.onNodeWithTag("withdraw-confirm").performClick()
        rule.runOnIdle { assertEquals(listOf("movie:11:"), withdrawn) }
    }

    @Test fun twoRequestsForOneSeriesEachWithdrawTheirOwn() {
        val media = DiscoverMedia("seerr-tv-12", "Severance", "2022", R.drawable.media_placeholder, false, remoteId = 12,
            mediaType = "tv", seerrStatus = 2, requested = true)
        val withdrawn = mutableListOf<String>()
        rule.setContent {
            ReelstackTheme {
                ReelstackSheets(ReelstackUiState(discover = listOf(media), activeSheet = AppSheet.TitleDetails(media.id),
                    trackedRequests = listOf(
                        TrackedRequest("tv:12:1", 12, "tv", "Severance", null, setOf(1), requestId = 5, stage = RequestStage.REQUESTED),
                        TrackedRequest("tv:12:2", 12, "tv", "Severance", null, setOf(2), requestId = 6, stage = RequestStage.REQUESTED)),
                    contentDetails = ContentDetails(media.id, media.title, "Seerr", "Serie · 2022", artworkRes = R.drawable.media_placeholder,
                        source = ServiceKind.SEERR, mediaType = "Series")),
                    null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, onWithdraw = { withdrawn += it })
            }
        }
        rule.onNodeWithTag("title-withdraw-tv:12:2").performScrollTo().performClick()
        rule.onNodeWithTag("withdraw-confirm").performClick()
        rule.runOnIdle { assertEquals(listOf("tv:12:2"), withdrawn) }
    }
}
