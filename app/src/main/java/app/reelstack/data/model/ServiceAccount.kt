package app.reelstack.data.model

/** Identity returned by the service, without credentials or a separately stored email address. */
data class ServiceAccount(
    val source: ServiceKind,
    val id: String,
    val displayName: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val isPersonal: Boolean = true,
)
