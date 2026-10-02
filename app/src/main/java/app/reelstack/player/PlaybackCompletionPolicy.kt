package app.reelstack.player

/** Jellyfin's default completion rule, used only until the server confirms its own play state. */
internal fun playbackNearEnd(positionMs: Long, durationMs: Long): Boolean =
    durationMs > 0 && positionMs > 0 &&
        (positionMs.toDouble() / durationMs > .9 || positionMs >= durationMs - 1_000)
