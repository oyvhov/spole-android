package app.reelstack.ui

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import app.reelstack.data.security.PinResult
import app.reelstack.data.network.readableMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns profile selection, PIN policy, and child account sign-in for the current session. Created and retired by ReelstackFeatures. */
internal class ProfileCoordinator(
    private val _uiState: SessionUiState,
    private val operationScope: CoroutineScope,
    private val accountProfileClient: app.reelstack.data.network.AccountProfileClient,
    private val appContext: Context,
    private val connectionRepository: app.reelstack.data.repository.ConnectionRepository,
    private val embyAuthenticationClient: app.reelstack.data.network.EmbyAuthenticationClient,
    private val jellyfinAuthenticationClient: app.reelstack.data.network.JellyfinAuthenticationClient,
    private val offlineDownloads: app.reelstack.offline.OfflineDownloadRepository,
    private val offlineStorage: app.reelstack.offline.OfflineStorage,
    private val pinSecurity: app.reelstack.data.security.PinSecurity,
    private val switchProfileNow: (String) -> Unit,
    private val closeSheet: () -> Unit,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    fun loadProfiles() {
        val profiles = connectionRepository.listProfiles()
        val allConnections = if (connectionRepository.isKidMode) emptyMap()
            else profiles.associate { it.id to connectionRepository.list(it.id) }
        _uiState.update {
            it.copy(
                profiles = profiles,
                allProfileConnections = allConnections,
                activeProfileId = connectionRepository.activeProfileId,
                isKidMode = connectionRepository.isKidMode,
                pinConfigured = pinSecurity.isPinConfigured(),
                pinLockoutSeconds = pinSecurity.remainingLockoutSeconds(),
            )
        }
    }

    private fun selectProfileGuarded(
        profile: UserProfile,
        onSwitchSuccess: (profileId: String) -> Unit,
        onPromptPin: (targetProfileId: String, isSetup: Boolean) -> Unit,
    ) {
        val currentActive = connectionRepository.activeProfileId
        if (profile.id == currentActive) {
            return
        }

        // Switching from kid mode to adult mode requires PIN if configured
        if (connectionRepository.isKidMode && profile.isMain) {
            if (pinSecurity.isPinConfigured()) {
                _uiState.update {
                    it.copy(
                        pinError = null,
                        pinLockoutSeconds = pinSecurity.remainingLockoutSeconds(),
                    )
                }
                onPromptPin(profile.id, false)
                return
            }
        }

        switchProfileGuarded(profile.id, onSwitchSuccess)
    }

    private fun switchProfileGuarded(profileId: String, onSwitchSuccess: (profileId: String) -> Unit) {
        connectionRepository.activeProfileId = profileId
        _uiState.update {
            it.copy(
                activeProfileId = profileId,
                isKidMode = profileId.isNotBlank(),
                pinError = null,
                pinLockoutSeconds = 0,
            )
        }
        onSwitchSuccess(profileId)
    }

    private fun submitPinGuarded(
        pin: String,
        targetProfileId: String,
        isSetup: Boolean,
        switchAfterSetup: Boolean = true,
        onSwitchSuccess: (profileId: String) -> Unit,
        onSetupComplete: () -> Unit = {},
    ) {
        if (isSetup) {
            if (connectionRepository.isKidMode || pinSecurity.isPinConfigured()) return
            pinSecurity.setPin(pin)
            _uiState.update { it.copy(pinConfigured = true, pinError = null, pinLockoutSeconds = 0) }
            if (switchAfterSetup) switchProfileGuarded(targetProfileId, onSwitchSuccess)
            else onSetupComplete()
            return
        }

        when (val result = pinSecurity.verifyPin(pin)) {
            is PinResult.Success -> {
                _uiState.update { it.copy(pinError = null, pinLockoutSeconds = 0) }
                switchProfileGuarded(targetProfileId, onSwitchSuccess)
            }
            is PinResult.Incorrect -> {
                _uiState.update {
                    it.copy(
                        pinError = appContext.getString(R.string.profile_wrong_pin),
                    )
                }
            }
            is PinResult.LockedOut -> {
                _uiState.update {
                    it.copy(
                        pinLockoutSeconds = result.secondsRemaining,
                        pinError = appContext.getString(R.string.profile_pin_locked, result.secondsRemaining),
                    )
                }
            }
            is PinResult.Corrupted -> {
                _uiState.update {
                    it.copy(
                        pinError = appContext.getString(R.string.profile_pin_corrupted),
                    )
                }
            }
        }
    }

    fun clearPinProtection(pin: String): Boolean {
        if (connectionRepository.isKidMode) return false
        val result = pinSecurity.verifyPin(pin)
        if (result !is PinResult.Success) {
            _uiState.update { it.copy(pinError = appContext.getString(R.string.profile_wrong_pin),
                pinLockoutSeconds = pinSecurity.remainingLockoutSeconds()) }
            return false
        }
        pinSecurity.clearPin()
        _uiState.update { it.copy(pinConfigured = false, pinError = null, pinLockoutSeconds = 0) }
        return true
    }

    fun openProfileSwitcher() {
        loadProfiles()
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.ProfileSwitcher,
            )
        }
    }

    fun selectProfile(profile: app.reelstack.data.model.UserProfile) {
        val currentActive = connectionRepository.activeProfileId
        if (profile.id == currentActive) {
            closeSheet()
            return
        }

        selectProfileGuarded(
            profile = profile,
            onSwitchSuccess = { switchProfileNow(it) },
            onPromptPin = { targetProfileId, isSetup ->
                _uiState.update {
                    it.copy(
                        activeSheet = AppSheet.PinPrompt(targetProfileId = targetProfileId, isSetup = isSetup),
                    )
                }
            },
        )
    }

    fun submitPin(pin: String, targetProfileId: String, isSetup: Boolean) {
        val prompt = _uiState.value.activeSheet as? AppSheet.PinPrompt ?: return
        if (prompt.disableAfterVerification) {
            if (clearPinProtection(pin)) closeSheet()
            return
        }
        val switchAfterSetup = (_uiState.value.activeSheet as? AppSheet.PinPrompt)?.switchAfterSetup ?: true
        submitPinGuarded(
            pin = pin,
            targetProfileId = targetProfileId,
            isSetup = isSetup,
            switchAfterSetup = switchAfterSetup,
            onSwitchSuccess = { switchProfileNow(it) },
            onSetupComplete = { _uiState.update { it.copy(activeSheet = null, pinError = null) } },
        )
    }

    /** Opens the existing PIN sheet without changing profile; used by parental settings. */
    fun requestPinSetup(targetProfileId: String = _uiState.value.activeProfileId) {
        if (connectionRepository.isKidMode || pinSecurity.isPinConfigured()) return
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.PinPrompt(
                    targetProfileId = targetProfileId,
                    isSetup = true,
                    switchAfterSetup = false,
                ),
                pinError = null,
            )
        }
    }

    fun disablePinProtection() {
        if (connectionRepository.isKidMode) return
        _uiState.update { it.copy(activeSheet = AppSheet.PinPrompt("", switchAfterSetup = false, disableAfterVerification = true), pinError = null) }
    }

    private fun primaryMediaServer(): ServiceConnection? =
        _uiState.value.connections.firstOrNull {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.token.isNotBlank()
        } ?: _uiState.value.connections.firstOrNull {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank()
        } ?: connectionRepository.list("").firstOrNull {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.token.isNotBlank()
        } ?: connectionRepository.list("").firstOrNull {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank()
        }

    fun recoverPinWithPassword(password: String, targetProfileId: String) {
        val owner = connectionRepository.captureSession()
        val primaryServer = connectionRepository.list("").firstOrNull {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
                it.token.isNotBlank() && it.userId.isNotBlank()
        }
        if (primaryServer == null) {
            _uiState.update { it.copy(pinError = appString(R.string.profile_pin_adult_needed)) }
            return
        }
        val recoveringProfile = _uiState.value.activeProfileId
        operationScope.launch {
            val success = withContext(ioDispatcher) {
                runCatching {
                    val account = accountProfileClient.load(primaryServer)
                    val user = account.displayName
                    val authentication = when (primaryServer.kind) {
                        ServiceKind.JELLYFIN -> jellyfinAuthenticationClient.authenticate(primaryServer.baseUrl, user, password)
                        ServiceKind.EMBY -> embyAuthenticationClient.authenticate(primaryServer.baseUrl, user, password)
                        else -> error("Ikkje-støtta teneste")
                    }
                    authentication.userId == primaryServer.userId
                }.getOrDefault(false)
            }
            if (!connectionRepository.isCurrent(owner) || _uiState.value.activeProfileId != recoveringProfile || _uiState.value.activeSheet !is AppSheet.PinPrompt) return@launch
            if (success) {
                pinSecurity.clearPin()
                loadProfiles()
                switchProfileNow(targetProfileId)
                closeSheet()
            } else {
                _uiState.update { it.copy(pinError = appString(R.string.err_feil_brukarnamn_eller_passord)) }
            }
        }
    }
    fun openAddProfile() {
        val mediaServers = (_uiState.value.connections +
                connectionRepository.list(""))
            .filter { it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank() }
            .distinctBy { it.baseUrl.trimEnd('/') }
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.AddProfile,
                loadingPublicUsers = mediaServers.isNotEmpty(),
                publicUsers = emptyList(),
                addProfileError = null,
                addProfileHasServer = mediaServers.isNotEmpty(),
                addProfileServers = mediaServers,
            )
        }
        if (mediaServers.isEmpty()) return
        operationScope.launch {
            val allUsers = mutableListOf<app.reelstack.data.network.PublicUser>()
            for (server in mediaServers) {
                val users = withContext(ioDispatcher) {
                    runCatching {
                        when (server.kind) {
                            ServiceKind.JELLYFIN -> jellyfinAuthenticationClient.publicUsers(server.baseUrl)
                            ServiceKind.EMBY -> embyAuthenticationClient.publicUsers(server.baseUrl)
                            else -> emptyList()
                        }.map { it.copy(serverKind = server.kind, serverUrl = server.baseUrl) }
                    }.getOrElse { emptyList() }
                }
                allUsers.addAll(users)
            }
            _uiState.update {
                it.copy(
                    loadingPublicUsers = false,
                    publicUsers = allUsers,
                )
            }
        }
    }

    fun addKidProfile(user: app.reelstack.data.network.PublicUser, password: String) {
        addKidProfileInternal(
            username = user.name,
            password = password,
            userId = user.id,
            avatarUrl = user.avatarUrl,
            serverKind = user.serverKind,
            serverUrl = user.serverUrl,
        )
    }

    fun addKidProfileManual(username: String, password: String, serverUrl: String? = null) {
        val server = serverUrl?.let { url ->
            _uiState.value.addProfileServers.firstOrNull { it.baseUrl.trimEnd('/') == url.trimEnd('/') }
        }
        addKidProfileInternal(
            username = username.trim(),
            password = password,
            userId = null,
            avatarUrl = null,
            serverKind = server?.kind,
            serverUrl = server?.baseUrl,
        )
    }

    private fun addKidProfileInternal(
        username: String,
        password: String,
        userId: String?,
        avatarUrl: String?,
        serverKind: ServiceKind? = null,
        serverUrl: String? = null,
    ) {
        if (connectionRepository.isKidMode) return
        val owner = connectionRepository.captureSession()
        val resolvedKind = serverKind ?: primaryMediaServer()?.kind
        val resolvedUrl = serverUrl ?: primaryMediaServer()?.baseUrl.orEmpty()
        val serverConnection = if (resolvedUrl.isNotBlank()) {
            (_uiState.value.connections + connectionRepository.list(""))
                .firstOrNull { it.baseUrl.trimEnd('/') == resolvedUrl.trimEnd('/') }
        } else null

        if (resolvedKind == null || resolvedUrl.isBlank() || serverConnection == null) {
            _uiState.update {
                it.copy(
                    loadingPublicUsers = false,
                    addProfileError = appString(R.string.err_fekk_ikkje_kontakt_med_2),
                )
            }
            return
        }
        if (username.isBlank()) return
        operationScope.launch {
            _uiState.update { it.copy(loadingPublicUsers = true, addProfileError = null) }
            val authResult = withContext(ioDispatcher) {
                runCatching {
                    when (resolvedKind) {
                        ServiceKind.JELLYFIN -> jellyfinAuthenticationClient.authenticate(resolvedUrl, username, password)
                        ServiceKind.EMBY -> embyAuthenticationClient.authenticate(resolvedUrl, username, password)
                        else -> error("Ikkje-støtta teneste")
                    }
                }
            }
            val auth = authResult.getOrNull()
            if (auth == null) {
                val ex = authResult.exceptionOrNull()
                val readable = ex?.readableMessage(appContext)
                _uiState.update {
                    it.copy(
                        loadingPublicUsers = false,
                        addProfileError = readable ?: appString(R.string.err_feil_brukarnamn_eller_passord),
                    )
                }
                return@launch
            }

            if (!connectionRepository.isCurrent(owner)) return@launch
            val finalUserId = auth.userId
            val finalAvatarUrl = avatarUrl ?: "${resolvedUrl.trimEnd('/')}/Users/$finalUserId/Images/Primary"
            val kidConnection = serverConnection.copy(
                token = auth.accessToken,
                userId = auth.userId,
                name = username,
            )
            connectionRepository.save(kidConnection, finalUserId)
            connectionRepository.registerKidProfile(finalUserId, username, finalAvatarUrl)

            val newProfiles = connectionRepository.listProfiles()
            _uiState.update {
                it.copy(
                    loadingPublicUsers = false,
                    profiles = newProfiles,
                    allProfileConnections = newProfiles.associate { p -> p.id to connectionRepository.list(p.id) },
                    activeSheet = null,
                )
            }
            val targetProfile = newProfiles.firstOrNull { it.id == finalUserId }
            if (targetProfile != null) {
                selectProfile(targetProfile)
            } else {
                switchProfileNow(finalUserId)
            }
        }
    }

    fun deleteKidProfile(profile: app.reelstack.data.model.UserProfile) {
        if (connectionRepository.isKidMode || profile.isMain) return
        connectionRepository.list(profile.id).filter {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
            .forEach { offlineDownloads.removeScope(profile.id, it) }
        offlineStorage.clearForProfile(profile.id)
        connectionRepository.deleteProfile(profile.id)
        loadProfiles()
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)
}
