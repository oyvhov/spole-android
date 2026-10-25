package app.reelstack

import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.PlaybackProgressOutbox
import app.reelstack.player.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Run seed/verify in separate instrumentation invocations; see scripts/test-progress-restart.sh. */
class PlaybackProgressRestartTest {
    @Test fun committedCheckpointSurvivesProcessRestart() {
        val phase = InstrumentationRegistry.getArguments().getString("progressRestartPhase")
        assumeTrue("Requires two process invocations", phase in listOf("seed", "verify"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connection = ServiceConnection(ServiceKind.JELLYFIN, "Synthetic", "https://example.test", "fixture", "progress-fixture")
        val scope = PlaybackProgressOutbox.scope("restart-fixture", connection)
        val outbox = PlaybackProgressOutbox(context)
        val marker = context.getSharedPreferences("progress_restart_fixture", 0)
        if (phase == "seed") {
            outbox.forget(scope, "restart-film")
            val entry = PendingPlaybackProgress("restart-checkpoint", scope, "restart-film", 42_000, false,
                PlaybackUserState(0, false, 0), System.currentTimeMillis(), "restart-journal")
            outbox.put(entry)
            check(marker.edit().putInt("pid", Process.myPid()).commit())
            val disconnected = object : PlaybackProgressRemote {
                override fun read(connection: ServiceConnection, itemId: String): RemotePlaybackProgress = throw java.io.IOException()
                override fun write(connection: ServiceConnection, entry: PendingPlaybackProgress, remote: RemotePlaybackProgress): Boolean = error("Offline")
            }
            assertEquals(PlaybackSyncResult.RETRY, PlaybackProgressSync(outbox, disconnected).sync(connection, entry, { true }) { fail() })
            assertTrue(outbox.current(entry))
        } else {
            assertTrue("Seed must run first", marker.contains("pid"))
            assertNotEquals("Must be a new app process", marker.getInt("pid", -1), Process.myPid())
            val entry = outbox.pending(scope).single()
            var state = entry.baseline
            var writes = 0
            val recovered = object : PlaybackProgressRemote {
                override fun read(connection: ServiceConnection, itemId: String) = RemotePlaybackProgress(state, JsonObject(emptyMap()))
                override fun write(connection: ServiceConnection, entry: PendingPlaybackProgress, remote: RemotePlaybackProgress): Boolean {
                    writes++; state = state.copy(positionTicks = entry.positionTicks); return true
                }
            }
            assertEquals(PlaybackSyncResult.SAVED, PlaybackProgressSync(outbox, recovered).sync(connection, entry, { true }) {
                assertEquals(420_000_000L, it.positionTicks)
            })
            assertEquals(1, writes)
            assertTrue(outbox.pending(scope).isEmpty())
            marker.edit().clear().commit()
        }
    }
}
