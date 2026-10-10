package app.reelstack.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.AccentPalette
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.SeasonalDecor
import app.reelstack.data.model.VisualTheme
import app.reelstack.data.model.seasonal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class SeasonalPreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file get() = context.getSharedPreferences("reelstack_preferences", Context.MODE_PRIVATE)

    /** Someone who had switched the old «Sesongpynt» off keeps it off; on becomes FULL. */
    @Test fun theOldSwitchCarriesOverAndIsThenRetired() {
        file.edit().remove("seasonal_decor").putBoolean("seasonal_ornament", false).commit()
        val repository = AppPreferencesRepository(context)
        assertEquals(SeasonalDecor.OFF, repository.personalization.seasonalDecor)
        repository.personalization = repository.personalization.copy(appLabel = "Spole")
        assertFalse(file.contains("seasonal_ornament"))
        assertEquals("OFF", file.getString("seasonal_decor", null))

        file.edit().remove("seasonal_decor").putBoolean("seasonal_ornament", true).commit()
        assertEquals(SeasonalDecor.FULL, AppPreferencesRepository(context).personalization.seasonalDecor)
    }

    @Test fun levelAndCalendarAreStored() {
        val repository = AppPreferencesRepository(context)
        repository.personalization = Personalization(seasonalDecor = SeasonalDecor.CALM, seasonCalendar = true)
        val read = AppPreferencesRepository(context).personalization
        assertEquals(SeasonalDecor.CALM, read.seasonalDecor)
        assertTrue(read.seasonCalendar)
    }

    /** What every screen does when it saves: copy what it was shown and change one thing. */
    @Test fun savingDuringACalendarSeasonNeverStoresTheSeason() {
        val repository = AppPreferencesRepository(context)
        repository.personalization = Personalization(visualTheme = VisualTheme.PLUM, accent = AccentPalette.ROSE, seasonCalendar = true)
        val shown = repository.personalization.seasonal(LocalDate.of(2026, 12, 20))
        assertEquals(VisualTheme.NOEL, shown.visualTheme)
        repository.personalization = shown.copy(sidebarExpanded = false)
        val stored = AppPreferencesRepository(context).personalization
        assertEquals(VisualTheme.PLUM, stored.visualTheme)
        assertEquals(AccentPalette.ROSE, stored.accent)
        assertEquals(false, stored.sidebarExpanded)
    }
}
