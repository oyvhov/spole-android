package app.reelstack.update

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class UpdateInstallIntentTest {
    private val apk = Uri.parse("content://app.reelstack.updates/updates/spole-update.apk")

    /**
     * On Loftet (Google TV Streamer, 2026-11-07) the installer task left behind by the previous update was
     * brought to the front instead, and closed at once: the first "Install update" did nothing at all.
     */
    @Test fun theInstallerStartsInAFreshTaskEvenWhenAnOldOneIsLeft() {
        val intent = updateInstallIntent(apk)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }

    @Test fun theInstallerMayReadTheDownloadedPackage() {
        val intent = updateInstallIntent(apk)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(apk, intent.data)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
