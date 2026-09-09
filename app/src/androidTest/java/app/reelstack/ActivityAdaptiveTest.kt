package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.*
import app.reelstack.ui.*
import app.reelstack.ui.screens.ActivityScreen
import app.reelstack.ui.components.StableSheetDialog
import app.reelstack.ui.components.SheetToolbar
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ActivityAdaptiveTest {
    @get:Rule val rule = createComposeRule()
    private val request = TrackedRequest("first", 1, "movie", "A film worth waiting for", null,
        emptySet(), false, RequestStage.AVAILABLE)
    private val state = ReelstackUiState(
        connections = listOf(ServiceConnection(ServiceKind.SEERR, "Seerr", "https://example.com", sessionCookie = true)),
        trackedRequests = listOf(request, request.copy(key = "second", mediaId = 2, title = "Another film")))

    @Test fun posterGalleryAdaptsWithoutChangingIdentityOrOverlappingControls() {
        var width by mutableStateOf(960.dp)
        var opened = ""
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width, 1000.dp))) {
                ReelstackTheme { ActivityScreen(state, PaddingValues(0.dp), { opened = it }) }
            }
        }
        val first = rule.onNodeWithTag("tracked-request-first").fetchSemanticsNode().boundsInRoot
        val second = rule.onNodeWithTag("tracked-request-second").fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, second.top, 1f)
        assertTrue(second.left > first.right)
        val heading = rule.onNodeWithTag("activity-heading-block").fetchSemanticsNode().boundsInRoot
        val filters = rule.onNodeWithTag("activity-filter-block").fetchSemanticsNode().boundsInRoot
        assertTrue(filters.top >= heading.bottom)
        assertTrue(first.top >= filters.bottom)
        rule.onNodeWithTag("tracked-details-second").performClick()
        assertEquals("second", opened)
        rule.runOnIdle { width = 412.dp }
        val narrowFirst = rule.onNodeWithTag("tracked-request-first").fetchSemanticsNode().boundsInRoot
        val narrowSecond = rule.onNodeWithTag("tracked-request-second").fetchSemanticsNode().boundsInRoot
        assertEquals(narrowFirst.top, narrowSecond.top, 1f)
        assertTrue(narrowSecond.left > narrowFirst.right)
        assertTrue(narrowFirst.width < first.width || narrowFirst.width < 220 * rule.density.density)
        rule.onNodeWithTag("activity-scope").assertDoesNotExist()
    }

    @Test fun withdrawalIsSecondaryAndStillRequiresConfirmation() {
        var cancelled = false
        rule.setContent { ReelstackTheme {
            TrackedRequestCard(request.copy(stage = RequestStage.REQUESTED, requestId = 9), {}, {},
                onCancel = { cancelled = true })
        } }
        rule.onNodeWithTag("cancel-request-first").assertDoesNotExist()
        rule.onNodeWithTag("request-options-first").performClick()
        rule.onNodeWithTag("cancel-request-first").performClick()
        assertFalse(cancelled)
        rule.onNodeWithText("Behald").performClick()
        assertFalse(cancelled)
    }

    @Test fun fullScreenDetailsKeepGeometryAndCloseWithoutMovingBackground() {
        var open by mutableStateOf(true)
        var detail by mutableStateOf("Kort tekst")
        rule.setContent { ReelstackTheme {
            Text("Bakgrunn", Modifier.testTag("background"))
            if (open) StableSheetDialog(true, { open = false }, fullScreen = true) { _, _, close ->
                Column { SheetToolbar("Detaljar", "Lukk", close, page = true); Text(detail) }
            }
        } }
        rule.onNodeWithTag("adaptive-dialog").assertDoesNotExist()
        val bounds = rule.onNodeWithTag("tv-detail-page").fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { detail = "Sein metadata. ".repeat(20) }
        assertEquals(bounds, rule.onNodeWithTag("tv-detail-page").fetchSemanticsNode().boundsInRoot)
        rule.onNodeWithText("Tilbake").assertIsDisplayed()
        rule.onNodeWithTag("sheet-close").performClick()
        rule.waitUntil(3000) { !open }
        rule.onNodeWithTag("background").assertIsDisplayed()
    }

    @Test fun tvArtworkStaysStillWhileLongMetadataScrolls() {
        lateinit var scroll: androidx.compose.foundation.ScrollState
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(960.dp, 540.dp))) {
                ReelstackTheme {
                    scroll = androidx.compose.foundation.rememberScrollState()
                    app.reelstack.ui.components.DetailReadingLayout(true, scroll, artwork = {
                        Box(Modifier.fillMaxWidth().height(300.dp))
                    }) {
                        Text("Lang omtale. ".repeat(300))
                        Button({}, Modifier.testTag("reading-end")) { Text("Slutt") }
                    }
                }
            }
        }
        val art = rule.onNodeWithTag("tv-detail-artwork").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("reading-end").performScrollTo().assertIsDisplayed()
        assertTrue(scroll.value > 0)
        assertEquals(art, rule.onNodeWithTag("tv-detail-artwork").fetchSemanticsNode().boundsInRoot)
        assertTrue(rule.onNodeWithTag("detail-scroll").fetchSemanticsNode().boundsInRoot.left > art.right)
    }
}
