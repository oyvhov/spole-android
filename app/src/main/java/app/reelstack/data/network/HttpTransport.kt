package app.reelstack.data.network

import app.reelstack.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

data class HttpResponse(
    val statusCode: Int,
    val body: String,
    val setCookies: List<String> = emptyList(),
    /**
     * `Location` from a 3xx answer. Redirects are never followed automatically — that could hand
     * a service token to whatever host the redirect names — so the address itself is what the
     * user needs to see in order to fix their own setup.
     */
    val location: String? = null,
    /** `Retry-After` in seconds, when the server said how long to wait. */
    val retryAfterSeconds: Long? = null,
)

interface JsonHttpTransport {
    fun get(url: String, headers: Map<String, String>): HttpResponse
    fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse

    /** Defaulted so a read-only fake stays valid; only the request flow needs to withdraw anything. */
    fun delete(url: String, headers: Map<String, String>): HttpResponse =
        error("Denne tenesta støttar ikkje sletting")
}

class HttpTransport(
    private val connectTimeoutMs: Int = 7_000,
    private val readTimeoutMs: Int = 9_000,
) : JsonHttpTransport {
    override fun get(url: String, headers: Map<String, String>): HttpResponse =
        // GET is the only method retried. A home server on the far side of a phone's mobile
        // connection drops the occasional first attempt, and re-reading a dashboard row is free.
        // POST is never retried: a resent request could reach Seerr twice, and "ingen dobbel
        // innsending" has to hold for a flaky network too, not only for a fast double tap.
        retrying { request(method = "GET", url = url, headers = headers, jsonBody = null) }

    override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
        request(method = "POST", url = url, headers = headers, jsonBody = jsonBody)

    override fun delete(url: String, headers: Map<String, String>): HttpResponse =
        request(method = "DELETE", url = url, headers = headers, jsonBody = null)

    private fun <T> retrying(block: () -> T): T = try {
        block()
    } catch (first: IOException) {
        try {
            block()
        } catch (_: IOException) {
            // The caller turns an IOException into "Fekk ikkje kontakt med …". Report the first
            // failure, not the second: they are the same outage, and the first one happened at
            // the moment the user actually asked for something.
            throw first
        }
    }

    private fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        jsonBody: String?,
    ): HttpResponse {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Spole/${BuildConfig.VERSION_NAME} Android")
            headers.forEach(connection::setRequestProperty)
            if (jsonBody != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(jsonBody.toByteArray(StandardCharsets.UTF_8)) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..399) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_RESPONSE_BYTES) { "Svaret frå tenaren var for stort" }
                    output.write(buffer, 0, count)
                }
                output.toString(StandardCharsets.UTF_8.name())
            }.orEmpty()
            HttpResponse(
                statusCode = status,
                body = body,
                setCookies = connection.headerFields.entries
                    .filter { it.key.equals("Set-Cookie", ignoreCase = true) }.flatMap { it.value },
                location = connection.getHeaderField("Location")?.takeIf(String::isNotBlank),
                retryAfterSeconds = connection.getHeaderField("Retry-After")?.trim()?.toLongOrNull(),
            )
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024
    }
}
