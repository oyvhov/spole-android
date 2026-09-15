package app.reelstack.data.model

/** Only public video pages; never forward arbitrary server URLs or credentials to another app. */
fun trailerLink(raw: String?): String? = runCatching {
    val uri = java.net.URI(raw?.trim() ?: return null)
    if (uri.scheme != "https" || uri.userInfo != null || uri.port != -1) return null
    val host = uri.host?.lowercase() ?: return null
    val id = when (host) {
        "youtu.be" -> uri.path.removePrefix("/")
        "youtube.com", "www.youtube.com", "m.youtube.com" -> {
            if (uri.path == "/watch") uri.rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("v=") }?.substringAfter('=')
            else uri.path.takeIf { it.startsWith("/embed/") || it.startsWith("/shorts/") }?.substringAfterLast('/')
        }
        else -> null
    }
    if (id != null && Regex("[A-Za-z0-9_-]{11}").matches(id)) "https://www.youtube.com/watch?v=$id" else null
}.getOrNull()
