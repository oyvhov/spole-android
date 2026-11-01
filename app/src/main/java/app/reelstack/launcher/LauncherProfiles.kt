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
    val target = launcherProfileFor(kind, user, profiles, connectionsOf) ?: return LauncherSelection.Unknown
    if (target.id == activeProfileId) return LauncherSelection.Already
    val active = profiles.firstOrNull { it.id == activeProfileId }
    val kidMode = activeProfileId.isNotBlank() && active?.isKid != false
    if (kidMode && pinConfigured) return LauncherSelection.Blocked
    return LauncherSelection.To(target.id)
}

/**
 * `content://<package>.launcher`, for Stova only:
 * - `profiles`: the profiles it may offer as tiles;
 * - `rows?kind=&user=`: that person's Home rows, fetched with Spole's own sign-in, so a Stova profile
 *   that follows the person needs no account of its own (see [launcherRowItems]);
 * - `call("select_profile", kind, user)`: follow the person chosen in Stova;
 * - `call("item_action", kind, user, item, action)`: mark a title watched, or take it out of «Hald fram»;
 * - `discover…?kind=&user=`: that person's Seerr, through Spole's sign-in (see [LauncherDiscoverPath]);
 * - `call("discover_request", kind, user, type, tmdb, seasons)` and `call("discover_withdraw", kind, user, request_id)`:
 *   ask that Seerr for a title, or withdraw a request the person follows (see [LauncherDiscoverActions]).
 * No token or address of a server ever crosses; picture addresses carry none.
 */
class LauncherProfilesProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        if (uri.pathSegments == listOf("rows")) return rows(uri)
        LauncherDiscoverPath.of(uri.pathSegments)?.let { return discover(it, uri) }
        val cursor = MatrixCursor(COLUMNS)
        if (!launcherMayList(callingPackage) || uri.pathSegments != listOf("profiles")) return cursor
        val repository = (context?.applicationContext as? ReelstackApplication)?.container?.connectionRepository ?: return cursor
        launcherProfiles(repository.listProfiles(), repository::list).forEach {
            cursor.addRow(arrayOf<Any?>(it.id, it.name, it.kind, it.user, it.avatar, if (it.kid) 1 else 0))
        }
        return cursor
    }

    /**
     * One person's rows, asked of the server on this binder thread: Stova calls it off its own main
     * thread. Every library account of the person's profile gives its rows, one after another.
     */
    private fun rows(uri: Uri): Cursor {
        val cursor = MatrixCursor(ROW_COLUMNS)
        if (!launcherMayList(callingPackage)) return cursor
        val (kind, user) = accountIn(uri) ?: return cursor
        val container = (context?.applicationContext as? ReelstackApplication)?.container ?: return cursor
        val repository = container.connectionRepository
        val profile = launcherProfileFor(kind, user, repository.listProfiles(), repository::list) ?: return cursor
        launcherLibraryAccounts(repository.list(profile.id)).forEach { connection ->
            runCatching { launcherRowItems(connection, container.mediaSyncRepository.launcherRows(connection)) }
                .getOrNull().orEmpty().forEach {
                    cursor.addRow(arrayOf<Any?>(it.row, it.kind, it.user, it.server, it.id, it.type, it.title, it.seriesName,
                        it.season, it.episode, it.year, it.runtime, it.progress, it.overview, it.wide, it.poster, it.backdrop,
                        it.logo, it.community, it.critic, it.official))
                }
        }
        return cursor
    }

    /**
     * Discover for one person, asked of their Seerr on this binder thread as [rows] is. Every answer
     * to Stova says in the `seerr` extra what the person may do; an unknown person, a child or a
     * profile without a Seerr that is set up gets `none` and no rows. A search says its `page` and
     * `has_more` too.
     */
    private fun discover(path: LauncherDiscoverPath, uri: Uri): Cursor {
        if (!launcherMayList(callingPackage)) return MatrixCursor(path.columns)
        val (seerr, access) = seerrFor(accountIn(uri))
        val container = (context?.applicationContext as? ReelstackApplication)?.container
        val answer = if (access == LauncherSeerrAccess.NONE || seerr == null || container == null) LauncherDiscoverAnswer.EMPTY
            else LauncherDiscoverQueries(container.mediaSyncRepository, container.requestTrackingRepository) { it.text(container.appContext) }
                .answer(path, uri, seerr, access)
        return answer.cursor(path, access)
    }

    /**
     * A request sent to the person's Seerr, or one they follow withdrawn, on this binder thread as
     * [discover] is (see [LauncherDiscoverActions]). Stova only ever gets a word back: when Spole
     * itself trips, that is `failed` too.
     */
    private fun discoverAction(method: String, extras: android.os.Bundle?): android.os.Bundle? {
        if (!launcherMayList(callingPackage)) return null
        val result = runCatching {
            val (seerr, access) = seerrFor(account(extras?.getString("kind"), extras?.getString("user")))
            val container = (context?.applicationContext as? ReelstackApplication)?.container
                ?: return@runCatching LauncherDiscoverResult.UNKNOWN
            val actions = LauncherDiscoverActions(container.mediaSyncRepository, container.requestTrackingRepository)
            if (method == DISCOVER_REQUEST) actions.request(extras, seerr, access) else actions.withdraw(extras, seerr, access)
        }.getOrDefault(LauncherDiscoverResult.FAILED)
        return android.os.Bundle().apply { putString("result", result.wire) }
    }

    /**
     * The Seerr of the Spole profile with this account, found as for [rows], and what the person may
     * do with it. An unknown person may do nothing.
     */
    private fun seerrFor(account: Pair<ServiceKind, String>?): Pair<ServiceConnection?, LauncherSeerrAccess> {
        val repository = (context?.applicationContext as? ReelstackApplication)?.container?.connectionRepository
            ?: return null to LauncherSeerrAccess.NONE
        val (kind, user) = account ?: return null to LauncherSeerrAccess.NONE
        val profile = launcherProfileFor(kind, user, repository.listProfiles(), repository::list) ?: return null to LauncherSeerrAccess.NONE
        val seerr = repository.list(profile.id).firstOrNull { it.kind == ServiceKind.SEERR }
        return seerr to launcherSeerrAccess(profile.isKid, seerr)
    }

    override fun call(method: String, arg: String?, extras: android.os.Bundle?): android.os.Bundle? {
        if (method == "item_action") return itemAction(extras)
        if (method == DISCOVER_REQUEST || method == DISCOVER_WITHDRAW) return discoverAction(method, extras)
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

    /**
     * A title marked watched, or taken out of «Hald fram», with the account Stova named. Spole's own
     * journal forgets the title too when that account belongs to the profile Spole has open, so
     * Spole's Home agrees the next time it is shown.
     */
    private fun itemAction(extras: android.os.Bundle?): android.os.Bundle? {
        if (!launcherMayList(callingPackage)) return null
        val kind = serviceKind(extras?.getString("kind")) ?: return null
        val user = extras?.getString("user")?.takeIf { LauncherLinks.isIdentifier(it) } ?: return null
        val item = extras.getString("item")?.takeIf { LauncherLinks.isIdentifier(it) } ?: return null
        val action = LauncherItemAction.of(extras.getString("action")) ?: return null
        val container = (context?.applicationContext as? ReelstackApplication)?.container ?: return null
        val repository = container.connectionRepository
        fun answer(word: String) = android.os.Bundle().apply { putString("result", word) }
        val profile = launcherProfileFor(kind, user, repository.listProfiles(), repository::list)
            ?: return answer(LauncherActionResult.UNKNOWN)
        val connection = repository.list(profile.id).firstOrNull { it.kind == kind && it.userId == user && it.token.isNotBlank() }
            ?: return answer(LauncherActionResult.UNKNOWN)
        val done = runCatching {
            when (action) {
                LauncherItemAction.WATCHED -> container.mediaServerClient.setPlayed(connection, connection.userId, item, true)
                LauncherItemAction.REMOVE_RESUME -> container.mediaServerClient.clearResume(connection, connection.userId, item)
            }
        }.isSuccess
        if (done && profile.id == repository.activeProfileId) container.localPlaybackStore.forget(connection, item)
        return answer(if (done) LauncherActionResult.OK else LauncherActionResult.FAILED)
    }

    /** The account a query names with `kind` and `user`, as `spole://profile` takes them. */
    private fun accountIn(uri: Uri): Pair<ServiceKind, String>? = account(uri.getQueryParameter("kind"), uri.getQueryParameter("user"))

    private fun account(kind: String?, user: String?): Pair<ServiceKind, String>? {
        val service = serviceKind(kind) ?: return null
        return service to (user?.takeIf { LauncherLinks.isIdentifier(it) } ?: return null)
    }

    private fun serviceKind(value: String?) = when (value) {
        "jellyfin" -> ServiceKind.JELLYFIN
        "emby" -> ServiceKind.EMBY
        else -> null
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.app.reelstack.profile"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private companion object {
        val COLUMNS = arrayOf("id", "name", "kind", "user", "avatar", "kid")
        val ROW_COLUMNS = arrayOf("row", "kind", "user", "server", "id", "type", "title", "series", "season", "episode",
            "year", "runtime", "progress", "overview", "wide", "poster", "backdrop", "logo", "community", "critic", "official")
        const val DISCOVER_REQUEST = "discover_request"
        const val DISCOVER_WITHDRAW = "discover_withdraw"
    }
}
