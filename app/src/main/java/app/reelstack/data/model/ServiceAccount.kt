package app.reelstack.data.model

/** Identity returned by the service, without credentials or a separately stored email address. */
data class ServiceAccount(
    val source: ServiceKind,
    val id: String,
    val displayName: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val isPersonal: Boolean = true,
    val isAdmin: Boolean = false,
    val permissions: Long = 0,
    val mediaUserId: String? = null,
)

/** Never equate accounts by display name, email or administrator status. */
fun matchesJellyfinAccount(seerr: ServiceAccount, jellyfinId: String): Boolean =
    seerr.source == ServiceKind.SEERR && seerr.isPersonal && jellyfinId.isNotBlank() &&
        seerr.mediaUserId?.replace("-", "")?.lowercase() == jellyfinId.replace("-", "").lowercase()

fun ServiceAccount.canRequestType(type: String): Boolean = isPersonal &&
    (isAdmin || permissions and 32L != 0L || permissions and (if (type == "tv") 524288L else 262144L) != 0L)

/** Seerr's 4K request rights: all of 4K, or 4K for this type of title. */
fun ServiceAccount.canRequest4k(type: String): Boolean = isPersonal &&
    (isAdmin || permissions and 1024L != 0L || permissions and (if (type == "tv") 4096L else 2048L) != 0L)

/** Seerr's «advanced requests», or a request manager: may choose quality and location. */
fun ServiceAccount.canChooseRequestOptions(): Boolean = isPersonal && (isAdmin || permissions and (8192L or 16L) != 0L)
