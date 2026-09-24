package app.reelstack.ui.state

import app.reelstack.data.model.HomeRow
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.ui.AppSheet
import app.reelstack.ui.ReelstackUiState

/**
 * Everything Home is allowed to render.
 *
 * It deliberately excludes sheets, search, profile switching and library navigation. The root
 * currently adapts its legacy state into this contract; a later HomeViewModel can publish it
 * directly without changing [app.reelstack.ui.screens.HomeScreen].
 */
data class HomeUiState(
    val activeSheet: AppSheet? = null,
    val connections: List<ServiceConnection> = emptyList(),
    val accounts: Map<ServiceKind, ServiceAccount> = emptyMap(),
    val accountErrors: Map<ServiceKind, String> = emptyMap(),
    val sessions: List<PlaybackSession> = emptyList(),
    val resume: List<LibraryMedia> = emptyList(),
    val nextUp: List<LibraryMedia> = emptyList(),
    val favourites: List<LibraryMedia> = emptyList(),
    val recentMovies: List<LibraryMedia> = emptyList(),
    val recentSeries: List<LibraryMedia> = emptyList(),
    val recommendations: List<app.reelstack.data.model.DiscoverMedia> = emptyList(),
    val upcoming: List<UpcomingMedia> = emptyList(),
    val recentReleases: List<UpcomingMedia> = emptyList(),
    val upcomingError: String? = null,
    val recentReleasesError: String? = null,
    val isRefreshing: Boolean = false,
    val failedServices: Set<ServiceKind> = emptySet(),
    val serviceWarnings: Map<ServiceKind, String> = emptyMap(),
    val lastUpdatedEpochMillis: Long? = null,
    val pendingSessionKey: String? = null,
    val homeSections: Set<HomeSection> = HomeSection.entries.toSet(),
    val homeRowOrder: List<HomeRow> = HomeRow.entries,
    /** Every row per server, in order and with its switch: what Home actually renders. */
    val homeLayout: app.reelstack.data.model.HomeLayout = app.reelstack.data.model.HomeLayout.fromLegacy(homeRowOrder, homeSections, showNextUp = true),
) {
    val configuredCount: Int get() = connections.count { it.baseUrl.isNotBlank() }
}

/** Transitional adapter while the remaining screen state still belongs to the root ViewModel. */
fun ReelstackUiState.toHomeUiState(): HomeUiState = HomeUiState(
    activeSheet = activeSheet,
    connections = connections,
    accounts = accounts,
    accountErrors = accountErrors,
    sessions = sessions,
    resume = resume,
    nextUp = nextUp,
    favourites = favourites,
    recentMovies = recentMovies,
    recentSeries = recentSeries,
    recommendations = recommendations,
    upcoming = upcoming,
    recentReleases = recentReleases,
    upcomingError = upcomingError,
    recentReleasesError = recentReleasesError,
    isRefreshing = isRefreshing,
    failedServices = failedServices,
    serviceWarnings = serviceWarnings,
    lastUpdatedEpochMillis = lastUpdatedEpochMillis,
    pendingSessionKey = pendingSessionKey,
    homeSections = homeSections,
    homeRowOrder = homeRowOrder,
    homeLayout = effectiveHomeLayout,
)

/** Prefer a verified personal identity, never the owner represented by a shared API key. */
fun HomeUiState.preferredHomeAccount(): ServiceAccount? =
    listOf(ServiceKind.SEERR, ServiceKind.JELLYFIN, ServiceKind.EMBY).firstNotNullOfOrNull { source ->
        val connection = connections.firstOrNull {
            it.kind == source && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        val account = accounts[source]?.takeIf {
            source !in accountErrors && it.source == source && it.displayName.isNotBlank()
        }
        account?.takeIf { it.isPersonal && connection != null && !connection.isSeerrApiKey() }
    }

private fun ServiceConnection.isSeerrApiKey(): Boolean =
    kind == ServiceKind.SEERR && baseUrl.isNotBlank() && token.isNotBlank() && !sessionCookie
