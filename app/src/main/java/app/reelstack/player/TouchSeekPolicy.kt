package app.reelstack.player

/** Leave the middle available for the play button; both sides are large video targets. */
internal fun touchSeekDelta(x: Float, width: Float): Long? = when {
    width <= 0 || x < 0 || x > width -> null
    x < width * .45f -> -10_000L
    x > width * .55f -> 10_000L
    else -> null
}

internal fun touchSeekTarget(position: Long, delta: Long, duration: Long): Long =
    (position.coerceIn(0, duration.coerceAtLeast(0)) + delta).coerceIn(0, duration.coerceAtLeast(0))
