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
            val action = intent.getStringExtra("launcher_action") ?: return null
            if (kind !in setOf("JELLYFIN", "EMBY") || action !in setOf("PLAY", "DETAILS", "LIBRARY")) return null
            val library = action == "LIBRARY"
            val id = intent.getStringExtra(if (library) "launcher_library" else "launcher_item") ?: return null
            // Reuse the external parser at this exported boundary, including its strict id checks.
            val host = if (library) "library" else "open"
            val parameter = if (library) "library" else "item"
            val actionQuery = if (library) "" else "&action=${action.lowercase(java.util.Locale.ROOT)}"
            val link = LauncherLinks.parse("spole://$host?kind=${kind.lowercase(java.util.Locale.ROOT)}&user=$user&$parameter=$id$actionQuery")
                ?: return null
            // Compare the parsed values as well: query injection in an exported intent is invalid.
            if (link.userId != user || link.itemId != id || link.action.name != action) return null
            return PendingLauncherOpen(link, repository.captureSession(), intent.getStringExtra("launcher_switch_profile"))
        }
    }
}

internal fun ConnectionRepository.resolve(link: LauncherLink): LauncherTarget {
    val kids = listProfiles().filter { it.isKid }
    return resolveLauncherTarget(link, list(""), activeProfileId, kids, kids.associate { it.id to list(it.id) })
}
