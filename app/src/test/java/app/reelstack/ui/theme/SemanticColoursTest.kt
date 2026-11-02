package app.reelstack.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.AccentPalette
import app.reelstack.data.model.Personalization
import app.reelstack.data.repository.AppPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The error and caution colours are the chosen accent's own, not two fixed constants.
 *
 * The test chooses the accent the way the settings page does, in the stored preferences, and reads
 * the colours back from inside the real theme, because that is the path every screen takes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-port-xhdpi", application = app.reelstack.SheetTestApplication::class)
class SemanticColoursTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private class Seen(var warning: Color = Color.Unspecified, var caution: Color = Color.Unspecified,
        var error: Color = Color.Unspecified, var primary: Color = Color.Unspecified)

    private fun themed(accent: AccentPalette): Seen {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppPreferencesRepository(context).personalization = Personalization(accent = accent)
        val seen = Seen()
        rule.setContent {
            ReelstackTheme {
                seen.warning = Warning
                seen.caution = Caution
                seen.error = MaterialTheme.colorScheme.error
                seen.primary = Primary
            }
        }
        rule.waitForIdle()
        return seen
    }

    @Test fun hollyReadsItsOwnWarningAndCaution() {
        // One composition per test: a rule takes one content. HOLLY is the case that was broken.
        val seen = themed(AccentPalette.HOLLY)
        assertEquals(Color(AccentPalette.HOLLY.argb), seen.primary)
        assertEquals(Color(AccentPalette.HOLLY.warningArgb), seen.warning)
        assertEquals(Color(AccentPalette.HOLLY.cautionArgb), seen.caution)
        assertEquals("the Material error colour follows too", seen.warning, seen.error)
    }

    @Test fun goldGetsACautionThatIsNotGold() {
        val seen = themed(AccentPalette.GOLD)
        assertEquals(Color(AccentPalette.GOLD.cautionArgb), seen.caution)
        assertEquals(Color(AccentPalette.GOLD.warningArgb), seen.warning)
    }

    @Test fun limeStaysExactlyAsItWas() {
        val seen = themed(AccentPalette.LIME)
        assertEquals(Color(0xFFFF7A7D), seen.warning)
        assertEquals(Color(0xFFFFC66D), seen.caution)
        assertEquals(Color(0xFFFF7A7D), seen.error)
    }
}
