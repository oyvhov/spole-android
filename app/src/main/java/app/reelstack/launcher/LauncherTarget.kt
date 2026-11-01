package app.reelstack.launcher

import androidx.annotation.StringRes
import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.UserProfile

sealed interface LauncherTarget {
    data class Open(val connection: ServiceConnection) : LauncherTarget
    /** A child's profile, an own grown-up one, or the main profile (id ""). */
    data class SwitchToProfile(val profile: UserProfile) : LauncherTarget
    data object BlockedInKidMode : LauncherTarget
    data object NotSignedIn : LauncherTarget
}

private val MAIN = UserProfile(id = "", name = "", isKid = false)

/**
 * The sentence for a link Spole does not open, or null when it opens. [LauncherTarget.BlockedInKidMode]
 * means kid mode, except a Discover link for a child's account while Spole is not in kid mode: only
 * Discover refuses that one, and saying «kid mode» there would be false.
 */
@StringRes
fun LauncherTarget.refusal(link: LauncherLink, kidMode: Boolean): Int? = when (this) {
    LauncherTarget.BlockedInKidMode ->
        if (link.action == LauncherAction.DISCOVER && !kidMode) R.string.launcher_discover_grown_ups else R.string.launcher_kid_mode
    LauncherTarget.NotSignedIn -> R.string.launcher_not_signed_in
    is LauncherTarget.Open, is LauncherTarget.SwitchToProfile -> null
}

/**
 * [connections] belongs to the main profile (explicitly `list("")` in the repository).
 * [profiles] are the other profiles, children's and own grown-up ones; [profileConnections] holds
 * each one's own connections, keyed by the local profile ID.
 */
fun resolveLauncherTarget(
    link: LauncherLink,
    connections: List<ServiceConnection>,
    activeProfileId: String,
    profiles: List<UserProfile>,
    profileConnections: Map<String, List<ServiceConnection>> = emptyMap(),
): LauncherTarget {
    val active = profiles.firstOrNull { it.id == activeProfileId }
    // A profile Spole cannot place is treated as a child's.
    val isKidMode = activeProfileId.isNotBlank() && active?.isKid != false
    fun ServiceConnection.isLinked() = kind == link.kind && userId == link.userId && token.isNotEmpty()
    fun ownOf(profile: UserProfile) = profileConnections[profile.id]?.firstOrNull { it.isLinked() }

    if (link.action == LauncherAction.PROFILE) {
        // An own or child profile with the account wins over the main profile, which has it too.
        val owner = profiles.sortedBy { it.isKid }.firstOrNull { ownOf(it) != null }
        val mainAccount = connections.firstOrNull { it.isLinked() }
        val target = owner ?: mainAccount?.let { MAIN } ?: return LauncherTarget.NotSignedIn
        if (target.id == activeProfileId) return LauncherTarget.Open(owner?.let(::ownOf) ?: mainAccount!!)
        if (isKidMode) return LauncherTarget.BlockedInKidMode
        return LauncherTarget.SwitchToProfile(target)
    }

    val main = connections.firstOrNull { it.kind == link.kind && it.token.isNotEmpty() }
        ?: return LauncherTarget.NotSignedIn

    if (link.userId == main.userId) {
        if (isKidMode) return LauncherTarget.BlockedInKidMode
        // An own profile opens the main user's title with its own sign-in when it has the account,
        // and otherwise hands back to the main profile first.
        if (active != null) return ownOf(active)?.let { LauncherTarget.Open(it) } ?: LauncherTarget.SwitchToProfile(MAIN)
        return LauncherTarget.Open(main)
    }

    val owner = profiles.firstOrNull { profile ->
        profileConnections[profile.id]?.any { it.kind == link.kind && it.userId == link.userId }
            ?: (profile.isKid && profile.id == link.userId)
    } ?: return LauncherTarget.NotSignedIn

    // Discover is for grown-ups: a child's account never opens it, and neither does kid mode.
    if (link.action == LauncherAction.DISCOVER && (isKidMode || owner.isKid)) return LauncherTarget.BlockedInKidMode
    if (!isKidMode) {
        if (owner.id != activeProfileId) return LauncherTarget.SwitchToProfile(owner)
        return ownOf(owner)?.let { LauncherTarget.Open(it) } ?: LauncherTarget.NotSignedIn
    }
    if (owner.id != activeProfileId) return LauncherTarget.BlockedInKidMode
    return ownOf(owner)?.let { LauncherTarget.Open(it) } ?: LauncherTarget.NotSignedIn
}
