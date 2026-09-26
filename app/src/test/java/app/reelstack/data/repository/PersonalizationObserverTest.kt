package app.reelstack.data.repository

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.Personalization
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class PersonalizationObserverTest {
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
}
