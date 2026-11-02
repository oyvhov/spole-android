package app.reelstack.player

import androidx.annotation.StringRes
import app.reelstack.R

/**
 * When the skip button exists, and what pressing it does.
 *
 * These are small numbers that are easy to get slightly wrong and hard to see wrong on a
 * television, so they sit in one pure function with a test per boundary and the screen only draws
 * the answer. The values are the ones other players settled on after living with the button: a
 * stretch you could not reach in time is not worth offering, and an offer that never leaves the
 * picture is a nuisance rather than a help.
 */
internal object SegmentRules {
    /** A shorter stretch is gone before a thumb is there; the button would only flash. */
    const val MIN_LENGTH_MS = 3_000L

    /** Nothing is offered in the last moments of a stretch: the jump would land where we already are. */
    const val END_GUARD_MS = 1_500L

    /** The button is on the picture this long from the start of the stretch, then only with the controls. */
    const val BUTTON_WINDOW_MS = 6_000L

    /** Analysis seldom ends on the last frame; credits this close to the end still run to it. */
    const val RUNS_TO_END_TOLERANCE_MS = 2_000L
}

/** What the button does when it is pressed. */
internal sealed interface SegmentAction {
    /** Seek to [targetMs], the end of a marked stretch inside this episode. */
    data class Skip(val kind: PlaybackSegment.Kind, val targetMs: Long) : SegmentAction

    /** The credits run to the end of the file, so there is nothing to skip to: leave for the next episode. */
    data object NextEpisode : SegmentAction
}

/**
 * [fresh] says the playhead is inside the first seconds of the stretch. A fresh offer is the one
 * that takes focus on television; one that only came back because the controls did must not take
 * the controls' focus away from them.
 */
internal data class SegmentOffer(val action: SegmentAction, val fresh: Boolean)

/** The stretch the playhead is inside, if it is long enough and not about to end. */
internal fun offerableSegment(segments: List<PlaybackSegment>, positionMs: Long): PlaybackSegment? =
    segments.filter { segment ->
        segment.endMs - segment.startMs >= SegmentRules.MIN_LENGTH_MS &&
            positionMs >= segment.startMs && positionMs < segment.endMs - SegmentRules.END_GUARD_MS
    }.minByOrNull { it.startMs }

/** Credits reaching the end of the file leave nothing behind them to skip to. */
internal fun runsToTheEnd(segment: PlaybackSegment, durationMs: Long): Boolean =
    segment.kind == PlaybackSegment.Kind.OUTRO && durationMs > 0 &&
        segment.endMs >= durationMs - SegmentRules.RUNS_TO_END_TOLERANCE_MS

/**
 * What the button should offer at [positionMs], or null for no button.
 *
 * An unknown length ([durationMs] of zero) never turns credits into «Neste episode»: that is a
 * claim about the end of the file, and nobody can make it without knowing where the end is. Credits
 * that do run to the end offer the next episode when there is one and nothing when there is not,
 * since a button that seeks to the last frame is not skipping anything.
 */
internal fun segmentOffer(
    segments: List<PlaybackSegment>, positionMs: Long, durationMs: Long,
    hasNextEpisode: Boolean, controlsVisible: Boolean,
): SegmentOffer? {
    val segment = offerableSegment(segments, positionMs) ?: return null
    val fresh = positionMs - segment.startMs < SegmentRules.BUTTON_WINDOW_MS
    if (!fresh && !controlsVisible) return null
    val action = when {
        runsToTheEnd(segment, durationMs) -> if (hasNextEpisode) SegmentAction.NextEpisode else return null
        else -> SegmentAction.Skip(segment.kind, segment.endMs)
    }
    return SegmentOffer(action, fresh)
}

internal fun PlayerScreenState.segmentOffer(controlsVisible: Boolean): SegmentOffer? =
    segmentOffer(segments, positionMs, durationMs, nextEpisode != null, controlsVisible)

@StringRes
internal fun SegmentAction.labelRes(): Int = when (this) {
    SegmentAction.NextEpisode -> R.string.player_next_episode
    is SegmentAction.Skip -> when (kind) {
        PlaybackSegment.Kind.INTRO -> R.string.player_skip_intro
        PlaybackSegment.Kind.OUTRO -> R.string.player_skip_outro
        PlaybackSegment.Kind.RECAP -> R.string.player_skip_recap
        PlaybackSegment.Kind.PREVIEW -> R.string.player_skip_preview
    }
}
