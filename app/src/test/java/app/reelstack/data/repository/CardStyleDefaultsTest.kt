package app.reelstack.data.repository

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import app.reelstack.data.model.ArtworkCorners
import app.reelstack.data.model.FocusStyle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Cards look like Stova's by standard: round 20 dp corners and focus that lifts the card. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "television")
class CardStyleDefaultsTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val stored get() = context.getSharedPreferences("reelstack_preferences", Context.MODE_PRIVATE)

    @Before fun clear() { stored.edit(commit = true) { clear() } }

    @Test fun aFreshInstallStartsWithStovasCardStyle() {
        val style = AppPreferencesRepository(context).personalization
        assertEquals(ArtworkCorners.ROUND, style.artworkCorners)
        assertEquals(20, style.artworkCorners.radius)
        assertEquals(FocusStyle.LIFT, style.focusStyle)
    }

    @Test fun theOldStandardSavedWithOtherSettingsMovesOnce() {
        // Settings are saved whole, so a television that never chose a card style has SOFT and BOLD on disk.
        stored.edit(commit = true) { putString("artwork_corners", "SOFT"); putString("focus_style", "BOLD") }
        val repository = AppPreferencesRepository(context)
        assertEquals(ArtworkCorners.ROUND, repository.personalization.artworkCorners)
        assertEquals(FocusStyle.LIFT, repository.personalization.focusStyle)
        // A choice made afterwards is kept, however often the repository is made again.
        repository.personalization = repository.personalization.copy(artworkCorners = ArtworkCorners.SOFT, focusStyle = FocusStyle.BOLD)
        assertEquals(FocusStyle.BOLD, AppPreferencesRepository(context).personalization.focusStyle)
        assertEquals(ArtworkCorners.SOFT, AppPreferencesRepository(context).personalization.artworkCorners)
    }

    @Test fun aTelevisionThatSavedTheOldWhiteRingMovesToo() {
        // Seen on the Shield: soft corners with WHITE on disk, from before BOLD became the television standard.
        stored.edit(commit = true) { putString("artwork_corners", "SOFT"); putString("focus_style", "WHITE") }
        assertEquals(FocusStyle.LIFT, AppPreferencesRepository(context).personalization.focusStyle)
    }

    @Test fun aStyleSomebodyChoseIsNeverReplaced() {
        stored.edit(commit = true) { putString("artwork_corners", "CRISP"); putString("focus_style", "BOLD") }
        val style = AppPreferencesRepository(context).personalization
        assertEquals(ArtworkCorners.CRISP, style.artworkCorners)
        assertEquals(FocusStyle.BOLD, style.focusStyle)
        stored.edit(commit = true) { clear(); putString("artwork_corners", "SOFT"); putString("focus_style", "ACCENT") }
        assertEquals(FocusStyle.ACCENT, AppPreferencesRepository(context).personalization.focusStyle)
    }
}
