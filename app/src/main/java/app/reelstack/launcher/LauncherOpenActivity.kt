package app.reelstack.launcher

import android.os.Bundle
import android.content.Intent
import app.reelstack.MainActivity
import app.reelstack.R
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
                        source = link.kind, kidsMode = connectionRepository.isKidMode)
                    else startActivity(mainIntent().withLauncherLink(link))
                }
                is LauncherTarget.SwitchToKid -> startActivity(mainIntent().withLauncherLink(link)
                    .putExtra("launcher_switch_profile", target.profile.id))
                LauncherTarget.BlockedInKidMode -> startActivity(mainIntent().putExtra("launcher_message", getString(R.string.launcher_kid_mode)))
                LauncherTarget.NotSignedIn -> startActivity(mainIntent().putExtra("launcher_message", getString(R.string.launcher_not_signed_in)))
            }
        }
        finish()
    }

    private fun mainIntent() = Intent(this, MainActivity::class.java).addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private fun Intent.withLauncherLink(link: LauncherLink): Intent =
        putExtra("launcher_kind", link.kind.name).putExtra("launcher_item", link.itemId)
            .putExtra("launcher_user", link.userId).putExtra("launcher_action", link.action.name)
}
