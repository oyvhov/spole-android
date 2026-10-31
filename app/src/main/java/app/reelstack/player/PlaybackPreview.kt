package app.reelstack.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.MediaArtwork
import kotlinx.serialization.json.JsonObject

data class TrickplayGrid(val width: Int, val height: Int, val columns: Int, val rows: Int,
    val intervalMs: Int, val count: Int, val baseUrl: String, val mediaSourceId: String) {
    fun frame(positionMs: Long): TrickplayFrame {
        val index = (positionMs.coerceAtLeast(0) / intervalMs).coerceAtMost(count.toLong() - 1).toInt()
        val perSheet = columns * rows
        return TrickplayFrame("$baseUrl/${index / perSheet}.jpg?mediaSourceId=${enc(mediaSourceId)}", index % perSheet % columns, index % perSheet / columns)
    }
}
data class TrickplayFrame(val url: String, val column: Int, val row: Int)

/** A large server sprite must not consume tens of megabytes for a small seek card. */
internal fun previewDecodeSize(grid: TrickplayGrid?): IntSize {
    val width = 160.0 * (grid?.columns ?: 1)
    val height = 160.0 * (grid?.height ?: 90) / (grid?.width ?: 160) * (grid?.rows ?: 1)
    val scale = minOf(1.0, kotlin.math.sqrt(1_500_000.0 / (width * height)), 4096 / width, 4096 / height)
    return IntSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
}

internal fun parseTrickplay(item: JsonObject, baseUrl: String?): List<TrickplayGrid> {
    if (baseUrl.isNullOrBlank()) return emptyList()
    val itemId = item.str("Id").takeIf(String::isNotBlank) ?: return emptyList()
    return item.obj("Trickplay").entries.take(16).flatMap { (source, data) ->
        (data as? JsonObject)?.values.orEmpty().take(8).mapNotNull { raw ->
            val obj = raw as? JsonObject ?: return@mapNotNull null
            fun number(key: String, max: Int) = obj.num(key)?.takeIf { it in 1..max.toLong() }?.toInt()
            val width = number("Width", 1024) ?: return@mapNotNull null
            val height = number("Height", 1024) ?: return@mapNotNull null
            val columns = number("TileWidth", 20) ?: return@mapNotNull null
            val rows = number("TileHeight", 20) ?: return@mapNotNull null
            val interval = number("Interval", 600_000) ?: return@mapNotNull null
            val count = number("ThumbnailCount", 100_000) ?: return@mapNotNull null
            TrickplayGrid(width, height, columns, rows, interval, count,
                safePlaybackUrl(baseUrl, "Videos/${enc(itemId)}/Trickplay/$width"), source)
        }
    }
}

/**
 * Where the seek picture goes: just above the point being sought, centred on it, and kept within
 * the timeline's ends. It used to sit centred on the screen whatever the position, far above the
 * line it described.
 */
internal fun seekPreviewOffset(track: Rect, fraction: Float, size: IntSize, gapPx: Int): IntOffset {
    val centre = track.left + fraction.coerceIn(0f, 1f) * track.width
    val maxX = (track.right - size.width).coerceAtLeast(track.left)
    val x = (centre - size.width / 2f).coerceIn(track.left, maxX)
    val y = (track.top - gapPx - size.height).coerceAtLeast(0f)
    return IntOffset(kotlin.math.round(x).toInt(), kotlin.math.round(y).toInt())
}

/** Places [content] by [seekPreviewOffset]. [track] is in root coordinates; fill the screen with this. */
@Composable
internal fun SeekPreviewPlacement(track: Rect, fraction: Float, gap: Dp = 8.dp, content: @Composable () -> Unit) {
    val gapPx = with(LocalDensity.current) { gap.roundToPx() }
    Layout(content) { measurables, constraints ->
        val pictures = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            pictures.forEach { picture ->
                val at = seekPreviewOffset(track, fraction, IntSize(picture.width, picture.height), gapPx)
                picture.place(at.x, at.y)
            }
        }
    }
}

/**
 * The picture at the sought position, and its time. Nothing else: no panel around it, no caption
 * under it. Loading or failing images never change transport or timeline geometry.
 */
@Composable
internal fun PlaybackSeekPreview(positionMs: Long, grid: TrickplayGrid?, chapters: List<PlaybackChapter>,
    source: ServiceKind, modifier: Modifier = Modifier, width: Dp = 168.dp) {
    val frame = grid?.frame(positionMs)
    val chapter = chapters.lastOrNull { it.startPositionMs <= positionMs }
    val url = frame?.url ?: chapter?.imageUrl ?: return
    var failed by remember(url) { mutableStateOf(false) }
    if (failed) return
    val shape = RoundedCornerShape(10.dp)
    val aspect = grid?.let { it.width.toFloat() / it.height }?.takeIf { it in .5f..3f } ?: (16f / 9f)
    Box(modifier.width(width).aspectRatio(aspect).shadow(12.dp, shape).clip(shape).background(Color.Black)
        .border(1.dp, Color.White.copy(alpha = .22f), shape).testTag("player-thumbnail-preview")) {
        val scale = remember(grid) { object : ContentScale {
            override fun computeScaleFactor(srcSize: androidx.compose.ui.geometry.Size, dstSize: androidx.compose.ui.geometry.Size): ScaleFactor {
                val factor = maxOf(dstSize.width * (grid?.columns ?: 1) / srcSize.width,
                    dstSize.height * (grid?.rows ?: 1) / srcSize.height)
                return ScaleFactor(factor, factor)
            }
        } }
        val alignment = remember(grid, frame?.column, frame?.row) { object : Alignment {
            override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection) =
                IntOffset(-(frame?.column ?: 0) * size.width / (grid?.columns ?: 1), -(frame?.row ?: 0) * size.height / (grid?.rows ?: 1))
        } }
        MediaArtwork(url, null, Modifier.fillMaxSize(),
            source = source, protectAspectRatio = false, crossfadeDurationMillis = 0, contentScale = scale, alignment = alignment,
            requestSize = previewDecodeSize(grid),
            onError = { failed = true })
        Text(playbackTime(positionMs), Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
            .background(Color.Black.copy(alpha = .62f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
    }
}
