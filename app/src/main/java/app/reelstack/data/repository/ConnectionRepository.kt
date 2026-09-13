package app.reelstack.data.repository

import android.content.Context
import androidx.core.content.edit
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.security.EncryptedTokenStore

class ConnectionRepository(context: Context) {
    private val preferences = context.getSharedPreferences("reelstack_connections", Context.MODE_PRIVATE)
    private val tokenStore = EncryptedTokenStore(context)
    private val tokenCache = mutableMapOf<ServiceKind, String>()

    fun list(): List<ServiceConnection> = ServiceKind.entries.map(::get)

    fun rememberedUrl(kind: ServiceKind): String = preferences.getString("${kind.name.lowercase()}.last_url", "").orEmpty()

    fun signOut(kind: ServiceKind) {
        val url = get(kind).baseUrl
        if (url.isNotBlank()) preferences.edit { putString("${kind.name.lowercase()}.last_url", url) }
        delete(kind)
    }

    /** Local sign-out, including services hidden by the current account's permissions. */
    fun signOutAll() = ServiceKind.entries.forEach(::signOut)

    fun get(kind: ServiceKind): ServiceConnection {
        val prefix = kind.name.lowercase()
        val savedUrl = preferences.getString("$prefix.url", null)
        val token = tokenFor(kind)
        // An address with no readable token, where a token was nevertheless written, means the
        // Keystore entry is gone. Reporting that as "Konfigurert" sent the user to a home screen
        // full of demo content with nothing anywhere saying why.
        val unreadable = !savedUrl.isNullOrBlank() && token.isBlank() &&
            tokenStore.hasStoredValue("$prefix.token")
        return ServiceConnection(
            kind = kind,
            name = preferences.getString("$prefix.name", null) ?: defaultName(kind),
            baseUrl = savedUrl.orEmpty(),
            token = token,
            userId = preferences.getString("$prefix.user_id", null).orEmpty(),
            sessionCookie = preferences.getBoolean("$prefix.session_cookie", false),
            alternateUrl = preferences.getString("$prefix.alt_url", null).orEmpty(),
            // Older installations have no identity key; their address is the identity.
            identityUrl = preferences.getString("$prefix.identity_url", null) ?: savedUrl.orEmpty(),
            state = when {
                savedUrl.isNullOrBlank() -> ConnectionState.DEMO
                unreadable -> ConnectionState.ERROR
                else -> ConnectionState.CONNECTED
            },
            detail = when {
                savedUrl.isNullOrBlank() -> "Demodata"
                unreadable -> "Innlogginga kan ikkje lesast på denne eininga · Logg inn på nytt"
                else -> "Konfigurert"
            },
        )
    }

    fun save(connection: ServiceConnection) {
        val prefix = connection.kind.name.lowercase()
        val normalized = EndpointValidatorFacade.normalize(connection.baseUrl)
        val alternate = connection.alternateUrl.takeIf(String::isNotBlank)
            ?.let(EndpointValidatorFacade::normalize)
        preferences.edit {
            putString("$prefix.name", connection.name.trim())
            putString("$prefix.url", normalized)
            if (alternate == null || alternate == normalized) remove("$prefix.alt_url")
            else putString("$prefix.alt_url", alternate)
            // Written once. A later failover changes the address, never the identity.
            if (!preferences.contains("$prefix.identity_url")) {
                putString("$prefix.identity_url", connection.identityUrl.takeIf(String::isNotBlank) ?: normalized)
            }
            putString("$prefix.user_id", connection.userId.trim())
            putBoolean("$prefix.session_cookie", connection.sessionCookie)
        }
        tokenStore.put("$prefix.token", connection.token)
        synchronized(tokenCache) {
            tokenCache[connection.kind] = connection.token
        }
    }

    fun delete(kind: ServiceKind) {
        val prefix = kind.name.lowercase()
        preferences.edit {
            remove("$prefix.name")
            remove("$prefix.url")
            remove("$prefix.alt_url")
            remove("$prefix.identity_url")
            remove("$prefix.user_id")
            remove("$prefix.session_cookie")
        }
        tokenStore.remove("$prefix.token")
        synchronized(tokenCache) {
            tokenCache.remove(kind)
        }
    }

    /**
     * Makes the alternate address the active one after a successful failover. Only the two
     * addresses swap: token, profile and identity are untouched.
     */
    fun promoteAlternate(kind: ServiceKind) {
        val current = get(kind)
        if (!current.hasAlternate) return
        val prefix = kind.name.lowercase()
        preferences.edit {
            putString("$prefix.url", current.alternateUrl)
            putString("$prefix.alt_url", current.baseUrl)
        }
    }

    private fun tokenFor(kind: ServiceKind): String = synchronized(tokenCache) {
        tokenCache.getOrPut(kind) {
            tokenStore.get("${kind.name.lowercase()}.token").orEmpty()
        }
    }

    private fun defaultName(kind: ServiceKind): String = when (kind) {
        ServiceKind.JELLYFIN -> "Heimetenar"
        ServiceKind.EMBY -> "Hyttetenar"
        else -> kind.displayName
    }
}

private object EndpointValidatorFacade {
    fun normalize(value: String): String = app.reelstack.data.network.EndpointValidator.normalizeBaseUrl(value)
}
