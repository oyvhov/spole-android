package app.reelstack.test

import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import app.reelstack.data.network.RemoteLibraryView

/**
 * Reusable, named test helpers and sample fixtures across test suites.
 */
object TestFixtures {

    fun sampleConnection(
        kind: ServiceKind = ServiceKind.JELLYFIN,
        name: String = "TestServer",
        baseUrl: String = "http://127.0.0.1:8096",
        token: String = "token_sample",
        userId: String = "user_sample",
    ) = ServiceConnection(
        kind = kind,
        name = name,
        baseUrl = baseUrl,
        token = token,
        userId = userId,
    )

    fun sampleUserProfile(
        id: String = "",
        name: String = "Hovudkonto",
        isKid: Boolean = false,
        avatarUrl: String? = null,
    ) = UserProfile(
        id = id,
        name = name,
        isKid = isKid,
        avatarUrl = avatarUrl,
    )

    fun sampleKidProfile(
        id: String = "kid_sample",
        name: String = "Eilev",
        avatarUrl: String? = null,
    ) = UserProfile(
        id = id,
        name = name,
        isKid = true,
        avatarUrl = avatarUrl,
    )

    fun sampleMedia(
        id: String = "media_sample",
        title: String = "Mummipappa på eventyr",
        source: ServiceKind = ServiceKind.JELLYFIN,
        libraryId: String? = "lib_kids",
    ) = LibraryMedia(
        id = id,
        title = title,
        subtitle = "Sesong 1",
        artworkRes = 0,
        source = source,
        libraryId = libraryId,
    )

    fun sampleLibraryView(
        id: String = "lib_kids",
        name: String = "Barne-Tv Serier",
        collectionType: String? = "tvshows",
        artworkUrl: String? = "http://127.0.0.1:8096/Items/lib_kids/Images/Primary",
    ) = RemoteLibraryView(
        id = id,
        name = name,
        collectionType = collectionType,
        artworkUrl = artworkUrl,
    )
}
