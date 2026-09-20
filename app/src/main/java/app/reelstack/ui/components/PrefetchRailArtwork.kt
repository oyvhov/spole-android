package app.reelstack.ui.components

import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import app.reelstack.data.model.LibraryMedia
import app.reelstack.ui.screens.railArtworkUrl
import app.reelstack.ui.theme.LocalPersonalization
import coil3.imageLoader
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged

/** Only the visible cards plus two neighbours; no full-library download or background decode. */
@Composable
internal fun PrefetchRailArtwork(
    items: List<LibraryMedia>,
    state: LazyListState,
    wide: Boolean? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val options = LocalPersonalization.current
    LaunchedEffect(items, state, density, options.artworkSize, options.lightweightTv, wide) {
        val requests = mutableMapOf<String, coil3.request.Disposable>()
        try {
            snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged().collect { first ->
                val count = if (options.lightweightTv) 4 else 7
                val upcoming = items.drop(first).take(count)
                val urls = upcoming.mapNotNull { item ->
                    val isEpisode = item.mediaType.equals("Episode", true) || (item.season != null && item.episode != null)
                    val itemWide = wide ?: !item.mediaType.equals("Movie", true)
                    railArtworkUrl(
                        wide = itemWide,
                        heroUrl = item.heroUrl,
                        posterUrl = item.posterUrl,
                        artworkUrl = item.artworkUrl,
                        isEpisode = isEpisode,
                    )?.let { url -> item to url }
                }
                val urlSet = urls.map { it.second }.toSet()
                requests.keys.filter { it !in urlSet }.toList().forEach { requests.remove(it)?.dispose() }
                for ((item, url) in urls) {
                    if (url in requests) continue
                    val request = ImageRequest.Builder(context).data(url)
                    MediaAuthHeaders.forUrl(context, item.source, url)?.let(request::httpHeaders)
                    requests[url] = context.imageLoader.enqueue(request.build())
                }
            }
        } finally { requests.values.forEach { it.dispose() } }
    }
}

