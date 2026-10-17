@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ForwardingRenderer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererConfiguration
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SampleStream

/**
 * Text is done when the picture is, not when its last line is.
 *
 * Media3 keeps a text renderer unfinished until its last cue has ended, and the player keeps
 * playing until every renderer has finished. A line that runs past the video — a watermark until
 * 9:59:59, a file made for another cut — held an episode at its end: no countdown to the next one,
 * no "Stopped" for the server. Once the text has been read to the end and the position has passed
 * the period's own duration, the text has nothing left to show.
 */
internal fun textRendererEnded(delegateEnded: Boolean, readToEnd: Boolean, positionUs: Long,
    offsetUs: Long, periodDurationUs: Long): Boolean =
    delegateEnded || readToEnd && periodDurationUs != C.TIME_UNSET && positionUs - offsetUs >= periodDurationUs

internal class MediaBoundTextRenderer(private val text: Renderer) : ForwardingRenderer(text) {
    private val period = Timeline.Period()
    @Volatile private var timeline: Timeline = Timeline.EMPTY
    private var periodId: MediaSource.MediaPeriodId? = null
    private var offsetUs = 0L
    private var positionUs = 0L

    override fun setTimeline(timeline: Timeline) { this.timeline = timeline; super.setTimeline(timeline) }
    override fun enable(configuration: RendererConfiguration, formats: Array<out Format>, stream: SampleStream,
        positionUs: Long, joining: Boolean, mayRenderStartOfStream: Boolean, startPositionUs: Long, offsetUs: Long,
        mediaPeriodId: MediaSource.MediaPeriodId) {
        periodId = mediaPeriodId; this.offsetUs = offsetUs; this.positionUs = positionUs
        super.enable(configuration, formats, stream, positionUs, joining, mayRenderStartOfStream, startPositionUs, offsetUs, mediaPeriodId)
    }
    override fun replaceStream(formats: Array<out Format>, stream: SampleStream, startPositionUs: Long, offsetUs: Long,
        mediaPeriodId: MediaSource.MediaPeriodId) {
        periodId = mediaPeriodId; this.offsetUs = offsetUs
        super.replaceStream(formats, stream, startPositionUs, offsetUs, mediaPeriodId)
    }
    override fun render(positionUs: Long, elapsedRealtimeUs: Long) { this.positionUs = positionUs; super.render(positionUs, elapsedRealtimeUs) }
    override fun isEnded(): Boolean {
        val duration = periodId?.let { id -> runCatching {
            timeline.getIndexOfPeriod(id.periodUid).takeIf { it != C.INDEX_UNSET }
                ?.let { timeline.getPeriod(it, period).durationUs }
        }.getOrNull() } ?: C.TIME_UNSET
        return textRendererEnded(super.isEnded(), hasReadStreamToEnd() && isCurrentStreamFinal, positionUs, offsetUs, duration)
    }
}
