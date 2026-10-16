package app.reelstack.data.model

data class RequestProfile(val id: Int, val name: String)

data class RequestFolder(val path: String, val freeBytes: Long?) {
    /** The last folder name is what a person recognises; the full path is detail. */
    val name: String get() = path.trimEnd('/').substringAfterLast('/').ifBlank { path }
}

/** The quality profiles and storage folders of the Radarr or Sonarr that a request would go to. */
data class RequestOptions(
    val serverId: Int,
    val is4k: Boolean,
    val profiles: List<RequestProfile>,
    val folders: List<RequestFolder>,
    val defaultProfileId: Int?,
    val defaultFolder: String?,
)

/** What the request sheet sends; only the choices it showed. */
data class RequestChoice(val is4k: Boolean = false, val serverId: Int? = null, val profileId: Int? = null, val rootFolder: String? = null)

/** «4,8 TB»: SI units, one decimal, the reader's decimal mark. */
fun formatFreeSpace(bytes: Long, locale: java.util.Locale): String {
    val (value, unit) = when {
        bytes >= 1_000_000_000_000 -> bytes / 1e12 to "TB"
        bytes >= 1_000_000_000 -> bytes / 1e9 to "GB"
        else -> bytes / 1e6 to "MB"
    }
    return String.format(locale, "%.1f %s", value, unit)
}
