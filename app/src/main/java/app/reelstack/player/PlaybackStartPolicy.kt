package app.reelstack.player

/** Null means ask. Positions always come from the authenticated Jellyfin user's item. */
internal fun playbackStartPosition(resumeMs: Long, durationMs: Long, played: Boolean, autoResume: Boolean): Long? {
    val position = if (played || resumeMs < 0 || durationMs > 0 && resumeMs >= durationMs) 0 else resumeMs
    return if (position > 0 && !autoResume) null else position
}
