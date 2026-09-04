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

    fun list(): List<ServiceConnection> = ServiceKind.entries.map(::get)

    fun get(kind: ServiceKind): ServiceConnection {
        val prefix = kind.name.lowercase()
        val savedUrl = preferences.getString("$prefix.url", null)
        return ServiceConnection(
            kind = kind,
            name = preferences.getString("$prefix.name", null) ?: defaultName(kind),
            baseUrl = savedUrl.orEmpty(),
            token = tokenStore.get("$prefix.token").orEmpty(),
            userId = preferences.getString("$prefix.user_id", null).orEmpty(),
            state = if (savedUrl.isNullOrBlank()) ConnectionState.DEMO else ConnectionState.CONNECTED,
            detail = if (savedUrl.isNullOrBlank()) "Demo data" else "Configured",
        )
    }

    fun save(connection: ServiceConnection) {
        val prefix = connection.kind.name.lowercase()
        preferences.edit {
            putString("$prefix.name", connection.name.trim())
            putString("$prefix.url", EndpointValidatorFacade.normalize(connection.baseUrl))
            putString("$prefix.user_id", connection.userId.trim())
        }
        tokenStore.put("$prefix.token", connection.token)
    }

    fun delete(kind: ServiceKind) {
        val prefix = kind.name.lowercase()
        preferences.edit {
            remove("$prefix.name")
            remove("$prefix.url")
            remove("$prefix.user_id")
        }
        tokenStore.remove("$prefix.token")
    }

    private fun defaultName(kind: ServiceKind): String = when (kind) {
        ServiceKind.JELLYFIN -> "Home server"
        ServiceKind.EMBY -> "Cabin server"
        else -> kind.displayName
    }
}

private object EndpointValidatorFacade {
    fun normalize(value: String): String = app.reelstack.data.network.EndpointValidator.normalizeBaseUrl(value)
}
