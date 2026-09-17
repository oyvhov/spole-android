package app.reelstack.data.network

import app.reelstack.R
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.matchesJellyfinAccount
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Creates independent sessions. Call on IO; no credential is persisted by this operation. */
internal suspend fun connectSeerrWithJellyfin(
    jellyfin: ServiceConnection,
    seerrUrl: String,
    jellyfinClient: JellyfinAuthenticationClient,
    seerrClient: SeerrAuthenticationClient,
    loadAccount: (ServiceConnection) -> ServiceAccount,
    pause: suspend () -> Unit = { delay(1_000) },
): ServiceConnection {
    require(jellyfin.kind == ServiceKind.JELLYFIN && jellyfin.token.isNotBlank())
    if (loadAccount(jellyfin).id != jellyfin.userId) serviceError(R.string.err_jellyfin_stadfesta_ikkje)
    coroutineContext.ensureActive()
    var challenge = seerrClient.initiateQuickConnect(seerrUrl)
    coroutineContext.ensureActive()
    // Prove the full secret belongs to this server before authorizing a short numeric code.
    val localChallenge = retryQuickConnectRead { jellyfinClient.quickConnectState(jellyfin.baseUrl, challenge.secret) }
    if (localChallenge.secret != challenge.secret || localChallenge.code != challenge.code) {
        serviceError(R.string.err_seerr_ikkje_kopla_jellyfin)
    }
    coroutineContext.ensureActive()
    jellyfinClient.authorizeQuickConnect(jellyfin.baseUrl, jellyfin.token, challenge.code)
    challenge = awaitQuickConnectApproval(
        initial = challenge,
        maxPolls = SEERR_APPROVAL_MAX_POLLS,
        pollIntervalMillis = 1_000,
        pause = { pause() },
        read = { current -> seerrClient.quickConnectState(seerrUrl, current) },
    )
    if (!challenge.authenticated) serviceError(R.string.err_seerr_vart_ikkje_klar)
    coroutineContext.ensureActive()
    val auth = seerrClient.authenticateWithQuickConnect(seerrUrl, challenge)
    val other = ServiceConnection(ServiceKind.SEERR, "Seerr", seerrUrl, auth.accessToken,
        userId = auth.userId, sessionCookie = true)
    if (!matchesJellyfinAccount(loadAccount(other), jellyfin.userId)) {
        serviceError(R.string.err_seerr_annan_jellyfin_konto)
    }
    coroutineContext.ensureActive()
    return other
}

/** Seerr may need time to observe the Jellyfin approval on a slow or sleeping host. */
private const val SEERR_APPROVAL_MAX_POLLS = 120
