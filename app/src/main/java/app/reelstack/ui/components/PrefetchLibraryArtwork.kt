package app.reelstack.ui.components

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import app.reelstack.data.model.*
import app.reelstack.data.network.RemoteLibraryItem
import coil3.imageLoader
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged

/** Prefetch just one following row. Visible cards get the network first; leaving cancels work. */
@Composable
internal fun PrefetchLibraryArtwork(items: List<RemoteLibraryItem>, state: LazyGridState,
    art: LibraryArtType, source: ServiceKind, listArtworkWidth: Int? = null) {
    val context = LocalContext.current
    val light = app.reelstack.ui.theme.LocalPersonalization.current.lightweightTv
    LaunchedEffect(items, state, art, source, listArtworkWidth, light) {
        val positions = items.mapIndexed { index, item -> item.id to index }.toMap()
        val requests = mutableMapOf<String, coil3.request.Disposable>()
        try {
            snapshotFlow {
                val visible = state.layoutInfo.visibleItemsInfo.filter { it.key in positions }
                val last = visible.maxOfOrNull { positions.getValue(it.key as String) }
                val columns = visible.map { it.offset.x }.distinct().size.coerceIn(1, if (light) 4 else 8)
                Triple(last, columns, listArtworkWidth ?: visible.firstOrNull()?.size?.width ?: 0)
            }.distinctUntilChanged().collect { (last, columns, width) ->
                val urls = if (last == null || width <= 0) emptyList() else items.drop(last + 1).take(columns)
                    .mapNotNull { librarySizedArtwork(libraryArtworkUrl(it, art), width) }.distinct()
                requests.keys.filter { it !in urls }.toList().forEach { requests.remove(it)?.dispose() }
                urls.forEach { url ->
                    if (url !in requests) {
                        val request = ImageRequest.Builder(context).data(url).size(width, (width / art.ratio).toInt().coerceAtLeast(1))
                        MediaAuthHeaders.forUrl(context, source, url)?.let(request::httpHeaders)
                        requests[url] = context.imageLoader.enqueue(request.build())
                    }
                }
            }
        } finally { requests.values.forEach { it.dispose() } }
    }
}
