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

/** `content://<package>.launcher/profiles`, for Stova only: the profiles it may offer as tiles. Read-only. */
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

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.app.reelstack.profile"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private companion object {
        val COLUMNS = arrayOf("id", "name", "kind", "user", "avatar", "kid")
    }
}
