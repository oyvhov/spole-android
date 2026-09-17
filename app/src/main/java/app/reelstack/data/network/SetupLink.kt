package app.reelstack.data.network

import app.reelstack.R
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
            if (uri.rawQuery != null || uri.rawFragment != null || uri.rawUserInfo != null) {
                invalidEndpoint(R.string.err_oppsettslenkje_berre_adresser)
            }
            return EndpointValidator.normalizeBaseUrl(value)
        }

        fun parse(value: String): SetupLink {
            if (value.length > 4096) invalidEndpoint(R.string.err_oppsettslenkje_for_lang)
            val uri = URI(value.trim())
            if (uri.scheme != "spole" || uri.rawAuthority != "setup" || !uri.path.isNullOrEmpty() || uri.rawFragment != null) {
                invalidEndpoint(R.string.err_oppsettslenkje_ikkje_spole)
            }
            val parts = uri.rawQuery.orEmpty().split('&').map {
                val pair = it.split('=', limit = 2)
                if (pair.size != 2) invalidEndpoint(R.string.err_oppsettslenkje_ufullstendig)
                pair[0] to URLDecoder.decode(pair[1], "UTF-8")
            }
            if (parts.any { it.first !in setOf("jellyfin", "seerr") } || parts.map { it.first }.distinct().size != parts.size) {
                invalidEndpoint(R.string.err_oppsettslenkje_ukjende_felt)
            }
            val values = parts.toMap()
            return SetupLink(address(values["jellyfin"].orEmpty()), values["seerr"]?.let(::address).orEmpty())
        }
    }
}
