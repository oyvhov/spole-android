package app.reelstack.player

internal enum class RemotePlaybackKey { SELECT, UP, DOWN, LEFT, RIGHT, TOGGLE, PLAY, PAUSE, REWIND, FORWARD }
internal enum class RemotePlaybackAction { DEFAULT, IGNORE, REVEAL, TOGGLE, REWIND, FORWARD }

/** Hidden video controls are not a focus destination. Never activate their stale focused button. */
internal fun remotePlaybackAction(
    key: RemotePlaybackKey, controlsVisible: Boolean, playing: Boolean, blocked: Boolean,
): RemotePlaybackAction {
    val mediaKey = key in setOf(RemotePlaybackKey.TOGGLE, RemotePlaybackKey.PLAY, RemotePlaybackKey.PAUSE,
        RemotePlaybackKey.REWIND, RemotePlaybackKey.FORWARD)
    if (blocked) return if (mediaKey) RemotePlaybackAction.IGNORE else RemotePlaybackAction.DEFAULT
    return when (key) {
        RemotePlaybackKey.TOGGLE -> RemotePlaybackAction.TOGGLE
        RemotePlaybackKey.PLAY -> if (!playing) RemotePlaybackAction.TOGGLE else RemotePlaybackAction.IGNORE
        RemotePlaybackKey.PAUSE -> if (playing) RemotePlaybackAction.TOGGLE else RemotePlaybackAction.IGNORE
        RemotePlaybackKey.REWIND -> RemotePlaybackAction.REWIND
        RemotePlaybackKey.FORWARD -> RemotePlaybackAction.FORWARD
        else -> if (controlsVisible) RemotePlaybackAction.DEFAULT else when (key) {
            RemotePlaybackKey.SELECT -> RemotePlaybackAction.TOGGLE
            RemotePlaybackKey.LEFT -> RemotePlaybackAction.REWIND
            RemotePlaybackKey.RIGHT -> RemotePlaybackAction.FORWARD
            else -> RemotePlaybackAction.REVEAL
        }
    }
}
