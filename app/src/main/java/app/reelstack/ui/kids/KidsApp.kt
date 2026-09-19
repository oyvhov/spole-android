package app.reelstack.ui.kids

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.AppSheet
import app.reelstack.ui.ReelstackViewModel
import app.reelstack.ui.components.PinEntrySheet
import app.reelstack.ui.components.ProfileMenu
import app.reelstack.ui.components.StableSheetDialog
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.*
import coil3.compose.AsyncImage

/**
 * The kid shell, beside [app.reelstack.ui.ReelstackApp] rather than a condition inside it.
 *
 * Five tabs and a side rail are not something a four-year-old navigates, so none of them exist
 * here: no bottom bar, no rail, no tabs, no search, and above all no detail sheet. A tap on a
 * poster plays. The only way out is the profile picture, and it asks for the PIN.
 */
@Composable
fun KidsApp(viewModel: ReelstackViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val television = (configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

    // Back leaves the episode list, and on the home screen does nothing: there is no screen behind
    // it, and dropping the kid out of the app is the one thing this shell exists to prevent.
    BackHandler(enabled = state.activeSheet == null) {
        if (state.kidsBrowse.open) viewModel.closeKidsSeries()
    }

    val activeProfile = state.profiles.firstOrNull { it.id == state.activeProfileId }

    // Everything on this screen must be one tap from playing. An entry with no playable id would
    // be a dead poster, and a dead poster is worse than a missing one.
    val keepWatching = remember(state.resume, state.nextUp) {
        (state.resume + state.nextUp)
            .filter { !it.remoteId.isNullOrBlank() }
            .distinctBy { it.remoteId }
    }
    // Everything the account may open. The libraries were chosen on the server; the shell shows
    // what they contain and filters nothing of its own. Until that listing arrives, the rows the
    // home sync already fetched stand in, so the screen is never empty on a cold start.
    LaunchedEffect(state.activeProfileId, state.connections.size) { viewModel.loadKidsLibrary() }

    val yourShows = remember(state.kidsLibrary, state.favourites, state.recentSeries, state.recentMovies, keepWatching) {
        val alreadyShown = keepWatching.mapNotNull { it.remoteId }.toSet()
        val source = state.kidsLibrary.ifEmpty { state.recentSeries + state.favourites + state.recentMovies }
        // Only types the grid knows what to do with. A favourited *episode* carries its series'
        // poster, so it looks like a series and then plays straight into the middle of one.
        source
            .filter { media ->
                !media.remoteId.isNullOrBlank() &&
                    media.remoteId !in alreadyShown &&
                    (media.isSeries || media.mediaType.equals("Movie", ignoreCase = true))
            }
            .distinctBy { it.remoteId }
    }

    val play: (LibraryMedia) -> Unit = { media ->
        val itemId = media.remoteId
        if (!itemId.isNullOrBlank()) {
            app.reelstack.player.JellyfinPlayerActivity.open(context, itemId, source = media.source, kidsMode = true)
        }
    }

    // A series opens its episodes; everything else plays. Three levels, never four.
    val choose: (LibraryMedia) -> Unit = { media ->
        if (media.isSeries) viewModel.openKidsSeries(media) else play(media)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                Color(0xFF141A24),
                                Color(0xFF0C1016),
                                Color(0xFF070A0E),
                            )
                        )
                    )
            )
            SpaceBackdrop(accent = Primary, modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .testTag("kids-app"),
            ) {
                val gridPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 8.dp,
                    bottom = if (television) 48.dp else 24.dp,
                )

                if (state.kidsBrowse.open) {
                    KidsEpisodesScreen(
                        browse = state.kidsBrowse,
                        onPlay = play,
                        onSelectSeason = viewModel::selectKidsSeason,
                        onBack = viewModel::closeKidsSeries,
                        columns = if (television) 4 else 2,
                        contentPadding = gridPadding,
                    )
                } else {
                    KidsTopBar(
                        name = activeProfile?.name.orEmpty(),
                        avatarUrl = activeProfile?.avatarUrl,
                        onProfile = viewModel::openProfileSwitcher,
                    )

                    val serverKind = state.connections.firstOrNull {
                        it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)
                    }?.kind ?: ServiceKind.JELLYFIN

                    KidsHomeScreen(
                        keepWatching = keepWatching,
                        yourShows = yourShows,
                        libraries = state.kidsLibraries,
                        source = serverKind,
                        onPlay = choose,
                        columns = if (television) 5 else 2,
                        contentPadding = gridPadding,
                    )
                }
            }
        }
    }

    KidsSheets(state.activeSheet, viewModel)
}

@Composable
private fun KidsTopBar(name: String, avatarUrl: String?, onProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (name.isBlank()) {
                stringResource(R.string.kids_greeting_plain)
            } else {
                stringResource(R.string.kids_greeting, name)
            },
            color = app.reelstack.ui.theme.Text,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )

        KidsAvatarButton(avatarUrl = avatarUrl, name = name, onClick = onProfile)
    }
}

/**
 * A planet, not a photo frame: the kid's picture inside a ring in their own accent.
 *
 * 64 dp, because nothing in this shell is allowed to be smaller.
 */
@Composable
private fun KidsAvatarButton(avatarUrl: String?, name: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val description = stringResource(R.string.kids_switch_profile)

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .focusOutline(interaction, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .testTag("kids-profile-button"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = name.trim().firstOrNull()?.uppercase().orEmpty(),
                    color = Primary,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                )
            }
        }
    }
}

/**
 * Only the two sheets a kid shell can reach.
 *
 * Adding a profile and opening settings belong to the adult app; [ProfileSwitcher] already hides
 * both when `isKidMode` is set, and nothing here offers another way in.
 */
@Composable
private fun KidsSheets(sheet: AppSheet?, viewModel: ReelstackViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (sheet == null) return

    // Falls out of the account picture in the corner, the same motion as the adult app.
    if (sheet is AppSheet.ProfileSwitcher) {
        ProfileMenu(
            profiles = state.profiles,
            activeProfileId = state.activeProfileId,
            isKidMode = true,
            mainAccountName = (state.accounts[ServiceKind.EMBY] ?: state.accounts[ServiceKind.JELLYFIN])
                ?.displayName,
            mainAccountAvatarUrl = (state.accounts[ServiceKind.EMBY] ?: state.accounts[ServiceKind.JELLYFIN])
                ?.avatarUrl,
            onSelectProfile = viewModel::selectProfile,
            onAddProfile = { },
            onOpenSettings = { },
            onDeleteProfile = { },
            onDismiss = viewModel::closeSheet,
        )
        return
    }
    if (sheet !is AppSheet.PinPrompt) return

    StableSheetDialog(
        dismissEnabled = true,
        onDismiss = viewModel::closeSheet,
    ) { _, _, close ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            when (sheet) {
                is AppSheet.PinPrompt -> PinEntrySheet(
                    title = stringResource(R.string.profile_enter_pin_title),
                    subtitle = stringResource(R.string.profile_enter_pin_subtitle),
                    error = state.pinError,
                    lockoutSeconds = state.pinLockoutSeconds,
                    isSetup = sheet.isSetup,
                    onPinComplete = { pin -> viewModel.submitPin(pin, sheet.targetProfileId, sheet.isSetup) },
                    onForgotPin = { password -> viewModel.recoverPinWithPassword(password, sheet.targetProfileId) },
                    onCancel = close,
                )

                else -> Unit
            }
        }
    }
}
