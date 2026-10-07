package app.reelstack.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import app.reelstack.data.network.EmbyAuthenticationClient
import app.reelstack.data.network.JellyfinAuthenticationClient
import app.reelstack.data.network.PublicUser
import app.reelstack.data.network.readableMessage
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.data.security.PinResult
import app.reelstack.data.security.PinSecurity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val activeProfileId: String = "",
    val profiles: List<UserProfile> = emptyList(),
    val allProfileConnections: Map<String, List<ServiceConnection>> = emptyMap(),
    val isKidMode: Boolean = false,
    val publicUsers: List<PublicUser> = emptyList(),
    val loadingPublicUsers: Boolean = false,
    val pinConfigured: Boolean = false,
    val pinError: String? = null,
    val pinLockoutSeconds: Int = 0,
    val addProfileError: String? = null,
    val addProfileHasServer: Boolean = true,
    val addProfileServers: List<ServiceConnection> = emptyList(),
)

class ProfileViewModel(
    private val connectionRepository: ConnectionRepository,
    private val pinSecurity: PinSecurity,
    private val jellyfinAuthClient: JellyfinAuthenticationClient,
    private val embyAuthClient: EmbyAuthenticationClient,
    private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            activeProfileId = connectionRepository.activeProfileId,
            isKidMode = connectionRepository.isKidMode,
            pinConfigured = pinSecurity.isPinConfigured(),
            pinLockoutSeconds = pinSecurity.remainingLockoutSeconds(),
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { connectionRepository.changes.collect { loadProfiles() } }
    }

    // The composition root owns this instance rather than a separate ViewModelStore.
    fun close() { viewModelScope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }

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

    fun selectProfile(
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

        switchProfileNow(profile.id, onSwitchSuccess)
    }

    private fun switchProfileNow(profileId: String, onSwitchSuccess: (profileId: String) -> Unit) {
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

    fun submitPin(
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
            if (switchAfterSetup) switchProfileNow(targetProfileId, onSwitchSuccess)
            else onSetupComplete()
            return
        }

        when (val result = pinSecurity.verifyPin(pin)) {
            is PinResult.Success -> {
                _uiState.update { it.copy(pinError = null, pinLockoutSeconds = 0) }
                switchProfileNow(targetProfileId, onSwitchSuccess)
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

    fun openAddProfileSheet(availableServers: List<ServiceConnection>) {
        val validServers = availableServers.filter {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank()
        }
        _uiState.update {
            it.copy(
                addProfileServers = validServers,
                addProfileHasServer = validServers.isNotEmpty(),
                addProfileError = null,
                loadingPublicUsers = false,
                publicUsers = emptyList(),
            )
        }
    }

    fun addKidProfile(
        serverConnection: ServiceConnection,
        username: String,
        password: String,
        avatarUrl: String?,
        userId: String?,
        onSwitchSuccess: (profileId: String) -> Unit,
    ) {
        if (username.isBlank() || connectionRepository.isKidMode) return
        val owner = connectionRepository.captureSession()
        viewModelScope.launch {
            _uiState.update { it.copy(loadingPublicUsers = true, addProfileError = null) }
            val authResult = withContext(ioDispatcher) {
                runCatching {
                    when (serverConnection.kind) {
                        ServiceKind.JELLYFIN -> jellyfinAuthClient.authenticate(serverConnection.baseUrl, username, password)
                        ServiceKind.EMBY -> embyAuthClient.authenticate(serverConnection.baseUrl, username, password)
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
                        addProfileError = readable ?: appContext.getString(R.string.err_feil_brukarnamn_eller_passord),
                    )
                }
                return@launch
            }

            if (!connectionRepository.isCurrent(owner)) return@launch
            val finalUserId = auth.userId
            val finalAvatarUrl = avatarUrl ?: "${serverConnection.baseUrl.trimEnd('/')}/Users/$finalUserId/Images/Primary"
            val kidConnection = serverConnection.copy(
                token = auth.accessToken,
                userId = auth.userId,
                name = username,
            )
            connectionRepository.save(kidConnection, finalUserId)
            connectionRepository.registerKidProfile(finalUserId, username, finalAvatarUrl)

            loadProfiles()
            switchProfileNow(finalUserId, onSwitchSuccess)
        }
    }

    fun deleteKidProfile(profile: UserProfile) {
        if (connectionRepository.isKidMode) return
        connectionRepository.deleteProfile(profile.id)
        loadProfiles()
    }
}
