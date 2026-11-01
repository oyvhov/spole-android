package app.reelstack.launcher

import app.reelstack.data.model.DiscoverMedia
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
    /** An item id for play/details, a library view id for LIBRARY, and empty for PROFILE and DISCOVER. */
    val itemId: String,
    val action: LauncherAction,
    /** `start=0` on a play link: from the beginning, without the question about where it stopped. */
    val fromStart: Boolean = false,
    /** The Seerr title of a DISCOVER link, `seerr-<movie|tv>-<tmdb>`, as Spole's Discover names it; null otherwise. */
    val discover: String? = null,
)

/**
 * PROFILE opens Spole as the profile that has the account, at Home: a launcher's tile for a person.
 * DISCOVER opens a Seerr title in Spole's own Discover details, for a grown-up's account only.
 */
enum class LauncherAction { DETAILS, PLAY, LIBRARY, PROFILE, DISCOVER }

/** Selects the main activity destination without interpreting a library id as a media item. */
internal inline fun LauncherLink.routeInMain(
    details: (LauncherLink) -> Unit,
    play: (LauncherLink) -> Unit,
    library: (LauncherLink) -> Unit,
    profile: (LauncherLink) -> Unit,
    discover: (DiscoverMedia) -> Unit,
) {
    when (action) {
        LauncherAction.DETAILS -> details(this)
        LauncherAction.PLAY -> play(this)
        LauncherAction.LIBRARY -> library(this)
        LauncherAction.PROFILE -> profile(this)
        LauncherAction.DISCOVER -> discoverTitle()?.let(discover)
    }
}

object LauncherLinks {
    private val identifier = Regex("[A-Za-z0-9_-]{1,128}")
    fun isIdentifier(value: String): Boolean = identifier.matches(value)
    private val encodedBytes = Regex("(?:%[0-9A-Fa-f]{2})+")
    /** A TMDB id as Spole writes it: above zero, no leading zero, at most nine digits. */
    private val tmdbId = Regex("[1-9][0-9]{0,8}")

    /** The type and TMDB id written in a `seerr-<type>-<tmdb>` id, unchecked: [parse] is what checks them. */
    internal fun discoverParts(id: String): Pair<String, String>? =
        id.split('-').takeIf { it.size == 3 && it[0] == "seerr" }?.let { it[1] to it[2] }

    fun parse(raw: String?): LauncherLink? {
        if (raw.isNullOrEmpty()) return null
        return try {
            val uri = URI(raw)
            if (uri.scheme != "spole" || uri.rawAuthority !in setOf("open", "library", "profile", "discover") ||
                !uri.rawPath.isNullOrEmpty() || uri.rawFragment != null
            ) return null

            val library = uri.rawAuthority == "library"
            val profile = uri.rawAuthority == "profile"
            val discover = uri.rawAuthority == "discover"
            val requiredParameters = when {
                library -> setOf("kind", "user", "library")
                profile -> setOf("kind", "user")
                discover -> setOf("kind", "user", "type", "tmdb")
                else -> setOf("kind", "user", "item", "action", "start")
            }
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
            val action = when {
                library -> LauncherAction.LIBRARY
                profile -> LauncherAction.PROFILE
                discover -> LauncherAction.DISCOVER
                else -> when (parameters["action"]) {
                    "details" -> LauncherAction.DETAILS
                    "play" -> LauncherAction.PLAY
                    else -> return null
                }
            }
            val userId = parameters["user"]?.takeIf(identifier::matches) ?: return null
            if (profile) return LauncherLink(kind, userId, "", action)
            if (discover) {
                val type = parameters["type"]?.takeIf { it == "movie" || it == "tv" } ?: return null
                val tmdb = parameters["tmdb"]?.takeIf(tmdbId::matches) ?: return null
                return LauncherLink(kind, userId, "", action, discover = "seerr-$type-$tmdb")
            }
            val itemId = parameters[if (library) "library" else "item"]?.takeIf(identifier::matches) ?: return null
            // Only «from the start» exists, and only for playing.
            val start = parameters["start"]
            if (start != null && (start != "0" || action != LauncherAction.PLAY)) return null
            LauncherLink(kind, userId, itemId, action, fromStart = start != null)
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
