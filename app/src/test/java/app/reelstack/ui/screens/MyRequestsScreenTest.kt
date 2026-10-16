package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.RequestStage
import app.reelstack.data.model.TrackedRequest
import app.reelstack.ui.ReelstackUiState
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
class MyRequestsScreenTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val requests = listOf(
        TrackedRequest("a", 1, "movie", "Coraline", "https://image.tmdb.org/t/p/w500/a.jpg", emptySet(),
            stage = RequestStage.AVAILABLE, updatedAt = 5),
        TrackedRequest("b", 2, "tv", "Severance", "https://image.tmdb.org/t/p/w500/b.jpg", setOf(3),
            stage = RequestStage.DOWNLOADING, percent = 64, updatedAt = 1),
    )

    private fun show(admin: Boolean = false, fontScale: Float = 1f, opened: MutableList<String> = mutableListOf()) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                ReelstackTheme {
                    MyRequestsScreen(ReelstackUiState(trackedRequests = requests, adminView = admin), PaddingValues(),
                        onOpen = { opened += it }, onBack = {}, onLoadOlder = {})
                }
            }
        }
    }

    @Test fun linesOpenTheTitleAndHaveNoButtons() {
        val opened = mutableListOf<String>()
        show(opened = opened)
        rule.onNodeWithTag("my-request-b").assertIsDisplayed().performClick()
        assertEquals(listOf("b"), opened)
        rule.onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag("my-request-b"))).assertCountEquals(0)
    }

    @Test fun serverTabIsForAdministratorsOnly() {
        show(admin = false)
        rule.onNodeWithTag("my-requests-tab-SERVER").assertDoesNotExist()
    }

    @Test fun administratorsSeeTheServerTab() {
        show(admin = true)
        rule.onNodeWithTag("my-requests-tab-SERVER").assertIsDisplayed()
    }

    /** Downloaded bytes are not a title in the library; an import says so, not «Lastar ned». */
    @Test fun importingIsNotShownAsDownloading() {
        var importing = ""
        var downloading = ""
        rule.setContent { ReelstackTheme {
            importing = androidx.compose.ui.res.stringResource(app.reelstack.R.string.flow_stage_importing)
            downloading = androidx.compose.ui.res.stringResource(app.reelstack.R.string.flow_stage_downloading)
            MyRequestsScreen(ReelstackUiState(trackedRequests = listOf(TrackedRequest("i", 3, "movie", "Bugonia",
                "https://image.tmdb.org/t/p/w500/i.jpg", emptySet(), stage = RequestStage.IMPORTING, percent = 100))),
                PaddingValues(), onOpen = {}, onBack = {}, onLoadOlder = {})
        } }
        rule.waitForIdle()
        rule.onNodeWithText(importing, substring = true).assertIsDisplayed()
        rule.onNodeWithText(downloading, substring = true).assertDoesNotExist()
    }

    @Test fun tabsAndLinesSurviveLargeText() {
        show(fontScale = 2f)
        rule.onNodeWithTag("my-requests-tab-READY").assertIsDisplayed()
        rule.onNodeWithText("Severance", substring = true).assertIsDisplayed()
    }
}
