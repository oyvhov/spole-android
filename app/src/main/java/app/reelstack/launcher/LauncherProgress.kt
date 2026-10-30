package app.reelstack.launcher

import android.content.Context
import android.content.Intent
import app.reelstack.data.model.ServiceKind

/**
 * Tells the TV's launcher (Stova) where a title stopped, so its "Continue" row is right the moment
 * the viewer comes back, before the server's own lists have caught up. Nothing in it is secret: the
 * user and item ids are the ones Stova sent in its `spole://` link.
 */
object LauncherProgress {
    const val ACTION = "app.reelstack.action.PLAYBACK_PROGRESS"

    fun extras(kind: ServiceKind, userId: String, itemId: String, positionMs: Long, durationMs: Long, ended: Boolean): Map<String, Any>? {
        val name = when (kind) { ServiceKind.JELLYFIN -> "jellyfin"; ServiceKind.EMBY -> "emby"; else -> return null }
        if (userId.isBlank() || itemId.isBlank()) return null
        return mapOf("kind" to name, "user" to userId, "item" to itemId,
            "position_ms" to positionMs.coerceAtLeast(0), "duration_ms" to durationMs.coerceAtLeast(0), "ended" to ended)
    }

    /** One explicit broadcast per launcher package; a launcher that is not installed simply gets none. */
    fun send(context: Context, kind: ServiceKind, userId: String, itemId: String, positionMs: Long, durationMs: Long, ended: Boolean) {
        val extras = extras(kind, userId, itemId, positionMs, durationMs, ended) ?: return
        for (launcher in LauncherApproval.LAUNCHERS) {
            val intent = Intent(ACTION).setPackage(launcher)
            extras.forEach { (key, value) ->
                when (value) { is String -> intent.putExtra(key, value); is Long -> intent.putExtra(key, value); is Boolean -> intent.putExtra(key, value) }
            }
            runCatching { context.sendBroadcast(intent) }
        }
    }
}
