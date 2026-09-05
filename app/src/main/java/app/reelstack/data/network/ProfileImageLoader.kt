package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI

/** Never forward a service credential to an external avatar host or through a redirect. */
internal fun profileImageHeaders(connection: ServiceConnection?, url: String, deviceId: String): Map<String, String> {
    if (connection == null) return emptyMap()
    return AccountProfileClient(deviceId = deviceId).avatarHeaders(connection, url)
}

internal fun loadProfileImage(url: String, connection: ServiceConnection?, deviceId: String): ByteArray? {
    val uri = URI(url)
    require(uri.scheme in setOf("https", "http") && uri.host != null && uri.userInfo == null)
    val request = uri.toURL().openConnection() as HttpURLConnection
    return try {
        request.connectTimeout = 3_000
        request.readTimeout = 4_000
        request.instanceFollowRedirects = false
        request.setRequestProperty("Accept", "image/*")
        profileImageHeaders(connection, url, deviceId).forEach(request::setRequestProperty)
        if (request.responseCode !in 200..299 || request.contentLengthLong > 1_048_576) return null
        request.inputStream.use { input ->
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
    } finally { request.disconnect() }
}
