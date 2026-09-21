package app.reelstack.ui

import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Refreshes the person behind each authenticated Home connection.
 *
 * This stays separate from profile selection: [app.reelstack.ui.viewmodels.ProfileViewModel]
 * owns changing profiles and PIN policy, while this class only reads the account the active
 * profile has already selected.
 */
internal class AccountRefreshCoordinator(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val readState: () -> ReelstackUiState,
    private val updateState: ((ReelstackUiState) -> ReelstackUiState) -> Unit,
) {
    private var refreshJob: Job? = null

    fun cancel() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun refresh() {
        cancel()
        val targets = readState().connections.filter {
            it.kind in ACCOUNT_SERVICES && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        val kinds = targets.map { it.kind }.toSet()
        updateState {
            it.copy(
                accounts = it.accounts.filterKeys(kinds::contains),
                accountErrors = it.accountErrors.filterKeys(kinds::contains),
                loadingAccounts = kinds,
            )
        }
        refreshJob = scope.launch {
            targets.forEach { connection ->
                launch profile@ {
                    val result = attempt {
                        withContext(Dispatchers.IO) { container.accountProfileClient.load(connection) }
                    }
                    if (!isActive) return@profile
                    // The machine name is not the adult's name. Only the adult profile may update
                    // the durable main-profile label used outside a child shell.
                    val account = result.getOrNull()
                    if (account != null && !container.connectionRepository.isKidMode &&
                        connection.kind in MEDIA_SERVERS && account.displayName.isNotBlank()
                    ) {
                        container.connectionRepository.setMainProfileInfo(account.displayName, account.avatarUrl)
                    }
                    updateState { current ->
                        val configured = current.connections.firstOrNull { it.kind == connection.kind }
                        if (configured == null || configured.baseUrl != connection.baseUrl ||
                            configured.token != connection.token || configured.userId != connection.userId ||
                            configured.sessionCookie != connection.sessionCookie
                        ) current else current.copy(
                            accounts = if (result.isSuccess) {
                                current.accounts + (connection.kind to result.getOrThrow())
                            } else current.accounts - connection.kind,
                            accountErrors = if (result.isSuccess) {
                                current.accountErrors - connection.kind
                            } else current.accountErrors + (
                                connection.kind to appString(R.string.error_account_verify)
                            ),
                            loadingAccounts = current.loadingAccounts - connection.kind,
                            requestHistory = if (connection.kind == ServiceKind.SEERR &&
                                (result.isFailure || result.getOrNull()?.id != current.accounts[ServiceKind.SEERR]?.id)
                            ) app.reelstack.data.model.RequestHistoryState() else current.requestHistory,
                        )
                    }
                }
            }
        }
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(container.appContext).getString(resId, *args)

    private companion object {
        val ACCOUNT_SERVICES = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY, ServiceKind.SEERR)
        val MEDIA_SERVERS = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)
    }
}
