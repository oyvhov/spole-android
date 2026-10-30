package app.reelstack.launcher

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import app.reelstack.data.network.EndpointValidator

/**
 * Stova, the TV launcher, shows a Jellyfin Quick Connect code and asks Spole to approve it as one of
 * the accounts Spole is signed in with on that server. Stova then gets its own sign-in from the
 * server: no token, password or address book ever leaves Spole. The names below are the contract
 * with Stova (its plan `2026-10-30-spole-bridge.md`) and change only together with it.
 */
object LauncherApproval {
    const val ACTION = "app.reelstack.action.APPROVE_SIGN_IN"
    const val EXTRA_SERVER_URL = "app.reelstack.extra.SERVER_URL"
    const val EXTRA_SERVER_ID = "app.reelstack.extra.SERVER_ID"
    const val EXTRA_CODE = "app.reelstack.extra.CODE"
    const val EXTRA_FOR_CHILD = "app.reelstack.extra.FOR_CHILD"
    const val EXTRA_ERROR = "app.reelstack.extra.ERROR"
    const val ERROR_NOT_SIGNED_IN = "not_signed_in"
    const val ERROR_DECLINED = "declined"
    const val ERROR_FAILED = "failed"

    /** The launchers Spole answers: Stova's release and debug builds. */
    val LAUNCHERS = setOf("app.stova", "app.stova.debug")
    /** Jellyfin's Quick Connect codes are six digits; a little room either way for other versions. */
    private val code = Regex("[0-9]{4,10}")
    private val serverId = Regex("[A-Za-z0-9-]{1,64}")

    data class Request(val launcher: String, val serverUrl: String, val serverId: String, val code: String, val forChild: Boolean)
    data class Choice(val profile: UserProfile, val connection: ServiceConnection)

    /**
     * The request, or null when Spole must not answer it. [caller] is the package Android names for an
     * activity started for a result; without one Spole cannot tell who asks.
     */
    fun request(caller: String?, serverUrl: String?, serverId: String?, code: String?, forChild: Boolean): Request? {
        if (caller !in LAUNCHERS) return null
        val digits = code?.trim()?.takeIf(this.code::matches) ?: return null
        val url = serverUrl?.let { runCatching { EndpointValidator.normalizeBaseUrl(it) }.getOrNull() } ?: return null
        return Request(caller!!, url, serverId?.takeIf(this.serverId::matches).orEmpty(), digits, forChild)
    }

    /**
     * Spole's Jellyfin accounts on the server Stova shows, across the main profile and the children's:
     * by address (any of the routes Spole knows) or, when the addresses differ, by the server's own id,
     * which [serverIdOf] reads from `System/Info/Public`. A child's accounts come first for a child.
     */
    fun choices(
        request: Request,
        profiles: List<UserProfile>,
        connectionsOf: (String) -> List<ServiceConnection>,
        serverIdOf: (String) -> String?,
    ): List<Choice> {
        val signedIn = profiles.flatMap { profile ->
            connectionsOf(profile.id).filter { it.kind == ServiceKind.JELLYFIN && it.token.isNotBlank() }.map { Choice(profile, it) }
        }
        fun routes(c: ServiceConnection) = listOf(c.baseUrl, c.alternateUrl, c.identityUrl).filter(String::isNotBlank)
            .mapNotNull { runCatching { EndpointValidator.normalizeBaseUrl(it) }.getOrNull() }
        val ids = mutableMapOf<String, String?>()
        val same = signedIn.filter { choice ->
            request.serverUrl in routes(choice.connection) || request.serverId.isNotEmpty() &&
                ids.getOrPut(choice.connection.baseUrl) { serverIdOf(choice.connection.baseUrl) } == request.serverId
        }
        return same.sortedBy { if (it.profile.isKid == request.forChild) 0 else 1 }
    }
}
