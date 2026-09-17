package app.reelstack.data.network

import app.reelstack.R
import java.io.IOException
import javax.net.ssl.SSLException

/**
 * Transport failures must never reach the user as raw Java network text. `UnknownHostException`
 * carries only the hostname, and `ConnectException` carries an internal address and port, so both
 * read as noise in an error field. Every user-facing connection path routes its calls through here
 * so a failure says what happened and what to try next, in nynorsk.
 */
internal inline fun <T> contacting(service: String, block: () -> T): T = try {
    block()
} catch (_: SSLException) {
    // Self-hosted servers often use a certificate Android does not trust. That is a different
    // problem from an unreachable server, and it needs a different next step.
    serviceError(R.string.err_klarte_ikkje_opprette_trygg, service)
} catch (error: IOException) {
    throw ServiceMessage(app.reelstack.localization.LocalizedText(R.string.err_kontakt_sjekk_adresse, service)).apply { initCause(error) }
}

/** Parses a service response body, without putting the raw body or parser text in the message. */
internal fun serviceJson(body: String, service: String) =
    runCatching { kotlinx.serialization.json.Json.parseToJsonElement(body) }
        .getOrElse { serviceError(R.string.err_sende_eit_uventa_svar, service, service) }
