package app.reelstack.launcher

import android.app.Application
import android.content.Intent
import android.net.Uri
import app.reelstack.MainActivity
import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.data.security.TokenStore
import app.reelstack.player.JellyfinPlayerActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LauncherOpenActivityTest {
    private lateinit var repository: ConnectionRepository

    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication()
        repository = ConnectionRepository(context, context.getSharedPreferences("launcher-test", 0).apply { edit().clear().commit() }, MemoryTokens())
        TestActivity.repositoryOverride = repository
        repository.save(connection("adult"), "")
        repository.registerKidProfile("kid", "Barn", null)
        repository.save(connection("child-user"), "kid")
    }

    @Test fun playOpensPlayerWithRemoteIdAndNoDuplicatePlayerStack() {
        val activity = launch("adult", "play")
        val next = shadowOf(activity).nextStartedActivity
        assertEquals(JellyfinPlayerActivity::class.java.name, next.component?.className)
        assertEquals("abc", next.getStringExtra("jellyfin_item_id"))
        assertEquals("JELLYFIN", next.getStringExtra("media_service_kind"))
        assertEquals(Intent.FLAG_ACTIVITY_CLEAR_TOP, next.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP)
        assertEquals(0, next.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP)
        assertTrue(activity.isFinishing)
    }

    @Test fun detailsForwardsCompleteActionAndMainTaskFlags() {
        val activity = launch("adult", "details")
        val next = shadowOf(activity).nextStartedActivity
        assertEquals(MainActivity::class.java.name, next.component?.className)
        assertEquals("JELLYFIN", next.getStringExtra("launcher_kind"))
        assertEquals("abc", next.getStringExtra("launcher_item"))
        assertEquals("adult", next.getStringExtra("launcher_user"))
        assertEquals("DETAILS", next.getStringExtra("launcher_action"))
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP, next.flags)
        assertTrue(activity.isFinishing)
    }

    @Test fun invalidLinkFinishesWithoutStartingAnything() {
        val activity = Robolectric.buildActivity(TestActivity::class.java, Intent(Intent.ACTION_VIEW, Uri.parse("spole://open?item=abc"))).create().get()
        assertNull(shadowOf(activity).nextStartedActivity)
        assertTrue(activity.isFinishing)
    }

    @Test fun unknownUserShowsLocalizedMessageInMain() {
        val activity = launch("unknown", "details")
        assertEquals(activity.getString(R.string.launcher_not_signed_in), shadowOf(activity).nextStartedActivity.getStringExtra("launcher_message"))
    }

    @Test fun adultLinkIsBlockedInKidMode() {
        repository.activeProfileId = "kid"
        val activity = launch("adult", "play")
        val next = shadowOf(activity).nextStartedActivity
        assertEquals(activity.getString(R.string.launcher_kid_mode), next.getStringExtra("launcher_message"))
        assertFalse(next.hasExtra("launcher_item"))
        assertEquals("kid", repository.activeProfileId)
    }

    @Test fun childSwitchForwardsRequestedPlayWithoutSwitchingInTrampoline() {
        val activity = launch("child-user", "play")
        val next = shadowOf(activity).nextStartedActivity
        assertEquals("kid", next.getStringExtra("launcher_switch_profile"))
        assertEquals("child-user", next.getStringExtra("launcher_user"))
        assertEquals("PLAY", next.getStringExtra("launcher_action"))
        assertEquals("", repository.activeProfileId)
    }

    @Test fun ownChildPlaybackUsesKidPlayer() {
        repository.activeProfileId = "kid"
        val next = shadowOf(launch("child-user", "play")).nextStartedActivity
        assertTrue(next.getBooleanExtra("kids_mode", false))
    }

    private fun launch(user: String, action: String) = Robolectric.buildActivity(TestActivity::class.java,
        Intent(Intent.ACTION_VIEW, Uri.parse("spole://open?kind=jellyfin&user=$user&item=abc&action=$action"))).create().get()

    class TestActivity : LauncherOpenActivity() {
        override val connectionRepository get() = repositoryOverride
        companion object { lateinit var repositoryOverride: ConnectionRepository }
    }

    private fun connection(user: String) = ServiceConnection(ServiceKind.JELLYFIN, "Test", "http://127.0.0.1:9", "fake-token", userId = user)
}

internal class MemoryTokens : TokenStore {
    private val values = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { values[key] = value }
    override fun get(key: String) = values[key]
    override fun remove(key: String) { values.remove(key) }
    override fun hasStoredValue(key: String) = values.containsKey(key)
}
