package app.reelstack.ui

import app.reelstack.data.model.LibraryMedia

internal data class SeriesBrowseTarget(val seriesId: String, val initialSeasonId: String? = null)

internal fun seriesBrowseTarget(media: LibraryMedia): SeriesBrowseTarget? {
    val seriesId = when {
        media.mediaType.equals("Series", true) -> media.remoteId
        media.mediaType.equals("Episode", true) || media.mediaType.equals("Season", true) -> media.seriesId
        else -> null
    }?.takeIf(String::isNotBlank) ?: return null
    return SeriesBrowseTarget(seriesId, media.remoteId?.takeIf {
        media.mediaType.equals("Season", true) && it.isNotBlank()
    })
}
