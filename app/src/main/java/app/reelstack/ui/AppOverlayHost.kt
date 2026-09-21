package app.reelstack.ui

import androidx.compose.runtime.Composable
import app.reelstack.data.model.ServiceKind

/**
 * App-wide transient surfaces. Keeping these above the destination host means an open sheet
 * survives a tab transition, while the main app composition only owns window and focus policy.
 */
@Composable
internal fun AppOverlayHost(
    viewModel: ReelstackViewModel,
    state: ReelstackUiState,
    connectionDraft: ConnectionDraft?,
    startTab: AppTab,
) {
    // The profile menu belongs to the corner the avatar sits in, so it is rendered above the
    // destination content rather than inside one particular screen.
    if (state.activeSheet is AppSheet.ProfileSwitcher) {
        app.reelstack.ui.components.ProfileMenu(
            profiles = state.profiles,
            activeProfileId = state.activeProfileId,
            isKidMode = state.isKidMode,
            mainAccountName = (state.accounts[ServiceKind.EMBY] ?: state.accounts[ServiceKind.JELLYFIN])?.displayName,
            mainAccountAvatarUrl = (state.accounts[ServiceKind.EMBY] ?: state.accounts[ServiceKind.JELLYFIN])?.avatarUrl,
            onSelectProfile = viewModel::selectProfile,
            onAddProfile = viewModel::openAddProfile,
            onOpenSettings = viewModel::openAccountsSettings,
            onDeleteProfile = viewModel::deleteKidProfile,
            onDismiss = viewModel::closeSheet,
        )
    }

    ReelstackSheets(
        state = state,
        connectionDraft = connectionDraft,
        onDismiss = viewModel::closeSheet,
        onDetailBack = viewModel::returnFromDetail,
        onPlaybackToggle = viewModel::togglePlayback,
        onConnectionNameChange = viewModel::updateConnectionName,
        onConnectionUrlChange = viewModel::updateConnectionUrl,
        onConnectionTokenChange = viewModel::updateConnectionToken,
        onConnectionUserIdChange = viewModel::updateConnectionUserId,
        onConnectionAlternateUrlChange = viewModel::updateConnectionAlternateUrl,
        onConnectionAuthModeChange = viewModel::updateConnectionAuthMode,
        onConnectionUsernameChange = viewModel::updateConnectionUsername,
        onConnectionPasswordChange = viewModel::updateConnectionPassword,
        onTestAndSaveConnection = viewModel::testAndSaveConnection,
        onRemoveConnection = viewModel::removeConnection,
        onCompanionLoginChange = viewModel::updateCompanionLogin,
        onImportSetupLink = viewModel::importSetupLink,
        onCancelConnection = viewModel::cancelConnectionSetup,
        onAddMedia = viewModel::requestMedia,
        onUpcomingClick = viewModel::openUpcomingDetails,
        onBackToCalendar = viewModel::backToCalendar,
        onSeerrAccount = viewModel::openSeerrAccount,
        onRequestSeason = viewModel::setRequestSeason,
        onRequestNotification = viewModel::setRequestNotification,
        onConfirmRequest = viewModel::confirmRequest,
        onSeasonWatch = viewModel::setSeasonWatch,
        onFavourite = viewModel::setMediaFavourite,
        onPlayed = viewModel::setMediaPlayed,
        onSeason = viewModel::selectSeason,
        onEpisodeSeries = viewModel::openEpisodeSeries,
        onEpisodeClick = viewModel::openEpisodeDetail,
        onPersonTitles = viewModel::personTitles,
        onPersonTitle = viewModel::openPersonTitle,
        onSelectProfile = viewModel::selectProfile,
        onOpenAddProfile = viewModel::openAddProfile,
        onOpenSettings = {
            viewModel.closeSheet()
            viewModel.selectTab(AppTab.SETTINGS)
        },
        onDeleteProfile = viewModel::deleteKidProfile,
        onSubmitPin = viewModel::submitPin,
        onRecoverPinWithPassword = viewModel::recoverPinWithPassword,
        onAddKidUser = viewModel::addKidProfile,
        onAddKidManual = viewModel::addKidProfileManual,
    )
    if (state.libraryChoicesOpen) {
        app.reelstack.ui.screens.LibraryChoicesDialog(
            state,
            viewModel::closeLibraryChoices,
            viewModel::openLibraryChoices,
            viewModel::saveLibraryChoices,
        )
    }
    app.reelstack.update.AppUpdateHost(
        state.selectedTab == startTab && state.activeSheet == null && !state.showOnboarding && !state.libraryChoicesOpen,
    )
}
