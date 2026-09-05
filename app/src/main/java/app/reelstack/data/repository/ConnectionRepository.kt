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
        preferences.edit { putString("${kind.name.lowercase()}.last_url", url) }
        delete(kind)
    }

    fun get(kind: ServiceKind): ServiceConnection {
        val prefix = kind.name.lowercase()
        val savedUrl = preferences.getString("$prefix.url", null)
        return ServiceConnection(
            kind = kind,
            name = preferences.getString("$prefix.name", null) ?: defaultName(kind),
            baseUrl = savedUrl.orEmpty(),
            token = tokenFor(kind),
            userId = preferences.getString("$prefix.user_id", null).orEmpty(),
            sessionCookie = preferences.getBoolean("$prefix.session_cookie", false),
            state = if (savedUrl.isNullOrBlank()) ConnectionState.DEMO else ConnectionState.CONNECTED,
            detail = if (savedUrl.isNullOrBlank()) "Demodata" else "Konfigurert",
        )
    }

    fun save(connection: ServiceConnection) {
        val prefix = connection.kind.name.lowercase()
        preferences.edit {
            putString("$prefix.name", connection.name.trim())
            putString("$prefix.url", EndpointValidatorFacade.normalize(connection.baseUrl))
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
            remove("$prefix.user_id")
            remove("$prefix.session_cookie")
        }
        tokenStore.remove("$prefix.token")
        synchronized(tokenCache) {
            tokenCache.remove(kind)
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
