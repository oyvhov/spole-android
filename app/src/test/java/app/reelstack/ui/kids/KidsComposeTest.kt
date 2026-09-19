package app.reelstack.ui.kids

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
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
    fun kidsProfileButton_displaysChildNameAndTriggersClick() {
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
        composeTestRule.onNodeWithText("Eilev").assertIsDisplayed()
        composeTestRule.onNodeWithText(KidsWorld.SPACE.title).assertIsDisplayed()

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
        composeTestRule.onNodeWithText("Astrid Lovise").assertIsDisplayed()
        composeTestRule.onNodeWithText(KidsWorld.SUNSET.title).assertIsDisplayed()
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
}
