package app.reelstack.ui

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.choice
import app.reelstack.data.network.EndpointValidator
import app.reelstack.data.network.readableMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns server discovery, address validation, and authentication for the current session. Created and retired by ReelstackFeatures. */
internal class ConnectionSetupCoordinator(
    private val _uiState: SessionUiState,
    private val operationScope: CoroutineScope,
    private val accountProfileClient: app.reelstack.data.network.AccountProfileClient,
    private val appContext: Context,
    private val connectionRepository: app.reelstack.data.repository.ConnectionRepository,
    private val connectionTester: app.reelstack.data.network.ServiceConnectionTester,
    private val embyAuthenticationClient: app.reelstack.data.network.EmbyAuthenticationClient,
    private val jellyfinAuthenticationClient: app.reelstack.data.network.JellyfinAuthenticationClient,
    private val seerrAuthenticationClient: app.reelstack.data.network.SeerrAuthenticationClient,
    private val serverDiscovery: app.reelstack.data.network.LanServerDiscovery,
    private val serverProbe: app.reelstack.data.network.ServerProbe,
    private val openSheet: (AppSheet) -> Unit,
    private val synchronizeSession: () -> Unit,
    private val commitConnections: suspend (app.reelstack.data.repository.SessionScope, List<ServiceConnection>) -> app.reelstack.data.repository.SessionScope,
    private val onConnected: (app.reelstack.data.repository.SessionScope, ServiceConnection, ServiceConnection?, String?) -> Unit,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private var quickConnectJob: Job? = null
    private var connectionJob: Job? = null
    private var discoveryJob: Job? = null
    private var addressJob: Job? = null
    fun cancelConnectionSetup() {
        connectionJob?.cancel()
        quickConnectJob?.cancel()
        addressJob?.cancel()
    }

    fun openCombinedSetup() {
        // First-run only: never silently replace an existing service account.
        if (!_uiState.value.showOnboarding || _uiState.value.configuredCount > 0) return
        openSheet(AppSheet.ConnectionEditor(ServiceKind.JELLYFIN))
        // Seerr starts off. It used to start on, which put a second address field in front of a
        // remote before anyone could press "Godkjenn på mobilen" — for a service many homes
        // do not run. A chosen LAN server fills it in when Seerr answers beside it.
        updateDraft { copy(simpleSetup = true, alsoConnect = false, authMode = ConnectionAuthMode.QUICK_CONNECT) }
    }

    /** Ask the network for Jellyfin and Emby. Keeps the last answer on screen while it asks again. */
    fun discoverServers() {
        discoveryJob?.cancel()
        _uiState.update { it.copy(discoveringServers = true) }
        discoveryJob = operationScope.launch {
            var latest = _uiState.value.discoveredServers
            runCatching {
                serverDiscovery.discover().collect { found ->
                    if (found.isNotEmpty()) {
                        latest = found
                        _uiState.update { it.copy(discoveredServers = found) }
                    }
                }
            }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
            _uiState.update { it.copy(discoveringServers = false, discoveredServers = latest) }
        }
    }

    /**
     * Start signing in to a server that answered on the network: its address is already known to
     * work, so the address step is skipped and the remote lands on the sign-in choice.
     */
    fun pickDiscoveredServer(server: app.reelstack.data.network.DiscoveredServer) {
        val kind = when (server.kind) {
            app.reelstack.data.network.DiscoveredServerKind.JELLYFIN -> ServiceKind.JELLYFIN
            app.reelstack.data.network.DiscoveredServerKind.EMBY -> ServiceKind.EMBY
        }
        val firstRun = _uiState.value.showOnboarding && _uiState.value.configuredCount == 0
        if (kind == ServiceKind.JELLYFIN && firstRun) openCombinedSetup() else openSheet(AppSheet.ConnectionEditor(kind))
        if (_uiState.value.connectionDraft?.kind != kind) return
        updateDraft {
            copy(url = server.address, serverName = server.name, addressResolved = true, error = null, finishOnboarding = firstRun,
                warning = if (EndpointValidator.isCleartext(server.address)) appString(R.string.warn_cleartext_http) else null)
        }
        if (kind == ServiceKind.EMBY || _uiState.value.connectionDraft?.authMode == ConnectionAuthMode.ACCOUNT) loadLoginUsers()
        if (kind == ServiceKind.JELLYFIN && _uiState.value.connectionDraft?.simpleSetup == true) {
            addressJob?.cancel()
            addressJob = operationScope.launch {
                val seerr = withContext(ioDispatcher) { serverProbe.seerrBeside(server.address) } ?: return@launch
                updateDraft {
                    if (url != server.address || alsoConnect || companionUrl.isNotBlank()) this
                    else copy(companionUrl = seerr, alsoConnect = true, seerrFound = true)
                }
            }
        }
    }

    /**
     * Turn what was typed into an address that really answers as this kind of server, then carry
     * on with [then]. "192.168.1.20" becomes `http://192.168.1.20:8096` when that is where the
     * server is; an Emby address in the Jellyfin flow is named as such instead of failing later.
     */
    fun confirmConnectionAddress(then: (() -> Unit)? = null) = confirmConnectionAddress(keepBusy = false, then = then)

    /**
     * [keepBusy] is for sign-in: the draft stays `saving` from the press of the button until the
     * sign-in itself has finished, so the form never looks idle in between the two steps.
     */
    private fun confirmConnectionAddress(keepBusy: Boolean, then: (() -> Unit)?) {
        val draft = _uiState.value.connectionDraft ?: return
        if (draft.resolvingAddress || draft.saving) return
        if (draft.addressResolved || draft.kind !in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) { then?.invoke(); return }
        if (draft.url.isBlank()) {
            updateDraft { copy(error = appString(R.string.setup_enter_server_address, draft.kind.displayName)) }
            return
        }
        val candidates = app.reelstack.data.network.serverAddressCandidates(draft.url)
        if (candidates.isEmpty()) {
            val reason = runCatching { EndpointValidator.normalizeBaseUrl(draft.url) }.exceptionOrNull()
            updateDraft { copy(error = reason?.readableMessage(appContext) ?: appString(R.string.error_enter_valid_url)) }
            return
        }
        updateDraft { copy(resolvingAddress = true, saving = keepBusy, error = null) }
        addressJob?.cancel()
        addressJob = operationScope.launch {
            val typed = draft.url
            val found = serverProbe.firstAnswering(candidates)
            val current = _uiState.value.connectionDraft
            if (current == null || current.url != typed || current.kind != draft.kind) return@launch
            val expected = if (draft.kind == ServiceKind.EMBY) app.reelstack.data.network.DiscoveredServerKind.EMBY
                else app.reelstack.data.network.DiscoveredServerKind.JELLYFIN
            val explicit = typed.contains("://")
            when {
                // A complete address is the owner's own decision. The probe has short timeouts;
                // the sign-in that follows has normal ones and names its own failure.
                found == null && explicit -> {
                    val normalized = runCatching { EndpointValidator.normalizeBaseUrl(typed) }.getOrNull()
                    if (normalized == null) updateDraft { copy(resolvingAddress = false, saving = false, error = appString(R.string.error_enter_valid_url)) }
                    else {
                        updateDraft { copy(url = normalized, addressResolved = true, resolvingAddress = false) }
                        then?.invoke()
                    }
                }
                found == null -> updateDraft { copy(resolvingAddress = false, saving = false,
                    error = appString(R.string.setup_server_not_found, typed.trim(), app.reelstack.data.network.DEFAULT_HTTP_PORT)) }
                found.kind != expected -> updateDraft { copy(resolvingAddress = false, saving = false,
                    error = appString(R.string.setup_server_wrong_kind, found.name, kindName(found.kind), draft.kind.displayName)) }
                else -> {
                    updateDraft { copy(url = found.baseUrl, serverName = found.name, addressResolved = true, resolvingAddress = false,
                        warning = if (EndpointValidator.isCleartext(found.baseUrl)) appString(R.string.warn_cleartext_http) else null) }
                    if (draft.kind == ServiceKind.EMBY || draft.authMode == ConnectionAuthMode.ACCOUNT) loadLoginUsers()
                    then?.invoke()
                }
            }
        }
    }

    private fun kindName(kind: app.reelstack.data.network.DiscoveredServerKind) = when (kind) {
        app.reelstack.data.network.DiscoveredServerKind.JELLYFIN -> ServiceKind.JELLYFIN.displayName
        app.reelstack.data.network.DiscoveredServerKind.EMBY -> ServiceKind.EMBY.displayName
    }

    /** The server's own sign-in list. Empty when it hides its users, which Jellyfin often does. */
    private fun loadLoginUsers() {
        val draft = _uiState.value.connectionDraft ?: return
        if (!draft.addressResolved || draft.kind !in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) return
        operationScope.launch {
            val users = withContext(ioDispatcher) {
                runCatching {
                    if (draft.kind == ServiceKind.EMBY) embyAuthenticationClient.publicUsers(draft.url)
                    else jellyfinAuthenticationClient.publicUsers(draft.url)
                }.getOrDefault(emptyList())
            }
            updateDraft { if (url == draft.url) copy(loginUsers = users) else this }
        }
    }

    fun pickLoginUser(user: app.reelstack.data.network.PublicUser) = updateDraft {
        copy(username = user.name, password = "", error = null)
    }

    fun importSetupLink(value: String) {
        if (!_uiState.value.showOnboarding || _uiState.value.configuredCount > 0 ||
            _uiState.value.connectionDraft?.saving == true || _uiState.value.connectionDraft?.quickConnectWaiting == true) return
        val setup = runCatching { app.reelstack.data.network.SetupLink.parse(value) }
        openCombinedSetup()
        setup.onSuccess { link ->
            updateDraft { copy(url = link.jellyfin, companionUrl = link.seerr, alsoConnect = link.seerr.isNotBlank(), setupImported = true) }
        }.onFailure {
            updateDraft { copy(error = appString(R.string.error_setup_link_invalid)) }
        }
    }

    fun openSeerrAccount() {
        openSheet(AppSheet.ConnectionEditor(ServiceKind.SEERR))
        if (_uiState.value.activeSheet == AppSheet.ConnectionEditor(ServiceKind.SEERR)) {
            updateConnectionAuthMode(ConnectionAuthMode.ACCOUNT)
        }
    }
    fun openEditor(sheet: AppSheet.ConnectionEditor): Boolean {
            if (!_uiState.value.canEditConnection(sheet.kind)) return false
            if (_uiState.value.requestingMediaIds.isNotEmpty()) {
                _uiState.update { it.copy(snackbar = appString(R.string.warn_request_in_flight)) }
                return false
            }
            connectionJob?.cancel()
            quickConnectJob?.cancel()
            val existing = _uiState.value.connections.first { it.kind == sheet.kind }
            _uiState.update { it.copy(connectionDraft = ConnectionDraft(
                kind = existing.kind,
                name = existing.name,
                url = existing.baseUrl.ifBlank { connectionRepository.rememberedUrl(existing.kind) },
                token = if (existing.sessionCookie || !_uiState.value.adminView) "" else existing.token,
                userId = existing.userId,
                alternateUrl = existing.alternateUrl,
                companionUrl = _uiState.value.connections.firstOrNull {
                    it.kind == if (existing.kind == ServiceKind.SEERR) ServiceKind.JELLYFIN else ServiceKind.SEERR
                }?.baseUrl.orEmpty(),
                authMode = if (existing.kind in setOf(ServiceKind.SEERR, ServiceKind.EMBY)) {
                    ConnectionAuthMode.ACCOUNT
                } else if (existing.kind == ServiceKind.JELLYFIN) {
                    ConnectionAuthMode.QUICK_CONNECT
                } else {
                    ConnectionAuthMode.API_KEY
                },
                warning = existing.baseUrl.takeIf(String::isNotBlank)?.let {
                    if (EndpointValidator.isCleartext(it)) appString(R.string.warn_cleartext_http) else null
                },
                // A saved address has already signed in once; probing it again before a fresh
                // sign-in would only add a way to fail on a slow mobile network.
                addressResolved = existing.baseUrl.isNotBlank(),
            )) }
            if (existing.baseUrl.isBlank() && existing.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
                !_uiState.value.discoveringServers) discoverServers()
        return true
    }
    fun updateConnectionName(value: String) = updateDraft { copy(name = value, error = null) }
    fun updateConnectionUrl(value: String) {
        quickConnectJob?.cancel()
        addressJob?.cancel()
        updateDraft {
            copy(
                url = value,
                quickConnectCode = null,
                quickConnectWaiting = false,
                error = null,
                // Any edit makes the address a guess again, until it answers.
                addressResolved = addressResolved && value == url,
                resolvingAddress = false,
                serverName = if (value == url) serverName else "",
                loginUsers = if (value == url) loginUsers else emptyList(),
                warning = runCatching {
                    if (value.isNotBlank() && EndpointValidator.isCleartext(value)) {
                        appString(R.string.warn_cleartext_http)
                    } else null
                }.getOrNull(),
            )
        }
    }
    fun updateConnectionToken(value: String) = updateDraft { copy(token = value, error = null) }
    fun updateConnectionUserId(value: String) = updateDraft { copy(userId = value, error = null) }
    fun updateConnectionAlternateUrl(value: String) = updateDraft { copy(alternateUrl = value, error = null) }
    fun updateConnectionAuthMode(value: ConnectionAuthMode) {
        quickConnectJob?.cancel()
        updateDraft {
            copy(
                authMode = value,
                saving = false,
                quickConnectCode = null,
                quickConnectWaiting = false,
                error = null,
            )
        }
        if (value == ConnectionAuthMode.ACCOUNT && _uiState.value.connectionDraft?.loginUsers.isNullOrEmpty()) loadLoginUsers()
    }
    fun updateConnectionUsername(value: String) = updateDraft { copy(username = value, error = null) }
    fun updateConnectionPassword(value: String) = updateDraft { copy(password = value, error = null) }
    fun updateCompanionLogin(enabled: Boolean, url: String) = updateDraft {
        copy(alsoConnect = enabled, companionUrl = url, error = null)
    }

    fun testAndSaveConnection() = testAndSaveConnection(resumed = false)

    /** [resumed]: called by the address step, which has kept the draft `saving` on purpose. */
    private fun testAndSaveConnection(resumed: Boolean) {
        val draft = _uiState.value.connectionDraft ?: return
        if ((draft.saving && !resumed) || draft.resolvingAddress || (draft.simpleSetup && draft.quickConnectWaiting)) return
        if (draft.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && !draft.addressResolved) {
            confirmConnectionAddress(keepBusy = true) { testAndSaveConnection(resumed = true) }
            return
        }
        // Every early answer below hands the form back, including after the address step.
        fun fail(message: String) = updateDraft { copy(saving = false, error = message) }
        val normalizedUrl = runCatching { EndpointValidator.normalizeBaseUrl(draft.url) }
            .getOrElse {
                fail(it.readableMessage(appContext) ?: appString(R.string.error_enter_valid_url))
                return
            }
        val useJellyfinAccount = draft.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.SEERR, ServiceKind.EMBY) && draft.authMode == ConnectionAuthMode.ACCOUNT
        val useQuickConnect = (draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.SEERR) && draft.authMode == ConnectionAuthMode.QUICK_CONNECT
        if (useQuickConnect) {
            if (draft.simpleSetup && draft.alsoConnect) {
                val companionUrl = runCatching { EndpointValidator.normalizeBaseUrl(draft.companionUrl) }.getOrElse {
                    fail(appString(R.string.error_check_seerr_url))
                    return
                }
                startQuickConnect(draft.copy(companionUrl = companionUrl), normalizedUrl)
                return
            }
            startQuickConnect(draft, normalizedUrl)
            return
        }
        if (useJellyfinAccount && draft.username.isBlank()) {
            fail(appString(R.string.error_enter_username))
            return
        }
        if (!useJellyfinAccount && draft.token.isBlank()) {
            fail(appString(R.string.error_enter_token))
            return
        }
        val alternateUrl = draft.alternateUrl.takeIf(String::isNotBlank)?.let { entered ->
            runCatching { EndpointValidator.normalizeBaseUrl(entered) }.getOrElse {
                fail(appString(R.string.error_check_alternate_url, it.readableMessage(appContext) ?: appString(R.string.error_enter_valid_url)))
                return
            }
        }.orEmpty()
        if (alternateUrl.isNotBlank() && alternateUrl == normalizedUrl) {
            fail(appString(R.string.error_alternate_same_url))
            return
        }
        val companionUrl = if (useJellyfinAccount && draft.alsoConnect && draft.kind != ServiceKind.EMBY) {
            runCatching { EndpointValidator.normalizeBaseUrl(draft.companionUrl) }.getOrElse {
                fail(appString(R.string.error_check_companion_url, it.readableMessage(appContext) ?: appString(R.string.error_enter_valid_url)))
                return
            }
        } else null
        updateDraft { copy(url = normalizedUrl, companionUrl = companionUrl ?: this.companionUrl, saving = true, error = null, password = "") }

        connectionJob?.cancel()
        connectionJob = operationScope.launch {
            val credentials = if (useJellyfinAccount) {
                attempt {
                    withContext(ioDispatcher) {
                        if (draft.kind == ServiceKind.SEERR) {
                            seerrAuthenticationClient.authenticate(normalizedUrl, draft.username.trim(), draft.password)
                        } else if (draft.kind == ServiceKind.EMBY) {
                            embyAuthenticationClient.authenticate(normalizedUrl, draft.username.trim(), draft.password)
                        } else jellyfinAuthenticationClient.authenticate(
                            baseUrl = normalizedUrl,
                            username = draft.username.trim(),
                            password = draft.password,
                        )
                    }
                }.getOrElse { error ->
                    updateDraft { copy(saving = false, error = error.readableMessage(appContext) ?: appString(R.string.error_jellyfin_rejected)) }
                    return@launch
                }
            } else null
            val candidate = ServiceConnection(
                kind = draft.kind,
                name = draft.name.ifBlank { draft.kind.displayName },
                baseUrl = normalizedUrl,
                token = credentials?.accessToken ?: draft.token,
                sessionCookie = draft.kind == ServiceKind.SEERR && credentials != null,
                userId = credentials?.userId ?: draft.userId,
                alternateUrl = alternateUrl,
                state = ConnectionState.TESTING,
            )
            val companion = if (companionUrl != null) {
                attempt {
                    withContext(ioDispatcher) {
                        val otherKind = if (draft.kind == ServiceKind.SEERR) ServiceKind.JELLYFIN else ServiceKind.SEERR
                        val auth = if (otherKind == ServiceKind.SEERR) seerrAuthenticationClient.authenticate(companionUrl, draft.username.trim(), draft.password)
                            else jellyfinAuthenticationClient.authenticate(companionUrl, draft.username.trim(), draft.password)
                        val other = ServiceConnection(otherKind, otherKind.displayName, companionUrl, auth.accessToken,
                            userId = auth.userId, sessionCookie = otherKind == ServiceKind.SEERR)
                        val seerr = accountProfileClient.load(if (draft.kind == ServiceKind.SEERR) candidate else other)
                        val jellyfinId = if (draft.kind == ServiceKind.JELLYFIN) candidate.userId else other.userId
                        check(app.reelstack.data.model.matchesJellyfinAccount(seerr, jellyfinId)) {
                            appString(R.string.error_seerr_not_linked)
                        }
                        other
                    }
                }.getOrElse { error ->
                    val failureDetail = error.readableMessage(appContext) ?: appString(R.string.error_companion_failed)
                    updateDraft { copy(saving = false, error = appString(R.string.error_no_connections_changed, failureDetail)) }
                    return@launch
                }
            } else null
            verifyAndSaveConnection(candidate, companion)
        }
    }

    private fun startQuickConnect(draft: ConnectionDraft, normalizedUrl: String) {
        quickConnectJob?.cancel()
        updateDraft {
            copy(
                url = normalizedUrl,
                saving = true,
                quickConnectCode = null,
                quickConnectWaiting = false,
                error = null,
            )
        }
        quickConnectJob = operationScope.launch {
            var quickConnect = attempt {
                withContext(ioDispatcher) {
                    if (draft.kind == ServiceKind.SEERR) seerrAuthenticationClient.initiateQuickConnect(normalizedUrl)
                    else jellyfinAuthenticationClient.initiateQuickConnect(normalizedUrl)
                }
            }.getOrElse { error ->
                showQuickConnectFailure(draft.kind, error, appString(R.string.error_quick_connect_start))
                return@launch
            }
            updateDraft {
                copy(
                    saving = false,
                    quickConnectCode = quickConnect.code,
                    quickConnectWaiting = true,
                    error = null,
                )
            }

            quickConnect = attempt {
                app.reelstack.data.network.awaitQuickConnectApproval(
                    initial = quickConnect,
                    maxPolls = QUICK_CONNECT_MAX_POLLS,
                    pollIntervalMillis = QUICK_CONNECT_POLL_INTERVAL_MS,
                    onUpdate = { updateDraft { copy(quickConnectCode = it.code) } },
                    read = { current -> withContext(ioDispatcher) {
                        if (draft.kind == ServiceKind.SEERR) {
                            seerrAuthenticationClient.quickConnectState(normalizedUrl, current)
                        } else {
                            jellyfinAuthenticationClient.quickConnectState(normalizedUrl, current.secret)
                        }
                    } },
                )
            }.getOrElse { error ->
                showQuickConnectFailure(draft.kind, error, appString(R.string.error_quick_connect_lost))
                return@launch
            }
            if (!quickConnect.authenticated) {
                updateDraft { copy(saving = false, quickConnectWaiting = false,
                    error = appString(R.string.error_quick_connect_expired)) }
                return@launch
            }

            updateDraft { copy(saving = true, quickConnectWaiting = false, error = null) }
            val credentials = attempt {
                withContext(ioDispatcher) {
                    if (draft.kind == ServiceKind.SEERR) {
                        seerrAuthenticationClient.authenticateWithQuickConnect(normalizedUrl, quickConnect)
                    } else {
                        jellyfinAuthenticationClient.authenticateWithQuickConnect(normalizedUrl, quickConnect.secret)
                    }
                }
            }.getOrElse { error ->
                showQuickConnectFailure(draft.kind, error, appString(R.string.error_quick_connect_incomplete))
                return@launch
            }
            val candidate = ServiceConnection(
                kind = draft.kind,
                name = draft.name.ifBlank { draft.kind.displayName },
                sessionCookie = draft.kind == ServiceKind.SEERR,
                baseUrl = normalizedUrl,
                token = credentials.accessToken,
                userId = credentials.userId,
                alternateUrl = runCatching {
                    draft.alternateUrl.takeIf(String::isNotBlank)?.let(EndpointValidator::normalizeBaseUrl).orEmpty()
                }.getOrDefault(""),
                state = ConnectionState.TESTING,
            )
            val companion = if (draft.simpleSetup && draft.alsoConnect) {
                attempt {
                    withContext(ioDispatcher) {
                        app.reelstack.data.network.connectSeerrWithJellyfin(
                            candidate, draft.companionUrl, jellyfinAuthenticationClient,
                            seerrAuthenticationClient, accountProfileClient::load,
                        )
                    }
                }.getOrElse {
                    // The user's Jellyfin approval is already valid. Keep it instead of forcing a
                    // second code merely because Seerr was slow or unavailable at the last step.
                    verifyAndSaveConnection(candidate, notice = appString(R.string.status_jellyfin_ready_seerr_retry))
                    return@launch
                }
            } else null
            verifyAndSaveConnection(candidate, companion)
        }
    }

    private fun showQuickConnectFailure(kind: ServiceKind, error: Throwable, fallback: String) {
        updateDraft {
            copy(
                authMode = if (kind == ServiceKind.SEERR) ConnectionAuthMode.ACCOUNT else authMode,
                saving = false,
                quickConnectCode = null,
                quickConnectWaiting = false,
                error = if (kind == ServiceKind.SEERR) {
                    appString(R.string.error_quick_connect_seerr_unavailable)
                } else error.readableMessage(appContext) ?: fallback,
            )
        }
    }

    private suspend fun verifyAndSaveConnection(candidate: ServiceConnection, companion: ServiceConnection? = null,
        notice: String? = null) {
        val owner = connectionRepository.captureSession()
        val personalLogin = _uiState.value.connectionDraft?.authMode != ConnectionAuthMode.API_KEY
        fun verify(connection: ServiceConnection): app.reelstack.data.model.ConnectionTestResult {
            if (personalLogin && connection.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
                val account = accountProfileClient.load(connection)
                check(account.id == connection.userId) { appString(R.string.error_account_verify) }
                return app.reelstack.data.model.ConnectionTestResult(true, 0, app.reelstack.localization.LocalizedText(R.string.status_signed_in))
            }
            return connectionTester.test(connection)
        }
        if (companion != null) {
            val otherResult = attempt { withContext(ioDispatcher) { verify(companion) } }
            if (otherResult.getOrNull()?.success != true) {
                updateDraft { copy(saving = false, error = appString(R.string.error_companion_verify, companion.kind.displayName)) }
                return
            }
        }
        val result = attempt {
            withContext(ioDispatcher) { verify(candidate) }
        }.getOrElse { error ->
            updateDraft {
                copy(saving = false, error = error.readableMessage(appContext) ?: appString(R.string.error_service_unreachable))
            }
            return
        }

        if (!result.success) {
            updateDraft { copy(saving = false, error = result.message.text(appContext)) }
            return
        }

        // The server accepted us, but the token still has to reach the keystore. If that write
        // fails, say so rather than reporting a connection the app cannot actually reuse.
        val stored = attempt { commitConnections(owner, listOfNotNull(candidate, companion)) }
        stored.exceptionOrNull()?.let {
            synchronizeSession()
            updateDraft { copy(saving = false, error = appString(R.string.error_store_token_secure)) }
            return
        }
        val committed = stored.getOrThrow()
        val saved = candidate.copy(state = ConnectionState.CONNECTED, latencyMs = result.latencyMs,
            detail = result.message.text(appContext))
        onConnected(committed, saved, companion, notice)
    }

    private fun updateDraft(transform: ConnectionDraft.() -> ConnectionDraft) {
        _uiState.update { it.copy(connectionDraft = it.connectionDraft?.transform()) }
    }

    private companion object {
        const val QUICK_CONNECT_POLL_INTERVAL_MS = 3_000L
        const val QUICK_CONNECT_MAX_POLLS = 60
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)
}
