package app.reelstack.launcher

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.UserProfile

sealed interface LauncherTarget {
    data class Open(val connection: ServiceConnection) : LauncherTarget
    data class SwitchToKid(val profile: UserProfile) : LauncherTarget
    data object BlockedInKidMode : LauncherTarget
    data object NotSignedIn : LauncherTarget
}

/**
 * [connections] belongs to the main profile (explicitly `list("")` in the repository).
 * [profileConnections] holds each child's own connections, keyed by the local profile ID.
 */
fun resolveLauncherTarget(
    link: LauncherLink,
    connections: List<ServiceConnection>,
    activeProfileId: String,
    kidProfiles: List<UserProfile>,
    profileConnections: Map<String, List<ServiceConnection>> = emptyMap(),
): LauncherTarget {
    val main = connections.firstOrNull { it.kind == link.kind && it.token.isNotEmpty() }
        ?: return LauncherTarget.NotSignedIn
    val isKidMode = activeProfileId.isNotBlank()

    if (link.userId == main.userId) {
        return if (isKidMode) LauncherTarget.BlockedInKidMode else LauncherTarget.Open(main)
    }

    val kid = kidProfiles.firstOrNull { profile ->
        profile.isKid && (profileConnections[profile.id]?.any {
            it.kind == link.kind && it.userId == link.userId
        } ?: (profile.id == link.userId))
    } ?: return LauncherTarget.NotSignedIn

    if (!isKidMode) return LauncherTarget.SwitchToKid(kid)
    if (kid.id != activeProfileId) return LauncherTarget.BlockedInKidMode

    val childConnection = profileConnections[kid.id]?.firstOrNull {
        it.kind == link.kind && it.userId == link.userId && it.token.isNotEmpty()
    } ?: return LauncherTarget.NotSignedIn
    return LauncherTarget.Open(childConnection)
}
