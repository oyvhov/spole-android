package app.reelstack.ui.kids

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.KidsWorld
import app.reelstack.data.model.ServiceKind
import app.reelstack.test.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class KidsComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun kidsProfileButton_displaysAvatarInitialAndTriggersClick() {
        var clicked = false
        composeTestRule.setContent {
            KidsProfileButton(
                name = "Eilev",
                avatarUrl = null,
                world = KidsWorld.SPACE,
                onClick = { clicked = true },
            )
        }

        composeTestRule.onNodeWithTag("kids-profile-button").assertIsDisplayed()
        composeTestRule.onNodeWithText("E").assertIsDisplayed()

        composeTestRule.onNodeWithTag("kids-profile-button").performClick()
        assertTrue("Expected profile button click to trigger callback", clicked)
    }

    @Test
    fun kidsProfileButton_rendersGracefullyWithDoubleTextScale() {
        // Accessibility 2.0x font scaling test
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1.5f, fontScale = 2.0f)
            ) {
                KidsProfileButton(
                    name = "Astrid Lovise",
                    avatarUrl = null,
                    world = KidsWorld.SUNSET,
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("kids-profile-button").assertIsDisplayed()
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun kidsLibraryCard_displaysTitleAndSelection() {
        val library = TestFixtures.sampleLibraryView(
            id = "lib-kids-tv",
            name = "Barne-Tv Serier",
        )
        var selectedId: String? = null

        composeTestRule.setContent {
            KidsLibraryCard(
                library = library,
                selected = true,
                source = ServiceKind.JELLYFIN,
                world = KidsWorld.FOREST,
                onClick = { selectedId = library.id },
            )
        }

        composeTestRule.onNodeWithTag("kids-library-lib-kids-tv").assertIsDisplayed()
        composeTestRule.onNodeWithText("Barne-Tv Serier").assertIsDisplayed()

        composeTestRule.onNodeWithTag("kids-library-lib-kids-tv").performClick()
        assertEquals("lib-kids-tv", selectedId)
    }

    @Test
    fun kidsProfileDialog_showsMiVerdAndBytProfil() {
        var miVerdOpened = false
        var switchProfileOpened = false

        composeTestRule.setContent {
            KidsProfileDialog(
                name = "Eilev",
                avatarUrl = null,
                world = KidsWorld.OCEAN,
                onAppearance = { miVerdOpened = true },
                onSwitchProfile = { switchProfileOpened = true },
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Mi verd").assertIsDisplayed()
        composeTestRule.onNodeWithText("Byt profil").assertIsDisplayed()

        composeTestRule.onNodeWithText("Mi verd").performClick()
        assertTrue("Expected Mi verd click to trigger callback", miVerdOpened)
    }

    @Test
    @Config(qualifiers = "w960dp-h1200dp-television-xhdpi")
    fun kidsHomeScreen_rendersFavouritesAndSuggestionsShelves_andOmitsFillerText() {
        val favItem = TestFixtures.sampleMedia(id = "fav-1", title = "Pippi Langstrømpe")
        val sugItem = TestFixtures.sampleMedia(id = "sug-1", title = "Postmann Pat")
        val libView = TestFixtures.sampleLibraryView(id = "lib-1", name = "Barnefilmar")

        composeTestRule.setContent {
            KidsHomeScreen(
                keepWatching = emptyList(),
                yourShows = listOf(favItem, sugItem),
                favourites = listOf(favItem),
                suggestions = listOf(sugItem),
                libraries = listOf(libView),
                source = ServiceKind.JELLYFIN,
                world = KidsWorld.SPACE,
                onPlay = {},
            )
        }

        // Favourites and suggestions remain separate media shelves; the visual cleanup targets
        // the wallpaper and hero, not the child-facing media controls.
        composeTestRule.onNodeWithTag("kids-favourites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Favorittar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kids-suggestions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forslag").assertIsDisplayed()

        // Unnecessary filler header "Bibliotek" must NOT be rendered
        composeTestRule.onNodeWithText("Bibliotek").assertDoesNotExist()
    }

    @Test
    fun kidsHomeScreen_libraryCardInvokesOpenCallback() {
        val library = TestFixtures.sampleLibraryView(id = "lib-open", name = "Barneseriar")
        val item = TestFixtures.sampleMedia(id = "show-open", title = "Mummidalen").copy(mediaType = "Series")
        var openedId: String? = null

        composeTestRule.setContent {
            KidsHomeScreen(
                keepWatching = emptyList(),
                yourShows = listOf(item),
                libraries = listOf(library),
                onPlay = {},
                onLibraryOpen = { openedId = it.id },
            )
        }

        composeTestRule.onNodeWithTag("kids-library-lib-open").assertIsDisplayed().performClick()
        assertEquals("lib-open", openedId)
    }

    @Test
    fun kidsLibraryScreen_usesContentSortsAndNotMediaTypeFilters() {
        val library = TestFixtures.sampleLibraryView(id = "lib-filter", name = "Barneseriar")
        val latest = TestFixtures.sampleMedia(id = "latest", title = "Nyaste eventyr").copy(
            addedAtEpochMillis = 200L,
            premiereDate = "2026-09-01",
            tmdbRating = 7.2f,
        )
        val older = TestFixtures.sampleMedia(id = "older", title = "Eldre eventyr").copy(
            addedAtEpochMillis = 100L,
            premiereDate = "2025-09-01",
            tmdbRating = 8.8f,
            played = true,
        )

        composeTestRule.setContent {
            KidsLibraryScreen(
                library = library,
                media = listOf(latest, older),
                onPlay = {},
                onBack = {},
                columns = 2,
                contentPadding = PaddingValues(12.dp),
            )
        }

        composeTestRule.onNodeWithTag("kids-sort-added").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kids-sort-released").performClick()
        composeTestRule.onNodeWithTag("kids-sort-rating").performClick()
        composeTestRule.onNodeWithTag("kids-status-unwatched").performClick()
        composeTestRule.onNodeWithText("Filmar").assertDoesNotExist()
        composeTestRule.onNodeWithText("Seriar").assertDoesNotExist()
        composeTestRule.onNodeWithText("Nyaste eventyr").assertIsDisplayed()
    }

    @Test
    fun pinEntrySheet_rendersCleanCardAndKeypad_andEntersDigits() {
        var completedPin: String? = null
        var cancelled = false

        composeTestRule.setContent {
            app.reelstack.ui.components.PinEntrySheet(
                title = "Lås opp vaksenprofil",
                subtitle = "Tast inn 4-sifra PIN-kode",
                onPinComplete = { completedPin = it },
                onCancel = { cancelled = true },
            )
        }

        composeTestRule.onNodeWithTag("pin-entry-sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pin-dots-row").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lås opp vaksenprofil").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pin-key-1").assertIsDisplayed()

        // Enter PIN 1-2-3-4
        composeTestRule.onNodeWithTag("pin-key-1").performClick()
        composeTestRule.onNodeWithTag("pin-key-2").performClick()
        composeTestRule.onNodeWithTag("pin-key-3").performClick()
        composeTestRule.onNodeWithTag("pin-key-4").performClick()

        assertEquals("1234", completedPin)
    }
}
