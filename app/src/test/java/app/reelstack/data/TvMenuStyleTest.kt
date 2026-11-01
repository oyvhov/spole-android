package app.reelstack.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.TvMenuStyle
import app.reelstack.data.repository.AppPreferencesRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class TvMenuStyleTest {
    @Test fun legacyHiddenSidebarMigratesOnlyWhenNoValidNewChoiceExists() {
        assertEquals(TvMenuStyle.SIDEBAR, TvMenuStyle.decode(null, false))
        assertEquals(TvMenuStyle.HIDDEN_SIDEBAR, TvMenuStyle.decode(null, true))
        assertEquals(TvMenuStyle.TOP_BAR, TvMenuStyle.decode("TOP_BAR", true))
        assertEquals(TvMenuStyle.SIDEBAR, TvMenuStyle.decode("SIDEBAR", true))
        assertEquals(TvMenuStyle.HIDDEN_SIDEBAR, TvMenuStyle.decode("unknown", true))
    }

    @Test fun theOldPropertyIsDerivedAndOnlyHidesTheSidebarStyle() {
        assertFalse(Personalization().hideTvSidebar)
        assertTrue(Personalization(tvMenuStyle = TvMenuStyle.HIDDEN_SIDEBAR).hideTvSidebar)
        assertFalse(Personalization(tvMenuStyle = TvMenuStyle.TOP_BAR).hideTvSidebar)
    }

    @Test fun storedChoiceSurvivesRecreationAndWinsOverLegacyFlag() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("reelstack_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().putBoolean("hide_tv_sidebar", true).commit()
        val repository = AppPreferencesRepository(context)
        assertEquals(TvMenuStyle.HIDDEN_SIDEBAR, repository.personalization.tvMenuStyle)
        repository.personalization = repository.personalization.copy(tvMenuStyle = TvMenuStyle.TOP_BAR)
        assertEquals("TOP_BAR", prefs.getString("tv_menu_style", null))
        assertEquals(TvMenuStyle.TOP_BAR, AppPreferencesRepository(context).personalization.tvMenuStyle)
        repository.personalization = repository.personalization.copy(tvMenuStyle = TvMenuStyle.SIDEBAR)
        assertEquals(TvMenuStyle.SIDEBAR, AppPreferencesRepository(context).personalization.tvMenuStyle)
    }
}
