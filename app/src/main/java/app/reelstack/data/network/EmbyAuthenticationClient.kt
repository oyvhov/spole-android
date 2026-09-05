package app.reelstack.data.network

import app.reelstack.BuildConfig
import java.io.IOException
import kotlinx.serialization.json.*

/** Local Emby Server accounts, not the separate emby.media Connect service. */
class EmbyAuthenticationClient(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val deviceId: String = "homereel-android",
) {
    fun authenticate(baseUrl: String, username: String, password: String): ServiceAuthentication {
        val response = try {
            transport.post(
                EndpointValidator.resolve(baseUrl, "Users/AuthenticateByName"),
                mapOf("X-Emby-Authorization" to
                    "Emby Client=\"Spole\", Device=\"Android\", DeviceId=\"$deviceId\", Version=\"${BuildConfig.VERSION_NAME}\""),
                buildJsonObject { put("Username", username); put("Pw", password) }.toString(),
            )
        } catch (_: IOException) {
            error("Fekk ikkje kontakt med Emby. Sjekk tenaradressa og nettet.")
        }
        when (response.statusCode) {
            in 200..299 -> Unit
            400 -> error("Emby avviste innloggingskallet. Sjekk brukarnamnet og prøv igjen.")
            401, 403 -> error("Feil Emby-brukarnamn eller passord.")
            404 -> error("Fann ikkje Emby-innlogginga. Sjekk tenaradressa.")
            in 500..599 -> error("Emby fekk ein tenarfeil under innlogginga (status ${response.statusCode}).")
            else -> error("Emby kunne ikkje logge deg inn (status ${response.statusCode}).")
        }
        val root = runCatching { Json.parseToJsonElement(response.body).jsonObject }.getOrNull()
            ?: error("Emby sende eit ugyldig innloggingssvar.")
        val token = root["AccessToken"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: error("Emby sende ikkje eit tilgangsteikn.")
        val userId = root["User"]?.jsonObject?.get("Id")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: error("Emby sende ingen profil-ID.")
        return ServiceAuthentication(token, userId)
    }
}
