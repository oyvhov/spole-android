package app.reelstack.data.network

import app.reelstack.R
import app.reelstack.BuildConfig
import app.reelstack.data.model.ServiceKind
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
            serviceError(R.string.err_fekk_ikkje_kontakt_med_2)
        }
        when (response.statusCode) {
            in 200..299 -> Unit
            400 -> serviceError(R.string.err_emby_avviste_innloggingskallet_sjekk)
            401, 403 -> serviceError(R.string.err_feil_emby_brukarnamn_eller)
            404 -> serviceError(R.string.err_fann_ikkje_emby_innlogginga)
            in 500..599 -> serviceError(R.string.err_emby_fekk_ein_tenarfeil, response.statusCode)
            in 300..399 -> serviceError(redirectMessage(ServiceKind.EMBY, response.location))
            else -> serviceError(R.string.err_emby_kunne_ikkje_logge, response.statusCode)
        }
        val root = runCatching { Json.parseToJsonElement(response.body).jsonObject }.getOrNull()
            ?: serviceError(R.string.err_emby_sende_eit_ugyldig)
        val token = root["AccessToken"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: serviceError(R.string.err_emby_sende_ikkje_eit)
        val userId = root["User"]?.jsonObject?.get("Id")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: serviceError(R.string.err_emby_sende_ingen_profil)
        return ServiceAuthentication(token, userId)
    }

    fun publicUsers(baseUrl: String): List<PublicUser> {
        val resolved = EndpointValidator.resolve(baseUrl, "Users/Public")
        val response = runCatching {
            transport.get(
                resolved,
                mapOf("Accept" to "application/json"),
            )
        }.getOrNull() ?: return emptyList()
        if (response.statusCode !in 200..299) return emptyList()
        return ServicePayloadParser.publicUsers(baseUrl, response.body)
    }
}
