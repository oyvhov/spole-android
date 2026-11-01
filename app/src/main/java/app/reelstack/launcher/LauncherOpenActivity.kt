package app.reelstack.launcher

import android.os.Bundle
import android.content.Intent
import app.reelstack.MainActivity
import app.reelstack.ReelstackApplication
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.player.JellyfinPlayerActivity

open class LauncherOpenActivity : app.reelstack.localization.LocalizedActivity() {
    protected open val connectionRepository: ConnectionRepository
        get() = (application as ReelstackApplication).container.connectionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatchLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchLink(intent)
    }

    private fun dispatchLink(intent: Intent) {
        val link = LauncherLinks.parse(intent.dataString)
        if (link != null) {
            when (val target = connectionRepository.resolve(link)) {
                is LauncherTarget.Open -> {
                    if (link.action == LauncherAction.PLAY) JellyfinPlayerActivity.open(this, link.itemId,
                        source = link.kind, kidsMode = connectionRepository.isKidMode, fromStart = link.fromStart)
                    else startActivity(mainIntent().withLauncherLink(link))
                }
                is LauncherTarget.SwitchToProfile -> startActivity(mainIntent().withLauncherLink(link)
                    .putExtra("launcher_switch_profile", target.profile.id))
                LauncherTarget.BlockedInKidMode, LauncherTarget.NotSignedIn -> target.refusal(link, connectionRepository.isKidMode)
                    ?.let { startActivity(mainIntent().putExtra("launcher_message", getString(it))) }
            }
        }
        finish()
    }

    private fun mainIntent() = Intent(this, MainActivity::class.java).addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private fun Intent.withLauncherLink(link: LauncherLink): Intent =
        putExtra("launcher_kind", link.kind.name)
            .apply { when (link.action) {
                LauncherAction.DETAILS, LauncherAction.PLAY -> putExtra("launcher_item", link.itemId)
                LauncherAction.LIBRARY -> putExtra("launcher_library", link.itemId)
                LauncherAction.DISCOVER -> putExtra("launcher_discover", link.discover)
                LauncherAction.PROFILE -> Unit
            } }
            .putExtra("launcher_user", link.userId).putExtra("launcher_action", link.action.name)
            .apply { if (link.fromStart) putExtra("launcher_start", true) }
}
