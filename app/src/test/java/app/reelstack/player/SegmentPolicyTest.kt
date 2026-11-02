package app.reelstack.player

import app.reelstack.player.PlaybackSegment.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * When the skip button exists, and what it does.
 *
 * The rules are small numbers that are easy to get slightly wrong and hard to see wrong on a
 * television: a button that flashes for a second, a "skip" that jumps to the last frame, a button
 * that stays on top of the picture for ninety seconds. They live in one pure function so each
 * boundary has a test, and the screen only draws the answer.
 */
class SegmentPolicyTest {

    private val intro = PlaybackSegment(Kind.INTRO, 10_000, 100_000)
    private val duration = 1_500_000L

    private fun offer(
        segments: List<PlaybackSegment>, position: Long, controls: Boolean = false,
        next: Boolean = true, length: Long = duration,
    ) = segmentOffer(segments, position, length, hasNextEpisode = next, controlsVisible = controls)

    private fun skip(offer: SegmentOffer?) = offer?.action as? SegmentAction.Skip

    @Test fun nothingIsOfferedOutsideEveryStretch() {
        assertNull(offer(emptyList(), 50_000))
        assertNull(offer(listOf(intro), 9_999, controls = true))
        assertNull(offer(listOf(intro), 100_000, controls = true))
        assertNull(offer(listOf(intro), 400_000, controls = true))
    }

    @Test fun aStretchShorterThanThreeSecondsGetsNoButton() {
        val short = PlaybackSegment(Kind.RECAP, 20_000, 22_999)
        assertNull(offer(listOf(short), 20_000, controls = true))
        assertNull(offer(listOf(short), 21_000, controls = true))
        val exactly = PlaybackSegment(Kind.RECAP, 20_000, 23_000)
        assertEquals(Kind.RECAP, skip(offer(listOf(exactly), 20_000))?.kind)
    }

    @Test fun theLastSecondAndAHalfOfAStretchIsNeverOffered() {
        // The guard is half-open, as it was in activeSegment: 98 500 is already too late.
        assertEquals(Kind.INTRO, skip(offer(listOf(intro), 98_499, controls = true))?.kind)
        assertNull(offer(listOf(intro), 98_500, controls = true))
        assertNull(offer(listOf(intro), 99_999, controls = true))
    }

    @Test fun theButtonIsOfferedForSixSecondsFromTheStartWhateverTheControlsDo() {
        assertEquals(true, offer(listOf(intro), 10_000)?.fresh)
        assertEquals(true, offer(listOf(intro), 15_999)?.fresh)
        assertNull(offer(listOf(intro), 16_000))
        assertNull(offer(listOf(intro), 50_000))
    }

    @Test fun afterSixSecondsTheButtonBelongsToTheControls() {
        val shown = offer(listOf(intro), 16_000, controls = true)
        assertEquals(Kind.INTRO, skip(shown)?.kind)
        assertEquals(100_000L, skip(shown)?.targetMs)
        assertEquals(false, shown?.fresh)
        // It goes with them again, every time, and the end guard still wins over the controls.
        assertNull(offer(listOf(intro), 60_000, controls = false))
        assertNull(offer(listOf(intro), 99_000, controls = true))
    }

    @Test fun seekingBackToTheStartMakesTheButtonFreshAgain() {
        assertNull(offer(listOf(intro), 60_000))
        assertEquals(true, offer(listOf(intro), 11_000)?.fresh)
    }

    @Test fun everySkippableKindOffersToJumpToTheEndOfItsStretch() {
        val recap = PlaybackSegment(Kind.RECAP, 0, 9_000)
        val preview = PlaybackSegment(Kind.PREVIEW, 600_000, 640_000)
        val outro = PlaybackSegment(Kind.OUTRO, 700_000, 760_000)
        val all = listOf(recap, intro, preview, outro)
        assertEquals(SegmentAction.Skip(Kind.RECAP, 9_000), offer(all, 1_000)?.action)
        assertEquals(SegmentAction.Skip(Kind.INTRO, 100_000), offer(all, 12_000)?.action)
        assertEquals(SegmentAction.Skip(Kind.PREVIEW, 640_000), offer(all, 601_000)?.action)
        assertEquals(SegmentAction.Skip(Kind.OUTRO, 760_000), offer(all, 701_000)?.action)
    }

