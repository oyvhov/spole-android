package app.reelstack.ui.components

import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import app.reelstack.data.model.LibraryMedia
import app.reelstack.ui.theme.LocalPersonalization
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.network.httpHeaders
import kotlinx.coroutines.flow.distinctUntilChanged

/** Only the visible cards plus two neighbours; no full-library download or background decode. */
@Composable
internal fun PrefetchRailArtwork(items: List<LibraryMedia>, state: LazyListState) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val options = LocalPersonalization.current
    LaunchedEffect(items, state, density, options.artworkSize, options.lightweightTv) {
        val requests = mutableMapOf<String, coil3.request.Disposable>()
        try {
            snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged().collect { first ->
                val count = if (options.lightweightTv) 4 else 7
                val upcoming = items.drop(first).take(count).filter { it.artworkUrl != null }
                val urls = upcoming.mapNotNull { it.artworkUrl }.toSet()
                requests.keys.filter { it !in urls }.toList().forEach { requests.remove(it)?.dispose() }
                for (item in upcoming) {
                    val url = item.artworkUrl ?: continue
                    if (url in requests) continue
                    val height = (139.5f * options.artworkSize.scale * density).toInt().coerceAtLeast(1)
                    val width = (height * if (item.mediaType.equals("Movie", true)) 2f / 3f else 16f / 9f).toInt().coerceAtLeast(1)
                    val request = ImageRequest.Builder(context).data(url).size(width, height)
                    MediaAuthHeaders.forUrl(context, item.source, url)?.let(request::httpHeaders)
                    requests[url] = context.imageLoader.enqueue(request.build())
                }
            }
        } finally { requests.values.forEach { it.dispose() } }
    }
}
