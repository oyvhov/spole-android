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

/** A final episode gets one clear way out, but only after the server confirmed there is no next one. */
internal fun PlayerScreenState.showSeriesFinishedOffer(): Boolean = ended && episode != null &&
    nextEpisodeResolved && nextEpisode == null && !busy && error == null && !browsing && !awaitingResume

/** The preview may appear near the credits, but automatic playback never cuts an episode short. */
internal fun PlayerScreenState.canCountDownNextEpisode(): Boolean = ended && !busy && error == null &&
    !browsing && !awaitingResume && shouldOfferNextEpisode(nextEpisode != null,
        true, nextEpisodeDismissed, ended, positionMs, durationMs,
        if (nextEpisodeOfferEnabled) nextEpisodeLeadSeconds else 0)