    @Test fun creditsThatRunToTheEndOfTheFileOfferTheNextEpisodeInstead() {
        val credits = PlaybackSegment(Kind.OUTRO, 1_440_000, duration)
        assertEquals(SegmentAction.NextEpisode, offer(listOf(credits), 1_441_000)?.action)
        assertEquals(true, offer(listOf(credits), 1_441_000)?.fresh)
        // The same six seconds, then only with the controls.
        assertNull(offer(listOf(credits), 1_447_000))
        assertEquals(SegmentAction.NextEpisode, offer(listOf(credits), 1_447_000, controls = true)?.action)
    }

    @Test fun creditsThatRunToTheEndWithNothingAfterThemOfferNothing() {
        val credits = PlaybackSegment(Kind.OUTRO, 1_440_000, duration)
        assertNull(offer(listOf(credits), 1_441_000, next = false))
        assertNull(offer(listOf(credits), 1_447_000, controls = true, next = false))
    }

    @Test fun creditsCountAsRunningToTheEndWithinTwoSecondsOfTheFile() {
        val exactly = PlaybackSegment(Kind.OUTRO, 1_440_000, duration - 2_000)
        assertEquals(SegmentAction.NextEpisode, offer(listOf(exactly), 1_441_000)?.action)
        val justShort = PlaybackSegment(Kind.OUTRO, 1_440_000, duration - 2_001)
        assertEquals(SegmentAction.Skip(Kind.OUTRO, duration - 2_001), offer(listOf(justShort), 1_441_000)?.action)
        // A past-the-end mark is still the end.
        val over = PlaybackSegment(Kind.OUTRO, 1_440_000, duration + 500)
        assertEquals(SegmentAction.NextEpisode, offer(listOf(over), 1_441_000)?.action)
    }

    @Test fun creditsWithASceneAfterThemAreSkippedWhetherOrNotThereIsANextEpisode() {
        val credits = PlaybackSegment(Kind.OUTRO, 1_300_000, 1_380_000)
        assertEquals(SegmentAction.Skip(Kind.OUTRO, 1_380_000), offer(listOf(credits), 1_301_000)?.action)
        assertEquals(SegmentAction.Skip(Kind.OUTRO, 1_380_000), offer(listOf(credits), 1_301_000, next = false)?.action)
    }

    @Test fun anUnknownLengthNeverTurnsCreditsIntoTheNextEpisode() {
        val credits = PlaybackSegment(Kind.OUTRO, 1_440_000, 1_500_000)
        assertEquals(SegmentAction.Skip(Kind.OUTRO, 1_500_000), offer(listOf(credits), 1_441_000, length = 0)?.action)
    }

    @Test fun onlyCreditsCanRunToTheEnd() {
        // Skipping the last seconds of a preview is still skipping; the episode then ends by itself.
        val preview = PlaybackSegment(Kind.PREVIEW, 1_440_000, duration)
        assertEquals(SegmentAction.Skip(Kind.PREVIEW, duration), offer(listOf(preview), 1_441_000, next = false)?.action)
    }

    @Test fun theEarliestStretchWinsWhereTwoOverlap() {
        val recap = PlaybackSegment(Kind.RECAP, 0, 60_000)
        val overlapping = PlaybackSegment(Kind.INTRO, 30_000, 120_000)
        assertEquals(Kind.RECAP, skip(offer(listOf(overlapping, recap), 31_000, controls = true))?.kind)
        assertEquals(Kind.INTRO, skip(offer(listOf(overlapping, recap), 70_000, controls = true))?.kind)
    }

    @Test fun theScreenStateReadsItsOwnFields() {
        val state = PlayerScreenState(
            segments = listOf(PlaybackSegment(Kind.OUTRO, 1_440_000, duration)),
            positionMs = 1_441_000, durationMs = duration,
        )
        assertNull(state.segmentOffer(controlsVisible = false))
        val next = PlayableItem("next", "Show", "Episode")
        assertEquals(SegmentAction.NextEpisode, state.copy(nextEpisode = next).segmentOffer(false)?.action)
        assertNull(state.copy(nextEpisode = next, positionMs = 1_448_000).segmentOffer(controlsVisible = false))
        assertEquals(SegmentAction.NextEpisode, state.copy(nextEpisode = next, positionMs = 1_448_000).segmentOffer(true)?.action)
    }
}
