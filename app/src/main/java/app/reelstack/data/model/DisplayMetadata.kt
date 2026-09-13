package app.reelstack.data.model

/** Existing server facts use a star prefix; years and age certificates are never scores. */
fun communityRatingLabel(facts: List<String>): String? {
    val label = facts.firstOrNull { it.trimStart().startsWith("★") }
        ?.trim()?.removePrefix("★")?.trim() ?: return null
    val value = label.replace(',', '.').toDoubleOrNull() ?: return null
    return label.takeIf { value.isFinite() && value in 0.0..10.0 }
}

enum class ServiceHealth { UNCONFIGURED, UNCHECKED, CHECKING, OK, WARNING, ERROR }

fun ServiceConnection.health(warning: String? = null): ServiceHealth = when (state) {
    ConnectionState.ERROR -> ServiceHealth.ERROR
    ConnectionState.TESTING -> ServiceHealth.CHECKING
    ConnectionState.CONNECTED -> if (!warning.isNullOrBlank() || detail?.startsWith("Tilkopla ·") == true)
        ServiceHealth.WARNING else ServiceHealth.OK
    ConnectionState.DEMO -> if (baseUrl.isNotBlank() && token.isNotBlank()) ServiceHealth.UNCHECKED else ServiceHealth.UNCONFIGURED
}
