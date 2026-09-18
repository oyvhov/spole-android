package app.reelstack

import app.reelstack.localization.LocalizedText
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.*
import app.reelstack.ui.screens.*
import app.reelstack.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LibraryServiceDisplayTest {
    @get:Rule val rule = createComposeRule()
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://media.example", "fixture", state = ConnectionState.CONNECTED)
    private fun library(id: String) = ReelstackUiState(connections = listOf(connection), libraryPath = listOf(id to "Filmar"),
        libraryEntries = listOf(
            RemoteLibraryItem("rated", "Ein vurdert film", "2026", null, "Movie", null, facts = listOf(LocalizedText.raw("2026"), LocalizedText.raw("★ 7,8"))),
            RemoteLibraryItem("unrated", "Utan vurdering", "2026", null, "Movie", null),
        ))

    @Test fun moviePosterShowsRealRatingAndRespectsHidePreference() {
        var options by mutableStateOf(Personalization(showRatings = true))
        var opened = ""
        rule.setContent { ReelstackTheme { CompositionLocalProvider(LocalPersonalization provides options) {
            LibraryScreen(library("rating-grid"), {}, { opened = it }, {})
        } } }
        rule.onNodeWithTag("library-rating-rated", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("library-rating-unrated", useUnmergedTree = true).assertDoesNotExist()
        capture("rating-grid")
        rule.onNodeWithTag("library-item-rated").performClick()
        rule.runOnIdle { assertEquals("rated", opened); options = options.copy(showRatings = false) }
        rule.onNodeWithTag("library-rating-rated", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun listRatingWorksWithLargeTextWithoutRepeatingTheStarInFacts() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = AppPreferencesRepository(context)
        val old = preferences.libraryDisplay("rating-list")
        try {
            preferences.setLibraryDisplay("rating-list", LibraryDisplay(view = LibraryView.LIST))
            rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                ReelstackTheme { CompositionLocalProvider(LocalPersonalization provides Personalization(showRatings = true)) {
                    LibraryScreen(library("rating-list"), {}, {}, {})
                } }
            } }
            rule.onNodeWithTag("library-item-rated").performScrollTo()
            rule.onNodeWithTag("library-rating-rated", useUnmergedTree = true).assertIsDisplayed()
            rule.onNodeWithText("★ 7,8", useUnmergedTree = true).assertDoesNotExist()
            capture("rating-list-large")
        } finally { preferences.setLibraryDisplay("rating-list", old) }
    }

    @Test fun serviceSettingsShowHealthyAndPartialConnectionsWithLargeText() {
        val state = ReelstackUiState(connections = listOf(connection,
            connection.copy(kind = ServiceKind.EMBY, name = "Emby", state = ConnectionState.ERROR, detail = "Tenesta svarar ikkje."),
            connection.copy(kind = ServiceKind.SEERR, name = "Seerr")),
            serviceWarnings = mapOf(ServiceKind.SEERR to "Nokre førespurnader kunne ikkje hentast."))
        var opened: ServiceKind? = null
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
            ReelstackTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                SettingsScreen(state, PaddingValues(0.dp), { opened = it }, {}, {}, { _, _ -> })
            } }
        } }
        rule.onNodeWithTag("settings-category-ACCOUNTS").performScrollTo().performClick()
        rule.onNodeWithTag("settings-service-icon-JELLYFIN", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("service-health-JELLYFIN-OK", useUnmergedTree = true).assertIsDisplayed()
        val tv = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(android.app.UiModeManager::class.java).currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        val prefix = if (tv) "tv" else "mobile"
        rule.onNodeWithTag("$prefix-service-EMBY").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(ServiceKind.EMBY, opened) }
        rule.onNodeWithTag("service-health-EMBY-ERROR", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("$prefix-service-SEERR").performScrollTo()
        rule.onNodeWithTag("service-health-SEERR-WARNING", useUnmergedTree = true).assertIsDisplayed()
        capture("settings-service-status-large")
    }

    @Test fun jellyfinDetailsOfferPlaybackWithoutExternalServerButton() = checkDetails(ServiceKind.JELLYFIN)
    @Test fun embyDetailsDoNotShowExternalServerButton() = checkDetails(ServiceKind.EMBY)

    private fun checkDetails(source: ServiceKind) {
        val key = "${source.name.lowercase()}-film"
        rule.setContent { ReelstackTheme {
            ReelstackSheets(ReelstackUiState(connections = listOf(connection.copy(kind = source)),
                activeSheet = AppSheet.TitleDetails(key), contentDetails = ContentDetails(key, "Ein film", source.displayName, "2026",
                    artworkRes = R.drawable.media_placeholder, source = source, mediaType = "Movie", libraryAvailable = true,
                    statusTitle = "I biblioteket ditt", statusDescription = "Bibliotekstatus som ikkje skal visast")),
                null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        rule.onNodeWithText("Ein film").assertIsDisplayed()
        if (source == ServiceKind.JELLYFIN) rule.onNodeWithTag("play-in-spole").assertIsDisplayed()
        rule.onNodeWithTag("open-in-server").assertDoesNotExist()
        rule.onNodeWithTag("detail-in-library").assertDoesNotExist()
        rule.onNodeWithText("I biblioteket ditt").assertDoesNotExist()
        rule.onNodeWithText("Bibliotekstatus som ikkje skal visast").assertDoesNotExist()
    }

    @Test fun ageIsReadableAtDoubleTextSizeAndDiscoveryKeepsAvailability() {
        var source by mutableStateOf(ServiceKind.JELLYFIN)
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val facts = app.reelstack.data.network.ServicePayloadParser.libraryDetails(
            """{"Id":"age","Name":"Film","Type":"Movie","OfficialRating":"15","ProductionYear":2026}"""
        ).facts.map { it.text(context) }
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { ReelstackTheme {
            val details = ContentDetails("age", "Film", "", "", artworkRes = R.drawable.media_placeholder,
                source = source, mediaType = "Movie", libraryAvailable = true, facts = facts)
            DetailAside(details, details, facts, tv = false)
        } } }
        rule.onNodeWithText("2026  ·  15 år").assertIsDisplayed()
        rule.onNodeWithTag("detail-in-library").assertDoesNotExist()
        rule.runOnIdle { source = ServiceKind.EMBY }
        rule.onNodeWithText("2026  ·  15 år").assertIsDisplayed()
        rule.onNodeWithTag("detail-in-library").assertDoesNotExist()
        rule.runOnIdle { source = ServiceKind.SEERR }
        rule.onNodeWithTag("detail-in-library").assertIsDisplayed()
        rule.onNodeWithText("I biblioteket ditt").assertIsDisplayed()
    }

    private fun capture(name: String) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
