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
        val motionWanted = app.reelstack.ui.theme.LocalMotionEnabled.current
        app.reelstack.ui.components.ProfileMenu(
            profiles = state.profiles,
            activeProfileId = state.activeProfileId,
            isKidMode = state.isKidMode,
            mainAccountName = state.mainProfileAccount()?.displayName,
            mainAccountAvatarUrl = state.mainProfileAccount()?.avatarUrl,
            onSelectProfile = { viewModel.selectProfile(it, motionWanted) },
            onPicked = { profile, bounds -> viewModel.profileSwitchMotion.pickedFrom(bounds,
                ProfileSwitchMotion.Picture(profile.avatarUrl, profile.name.trim().firstOrNull()?.uppercase()), onScreen = true) },
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
        onPersonPageTitle = viewModel::openPersonTitle,
        onWithdraw = viewModel::cancelTrackedRequest,
        onNotify = viewModel::setFollowNotification,
        onRelated = viewModel::openRelatedDetails,
        onSeerrPerson = viewModel::openPerson,
        onRequestEdition = viewModel::setRequestEdition,
        onRequestProfile = viewModel::setRequestProfile,
        onRequestFolder = viewModel::setRequestFolder,
        onPlaybackToggle = viewModel::togglePlayback,
        onSessionSelected = { viewModel.openSheet(AppSheet.SessionDetails(it)) },
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
        onPickServer = viewModel::pickDiscoveredServer,
        onConfirmAddress = viewModel::confirmConnectionAddress,
        onPickLoginUser = viewModel::pickLoginUser,
        onCancelConnection = viewModel::cancelConnectionSetup,
        onAddMedia = viewModel::requestMedia,
        onUpcomingClick = viewModel::openUpcomingDetails,
        onBackToCalendar = viewModel::backToCalendar,
        onCalendarFollow = viewModel::toggleCalendarFollow,
        onCalendarTitle = viewModel::openCalendarTitle,
        onSeerrAccount = viewModel::openSeerrAccount,
        onRequestSeason = viewModel::setRequestSeason,
        onRequestNotification = viewModel::setRequestNotification,
        onConfirmRequest = viewModel::confirmRequest,
        onSeasonWatch = viewModel::setSeasonWatch,
        onFavourite = viewModel::setMediaFavourite,
        onPlayed = viewModel::setMediaPlayed,
        onOfflineDownload = viewModel::enqueueOfflineDownload,
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
        app.reelstack.ui.screens.LibraryEditorDialog(
            state,
            onRetry = viewModel::openLibraryChoices,
            onDismiss = viewModel::closeLibraryChoices,
            onSave = viewModel::saveLibraryChoices,
        )
    }
    app.reelstack.update.AppUpdateHost(
        state.selectedTab == startTab && state.activeSheet == null && !state.showOnboarding && !state.libraryChoicesOpen,
    )
}

/** The account that names the main profile: Emby before Jellyfin, but never one that has its own profile. */
private fun ReelstackUiState.mainProfileAccount(): app.reelstack.data.model.ServiceAccount? =
    listOf(ServiceKind.EMBY, ServiceKind.JELLYFIN).firstNotNullOfOrNull { kind ->
        val userId = connections.firstOrNull { it.kind == kind }?.userId.orEmpty()
        accounts[kind]?.takeIf { userId.isBlank() || profiles.none { it.id == app.reelstack.data.repository.ownProfileId(kind, userId) } }
    }
