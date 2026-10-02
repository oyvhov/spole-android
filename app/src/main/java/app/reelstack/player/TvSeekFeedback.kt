package app.reelstack.player

/** Actual movement in one direction; retained while the on-screen message fades away. */
internal data class TvSeekFeedback(val deltaMs: Long, val elapsedMs: Long)

internal fun tvSeekFeedback(previous: TvSeekFeedback?, fromMs: Long, toMs: Long, elapsedMs: Long): TvSeekFeedback? {
    val delta = toMs - fromMs
    if (delta == 0L) return previous
    val continuing = previous != null && elapsedMs - previous.elapsedMs in 0L..999L &&
        (previous.deltaMs > 0) == (delta > 0)
    return TvSeekFeedback(delta + if (continuing) previous.deltaMs else 0L, elapsedMs)
}
