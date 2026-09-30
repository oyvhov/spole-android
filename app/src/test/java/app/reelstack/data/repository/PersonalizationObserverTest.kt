package app.reelstack.data.repository

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.Personalization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class PersonalizationObserverTest {
    @Test fun changingOnlyPlaybackInfoImmediatelyReachesObservers() {
        val repository = AppPreferencesRepository(ApplicationProvider.getApplicationContext())
        repository.personalization = Personalization()
        var seen = repository.personalization
        val stop = repository.observePersonalization { seen = it }
        repository.personalization = repository.personalization.copy(showPlaybackModeInOsd = true)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(seen.showPlaybackModeInOsd)
        repository.personalization = repository.personalization.copy(showPlaybackModeInOsd = false)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(seen.showPlaybackModeInOsd)
        stop()
    }

    @Test fun playbackModeIsOffOnFreshInstallAndOnceOnUpgrade() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = context.getSharedPreferences("reelstack_preferences", android.content.Context.MODE_PRIVATE)
        file.edit().remove("quiet_osd_default_applied").remove("show_playback_mode_in_osd").commit()
        assertFalse(Personalization().showPlaybackModeInOsd)
        assertFalse(AppPreferencesRepository(context).personalization.showPlaybackModeInOsd)

        file.edit().remove("quiet_osd_default_applied").putBoolean("show_playback_mode_in_osd", true).commit()
        file.edit().putString("preferred_library_source", "EMBY").commit()
        val upgraded = AppPreferencesRepository(context)
        assertFalse(upgraded.personalization.showPlaybackModeInOsd)
        assertEquals(app.reelstack.data.model.ServiceKind.EMBY, upgraded.preferredLibrarySource)

        upgraded.personalization = upgraded.personalization.copy(showPlaybackModeInOsd = true)
        assertTrue(AppPreferencesRepository(context).personalization.showPlaybackModeInOsd)
    }

    /**
     * The screen only hears about keys the observer lists. The two new choices were missing, so
     * switching one of them off in Settings changed the stored value and nothing on screen.
     */
    @Test fun aChangeToOnlyTheMenuOrSearchChoiceReachesTheScreen() {
        val repository = AppPreferencesRepository(ApplicationProvider.getApplicationContext())
        repository.personalization = Personalization()
        var seen = repository.personalization
        val stop = repository.observePersonalization { seen = it }

        repository.personalization = repository.personalization.copy(showHomeSearchBar = true)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(seen.showHomeSearchBar)
        repository.personalization = repository.personalization.copy(showHomeSearchBar = false)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(seen.showHomeSearchBar)

        repository.personalization = repository.personalization.copy(showDownloadsInMenu = true)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(seen.showDownloadsInMenu)
        repository.personalization = repository.personalization.copy(showDownloadsInMenu = false)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(seen.showDownloadsInMenu)
        stop()
    }

    /** Hiding a library tile had no key in the list, so the page kept showing it until a restart. */
    @Test fun anOldHiddenLibraryListReachesTheScreenWhenItIsEmptied() {
        val repository = AppPreferencesRepository(ApplicationProvider.getApplicationContext())
        repository.personalization = Personalization(libraryHidden = setOf("kids"))
        var seen = repository.personalization
        val stop = repository.observePersonalization { seen = it }
        repository.personalization = repository.personalization.copy(libraryHidden = emptySet())
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(emptySet<String>(), seen.libraryHidden)
        stop()
    }

    /**
     * Seven switches are gone and their behaviour is fixed. A stale "off" left in the file must not
     * come back to life if a later version reads the same key again.
     */
    @Test fun switchesThatNoLongerExistAreRemovedFromTheFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = context.getSharedPreferences("reelstack_preferences", android.content.Context.MODE_PRIVATE)
        val gone = listOf("hero_rotate", "hero_logo", "hero_compact", "library_hub", "show_upcoming_episodes",
            "show_ratings", "show_quality")
        file.edit().apply { gone.forEach { putBoolean(it, false) } }.commit()
        val repository = AppPreferencesRepository(context)
        repository.personalization = repository.personalization.copy(showHero = false)
        gone.forEach { assertFalse("$it is still stored", file.contains(it)) }
        assertFalse(repository.personalization.showHero)
    }
}
