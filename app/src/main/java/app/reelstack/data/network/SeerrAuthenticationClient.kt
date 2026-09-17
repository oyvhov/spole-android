package app.reelstack.data.network

import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Uses Seerr's own Jellyfin login; no Jellyfin token is forwarded to another server. */
class SeerrAuthenticationClient(private val transport: JsonHttpTransport = HttpTransport()) {
    fun authenticate(baseUrl: String, username: String, password: String): ServiceAuthentication {
        val cookies = bootstrap(baseUrl)
        val response = contacting(SEERR) {
            transport.post(
                EndpointValidator.resolve(baseUrl, "api/v1/auth/jellyfin"), seerrCookieHeaders(cookies),
                buildJsonObject { put("username", username); put("password", password) }.toString(),
            )
        }
        return authenticated(response, cookies)
    }

    fun initiateQuickConnect(baseUrl: String): QuickConnectChallenge {
        val cookies = bootstrap(baseUrl)
        val response = contacting(SEERR) {
            transport.post(EndpointValidator.resolve(baseUrl, "api/v1/auth/jellyfin/quickconnect/initiate"),
                seerrCookieHeaders(cookies), "{}")
        }
        requireSuccess(response, quickConnect = true)
        val root = serviceJson(response.body, SEERR).jsonObject
        val code = root["code"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val secret = root["secret"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (code.isBlank() || secret.isBlank()) serviceError(R.string.err_seerr_ingen_quick_connect)
        return QuickConnectChallenge(secret, code, false, mergeSeerrCookies(cookies, response.setCookies))
    }

    fun quickConnectState(baseUrl: String, challenge: QuickConnectChallenge): QuickConnectChallenge {
        val secret = URLEncoder.encode(challenge.secret, "UTF-8")
        val response = contacting(SEERR) {
            transport.get(EndpointValidator.resolve(baseUrl, "api/v1/auth/jellyfin/quickconnect/check?secret=$secret"),
                seerrCookieHeaders(challenge.cookies))
        }
        if (response.statusCode == 404) serviceError(R.string.err_quick_connect_koden_utgatt)
        requireSuccess(response, quickConnect = true)
        val root = serviceJson(response.body, SEERR).jsonObject
        return challenge.copy(authenticated = root["authenticated"]?.jsonPrimitive?.contentOrNull == "true",
            cookies = mergeSeerrCookies(challenge.cookies, response.setCookies))
    }

    fun authenticateWithQuickConnect(baseUrl: String, challenge: QuickConnectChallenge): ServiceAuthentication {
        val response = contacting(SEERR) {
            transport.post(EndpointValidator.resolve(baseUrl, "api/v1/auth/jellyfin/quickconnect/authenticate"),
                seerrCookieHeaders(challenge.cookies), buildJsonObject { put("secret", challenge.secret) }.toString())
        }
        return authenticated(response, challenge.cookies)
    }

    private fun bootstrap(baseUrl: String): String {
        val response = contacting(SEERR) {
            transport.get(EndpointValidator.resolve(baseUrl, "api/v1/status"), emptyMap())
        }
        requireSuccess(response)
        return mergeSeerrCookies("", response.setCookies)
    }

    private fun authenticated(response: HttpResponse, existingCookies: String): ServiceAuthentication {
        requireSuccess(response)
        val cookies = mergeSeerrCookies(existingCookies, response.setCookies)
        if (cookies.split("; ").none { it.startsWith("connect.sid=") && it.substringAfter('=').isNotBlank() }) {
            serviceError(R.string.err_seerr_inga_okt)
        }
        val root = serviceJson(response.body, SEERR).jsonObject
        val userId = root["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (userId.isBlank()) serviceError(R.string.err_seerr_ingen_brukarkonto)
        return ServiceAuthentication(cookies, userId)
    }

    private fun requireSuccess(response: HttpResponse, quickConnect: Boolean = false) {
        when (response.statusCode) {
            in 200..299 -> Unit
            401 -> serviceError(R.string.err_feil_jellyfin_brukarnamn_eller)
            403 -> serviceError(R.string.err_seerr_avviste_innlogginga_sjekk)
            404 -> serviceError(if (quickConnect) R.string.err_seerr_quick_connect_versjon else R.string.err_fann_ikkje_seerr)
            429 -> serviceError(busyMessage(ServiceKind.SEERR, response.retryAfterSeconds))
            in 300..399 -> serviceError(redirectMessage(ServiceKind.SEERR, response.location))
            else -> serviceError(if (quickConnect) R.string.err_seerr_quick_connect_start else R.string.err_seerr_innlogging_aktivert)
        }
    }

    private companion object {
        const val SEERR = "Seerr"
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
        if (decoded.any { it == '\r' || it == '\n' }) serviceError(R.string.err_seerr_ugyldig_okt)
        put("X-XSRF-TOKEN", decoded)
    }
}
