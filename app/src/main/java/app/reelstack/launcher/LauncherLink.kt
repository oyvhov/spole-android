package app.reelstack.launcher

import app.reelstack.data.model.ServiceKind
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.StandardCharsets

data class LauncherLink(
    val kind: ServiceKind,
    val userId: String,
    /** An item id for play/details, or a library view id for LIBRARY. */
    val itemId: String,
    val action: LauncherAction,
)

enum class LauncherAction { DETAILS, PLAY, LIBRARY }

/** Selects the main activity destination without interpreting a library id as a media item. */
internal inline fun LauncherLink.routeInMain(
    details: (LauncherLink) -> Unit,
    play: (LauncherLink) -> Unit,
    library: (LauncherLink) -> Unit,
) {
    when (action) {
        LauncherAction.DETAILS -> details(this)
        LauncherAction.PLAY -> play(this)
        LauncherAction.LIBRARY -> library(this)
    }
}

object LauncherLinks {
    private val identifier = Regex("[A-Za-z0-9_-]{1,128}")
    private val encodedBytes = Regex("(?:%[0-9A-Fa-f]{2})+")

    fun parse(raw: String?): LauncherLink? {
        if (raw.isNullOrEmpty()) return null
        return try {
            val uri = URI(raw)
            if (uri.scheme != "spole" || uri.rawAuthority !in setOf("open", "library") ||
                !uri.rawPath.isNullOrEmpty() || uri.rawFragment != null
            ) return null

            val library = uri.rawAuthority == "library"
            val requiredParameters = if (library) setOf("kind", "user", "library")
                else setOf("kind", "user", "item", "action")
            val query = uri.rawQuery ?: return null
            val parameters = mutableMapOf<String, String>()
            query.split('&').forEach { parameter ->
                val parts = parameter.split('=', limit = 2)
                val name = decode(parts[0])
                val value = decode(parts.getOrElse(1) { "" })
                if (name in requiredParameters) {
                    if (parameters.put(name, value) != null) return null
                }
            }
            val kind = when (parameters["kind"]) {
                "jellyfin" -> ServiceKind.JELLYFIN
                "emby" -> ServiceKind.EMBY
                else -> return null
            }
            val action = if (library) LauncherAction.LIBRARY else when (parameters["action"]) {
                    "details" -> LauncherAction.DETAILS
                    "play" -> LauncherAction.PLAY
                    else -> return null
                }
            val userId = parameters["user"]?.takeIf(identifier::matches) ?: return null
            val itemId = parameters[if (library) "library" else "item"]?.takeIf(identifier::matches) ?: return null
            LauncherLink(kind, userId, itemId, action)
        } catch (_: URISyntaxException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: CharacterCodingException) {
            null
        }
    }

    private fun decode(raw: String): String {
        // URLDecoder replaces malformed UTF-8, so validate escaped bytes strictly first.
        encodedBytes.findAll(raw).forEach { match ->
            val bytes = match.value.chunked(3).map { it.substring(1).toInt(16).toByte() }.toByteArray()
            StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes))
        }
        return URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
    }
}
