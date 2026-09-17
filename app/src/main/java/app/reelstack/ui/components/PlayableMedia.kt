package app.reelstack.ui.components

import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind

/**
 * Whether a title can be started straight from a card or the hero, without opening it first.
 *
 * Three things have to be true, and they were previously spelled out inline wherever playback was
 * offered — which is how the card menu ended up still restricted to Jellyfin after Emby playback
 * shipped in 0.17.0-beta04. One answer, one place to change it when a third service gains a player.
 *
 * A series is deliberately excluded: "play" on a series means "play the next unwatched episode",
 * and which episode that is comes from the server, not from the card.
 */
internal fun LibraryMedia.playableNow(): Boolean =
    source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
        mediaType in setOf("Movie", "Episode") &&
        !remoteId.isNullOrBlank()
