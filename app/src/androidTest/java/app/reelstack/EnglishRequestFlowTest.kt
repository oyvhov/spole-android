package app.reelstack

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.data.model.*
import app.reelstack.localization.*
import app.reelstack.ui.*
import app.reelstack.ui.components.SettingsAccounts
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class EnglishRequestFlowTest {
    @get:Rule val rule = createComposeRule()
    @Composable private fun English(content: @Composable () -> Unit) {
        val registryOwner = requireNotNull(androidx.activity.compose.LocalActivityResultRegistryOwner.current)
        val context = AppLanguages.wrap(LocalContext.current, AppLanguage.ENGLISH)
        CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration,
            androidx.activity.compose.LocalActivityResultRegistryOwner provides registryOwner) {
            ReelstackTheme { content() }
        }
    }
    @Test fun seasonSelectionAndReadyNotificationRemainExplicitInEnglish() {
        var draft by mutableStateOf(RequestDraft(
            DiscoverMedia("fixture", "Test series", "Serie", R.drawable.media_placeholder, true, mediaType = "tv"),
            listOf(RequestSeason(1, "Sesong 1", 1, 5), RequestSeason(2, "Sesong 2", 8, 1)), loading = false))
        var sent = 0
        rule.setContent { English {
            RequestComposer(ReelstackUiState(requestDraft = draft), { number, checked ->
                draft = draft.copy(selected = if (checked) setOf(number) else emptySet())
            }, { draft = draft.copy(notify = it) }, { sent++ }, {}, {}, {})
        } }
        rule.onNodeWithText("Seasons").assertIsDisplayed()
        rule.onNodeWithText("Season 1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("request-season-1").assertIsNotEnabled()
        rule.onNodeWithTag("request-season-2").performScrollTo().assertIsOff().performClick()
        rule.onNodeWithText("Send request · 1 season").assertIsDisplayed()
        rule.onNodeWithTag("request-notification").performScrollTo().assertIsOn().performClick().assertIsOff()
        assertEquals(0, sent)
        rule.onNodeWithTag("confirm-request").performClick()
        assertEquals(1, sent)
    }
    @Test fun requestStageDoesNotTurnImportingIntoDownloading() {
        val request = TrackedRequest("fixture", 1, "tv", "A title", null, setOf(2), stage = RequestStage.IMPORTING)
        rule.setContent { English { TrackedRequestCard(request, {}, {}) } }
        rule.onNodeWithText("Adding to library").assertIsDisplayed()
        rule.onNodeWithText("Season 2").assertIsDisplayed()
        rule.onNodeWithText("Downloading").assertDoesNotExist()
        rule.onNodeWithContentDescription("Notifications on for A title").assertExists()
    }
    @Test fun accountPanelTranslatesControlsWithoutChangingTheActor() {
        val account = ServiceAccount(ServiceKind.SEERR, "7", "Maya", permissions = 32)
        rule.setContent { English { SettingsAccounts(ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.SEERR, name = "Test Seerr", baseUrl = "https://fixture.example", token = "fixture", sessionCookie = true)),
            accounts = mapOf(ServiceKind.SEERR to account)), {}) } }
        rule.onNodeWithText("Your account").assertIsDisplayed()
        rule.onNodeWithText("Maya").assertIsDisplayed()
        rule.onNodeWithContentDescription("Change Seerr account").assertExists()
    }
    @Test fun datesMissingDatesAndPartialStatusUseUiLanguage() {
        val today = LocalDate.of(2026, 9, 9)
        rule.setContent { English { Column {
            androidx.compose.material3.Text(seasonDescription(RequestSeason(2, "Sesong 2", 8, 4), today))
            androidx.compose.material3.Text(seasonDescription(RequestSeason(3, "Sesong 3", 8, 1), today))
            androidx.compose.material3.Text(nextEpisodeDescription(SeriesNextEpisode(3, 2, today.plusDays(2)), today)!!)
        } } }
        rule.onNodeWithText("Partially in your library").assertExists()
        rule.onNodeWithText("Premiere not confirmed").assertExists()
        rule.onNodeWithText("Next episode · S03 E02", substring = true).assertExists()
    }
}
