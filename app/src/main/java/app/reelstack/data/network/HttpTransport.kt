package app.reelstack.data.network

import app.reelstack.BuildConfig
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

data class HttpResponse(
    val statusCode: Int,
    val body: String,
    val setCookies: List<String> = emptyList(),
)

interface JsonHttpTransport {
    fun get(url: String, headers: Map<String, String>): HttpResponse
    fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse
}

class HttpTransport(
    private val connectTimeoutMs: Int = 7_000,
    private val readTimeoutMs: Int = 9_000,
) : JsonHttpTransport {
    override fun get(url: String, headers: Map<String, String>): HttpResponse =
        request(method = "GET", url = url, headers = headers, jsonBody = null)

    override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
        request(method = "POST", url = url, headers = headers, jsonBody = jsonBody)

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
            connection.setRequestProperty("User-Agent", "HomeReel/${BuildConfig.VERSION_NAME} Android")
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
            HttpResponse(statusCode = status, body = body,
                setCookies = connection.headerFields.entries
                    .filter { it.key.equals("Set-Cookie", ignoreCase = true) }.flatMap { it.value })
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024
    }
}
