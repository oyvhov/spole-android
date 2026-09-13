package app.reelstack.data.network

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/** Public server locations only. Never accepts credentials or performs network requests. */
data class SetupLink(val jellyfin: String, val seerr: String = "") {
    fun encode(): String = "spole://setup?jellyfin=${encodePart(address(jellyfin))}" +
        seerr.takeIf(String::isNotBlank)?.let { "&seerr=${encodePart(address(it))}" }.orEmpty()

    companion object {
        private fun encodePart(value: String) = URLEncoder.encode(value, "UTF-8")
        private fun address(value: String): String {
            val uri = URI(value.trim())
            require(uri.rawQuery == null && uri.rawFragment == null && uri.rawUserInfo == null) {
                "Oppsettslenkja skal berre innehalde tenesteadresser, utan innloggingsdata."
            }
            return EndpointValidator.normalizeBaseUrl(value)
        }

        fun parse(value: String): SetupLink {
            require(value.length <= 4096) { "Oppsettslenkja er for lang." }
            val uri = URI(value.trim())
            require(uri.scheme == "spole" && uri.rawAuthority == "setup" && uri.path.isNullOrEmpty() && uri.rawFragment == null) {
                "Dette er ikkje ei Spole-oppsettslenkje."
            }
            val parts = uri.rawQuery.orEmpty().split('&').map {
                val pair = it.split('=', limit = 2)
                require(pair.size == 2) { "Oppsettslenkja er ufullstendig." }
                pair[0] to URLDecoder.decode(pair[1], "UTF-8")
            }
            require(parts.all { it.first in setOf("jellyfin", "seerr") } && parts.map { it.first }.distinct().size == parts.size) {
                "Oppsettslenkja inneheld ukjende eller gjentekne felt."
            }
            val values = parts.toMap()
            return SetupLink(address(values["jellyfin"].orEmpty()), values["seerr"]?.let(::address).orEmpty())
        }
    }
}
