package app.reelstack.data.network

import app.reelstack.BuildConfig
import app.reelstack.data.model.ServiceKind
import kotlinx.serialization.json.*

data class EmbyConnectSession(val accessToken: String, val userId: String)

data class EmbyConnectServer(
    val id: String,
    val name: String,
    val remoteUrl: String?,
    val localUrl: String?,
    val accessKey: String,
)

/** Emby.media account discovery. Server tokens are exchanged locally and never shared between servers. */
class EmbyConnectClient(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val application: String = "Spole/${BuildConfig.VERSION_NAME}",
) {
    fun authenticate(username: String, password: String): EmbyConnectSession {
        val response = transport.post(
            "$CONNECT_BASE/service/user/authenticate",
            mapOf("X-Application" to application),
            buildJsonObject { put("nameOrEmail", username); put("rawpw", password) }.toString(),
        )
        require(response.statusCode in 200..299) { "Emby Connect avviste innlogginga." }
        val root = parse(response.body)
        return EmbyConnectSession(
            root.str("ConnectAccessToken").also { require(it.isNotBlank()) { "Emby Connect sende ikkje tilgangsteikn." } },
            root.str("ConnectUserId").also { require(it.isNotBlank()) { "Emby Connect sende ingen brukar-ID." } },
        )
    }

    fun servers(session: EmbyConnectSession): List<EmbyConnectServer> {
        val response = transport.get(
            "$CONNECT_BASE/service/servers?userId=${enc(session.userId)}",
            mapOf("X-Application" to application, "X-Connect-UserToken" to session.accessToken),
        )
        require(response.statusCode in 200..299) { "Emby Connect kunne ikkje hente serverane." }
        val array = Json.parseToJsonElement(response.body) as? JsonArray ?: JsonArray(emptyList())
        return array.mapNotNull { value ->
            val item = value as? JsonObject ?: return@mapNotNull null
            val id = item.str("SystemId").ifBlank { item.str("Id") }
            val key = item.str("AccessKey")
            if (id.isBlank() || key.isBlank()) null else EmbyConnectServer(
                id, item.str("Name").ifBlank { "Emby" }, item.str("Url").ifBlank { null },
                item.str("LocalAddress").ifBlank { null }, key,
            )
        }
    }

    fun exchange(server: EmbyConnectServer, session: EmbyConnectSession, baseUrl: String): ServiceAuthentication {
        val response = transport.get(
            EndpointValidator.resolve(baseUrl, "Connect/Exchange?format=json&ConnectUserId=${enc(session.userId)}"),
            mapOf(
                "X-Emby-Token" to server.accessKey,
                "X-Emby-Authorization" to "Emby Client=\"Spole\", Device=\"Android\", DeviceId=\"emby-connect\", Version=\"${BuildConfig.VERSION_NAME}\"",
            ),
        )
        require(response.statusCode in 200..299) { "Emby-serveren kunne ikkje fullføre Emby Connect." }
        val root = parse(response.body)
        return ServiceAuthentication(
            root.str("AccessToken").also { require(it.isNotBlank()) { "Emby-serveren sende ikkje lokalt tilgangsteikn." } },
            root.str("LocalUserId").also { require(it.isNotBlank()) { "Emby-serveren sende ingen lokal profil." } },
        )
    }

    private fun parse(body: String) = Json.parseToJsonElement(body).jsonObject
    private fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun enc(value: String) = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    private companion object { const val CONNECT_BASE = "https://connect.emby.media" }
}
