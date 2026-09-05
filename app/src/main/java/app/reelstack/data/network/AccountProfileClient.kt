package app.reelstack.data.network

import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import java.io.IOException
import java.net.URI
import java.net.URLEncoder

/** Loads current server identity on every call. Never infers an account from connection form fields. */
class AccountProfileClient(
    private val deviceId: String = "homereel-android",
    private val transport: JsonHttpTransport = HttpTransport(),
) {
    fun load(connection: ServiceConnection): ServiceAccount {
        val path = when (connection.kind) {
            ServiceKind.JELLYFIN, ServiceKind.EMBY -> "Users/Me"
            ServiceKind.SEERR -> "api/v1/auth/me"
            else -> error("Kontovisning er berre støtta for Jellyfin og Seerr.")
        }
        check(connection.token.isNotBlank()) { "Logg inn på ${connection.kind.displayName} for å sjå kontoen din." }
        val response = try {
            transport.get(EndpointValidator.resolve(connection.baseUrl, path), accountHeaders(connection))
        } catch (_: IOException) {
            // Transport messages can include request context; expose only a safe, actionable message.
            error("Fekk ikkje kontakt med ${connection.kind.displayName}. Prøv igjen.")
        }
        when (response.statusCode) {
            in 200..299 -> Unit
            400 -> error("${connection.kind.displayName} kunne ikkje stadfeste ein personleg konto. Logg inn på nytt.")
            401, 403 -> error("${connection.kind.displayName} avviste kontotilgangen. Logg inn på nytt.")
            404 -> error("${connection.kind.displayName} tilbyr ikkje kontoinformasjon på denne adressa.")
            408, 429 -> error("${connection.kind.displayName} er mellombels oppteken. Prøv igjen seinare.")
            in 500..599 -> error("${connection.kind.displayName} er utilgjengeleg no. Prøv igjen seinare.")
            else -> error("${connection.kind.displayName} svara med status ${response.statusCode}.")
        }
        val root = runCatching { Json.parseToJsonElement(response.body) as? JsonObject }.getOrNull()
            ?: error("${connection.kind.displayName} sende ugyldig kontoinformasjon.")
        return when (connection.kind) {
            ServiceKind.JELLYFIN, ServiceKind.EMBY -> jellyfinAccount(connection, root)
            ServiceKind.SEERR -> seerrAccount(connection, root)
            else -> error("Kontotypen er ikkje støtta.")
        }
    }

    /** Only attach these headers to this URL; image loaders must not forward them across redirects. */
    fun avatarHeaders(connection: ServiceConnection, avatarUrl: String?): Map<String, String> {
        if (avatarUrl == null || connection.token.isBlank()) return emptyMap()
        val avatar = safeHttpUri(avatarUrl) ?: return emptyMap()
        val base = runCatching { URI(EndpointValidator.normalizeBaseUrl(connection.baseUrl)) }.getOrNull()
            ?: return emptyMap()
        val basePath = base.path.orEmpty().trimEnd('/')
        if (!avatar.scheme.equals(base.scheme, ignoreCase = true) ||
            !avatar.host.equals(base.host, ignoreCase = true) || effectivePort(avatar) != effectivePort(base) ||
            (basePath.isNotEmpty() && !avatar.path.orEmpty().startsWith("$basePath/"))
        ) return emptyMap()
        return accountHeaders(connection)
    }

    private fun jellyfinAccount(connection: ServiceConnection, root: JsonObject): ServiceAccount {
        val id = root.text("Id") ?: root.text("id") ?: error("Jellyfin sende ingen konto-ID.")
        val name = root.text("Name") ?: root.text("name") ?: error("Jellyfin sende ikkje noko brukarnamn.")
        val imageTag = root.text("PrimaryImageTag") ?: root.text("primaryImageTag")
        return ServiceAccount(
            source = connection.kind,
            id = id,
            displayName = name,
            username = name,
            avatarUrl = imageTag?.let {
                EndpointValidator.resolve(connection.baseUrl, "Users/${encode(id)}/Images/Primary?tag=${encode(it)}")
            },
            isPersonal = true, // /Users/Me succeeds only when the token is associated with a user.
            isAdmin = ((root["Policy"] as? JsonObject)?.get("IsAdministrator") as? JsonPrimitive)?.booleanOrNull == true,
        )
    }

    private fun seerrAccount(connection: ServiceConnection, root: JsonObject): ServiceAccount {
        val id = (root["id"] as? JsonPrimitive)?.contentOrNull
            ?.takeIf { it.toLongOrNull()?.let { value -> value > 0 } == true }
            ?: error("Seerr sende ingen gyldig konto-ID.")
        val username = root.text("username") ?: root.text("jellyfinUsername") ?: root.text("plexUsername")
        return ServiceAccount(
            source = ServiceKind.SEERR,
            id = id,
            displayName = root.text("displayName") ?: username ?: "Brukar $id",
            username = username,
            avatarUrl = resolveAvatar(connection.baseUrl, root.text("avatar")),
            isPersonal = connection.sessionCookie,
            permissions = (root["permissions"] as? JsonPrimitive)?.longOrNull ?: 0,
            isAdmin = ((root["permissions"] as? JsonPrimitive)?.longOrNull ?: 0) and 2L != 0L,
            mediaUserId = root.text("jellyfinUserId"),
        )
    }

    private fun accountHeaders(connection: ServiceConnection): Map<String, String> = when (connection.kind) {
        ServiceKind.JELLYFIN -> mapOf("Authorization" to jellyfinAuthorization(deviceId, connection.token))
        ServiceKind.EMBY -> mapOf("X-Emby-Token" to connection.token)
        ServiceKind.SEERR -> if (connection.sessionCookie) seerrCookieHeaders(connection.token)
            else mapOf("X-Api-Key" to connection.token)
        else -> emptyMap()
    }

    private fun resolveAvatar(baseUrl: String, value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val base = URI(EndpointValidator.normalizeBaseUrl(baseUrl))
            val reference = URI(value)
            val resolved = when {
                reference.isAbsolute -> reference
                reference.rawAuthority != null -> URI("${base.scheme}:$value")
                // Seerr emits /avatarproxy/... relative to the app, including behind a base-path proxy.
                base.path.orEmpty().isNotEmpty() && value.startsWith("${base.path}/") -> base.resolve(reference)
                else -> URI(EndpointValidator.resolve(baseUrl, value))
            }
            safeHttpUri(resolved.toString())?.toASCIIString()
        }.getOrNull()
    }

    private fun safeHttpUri(value: String): URI? = runCatching {
        val uri = URI(value)
        require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true))
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawFragment == null)
        require(value.none { it.isISOControl() || it == '\\' })
        require(uri.path.orEmpty().none { it.isISOControl() || it == '\\' })
        require(uri.path.orEmpty().split('/').none { it == "." || it == ".." })
        EndpointValidator.normalizeBaseUrl(uri.toString()) // Retain the existing cleartext/LAN policy.
        uri
    }.getOrNull()

    private fun effectivePort(uri: URI): Int = if (uri.port != -1) uri.port else if (uri.scheme.equals("https", true)) 443 else 80
    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }
        ?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
}
