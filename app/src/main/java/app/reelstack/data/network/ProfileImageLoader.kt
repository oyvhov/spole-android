package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import okhttp3.Request
import java.io.ByteArrayOutputStream

/** Never forward a service credential to an external avatar host or through a redirect. */
internal fun profileImageHeaders(connection: ServiceConnection?, url: String, deviceId: String): Map<String, String> {
    if (connection == null) return emptyMap()
    return AccountProfileClient(deviceId = deviceId).avatarHeaders(connection, url)
}

internal fun loadProfileImage(url: String, connection: ServiceConnection?, deviceId: String): ByteArray? {
    val validatedUrl = EndpointValidator.validateRequestUrl(url)
    val requestBuilder = Request.Builder().url(validatedUrl)
    requestBuilder.header("Accept", "image/*")
    profileImageHeaders(connection, url, deviceId).forEach { (k, v) -> requestBuilder.header(k, v) }
    return runCatching {
        HttpTransport.sharedClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            if (body.contentLength() > 1_048_576) return null
            body.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 1_048_576) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        }
    }.getOrNull()
}
