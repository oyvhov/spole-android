package app.reelstack.launcher

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import app.reelstack.ReelstackApplication
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import app.reelstack.data.network.EndpointValidator
import app.reelstack.data.repository.ownProfileId

/** A profile Stova can offer as a tile: who it is, the account `spole://profile` names, and their picture. */
data class LauncherProfileRow(val id: String, val name: String, val kind: String, val user: String, val avatar: String?, val kid: Boolean)

fun launcherMayList(caller: String?): Boolean = caller in LauncherApproval.LAUNCHERS

/**
 * One row per profile with a signed-in Jellyfin or Emby account. The main profile opens with an
 * account that has no own profile, Jellyfin before Emby. The picture is the server's user image,
 * whose address carries no token; nothing else about the server crosses.
 */
fun launcherProfiles(profiles: List<UserProfile>, connectionsOf: (String) -> List<ServiceConnection>): List<LauncherProfileRow> {
    val ids = profiles.map { it.id }.toSet()
    return profiles.mapNotNull { profile ->
        val library = connectionsOf(profile.id).filter {
            (it.kind == ServiceKind.JELLYFIN || it.kind == ServiceKind.EMBY) && it.token.isNotBlank() && it.userId.isNotBlank()
        }.sortedBy { it.kind != ServiceKind.JELLYFIN }
        val account = (if (profile.isMain) library.firstOrNull { ownProfileId(it.kind, it.userId) !in ids } else null)
            ?: library.firstOrNull() ?: return@mapNotNull null
        LauncherProfileRow(profile.id, profile.name, account.kind.name.lowercase(), account.userId,
            profile.avatarUrl?.takeIf { !profile.isMain } ?: runCatching {
                EndpointValidator.resolve(account.baseUrl, "Users/${account.userId}/Images/Primary")
            }.getOrNull(), profile.isKid)
    }
}

/** What Spole does when the person changes in Stova. */
sealed interface LauncherSelection {
    data class To(val profileId: String) : LauncherSelection
    data object Already : LauncherSelection
    /** Out of a child's profile past Spole's PIN: a launcher cannot do that, a parent can in Spole. */
    data object Blocked : LauncherSelection
    data object Unknown : LauncherSelection
}

/**
 * The profile with this account, an own or child profile before the main one, as for `spole://profile`.
 * Spole follows the person chosen in Stova, but never out of a child's profile when Spole has a PIN.
 */
fun launcherSelection(kind: ServiceKind, user: String, activeProfileId: String, profiles: List<UserProfile>,
    connectionsOf: (String) -> List<ServiceConnection>, pinConfigured: Boolean): LauncherSelection {
    fun has(id: String) = connectionsOf(id).any { it.kind == kind && it.userId == user && it.token.isNotBlank() }
    val target = profiles.filterNot { it.isMain }.sortedBy { it.isKid }.firstOrNull { has(it.id) }
        ?: profiles.firstOrNull { it.isMain }?.takeIf { has("") } ?: return LauncherSelection.Unknown
    if (target.id == activeProfileId) return LauncherSelection.Already
    val active = profiles.firstOrNull { it.id == activeProfileId }
    val kidMode = activeProfileId.isNotBlank() && active?.isKid != false
    if (kidMode && pinConfigured) return LauncherSelection.Blocked
    return LauncherSelection.To(target.id)
}

/**
 * `content://<package>.launcher/profiles`, for Stova only: the profiles it may offer as tiles, and
 * `call("select_profile", kind, user)` to follow the person chosen in Stova. Nothing else is written.
 */
class LauncherProfilesProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val cursor = MatrixCursor(COLUMNS)
        if (!launcherMayList(callingPackage) || uri.pathSegments != listOf("profiles")) return cursor
        val repository = (context?.applicationContext as? ReelstackApplication)?.container?.connectionRepository ?: return cursor
        launcherProfiles(repository.listProfiles(), repository::list).forEach {
            cursor.addRow(arrayOf<Any?>(it.id, it.name, it.kind, it.user, it.avatar, if (it.kid) 1 else 0))
        }
        return cursor
    }

    override fun call(method: String, arg: String?, extras: android.os.Bundle?): android.os.Bundle? {
        if (method != "select_profile" || !launcherMayList(callingPackage)) return null
        val kind = when (extras?.getString("kind")) { "jellyfin" -> ServiceKind.JELLYFIN; "emby" -> ServiceKind.EMBY; else -> return null }
        val user = extras.getString("user")?.takeIf { LauncherLinks.isIdentifier(it) } ?: return null
        val container = (context?.applicationContext as? ReelstackApplication)?.container ?: return null
        val repository = container.connectionRepository
        val result = launcherSelection(kind, user, repository.activeProfileId, repository.listProfiles(), repository::list,
            container.pinSecurity.isPinConfigured())
        if (result is LauncherSelection.To) repository.activeProfileId = result.profileId
        // Fixed words: R8 renames classes in a release.
        val word = when (result) {
            is LauncherSelection.To -> "to"
            LauncherSelection.Already -> "already"
            LauncherSelection.Blocked -> "blocked"
            LauncherSelection.Unknown -> "unknown"
        }
        return android.os.Bundle().apply { putString("result", word) }
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.app.reelstack.profile"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private companion object {
        val COLUMNS = arrayOf("id", "name", "kind", "user", "avatar", "kid")
    }
}
