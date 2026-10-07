package app.reelstack.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** What a media server says about itself before anyone signs in. */
data class PublicServerInfo(
    val kind: DiscoveredServerKind,
    val id: String,
    val name: String,
    val baseUrl: String,
    val version: String = "",
) {
    val capabilities: ServerCapabilities get() = ServerCapabilities(kind, version)
}

/** Version is diagnostic evidence. Optional features remain unknown until their routes answer. */
data class ServerCapabilities(
    val kind: DiscoveredServerKind,
    val version: String,
    val quickConnectEnabled: Boolean? = null,
    val authenticatedNotifications: Boolean? = null,
)

/**
 * Read `/System/Info/Public`. Jellyfin names itself in `ProductName`; Emby leaves the field out
 * (4.x) or says "Emby Server". Anything without an `Id` is not a media server at all — a router
 * page or a different service on the same port.
 */
internal fun parsePublicServerInfo(baseUrl: String, body: String): PublicServerInfo? {
    val json = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
    fun text(key: String) = runCatching { json[key]?.jsonPrimitive?.contentOrNull }.getOrNull()?.trim()
    val id = text("Id")?.takeIf { it.isNotEmpty() } ?: return null
    val product = text("ProductName").orEmpty().lowercase()
    val kind = if ("jellyfin" in product) DiscoveredServerKind.JELLYFIN else DiscoveredServerKind.EMBY
    val name = text("ServerName")?.takeIf { it.isNotEmpty() } ?: baseUrl
    return PublicServerInfo(kind, id, name, baseUrl, text("Version").orEmpty())
}

/**
 * Finds which of several addresses really is a media server, without signing in.
 *
 * Candidates are asked at once, so a dead guess costs one timeout and not one each, but the answer
 * follows the given order: when both `http://host:8096` and `https://host:8920` answer, the
 * first guess was the better one.
 */
class ServerProbe(
    private val transport: JsonHttpTransport = HttpTransport(connectTimeoutMs = 2_000, readTimeoutMs = 3_000),
) {
    fun info(baseUrl: String): PublicServerInfo? = runCatching {
        val response = transport.get(EndpointValidator.resolve(baseUrl, "System/Info/Public"), emptyMap())
        if (response.statusCode !in 200..299) null else parsePublicServerInfo(baseUrl, response.body)
    }.getOrNull()

    /**
     * Seerr on the same machine as a local media server, at its default port, or null.
     *
     * Only ever tried next to a server found on the LAN: the address is plain HTTP, and the
     * validator refuses that for anything public, so a published host name never gets a guess.
     */
    fun seerrBeside(mediaServerUrl: String): String? {
        val host = runCatching { java.net.URI(mediaServerUrl).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val candidate = runCatching {
            EndpointValidator.normalizeBaseUrl("http://${if (host.contains(':')) "[$host]" else host}:$SEERR_DEFAULT_PORT")
        }.getOrNull() ?: return null
        val response = runCatching { transport.get(EndpointValidator.resolve(candidate, "api/v1/status"), emptyMap()) }
            .getOrNull()?.takeIf { it.statusCode in 200..299 } ?: return null
        val version = runCatching {
            Json.parseToJsonElement(response.body).jsonObject["version"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        return candidate.takeIf { !version.isNullOrBlank() }
    }

    suspend fun firstAnswering(candidates: List<String>): PublicServerInfo? = coroutineScope {
        val answers = candidates.map { candidate -> async(Dispatchers.IO) { info(candidate) } }
        for (answer in answers) {
            answer.await()?.let { found ->
                answers.forEach { it.cancel() }
                return@coroutineScope found
            }
        }
        null
    }

    companion object {
        const val SEERR_DEFAULT_PORT = 5055
    }
}
