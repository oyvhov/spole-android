package app.reelstack.data.network

import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Uses Seerr's own Jellyfin login; no Jellyfin token is forwarded to another server. */
class SeerrAuthenticationClient(private val transport: JsonHttpTransport = HttpTransport()) {
    fun authenticate(baseUrl: String, username: String, password: String): ServiceAuthentication {
        val cookies = bootstrap(baseUrl)
        val response = transport.post(
            EndpointValidator.resolve(baseUrl, "api/v1/auth/jellyfin"), seerrCookieHeaders(cookies),
            buildJsonObject { put("username", username); put("password", password) }.toString(),
        )
        return authenticated(response, cookies)
    }

    fun initiateQuickConnect(baseUrl: String): QuickConnectChallenge {
        val cookies = bootstrap(baseUrl)
        val response = transport.post(EndpointValidator.resolve(baseUrl, "api/v1/auth/jellyfin/quickconnect/initiate"),
            seerrCookieHeaders(cookies), "{}")
        requireSuccess(response, quickConnect = true)
        val root = Json.parseToJsonElement(response.body).jsonObject
        val code = root["code"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val secret = root["secret"]?.jsonPrimitive?.contentOrNull.orEmpty()
        check(code.isNotBlank() && secret.isNotBlank()) { "Seerr gav ingen Quick Connect-kode. Prøv Jellyfin-konto." }
        return QuickConnectChallenge(secret, code, false, mergeSeerrCookies(cookies, response.setCookies))
    }

    fun quickConnectState(baseUrl: String, challenge: QuickConnectChallenge): QuickConnectChallenge {
        val secret = URLEncoder.encode(challenge.secret, "UTF-8")
        val response = transport.get(EndpointValidator.resolve(baseUrl, "api/v1/auth/jellyfin/quickconnect/check?secret=$secret"),
            seerrCookieHeaders(challenge.cookies))
        if (response.statusCode == 404) error("Quick Connect-koden er utgått. Lag ein ny kode.")
        requireSuccess(response, quickConnect = true)
        val root = Json.parseToJsonElement(response.body).jsonObject
        return challenge.copy(authenticated = root["authenticated"]?.jsonPrimitive?.contentOrNull == "true",
            cookies = mergeSeerrCookies(challenge.cookies, response.setCookies))
    }

    fun authenticateWithQuickConnect(baseUrl: String, challenge: QuickConnectChallenge): ServiceAuthentication {
        val response = transport.post(EndpointValidator.resolve(baseUrl, "api/v1/auth/jellyfin/quickconnect/authenticate"),
            seerrCookieHeaders(challenge.cookies), buildJsonObject { put("secret", challenge.secret) }.toString())
        return authenticated(response, challenge.cookies)
    }

    private fun bootstrap(baseUrl: String): String {
        val response = transport.get(EndpointValidator.resolve(baseUrl, "api/v1/status"), emptyMap())
        requireSuccess(response)
        return mergeSeerrCookies("", response.setCookies)
    }

    private fun authenticated(response: HttpResponse, existingCookies: String): ServiceAuthentication {
        requireSuccess(response)
        val cookies = mergeSeerrCookies(existingCookies, response.setCookies)
        check(cookies.split("; ").any { it.startsWith("connect.sid=") && it.substringAfter('=').isNotBlank() }) {
            "Seerr gav inga innloggingsøkt. Sjekk tenaradressa og prøv igjen."
        }
        val root = Json.parseToJsonElement(response.body).jsonObject
        val userId = root["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
        check(userId.isNotBlank()) { "Seerr gav ingen brukarkonto tilbake." }
        return ServiceAuthentication(cookies, userId)
    }

    private fun requireSuccess(response: HttpResponse, quickConnect: Boolean = false) {
        when (response.statusCode) {
            in 200..299 -> Unit
            401 -> error("Feil Jellyfin-brukarnamn eller passord.")
            403 -> error("Seerr avviste innlogginga. Sjekk at kontoen har tilgang og at Jellyfin-innlogging er slått på.")
            404 -> error(if (quickConnect) "Denne Seerr-versjonen støttar ikkje Quick Connect. Vel Jellyfin-konto." else "Fann ikkje Seerr. Sjekk tenaradressa.")
            429 -> error("For mange innloggingsforsøk. Vent litt og prøv igjen.")
            else -> error(if (quickConnect) "Fekk ikkje starta Quick Connect via Seerr. Prøv Jellyfin-konto." else "Seerr kunne ikkje logge deg inn. Sjekk at Jellyfin-innlogging er aktivert på tenaren.")
        }
    }
}

/** Persist only the three cookies required by Seerr. Never persist password or cookie attributes. */
internal fun mergeSeerrCookies(existing: String, setCookies: List<String>): String {
    val values = linkedMapOf<String, String>()
    (existing.split(';') + setCookies.map { it.substringBefore(';') }).forEach { pair ->
        val name = pair.substringBefore('=').trim()
        val value = pair.substringAfter('=', "").trim()
        if (name in setOf("connect.sid", "_csrf", "XSRF-TOKEN") && value.length <= 8192 &&
            value.none { it == '\r' || it == '\n' }) {
            if (value.isEmpty()) values.remove(name) else values[name] = value
        }
    }
    return values.entries.joinToString("; ") { "${it.key}=${it.value}" }
}

internal fun seerrCookieHeaders(cookies: String): Map<String, String> = buildMap {
    if (cookies.isNotBlank()) put("Cookie", cookies)
    val csrf = cookies.split(';').map(String::trim).firstOrNull { it.startsWith("XSRF-TOKEN=") }
        ?.substringAfter('=')
    if (!csrf.isNullOrBlank()) {
        val decoded = URLDecoder.decode(csrf, "UTF-8")
        require(decoded.none { it == '\r' || it == '\n' }) { "Ugyldig økt frå Seerr" }
        put("X-XSRF-TOKEN", decoded)
    }
}
