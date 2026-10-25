package app.reelstack.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

/** Painted as an overlay: loading/failing images never change transport or timeline geometry. */
@Composable
internal fun PlaybackSeekPreview(positionMs: Long, grid: TrickplayGrid?, chapters: List<PlaybackChapter>,
    source: ServiceKind, modifier: Modifier = Modifier) {
    val frame = grid?.frame(positionMs)
    val chapter = chapters.lastOrNull { it.startPositionMs <= positionMs }
    val url = frame?.url ?: chapter?.imageUrl ?: return
    var failed by remember(url) { mutableStateOf(false) }
    if (failed) return
    Surface(modifier.width(168.dp).testTag("player-thumbnail-preview"), shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = .85f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            MediaArtwork(url, null, Modifier.fillMaxWidth().height(94.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black),
                source = source, protectAspectRatio = false, crossfadeDurationMillis = 0, contentScale = scale, alignment = alignment,
                requestSize = previewDecodeSize(grid),
                onError = { failed = true })
            Text(playbackTime(positionMs), Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
        }
    }
}
