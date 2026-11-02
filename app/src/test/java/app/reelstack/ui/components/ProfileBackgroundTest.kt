package app.reelstack.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.AccentPalette
import app.reelstack.data.model.Personalization
import app.reelstack.ui.theme.LocalPersonalization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The composable the two avatars call: the hue follows the name and the accent, and no name keeps the raised surface. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class ProfileBackgroundTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private var named = Color.Unspecified
    private var otherAccent = Color.Unspecified
    private var blank = Color.Unspecified
    private var missing = Color.Unspecified
    private var raised = Color.Unspecified

    @Test fun theBackgroundIsTheNamesHueInTheAccentAndTheSurfaceWithoutAName() {
        rule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalPersonalization provides Personalization(accent = AccentPalette.LIME)) {
                    named = profileBackground("Kari")
                    blank = profileBackground("  ")
                    missing = profileBackground(null)
                    raised = MaterialTheme.colorScheme.surfaceVariant
                }
                CompositionLocalProvider(LocalPersonalization provides Personalization(accent = AccentPalette.OCEAN)) {
                    otherAccent = profileBackground("Kari")
                }
            }
        }
        rule.waitForIdle()
        assertEquals(Color(ProfileHue.color(AccentPalette.LIME.argb, "Kari")), named)
        assertEquals(Color(ProfileHue.color(AccentPalette.OCEAN.argb, "Kari")), otherAccent)
        assertNotEquals("the accent turns the whole wheel", named, otherAccent)
        assertEquals(raised, blank)
        assertEquals(raised, missing)
    }
}
