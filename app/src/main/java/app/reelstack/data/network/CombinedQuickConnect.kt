package app.reelstack.data.network

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
    check(loadAccount(jellyfin).id == jellyfin.userId) { "Jellyfin stadfesta ikkje kontoen." }
    coroutineContext.ensureActive()
    var challenge = seerrClient.initiateQuickConnect(seerrUrl)
    coroutineContext.ensureActive()
    // Prove the full secret belongs to this server before authorizing a short numeric code.
    val localChallenge = jellyfinClient.quickConnectState(jellyfin.baseUrl, challenge.secret)
    check(localChallenge.secret == challenge.secret && localChallenge.code == challenge.code) {
        "Seerr er ikkje kopla til denne Jellyfin-tenaren."
    }
    coroutineContext.ensureActive()
    jellyfinClient.authorizeQuickConnect(jellyfin.baseUrl, jellyfin.token, challenge.code)
    var polls = 0
    while (!challenge.authenticated && polls++ < 10) {
        coroutineContext.ensureActive()
        challenge = seerrClient.quickConnectState(seerrUrl, challenge)
        if (!challenge.authenticated) pause()
    }
    check(challenge.authenticated) { "Seerr vart ikkje klar. Prøv igjen." }
    coroutineContext.ensureActive()
    val auth = seerrClient.authenticateWithQuickConnect(seerrUrl, challenge)
    val other = ServiceConnection(ServiceKind.SEERR, "Seerr", seerrUrl, auth.accessToken,
        userId = auth.userId, sessionCookie = true)
    check(matchesJellyfinAccount(loadAccount(other), jellyfin.userId)) {
        "Seerr brukar ein annan Jellyfin-konto. Vel separate innloggingar."
    }
    coroutineContext.ensureActive()
    return other
}
