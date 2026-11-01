package app.reelstack.launcher

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Stova's cinema mode hears when a film plays, pauses and stops; nothing else crosses. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LauncherPlaybackTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before fun fresh() { LauncherPlayback.reset() }

    @Test fun eachChangeReachesEveryLauncherOnceWithOnlyTheState() {
        LauncherPlayback.send(context, LauncherPlayback.State.PLAYING)
        LauncherPlayback.send(context, LauncherPlayback.State.PLAYING)
        LauncherPlayback.send(context, LauncherPlayback.State.PAUSED)
        val sent = shadowOf(context).broadcastIntents.filter { it.action == LauncherPlayback.ACTION }
        assertEquals(LauncherApproval.LAUNCHERS.size * 2, sent.size)
        assertEquals(LauncherApproval.LAUNCHERS.toSet(), sent.mapNotNull { it.`package` }.toSet())
        assertEquals(listOf("playing", "paused"), sent.map { it.getStringExtra("state") }.distinct())
        assertEquals(setOf("state"), sent.flatMap { it.extras!!.keySet() }.toSet())
    }
}
