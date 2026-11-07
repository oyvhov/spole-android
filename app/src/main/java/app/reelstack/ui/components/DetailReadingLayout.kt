package app.reelstack.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** TV art stays anchored while the adjacent reading column scrolls independently. */
@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal fun DetailReadingLayout(tv: Boolean, scroll: ScrollState,
    artwork: @Composable () -> Unit, series: Boolean = false,
    heading: @Composable () -> Unit = {}, cinematic: Boolean = false,
    content: @Composable ColumnScope.() -> Unit) {
    if (tv) {
        if (series) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides rememberSafeDetailBringIntoView()) {
                Column(Modifier.fillMaxSize().testTag("detail-scroll").verticalScroll(scroll)
                    .padding(horizontal = 32.dp, vertical = 16.dp)) {
                    Row(Modifier.fillMaxWidth().testTag("tv-series-header"),
                        horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { heading() }
                        Box(Modifier.fillMaxWidth(.30f).testTag("tv-detail-artwork")) { artwork() }
                    }
                    content()
                    Spacer(Modifier.height(32.dp))
                }
            }
            return
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
            // A supporting poster leaves room for the title, synopsis and playback choices.
            val artworkWidth = minOf(220.dp, maxWidth * .24f, maxHeight * .43f)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.Top) {
                Box(Modifier.width(artworkWidth).testTag("tv-detail-artwork")) { artwork() }
                // TV's default pivot scrolls Play towards the middle even when it is visible,
                // clipping the heading on entry. Move only enough to reveal the focused control.
                CompositionLocalProvider(LocalBringIntoViewSpec provides DetailBringIntoView) {
                    Column(Modifier.weight(1f).fillMaxHeight().testTag("detail-scroll")
                        .verticalScroll(scroll).padding(bottom = 32.dp)) { heading(); content() }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().testTag("detail-scroll").verticalScroll(scroll)
            .padding(start = if (cinematic) 0.dp else 24.dp, end = if (cinematic) 0.dp else 24.dp,
                bottom = 40.dp), content = content)
    }
}

/** What a television detail page keeps between a focused control and the bottom of the screen: 28 dp is the safe area, the rest is air. */
internal val DetailSafeBottom = 32.dp

/**
 * [DetailBringIntoView] that stops [bottomInset] short of the bottom edge.
 *
 * The least distance puts the focused tile flush with the bottom of the screen, so an episode's caption
 * sat 26 px above it on the Shield (2026-11-07), inside the margin tvOS and Google TV both keep clear.
 * A control taller than the room that is left is aligned to its own top instead of being scrolled past.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal class SafeBottomBringIntoView(private val bottomInset: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val end = containerSize - bottomInset
        return when {
            offset >= 0 && offset + size <= end -> 0f
            offset < 0 && offset + size > end -> 0f
            offset < 0 -> offset
            else -> minOf(offset + size - end, offset)
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal fun rememberSafeDetailBringIntoView(): BringIntoViewSpec {
    val inset = with(androidx.compose.ui.platform.LocalDensity.current) { DetailSafeBottom.toPx() }
    return androidx.compose.runtime.remember(inset) { SafeBottomBringIntoView(inset) }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal object DetailBringIntoView : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = when {
        offset >= 0 && offset + size <= containerSize -> 0f
        offset < 0 && offset + size > containerSize -> 0f
        offset < 0 -> offset
        else -> offset + size - containerSize
    }
}
