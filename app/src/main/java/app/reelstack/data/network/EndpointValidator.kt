package app.reelstack.data.network

import java.net.URI
import java.util.Locale

object EndpointValidator {
    fun normalizeBaseUrl(value: String): String {
        require(value.isNotBlank()) { "Skriv inn tenaradressa først" }
        require(value.trim().none(Char::isWhitespace)) { "Tenaradressa inneheld mellomrom. Fjern dei og prøv igjen." }
        val candidate = value.trim().let {
            if (it.contains("://")) it else "https://$it"
        }
        val uri = runCatching { URI(candidate) }
            .getOrElse { throw IllegalArgumentException("Skriv inn ei gyldig tenaradresse") }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        require(scheme == "http" || scheme == "https") {
            "Berre HTTP- og HTTPS-adresser er støtta"
        }
        require(!uri.host.isNullOrBlank()) { "Skriv inn ei fullstendig tenaradresse" }
        require(uri.userInfo == null) { "Ikkje legg inn påloggingsdata i tenaradressa" }
        require(uri.port == -1 || uri.port in 1..65535) { "Portnummeret må vere mellom 1 og 65535" }
        require(scheme != "http" || isTrustedLanHost(uri.host)) {
            "Vanleg HTTP er berre tillate for localhost eller private lokalnettadresser"
        }

        val path = (uri.path ?: "").trimEnd('/')
        return URI(scheme, null, uri.host.lowercase(Locale.ROOT), uri.port, path.ifEmpty { null }, null, null).toString()
    }

    fun resolve(baseUrl: String, path: String): String =
        "${normalizeBaseUrl(baseUrl)}/${path.trimStart('/')}"

    fun isCleartext(baseUrl: String): Boolean =
        runCatching { URI(normalizeBaseUrl(baseUrl)).scheme == "http" }.getOrDefault(false)

    private fun isTrustedLanHost(host: String): Boolean {
        val normalized = host.lowercase(Locale.ROOT).trim('[', ']')
        if (normalized == "localhost" || normalized == "::1" || normalized.endsWith(".local")) return true
        if (normalized.startsWith("127.") || normalized.startsWith("10.") || normalized.startsWith("192.168.")) return true
        val octets = normalized.split('.')
        if (octets.size == 4 && octets[0] == "172") {
            val second = octets[1].toIntOrNull()
            if (second != null && second in 16..31) return true
        }
        return normalized.startsWith("fc") || normalized.startsWith("fd") || normalized.startsWith("fe80:")
    }
}
