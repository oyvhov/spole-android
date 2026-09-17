package app.reelstack.data.network

import app.reelstack.BuildConfig
import app.reelstack.R
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

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
        error("this transport does not implement delete")
}

class HttpTransport(
    private val connectTimeoutMs: Int = 7_000,
    private val readTimeoutMs: Int = 9_000,
    private val client: OkHttpClient = if (connectTimeoutMs == 7_000 && readTimeoutMs == 9_000) sharedClient
    else sharedClient.newBuilder()
        .connectTimeout(connectTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
        .build(),
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
        val requestBuilder = Request.Builder().url(url)
        requestBuilder.header("Accept", "application/json")
        requestBuilder.header("User-Agent", "Spole/${BuildConfig.VERSION_NAME} Android")
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }

        val body = when {
            jsonBody != null -> jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            method == "POST" -> ByteArray(0).toRequestBody(null)
            else -> null
        }
        requestBuilder.method(method, body)

        return client.newCall(requestBuilder.build()).execute().use { response ->
            val status = response.code
            val stream = response.body?.byteStream()
            val responseBody = stream?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) serviceError(R.string.err_svaret_var_for_stort)
                    output.write(buffer, 0, count)
                }
                output.toString(StandardCharsets.UTF_8.name())
            }.orEmpty()

            HttpResponse(
                statusCode = status,
                body = responseBody,
                setCookies = response.headers("Set-Cookie"),
                location = response.header("Location")?.takeIf(String::isNotBlank),
                retryAfterSeconds = response.header("Retry-After")?.trim()?.toLongOrNull(),
            )
        }
    }

    companion object {
        const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024

        val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(7, TimeUnit.SECONDS)
                .readTimeout(9, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
                .build()
        }
    }
}
