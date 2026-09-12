package app.reelstack.player

/** Unknown runtimes never produce an early offer. Dismissal lasts for this episode. */
internal fun shouldOfferNextEpisode(
    hasNext: Boolean, enabled: Boolean, dismissed: Boolean, ended: Boolean,
    positionMs: Long, durationMs: Long, leadSeconds: Int,
): Boolean = hasNext && enabled && !dismissed && (ended ||
    (durationMs > 0 && positionMs > 0 && positionMs <= durationMs &&
        durationMs - positionMs <= leadSeconds.coerceIn(0, 300) * 1_000L))

internal fun PlayerScreenState.showNextEpisodeOffer(): Boolean = !busy && error == null &&
    !browsing && !awaitingResume && shouldOfferNextEpisode(nextEpisode != null,
        nextEpisodeOfferEnabled || (ended && nextEpisodeCountdown != null), nextEpisodeDismissed,
        ended, positionMs, durationMs, nextEpisodeLeadSeconds)
