package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.*
import app.reelstack.localization.AppLanguage
import app.reelstack.localization.AppLanguages
import app.reelstack.ui.*
import app.reelstack.RoadmapTestTheme as ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class RequestRulesUiTest {
    @get:Rule val rule = createComposeRule()
    private val media = DiscoverMedia("fixture", "Ei lang forteljing", "Serie", R.drawable.media_placeholder,
        false, remoteId = 42, mediaType = "tv")
    private val connection = ServiceConnection(ServiceKind.SEERR, "Fixture", "https://seerr.example", "fixture", "7", sessionCookie = true)
    private val account = ServiceAccount(ServiceKind.SEERR,"7","Testperson",permissions=32)
    private fun state(draft: RequestDraft) = ReelstackUiState(connections = listOf(connection),
        accounts = mapOf(ServiceKind.SEERR to account), requestDraft = draft, notificationsEnabled = false)
    private val draft = RequestDraft(media, listOf(RequestSeason(1,"Sesong 1",8,1),RequestSeason(2,"Sesong 2",8,1)),
        selected = setOf(1,2), loading = false, rules = RequestRules(true,true,RequestQuota(5,1,7)))

    @Test fun exceedingSeasonQuotaDisablesSendUntilSelectionFits() {
        var current by mutableStateOf(draft)
        var sent = 0
        rule.setContent { ReelstackTheme { RequestComposer(state(current), { number, checked ->
            current = current.copy(selected = if (checked) current.selected + number else current.selected - number)
        }, {}, { sent++ }, {}, {}, {}) } }
        rule.onNodeWithTag("confirm-request").assertIsNotEnabled()
        rule.onNodeWithTag("request-quota-exceeded").performScrollTo().assertIsDisplayed()
        rule.onRoot().saveRoadmapImage("quota-nn-phone.png")
        rule.onNodeWithTag("request-season-2").performScrollTo().performClick()
        rule.onNodeWithTag("confirm-request").assertIsEnabled()
        rule.runOnIdle { assertEquals(0,sent) }
    }
    @Test fun unknownQuotaIsExplicitAndDoesNotPretendUnlimited() {
        rule.setContent { ReelstackTheme { RequestComposer(state(draft.copy(rules = RequestRules(true,true,null))),
            { _, _ -> }, {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("Kvoten er ukjend. Seerr kontrollerer kvoten når du sender.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("confirm-request").assertIsEnabled()
        rule.onNodeWithText("Seerr oppgir inga førespurnadskvote for denne medietypen.").assertDoesNotExist()
    }
    @Test fun refreshedPermissionsCanDisableSendingWithoutRemovingWatchControls() {
        rule.setContent { ReelstackTheme { RequestComposer(state(draft.copy(rules = RequestRules(false,true,null))),
            { _, _ -> }, {}, {}, {}, {}, {}) } }
        rule.onNodeWithTag("confirm-request").assertIsNotEnabled()
        rule.onNodeWithText("Kontoen din kan ikkje førespørje denne medietypen.").performScrollTo().assertIsDisplayed()
    }
    @Test fun englishLargeTypeKeepsQuotaAndConfirmationReachable() {
        rule.setContent {
            val registry = requireNotNull(androidx.activity.compose.LocalActivityResultRegistryOwner.current)
            val context = AppLanguages.wrap(LocalContext.current, AppLanguage.ENGLISH)
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides registry) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(960.dp,540.dp)) then DeviceConfigurationOverride.FontScale(2f)) {
                    ReelstackTheme { RequestComposer(state(draft), { _, _ -> }, {}, {}, {}, {}, {}) }
                }
            }
        }
        rule.onNodeWithText("1 season remaining").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("confirm-request").assertIsDisplayed().assertIsNotEnabled()
        rule.onRoot().saveRoadmapImage("quota-en-landscape-2x.png")
    }
}
