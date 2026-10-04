package app.reelstack.ui.components

import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.LibraryDisplay
import app.reelstack.data.model.LibraryArtType
import app.reelstack.data.model.libraryArtworkUrl
import app.reelstack.data.model.librarySizedArtwork
import app.reelstack.ui.screens.railArtworkUrl
import app.reelstack.ui.theme.LocalPersonalization
import coil3.imageLoader
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged

/** The widest rail cards Spole draws, on a tablet; phones and TV use narrower ones. */
private const val WIDE_CARD_DP = 292f
private const val POSTER_CARD_DP = 158f

/** Only the visible cards plus two neighbours; no full-library download or background decode. */
@Composable
internal fun PrefetchRailArtwork(
    items: List<LibraryMedia>,
    state: LazyListState,
    wide: Boolean? = null,
    libraryDisplay: LibraryDisplay? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val options = LocalPersonalization.current
    val tablet = app.reelstack.ui.theme.LocalTabletCanvas.current
    fun width(itemWide: Boolean): Int {
        val cardDp = if (libraryDisplay == null) { if (itemWide) WIDE_CARD_DP else POSTER_CARD_DP }
        else if (itemWide) { if (tablet) 292f else app.reelstack.ui.theme.ReelLayout.EpisodeWidth.value }
        else if (tablet) 158f else app.reelstack.ui.theme.ReelLayout.PosterWidth.value
        return (cardDp * options.artworkSize.scale * (libraryDisplay?.size?.scale ?: 1f) * density).toInt().coerceAtLeast(1)
    }
    fun type(itemWide: Boolean) = libraryDisplay?.artType?.takeIf { it != LibraryArtType.AUTO }
        ?: if (itemWide) LibraryArtType.THUMB else LibraryArtType.POSTER
    LaunchedEffect(items, state, density, options.artworkSize, options.lightweightTv, wide, libraryDisplay, tablet) {
        val requests = mutableMapOf<String, coil3.request.Disposable>()
        try {
            snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged().collect { first ->
                val count = if (options.lightweightTv) 4 else 7
                val upcoming = items.drop(first).take(count)
                val urls = upcoming.mapNotNull { item ->
                    val isEpisode = item.mediaType.equals("Episode", true) || (item.season != null && item.episode != null)
                    val itemWide = wide ?: !item.mediaType.equals("Movie", true)
                    val url = if (libraryDisplay != null) librarySizedArtwork(libraryArtworkUrl(item, type(itemWide)), width(itemWide))
                    else railArtworkUrl(
                        wide = itemWide,
                        heroUrl = item.heroUrl,
                        posterUrl = item.posterUrl,
                        artworkUrl = item.artworkUrl,
                        isEpisode = isEpisode,
                    )
                    url?.let { item to it }
                }
                val urlSet = urls.map { it.second }.toSet()
                requests.keys.filter { it !in urlSet }.toList().forEach { requests.remove(it)?.dispose() }
                for ((item, url) in urls) {
                    if (url in requests) continue
                    // Decode at the card's size. Without a size Coil decoded every neighbour at the
                    // image's full resolution. The widest card (tablet) sets the size, so the
                    // card itself still finds the neighbour in the memory cache.
                    val itemWide = wide ?: !item.mediaType.equals("Movie", true)
                    val widthPx = width(itemWide)
                    val heightPx = (widthPx / type(itemWide).ratio).toInt().coerceAtLeast(1)
                    val request = ImageRequest.Builder(context).data(url).size(widthPx, heightPx)
                    MediaAuthHeaders.forUrl(context, item.source, url)?.let(request::httpHeaders)
                    requests[url] = context.imageLoader.enqueue(request.build())
                }
            }
        } finally { requests.values.forEach { it.dispose() } }
    }
}

