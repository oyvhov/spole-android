package app.reelstack

import android.media.tv.TvContract.WatchNextPrograms as Programs
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.player.PlayableItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WatchNextTest {
    @Test fun progressPublishesWithoutCredentialsAndCompletionAndLogoutRemoveIt() = checkPublishing(ServiceKind.JELLYFIN)
    @Test fun embyProgressPublishesAndIsRevokedOnLogout() = checkPublishing(ServiceKind.EMBY)
    private fun checkPublishing(kind: ServiceKind) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        org.junit.Assume.assumeTrue(context.packageManager.hasSystemFeature("android.software.leanback"))
        val container = (context.applicationContext as ReelstackApplication).container
        val prefs = container.preferencesRepository
        val previous = prefs.personalization
        val account = ServiceConnection(kind, "Synthetic", "http://127.0.0.1:54321", "synthetic-private-token", "synthetic-user")
        fun owned(): List<Pair<String, String>> = buildList {
            context.contentResolver.query(Programs.CONTENT_URI, arrayOf(Programs.COLUMN_PACKAGE_NAME, Programs.COLUMN_TITLE, Programs.COLUMN_INTENT_URI), null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) if (cursor.getString(0) == context.packageName) add(cursor.getString(1) to cursor.getString(2))
            }
        }
        try {
            container.connectionRepository.save(account)
            val storedAccount = container.connectionRepository.get(kind)
            prefs.personalization = previous.copy(watchNextEnabled = true)
            val movie = PlayableItem("phase4movie", "Phase four film", "Movie", durationMs = 600_000)
            container.localPlaybackStore.record(storedAccount, movie, 120_000, 600_000, false)
            assertEquals(movie.title, container.localPlaybackStore.merge(storedAccount, emptyList()).first().title)
            assertTrue("TV provider rejected publishing", container.watchNextSync.sync())
            val published = owned().single { it.first == movie.title }
            assertFalse(published.second.contains(account.token))
            assertFalse(published.second.contains(account.baseUrl))
            assertFalse(published.second.contains(account.userId))
            container.localPlaybackStore.record(storedAccount, movie, 600_000, 600_000, true)
            assertTrue(container.watchNextSync.sync())
            assertTrue(owned().isEmpty())
            container.localPlaybackStore.record(storedAccount, movie, 140_000, 600_000, false)
            assertTrue(container.watchNextSync.sync())
            assertEquals(1, owned().size)
            container.connectionRepository.delete(kind)
            assertTrue(container.watchNextSync.sync())
            assertTrue(owned().isEmpty())
        } finally {
            prefs.personalization = previous.copy(watchNextEnabled = false)
            container.connectionRepository.delete(kind)
            container.localPlaybackStore.clear()
            container.watchNextSync.sync()
            prefs.personalization = previous
        }
    }
}
