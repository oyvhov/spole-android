package app.reelstack.launcher

import android.content.Intent
import app.reelstack.data.model.UserProfile
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.data.repository.SessionScope

/** A complete action owned by the session that received it, including while Compose starts. */
internal class PendingLauncherOpen private constructor(
    private val link: LauncherLink,
    private val owner: SessionScope,
    private val switchProfileId: String?,
) {
    private var consumed = false

    fun consume(repository: ConnectionRepository, selectProfile: (UserProfile) -> Unit,
        open: (LauncherLink, LauncherTarget.Open) -> Unit): LauncherTarget? {
        if (consumed) return null
        consumed = true
        if (!repository.isCurrent(owner)) return null
        var target = repository.resolve(link)
        if (target is LauncherTarget.SwitchToKid) {
            // MainActivity is exported: a raw profile extra is never authority to switch accounts.
            if (switchProfileId != target.profile.id || owner.kidsMode) return null
            val profile = target.profile
            selectProfile(profile)
            if (repository.activeProfileId != profile.id) return null
            target = repository.resolve(link)
        }
        if (target is LauncherTarget.Open) open(link, target)
        return target
    }

    companion object {
        fun from(intent: Intent, repository: ConnectionRepository): PendingLauncherOpen? {
            // NEW_TASK may resume a task whose original base intent was the short-lived
            // receiver. Android can then deliver that raw VIEW intent to MainActivity.
            // Resolve it with the same parser and account rules as the receiver, rather
            // than requiring the internal forwarding extras to have survived task reuse.
            if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
                val link = LauncherLinks.parse(intent.dataString) ?: return null
                val switchProfile = (repository.resolve(link) as? LauncherTarget.SwitchToKid)?.profile?.id
                return PendingLauncherOpen(link, repository.captureSession(), switchProfile)
            }
            val kind = intent.getStringExtra("launcher_kind") ?: return null
            val user = intent.getStringExtra("launcher_user") ?: return null
            val item = intent.getStringExtra("launcher_item") ?: return null
            val action = intent.getStringExtra("launcher_action") ?: return null
            // Reuse the strict external parser; all four fields are mandatory at this boundary too.
            if (kind !in setOf("JELLYFIN", "EMBY") || action !in setOf("PLAY", "DETAILS")) return null
            val link = LauncherLinks.parse("spole://open?kind=${kind.lowercase(java.util.Locale.ROOT)}&user=$user&item=$item&action=${action.lowercase(java.util.Locale.ROOT)}")
                ?: return null
            // Compare the parsed values as well: query injection in an exported intent is invalid.
            if (link.userId != user || link.itemId != item) return null
            return PendingLauncherOpen(link, repository.captureSession(), intent.getStringExtra("launcher_switch_profile"))
        }
    }
}

internal fun ConnectionRepository.resolve(link: LauncherLink): LauncherTarget {
    val kids = listProfiles().filter { it.isKid }
    return resolveLauncherTarget(link, list(""), activeProfileId, kids, kids.associate { it.id to list(it.id) })
}
