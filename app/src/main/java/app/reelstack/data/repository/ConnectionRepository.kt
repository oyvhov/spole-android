package app.reelstack.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import app.reelstack.R
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import app.reelstack.data.security.EncryptedTokenStore
import app.reelstack.data.security.TokenStore
import kotlinx.coroutines.flow.asStateFlow

class ConnectionRepository(
    context: Context,
    private val preferences: SharedPreferences = context.getSharedPreferences("reelstack_connections", Context.MODE_PRIVATE),
    private val tokenStore: TokenStore = EncryptedTokenStore(context),
) {
    // The application context: a row label has to be read in the language the app is set to, and
    // this repository outlives whatever happened to construct it.
    private val appContext = context.applicationContext
    private val tokenCache = mutableMapOf<Pair<String, ServiceKind>, String>()
    private val revision = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val changes = revision.asStateFlow()

    var activeProfileId: String
        get() = preferences.getString("active_profile_id", "").orEmpty()
        set(value) {
            val sanitized = value.trim()
            if (activeProfileId == sanitized) return
            preferences.edit { putString("active_profile_id", sanitized) }
            revision.value++
        }

    val isKidMode: Boolean get() = activeProfileId.isNotBlank()

    fun prefixFor(kind: ServiceKind, profileId: String = activeProfileId): String {
        val base = kind.name.lowercase()
        val p = profileId.trim()
        return if (p.isBlank()) base else if (p.startsWith("kid.")) "$p.$base" else "kid.$p.$base"
    }

    fun list(profileId: String = activeProfileId): List<ServiceConnection> =
        ServiceKind.entries.map { get(it, profileId) }

    fun rememberedUrl(kind: ServiceKind): String =
        preferences.getString("${kind.name.lowercase()}.last_url", "").orEmpty()

    fun signOut(kind: ServiceKind, profileId: String = activeProfileId) {
        val url = get(kind, profileId).baseUrl
        if (url.isNotBlank()) preferences.edit { putString("${kind.name.lowercase()}.last_url", url) }
        delete(kind, profileId)
    }

    /** Local sign-out, including services hidden by the current account's permissions. */
    fun signOutAll(profileId: String = activeProfileId) =
        ServiceKind.entries.forEach { signOut(it, profileId) }

    fun get(kind: ServiceKind, profileId: String = activeProfileId): ServiceConnection {
        val prefix = prefixFor(kind, profileId)
        val savedUrl = preferences.getString("$prefix.url", null)
            ?: if (profileId.isNotBlank() && (kind == ServiceKind.JELLYFIN || kind == ServiceKind.EMBY)) {
                preferences.getString("${kind.name.lowercase()}.url", null)
            } else null
        val token = tokenFor(kind, profileId)
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
                savedUrl.isNullOrBlank() -> runCatching { appContext.getString(R.string.connection_detail_demo) }.getOrDefault("Demodata")
                unreadable -> runCatching { appContext.getString(R.string.connection_detail_unreadable) }.getOrDefault("Uleseleg innlogging")
                else -> runCatching { appContext.getString(R.string.connection_detail_configured) }.getOrDefault("Konfigurert")
            },
        )
    }

    fun save(connection: ServiceConnection, profileId: String = activeProfileId) {
        val prefix = prefixFor(connection.kind, profileId)
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
            tokenCache[profileId to connection.kind] = connection.token
        }
        revision.value++
    }

    fun delete(kind: ServiceKind, profileId: String = activeProfileId) {
        val prefix = prefixFor(kind, profileId)
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
            tokenCache.remove(profileId to kind)
        }
        revision.value++
    }

    /**
     * Makes the alternate address the active one after a successful failover. Only the two
     * addresses swap: token, profile and identity are untouched.
     */
    fun promoteAlternate(kind: ServiceKind, profileId: String = activeProfileId) {
        val current = get(kind, profileId)
        if (!current.hasAlternate) return
        val prefix = prefixFor(kind, profileId)
        preferences.edit {
            putString("$prefix.url", current.alternateUrl)
            putString("$prefix.alt_url", current.baseUrl)
        }
    }

    fun getKidProfileIds(): Set<String> =
        preferences.getStringSet("kid_profile_ids", emptySet()).orEmpty()

    fun registerKidProfile(id: String, name: String, avatarUrl: String?) {
        val sanitized = id.trim().removePrefix("kid.")
        val current = getKidProfileIds().toMutableSet()
        current.add(sanitized)
        preferences.edit {
            putStringSet("kid_profile_ids", current)
            putString("kid.$sanitized.name", name.trim())
            if (avatarUrl != null) putString("kid.$sanitized.avatar_url", avatarUrl)
            else remove("kid.$sanitized.avatar_url")
        }
        revision.value++
    }

    fun setMainProfileInfo(name: String, avatarUrl: String?) {
        preferences.edit {
            putString("main_profile_name", name.trim())
            if (avatarUrl != null) putString("main_profile_avatar", avatarUrl)
        }
        revision.value++
    }

    fun deleteProfile(profileId: String) {
        val sanitized = profileId.trim().removePrefix("kid.")
        if (sanitized.isBlank()) return
        ServiceKind.entries.forEach { kind ->
            delete(kind, sanitized)
        }
        val current = getKidProfileIds().toMutableSet()
        current.remove(sanitized)
        preferences.edit {
            putStringSet("kid_profile_ids", current)
            remove("kid.$sanitized.name")
            remove("kid.$sanitized.avatar_url")
        }
        if (activeProfileId == sanitized || activeProfileId == "kid.$sanitized") {
            activeProfileId = ""
        }
        revision.value++
    }

    fun listProfiles(): List<UserProfile> {
        val list = mutableListOf<UserProfile>()
        val mainName = preferences.getString("main_profile_name", null)
            ?: preferences.getString("jellyfin.name", null)
            ?: preferences.getString("emby.name", null)
            ?: runCatching { appContext.getString(R.string.profile_main) }.getOrNull()
            ?: "Hovudkonto"
        val mainAvatar = preferences.getString("main_profile_avatar", null)
            ?: runCatching {
                val embyUrl = preferences.getString("emby.url", null)
                val embyUser = preferences.getString("emby.user_id", null)
                if (!embyUrl.isNullOrBlank() && !embyUser.isNullOrBlank()) {
                    "${embyUrl.trimEnd('/')}/Users/$embyUser/Images/Primary"
                } else {
                    val jfUrl = preferences.getString("jellyfin.url", null)
                    val jfUser = preferences.getString("jellyfin.user_id", null)
                    if (!jfUrl.isNullOrBlank() && !jfUser.isNullOrBlank()) {
                        "${jfUrl.trimEnd('/')}/Users/$jfUser/Images/Primary"
                    } else null
                }
            }.getOrNull()
        list.add(UserProfile(id = "", name = mainName, isKid = false, avatarUrl = mainAvatar))

        val kidIds = getKidProfileIds()
        for (kidId in kidIds) {
            val name = preferences.getString("kid.$kidId.name", null) ?: "Barn"
            val avatar = preferences.getString("kid.$kidId.avatar_url", null)
            list.add(UserProfile(id = kidId, name = name, isKid = true, avatarUrl = avatar))
        }
        return list
    }

    private fun tokenFor(kind: ServiceKind, profileId: String = activeProfileId): String = synchronized(tokenCache) {
        tokenCache.getOrPut(profileId to kind) {
            val prefix = prefixFor(kind, profileId)
            tokenStore.get("$prefix.token").orEmpty()
        }
    }

    private fun defaultName(kind: ServiceKind): String = when (kind) {
        ServiceKind.JELLYFIN -> runCatching { appContext.getString(R.string.server_default_name_jellyfin) }.getOrDefault("Jellyfin")
        ServiceKind.EMBY -> runCatching { appContext.getString(R.string.server_default_name_emby) }.getOrDefault("Emby")
        else -> kind.displayName
    }
}

private object EndpointValidatorFacade {
    fun normalize(value: String): String = app.reelstack.data.network.EndpointValidator.normalizeBaseUrl(value)
}
