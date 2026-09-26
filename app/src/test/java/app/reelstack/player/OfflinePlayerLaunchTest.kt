package app.reelstack.player

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class OfflinePlayerLaunchTest {
    /**
     * «Sjå fråkopla» on the Downloads screen handed the player the application context. Android
     * refuses to start an activity from there without a task of its own, so the tap crashed.
     */
    @Test fun opensFromTheApplicationContextInItsOwnTask() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        OfflinePlayerActivity.open(application, "download-1")
        val started = shadowOf(application).nextStartedActivity
        assertNotNull(started)
        assertEquals(OfflinePlayerActivity::class.java.name, started.component?.className)
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun anActivityKeepsThePlayerInItsOwnTask() {
        val activity = Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()
        OfflinePlayerActivity.open(activity, "download-1")
        val started = shadowOf(activity).nextStartedActivity
        assertNotNull(started)
        assertEquals(0, started.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test fun refusesAnIdThatIsNotADownloadId() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        OfflinePlayerActivity.open(application, "../other")
        assertNull(shadowOf(application).nextStartedActivity)
    }
}
