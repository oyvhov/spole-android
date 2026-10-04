package app.reelstack.ui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** Images may start as soon as the catalogue arrives; slow filter metadata cannot hold it back. */
internal suspend fun <Items, Facets> loadLibraryPage(
    io: CoroutineDispatcher,
    readItems: suspend () -> Items,
    readFacets: suspend () -> Facets,
    onItems: (Items) -> Unit,
    onFacets: (Facets) -> Unit,
) = coroutineScope {
    val facets = async(io) { readFacets() }
    val items = withContext(io) { readItems() }
    onItems(items)
    onFacets(facets.await())
}
