package app.reelstack.ui

import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind

/**
 * Resolves the media a server-side watched/favourite action applies to.
 *
 * A detail page can outlive the shelf card that opened it: episode → series → Back restores the
 * episode sheet while `libraryDetailMedia` points at the series. The visible detail therefore
 * remains a valid action target in its own right instead of making the press disappear silently.
 */
internal fun mediaActionTarget(state: ReelstackUiState, id: String): LibraryMedia? =
    (state.resume + state.nextUp + state.libraryShelves.resume + state.libraryShelves.nextUp +
        state.recentMovies + state.recentSeries + state.favourites + state.librarySearchResults +
        listOfNotNull(state.libraryDetailMedia))
        .firstOrNull { it.id == id }
        ?: state.contentDetails?.takeIf { details ->
            details.key == id && details.libraryAvailable && details.source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)
        }?.let { details ->
            LibraryMedia(
                id = details.key,
                title = details.title,
                subtitle = details.subtitle,
                progress = details.progress,
                artworkRes = details.artworkRes,
                source = requireNotNull(details.source),
                artworkUrl = details.artworkUrl,
                remoteId = details.remoteId,
                overview = details.overview,
                facts = details.facts,
                genres = details.genres,
                mediaType = details.mediaType.orEmpty(),
                logoUrl = details.logoUrl,
                heroUrl = details.backdropUrl,
                season = details.season,
                episode = details.episode,
                favourite = details.favourite,
                played = details.played,
            )
        }
