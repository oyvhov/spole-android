package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.PersonalizationSettings
import app.reelstack.ui.screens.HomeScreen
import app.reelstack.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PersonalizationTest {
    @get:Rule val rule = createComposeRule()
    private val media = LibraryMedia(id = "fixture", title = "A film", subtitle = "2026",
        source = ServiceKind.JELLYFIN, artworkRes = R.drawable.media_placeholder, mediaType = "Movie")

    @Test fun wideMediaReachesTheEdgeButProfileKeepsItsInset() {
        var density = 1f
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))) {
                density = LocalDensity.current.density
                ReelstackTheme { HomeScreen(ReelstackUiState(recentMovies = List(12) { media.copy(id = "fixture-$it") },
                    homeSections = setOf(HomeSection.JELLYFIN_MOVIES)), PaddingValues(0.dp),
                    {}, {}, {}, {}, {}, onRefresh = {}, showSearch = false) }
            }
        }
        val feed = rule.onNodeWithTag("home-feed").fetchSemanticsNode().boundsInRoot
        val rail = rule.onAllNodesWithTag("library-rail").onFirst().fetchSemanticsNode().boundsInRoot
        val profile = rule.onNodeWithTag("home-account").fetchSemanticsNode().boundsInRoot
        assertEquals(feed.right, rail.right, 1f)
        assertEquals(24f, (feed.right - profile.right) / density, 1f)
    }

    @Test fun artworkScalesWithoutChangingAspectRatio() {
        var size by mutableStateOf(ArtworkSize.STANDARD)
        var density = 1f
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(412.dp, 900.dp))) {
                density = LocalDensity.current.density
                ReelstackTheme {
                    CompositionLocalProvider(LocalPersonalization provides Personalization(artworkSize = size)) {
                        HomeScreen(ReelstackUiState(recentMovies = listOf(media),
                            homeSections = setOf(HomeSection.JELLYFIN_MOVIES)), PaddingValues(0.dp),
                            {}, {}, {}, {}, {}, onRefresh = {}, showSearch = false)
                    }
                }
            }
        }
        ArtworkSize.entries.forEach { selected ->
            rule.runOnIdle { size = selected }
            val art = rule.onNodeWithTag("library-artwork-fixture", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(132f * selected.scale, art.width / density, 1f)
            assertEquals(1.5f, art.height / art.width, .02f)
        }
    }

    @Test fun discoverLoadingUsesTheSameAdaptiveColumnsAsResults() {
        var loading by mutableStateOf(true)
        var density = 1f
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))) {
                density = LocalDensity.current.density
                ReelstackTheme {
                    CompositionLocalProvider(LocalPersonalization provides Personalization(artworkSize = ArtworkSize.LARGE)) {
                        app.reelstack.ui.screens.DiscoverScreen(ReelstackUiState(isSearching = loading,
                            discover = listOf(DiscoverMedia("fixture", "A film", "2026", R.drawable.media_placeholder, false))),
                            PaddingValues(0.dp), {}, {}, {})
                    }
                }
            }
        }
        val skeleton = rule.onAllNodesWithContentDescription("Lastar søkjeresultat").onFirst().fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { loading = false }
        val artwork = rule.onNodeWithTag("discover-cover-fixture").fetchSemanticsNode().boundsInRoot
        assertEquals(skeleton.width, artwork.width, 1f)
        assertEquals(360f, artwork.height / density, 2f)
    }

    @Test fun controlsChangeIndependentlyAndResetDoesNotChangePlayback() {
        var value by mutableStateOf(Personalization())
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(412.dp, 900.dp))) {
                ReelstackTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    PersonalizationSettings(value, { value = it })
                } }
            }
        }
        rule.onNodeWithTag("auto-resume").performScrollTo().assertIsOn().performClick().assertIsOff()
        rule.onNodeWithTag("appearance-expand").performScrollTo().performClick()
        rule.onNodeWithTag("accent-OCEAN").performScrollTo().performClick().assertIsSelected()
        rule.onNodeWithTag("artwork-LARGE").performScrollTo().performClick().assertIsSelected()
        rule.runOnIdle { assertEquals(Personalization(AccentPalette.OCEAN, ArtworkSize.LARGE, false), value) }
        rule.onNodeWithTag("appearance-reset").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(Personalization(autoResume = false), value) }
    }

    @Test fun savedChoicesUpdateTheThemeAndSurviveRepositoryRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppPreferencesRepository(context)
        val original = repository.personalization
        var primary = Color.Unspecified
        try {
            repository.personalization = Personalization()
            rule.setContent { ReelstackTheme { primary = MaterialTheme.colorScheme.primary } }
            rule.runOnIdle { repository.personalization = Personalization(AccentPalette.IRIS, ArtworkSize.COMPACT, false) }
            rule.waitForIdle()
            rule.runOnIdle {
                assertEquals(Color(AccentPalette.IRIS.argb), primary)
                assertEquals(Personalization(AccentPalette.IRIS, ArtworkSize.COMPACT, false),
                    AppPreferencesRepository(context).personalization)
            }
        } finally {
            rule.runOnIdle { repository.personalization = original }
        }
    }
}
