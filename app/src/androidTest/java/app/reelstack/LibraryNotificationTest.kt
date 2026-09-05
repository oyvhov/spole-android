package app.reelstack

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.background.LibraryNotifications
import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class LibraryNotificationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val item = TrackedRequest("notification-test", 42, "tv", "Testserie", null, setOf(2), stage = RequestStage.AVAILABLE)
    @Before fun grantTestPermission() {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        manager.cancelAll()
        awaitNotifications(0)
    }
    @After fun removeTestNotifications() { manager.cancelAll(); awaitNotifications(0) }

    private fun awaitNotifications(count: Int) {
        val deadline = android.os.SystemClock.uptimeMillis() + 5_000
        while (manager.activeNotifications.size != count && android.os.SystemClock.uptimeMillis() < deadline) {
            android.os.SystemClock.sleep(25)
        }
        assertEquals(count, manager.activeNotifications.size)
    }

    @Test fun expandedNotificationIncludesPictureSeasonTextAndPrivateVisibility() {
        assertTrue(LibraryNotifications.show(context, "test", item, artworkLoader = {
            BitmapFactory.decodeResource(context.resources, R.drawable.desert_arrival)
        }))
        awaitNotifications(1)
        val notification = manager.activeNotifications.single().notification
        assertNotNull(notification.extras.getParcelable<android.graphics.Bitmap>(Notification.EXTRA_PICTURE))
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("Sesong 2"))
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertNotNull(notification.contentIntent)
    }
    @Test fun missingArtworkStillPostsTextNotification() {
        assertTrue(LibraryNotifications.show(context, "test", item, artworkLoader = { null }))
        awaitNotifications(1)
        assertEquals("Testserie · i biblioteket", manager.activeNotifications.single().notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    }
    @Test fun switchedAccountOrDisabledFollowCannotPostAfterImageLoad() {
        assertFalse(LibraryNotifications.show(context, "test", item, stillEligible = { false }, artworkLoader = { null }))
        assertTrue(manager.activeNotifications.isEmpty())
    }
}
