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
        if (normalized == "localhost" || normalized.endsWith(".local")) return true
        // Only literal addresses count as private. A registered name that merely begins like one
        // still resolves through public DNS to whatever its owner points it at: "fcbarcelona.com"
        // starts with "fc", and wildcard resolvers hand out names like "192.168.1.5.nip.io".
        // Treating either as LAN would quietly permit cleartext HTTP over the open internet.
        if (normalized.contains(':')) return isPrivateIpv6(normalized)
        return isPrivateIpv4(normalized)
    }

    private fun isPrivateIpv4(host: String): Boolean {
        val octets = host.split('.')
        if (octets.size != 4) return false
        val values = octets.map { octet ->
            if (octet.isEmpty() || octet.length > 3 || !octet.all(Char::isDigit)) return false
            octet.toInt().also { if (it > 255) return false }
        }
        return when (values[0]) {
            10, 127 -> true
            172 -> values[1] in 16..31
            192 -> values[1] == 168
            else -> false
        }
    }

    /** Reached only for a literal address, so a leading "fc"/"fd" here really is fc00::/7. */
    private fun isPrivateIpv6(host: String): Boolean =
        host == "::1" || host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80:")
}
