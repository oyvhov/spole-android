package app.reelstack.data.model

/** Server-verified authority, never a role selected in the app or inferred from a username. */
data class ViewerAccess(val seerrConfigured: Boolean, val accounts: Map<ServiceKind, ServiceAccount>) {
    val isAdmin: Boolean get() = accounts[ServiceKind.SEERR]?.isAdmin == true

    fun canSeeAllSessions(source: ServiceKind): Boolean =
        isAdmin && accounts[source]?.let { it.isPersonal && it.isAdmin } == true

    fun ownMediaUser(source: ServiceKind): String? {
        val account = accounts[source]?.takeIf { it.isPersonal } ?: return null
        val seerr = accounts[ServiceKind.SEERR]
        if (seerrConfigured && seerr == null) return null
        if (seerrConfigured && !isAdmin && source == ServiceKind.JELLYFIN &&
            seerr?.mediaUserId != null && seerr.mediaUserId != account.id) return null
        // A shared administrator connection must never masquerade as an ordinary Seerr user's account.
        if (seerrConfigured && !isAdmin && account.isAdmin && seerr?.mediaUserId != account.id) return null
        return account.id
    }
}

fun isExcludedHomeLibrary(name: String): Boolean = name.lowercase(java.util.Locale.ROOT)
    .filter(Char::isLetterOrDigit) in setOf("barneserier", "barneseriar", "barnetv", "barnetvserier", "barnetvseriar")
