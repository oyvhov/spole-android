package app.reelstack.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.AppContainer
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.player.PendingPlaybackProgress
import app.reelstack.player.PlaybackUserState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Unsent checkpoints belong to their own account scope. Signing in elsewhere must not drop them. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class AccountChangeProgressTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://example.test", "fixture", "user")
    private val emby = jellyfin.copy(kind = ServiceKind.EMBY, baseUrl = "https://emby.example.test")
    private fun entry(scope: String, item: String) = PendingPlaybackProgress("id-$item", scope, item, 60_000, false,
        PlaybackUserState(0, false), System.currentTimeMillis(), "id-$item")
    @Before fun reset() { PlaybackProgressOutbox(context).clear() }

    @Test fun signingInToAnotherServiceKeepsUnsentCheckpoints() {
        val container = AppContainer(context)
        val scope = PlaybackProgressOutbox.scope("", jellyfin)
        container.playbackProgressOutbox.put(entry(scope, "film"))
        // Every connection commit clears the resume journal (ReelstackViewModel.commitConnections).
        container.localPlaybackStore.clear()
        assertEquals("film", container.playbackProgressOutbox.pending(scope).single().itemId)
    }

    @Test fun signingOutOfOneServiceForgetsOnlyItsOwnCheckpoints() {
        val container = AppContainer(context)
        val mine = PlaybackProgressOutbox.scope("", jellyfin)
        val other = PlaybackProgressOutbox.scope("", emby)
        val child = PlaybackProgressOutbox.scope("child", jellyfin)
        container.playbackProgressOutbox.put(entry(mine, "film"))
        container.playbackProgressOutbox.put(entry(other, "episode"))
        container.playbackProgressOutbox.put(entry(child, "cartoon"))
        container.forgetUnsentProgress("", listOf(jellyfin))
        assertTrue(container.playbackProgressOutbox.pending(mine).isEmpty())
        assertEquals("episode", container.playbackProgressOutbox.pending(other).single().itemId)
        assertEquals("cartoon", container.playbackProgressOutbox.pending(child).single().itemId)
    }
}
