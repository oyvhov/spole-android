package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Home on a television looks like Home in Stova: a large hero, and one shelf at the bottom of the
 * screen — «Sjå vidare» — with the rest of the rows further down.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
@OptIn(ExperimentalTestApi::class)
class HomeTvHeroLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val episodes = List(6) { index -> LibraryMedia(
        id = "episode-$index", title = "Serie $index", subtitle = "Episode", progress = .4f,
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode",
        heroUrl = "https://example.com/Items/series-$index/Images/Thumb?maxWidth=1920&quality=90&tag=t",
        season = 1, episode = index + 1,
    ) }

    private fun showHome(fontScale: Float = 1f) {
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://example.com", "fixture", userId = "u")),
            resume = episodes,
            recentSeries = episodes,
            homeSections = setOf(HomeSection.CONTINUE_WATCHING, HomeSection.JELLYFIN_SERIES),
        )
        rule.setContent {
            ReelstackTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                    CompositionLocalProvider(LocalPersonalization provides Personalization(showHero = true)) {
                        HomeScreen(state, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}, showSearch = false)
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test fun theHeroReachesDownToAWholeFirstShelfAtTheBottomOfTheScreen() {
        showHome()
        val screen = rule.onRoot().getBoundsInRoot()
        val hero = rule.onNodeWithTag("tablet-library-feature").getBoundsInRoot()
        val shelf = rule.onNodeWithTag("continue-watching-JELLYFIN").getBoundsInRoot()
        assertTrue("the hero is larger than the old 44 %: ${hero.height}", hero.height > screen.height * .44f)
        assertTrue("the whole shelf is on screen: ${shelf.bottom} of ${screen.bottom}", shelf.bottom <= screen.bottom)
        assertTrue("and it stands at the bottom, not half-way up: ${screen.bottom - shelf.bottom}",
            screen.bottom - shelf.bottom < 48.dp)
        assertTrue("nothing but that shelf is on screen under the hero: ${shelf.top - hero.bottom}",
            shelf.top - hero.bottom < 8.dp)
        val picture = rule.onNodeWithTag("feature-artwork", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("the picture runs on behind the shelf to the bottom: ${picture.bottom}", picture.bottom >= screen.bottom - 1.dp)
    }

    @Test fun largeTextKeepsTheShelfOnScreenByShrinkingTheHero() {
        showHome(fontScale = 1.5f)
        val screen = rule.onRoot().getBoundsInRoot()
        val shelf = rule.onNodeWithTag("continue-watching-JELLYFIN").getBoundsInRoot()
        val hero = rule.onNodeWithTag("tablet-library-feature").getBoundsInRoot()
        assertTrue("the shelf still begins on screen: ${shelf.top} of ${screen.bottom}", shelf.top < screen.bottom)
        assertTrue("the hero never drops below the old 44 %: ${hero.height}", hero.height >= screen.height * .44f - 1.dp)
    }
}
