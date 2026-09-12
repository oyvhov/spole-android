package app.reelstack.ui.components

import android.content.Context
import app.reelstack.ReelstackApplication
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.jellyfinAuthorization
import app.reelstack.data.repository.DeviceIdentity
import coil3.network.NetworkHeaders

/**
 * The media server's authorisation header, built once per sign-in instead of once per poster.
 *
 * Every artwork request used to read the connection out of preferences, hash a device id and build
 * an authorisation string of its own. That is cheap in isolation and not cheap at all when a
 * library page fills with sixty covers at once, each one doing it again while the list scrolls.
 *
 * The cache is keyed on the connection itself, so signing out, switching server or rotating a token
 * produces a different key and the old header is never reused. Nothing is invalidated by hand,
 * which is the point: there is no state here that can drift out of step with the connection.
 */
internal object MediaAuthHeaders {

    private data class Key(val kind: ServiceKind, val baseUrl: String, val token: String)

    @Volatile
    private var cached: Pair<Key, NetworkHeaders>? = null

    /**
     * Headers for [url], or null when it does not belong to a signed-in media server — an artwork
     * address from Seerr or a bundled drawable must not carry someone's Jellyfin token.
     */
    fun forUrl(context: Context, source: ServiceKind?, url: String): NetworkHeaders? {
        if (source != ServiceKind.JELLYFIN && source != ServiceKind.EMBY) return null
        val connection = (context.applicationContext as? ReelstackApplication)
            ?.container?.connectionRepository?.get(source) ?: return null
        if (connection.token.isBlank()) return null
        if (!url.startsWith("${connection.baseUrl.trimEnd('/')}/")) return null

        val key = Key(source, connection.baseUrl, connection.token)
        cached?.let { (cachedKey, headers) -> if (cachedKey == key) return headers }
        val headers = NetworkHeaders.Builder().apply {
            if (source == ServiceKind.JELLYFIN) {
                set("Authorization", jellyfinAuthorization(DeviceIdentity.get(context), connection.token))
            } else {
                set("X-Emby-Token", connection.token)
            }
        }.build()
        cached = key to headers
        return headers
    }
}
