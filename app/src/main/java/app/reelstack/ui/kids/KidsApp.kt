package app.reelstack.ui.kids

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
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
import app.reelstack.data.model.KidsWorld
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
    val options = rememberKidsPreferences(state.activeProfileId)
    val preferences = remember(context) { app.reelstack.data.repository.KidsPreferencesRepository(context) }
    var appearanceOpen by rememberSaveable(state.activeProfileId) { mutableStateOf(false) }
    BackHandler(appearanceOpen) { appearanceOpen = false }

    var profileMenuOpen by rememberSaveable(state.activeProfileId) { mutableStateOf(false) }

    // Everything on this screen must be one tap from playing. An entry with no playable id would
    // be a dead poster, and a dead poster is worse than a missing one.
    val keepWatching = remember(state.resume, state.nextUp) {
        (state.resume + state.nextUp)
            .filter { !it.remoteId.isNullOrBlank() }
            .distinctBy { it.source to it.remoteId }
    }
    // Everything the account may open. The libraries were chosen on the server; the shell shows
    // what they contain and filters nothing of its own. Until that listing arrives, the rows the
    // home sync already fetched stand in, so the screen is never empty on a cold start.
    LaunchedEffect(state.activeProfileId, state.connections.size) { viewModel.loadKidsLibrary() }

    val yourShows = remember(state.kidsLibrary, state.favourites, state.recentSeries, state.recentMovies, keepWatching) {
        val source = state.kidsLibrary.ifEmpty { state.recentSeries + state.favourites + state.recentMovies }
        // Only types the grid knows what to do with. A favourited *episode* carries its series'
        // poster, so it looks like a series and then plays straight into the middle of one.
        source
            .filter { media ->
                !media.remoteId.isNullOrBlank() &&
                    (media.isSeries || media.mediaType.equals("Movie", ignoreCase = true))
            }
            .distinctBy { it.source to it.remoteId }
    }

    val favourites = remember(state.favourites, state.kidsLibrary) {
        (state.favourites + state.kidsLibrary.filter { it.favourite })
            .filter { media ->
                !media.remoteId.isNullOrBlank() &&
                    (media.isSeries || media.mediaType.equals("Movie", ignoreCase = true))
            }
            .distinctBy { it.source to it.remoteId }
    }

    val suggestions = remember(state.kidsLibrary, state.recentSeries, state.recentMovies, keepWatching, favourites) {
        val excludedIds = (keepWatching.mapNotNull { it.remoteId } + favourites.mapNotNull { it.remoteId }).toSet()
        (state.recentSeries + state.recentMovies + state.kidsLibrary)
            .filter { media ->
                !media.remoteId.isNullOrBlank() &&
                    (media.isSeries || media.mediaType.equals("Movie", ignoreCase = true)) &&
                    media.remoteId !in excludedIds
            }
            .distinctBy { it.source to it.remoteId }
            .take(15)
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

    KidsWorldTheme(options) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                Color(options.world.sky),
                                Color(options.world.sky),
                                Color(0xFF070A0E),
                            )
                        )
                    )
            )
            if (options.decorations) {
                WorldLandscape(options.world, Modifier.matchParentSize().alpha(0.28f))
                if (options.world == KidsWorld.SPACE) {
                    SpaceBackdrop(Modifier.matchParentSize(), accent = Color(options.world.glow))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .testTag("kids-app"),
            ) {
                val gridPadding = PaddingValues(
                    start = if (television) 48.dp else 24.dp,
                    end = if (television) 48.dp else 24.dp,
                    top = 0.dp,
                    bottom = if (television) 48.dp else 24.dp,
                )

                val page = when {
                    appearanceOpen && options.allowAppearance -> "appearance"
                    state.kidsBrowse.open -> "episodes"
                    else -> "home"
                }
                androidx.compose.animation.Crossfade(targetState = page,
                    animationSpec = androidx.compose.animation.core.tween(if (LocalMotionEnabled.current) 220 else 0),
                    label = "kids-page", modifier = Modifier.weight(1f)) { visiblePage ->
                if (visiblePage == "appearance") {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                            .verticalScroll(rememberScrollState())
                            .padding(gridPadding)
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        app.reelstack.ui.components.SpoleSecondaryButton(
                            onClick = { appearanceOpen = false },
                            modifier = Modifier.heightIn(min = 64.dp)
                        ) {
                            Text("Tilbake til historiene")
                        }
                        Text("Mi verd", style = MaterialTheme.typography.headlineLarge)
                        Text("Vel ein stad du likar. Historiene dine blir med.", color = Muted)
                        KidsWorldPicker(options, onChange = { preferences.saveAppearance(state.activeProfileId, it, options.decorations) })
                        app.reelstack.ui.components.SettingsToggleRow(
                            "Pynt i verda mi", "Planetar, bølgjer og landskap",
                            options.decorations, "kids-decoration"
                        ) {
                            preferences.saveAppearance(state.activeProfileId, options.world, it)
                        }
                    }
                } else if (visiblePage == "episodes") {
                    KidsEpisodesScreen(
                        browse = state.kidsBrowse,
                        onPlay = play,
                        onSelectSeason = viewModel::selectKidsSeason,
                        onBack = viewModel::closeKidsSeries,
                        onRetry = {
                            if (state.kidsBrowse.selectedSeasonId.isNotBlank()) viewModel.selectKidsSeason(state.kidsBrowse.selectedSeasonId)
                            else yourShows.firstOrNull { it.remoteId == state.kidsBrowse.seriesId }?.let(viewModel::openKidsSeries)
                        },
                        columns = if (television) 4 else 2,
                        contentPadding = gridPadding,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
                    )
                } else {
                    val serverKind = state.connections.firstOrNull {
                        it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.token.isNotBlank()
                    }?.kind ?: ServiceKind.JELLYFIN

                    KidsHomeScreen(
                        keepWatching = keepWatching,
                        yourShows = yourShows,
                        favourites = favourites,
                        suggestions = suggestions,
                        libraries = state.kidsLibraries,
                        source = serverKind,
                        world = options.world,
                        profileButton = {
                            KidsProfileButton(
                                name = activeProfile?.name.orEmpty(),
                                avatarUrl = activeProfile?.avatarUrl,
                                world = options.world,
                                onClick = { profileMenuOpen = true },
                            )
                        },
                        onPlay = choose,
                        columns = if (television) 6 else 2,
                        contentPadding = gridPadding,
                        loading = state.kidsLibraryLoading,
                        error = state.kidsLibraryError,
                        onRetry = viewModel::loadKidsLibrary,
                    )
                }
                }
            }
        }
    }

    if (profileMenuOpen) {
        KidsProfileDialog(
            name = activeProfile?.name.orEmpty(),
            avatarUrl = activeProfile?.avatarUrl,
            world = options.world,
            onAppearance = { appearanceOpen = true },
            onSwitchProfile = viewModel::openProfileSwitcher,
            onDismiss = { profileMenuOpen = false },
        )
    }

    KidsSheets(state.activeSheet, viewModel)
    }
}

/**
 * Integrated child profile button with compact circular avatar and zero external text labels.
 */
@Composable
internal fun KidsProfileButton(
    name: String,
    avatarUrl: String?,
    world: KidsWorld,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val glow = Color(world.glow)
    val description = stringResource(R.string.kids_switch_profile)

    Box(
        modifier = modifier
            .size(44.dp)
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
                .size(38.dp)
                .clip(CircleShape)
                .background(SurfaceRaised)
                .border(1.5.dp, glow.copy(alpha = 0.75f), CircleShape),
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
                    text = name.trim().firstOrNull()?.uppercase().orEmpty().ifBlank { "B" },
                    color = Primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Child profile dialog offering instant access to "Mi verd" (theme/landscape picker)
 * without requiring a PIN, while keeping profile switching protected by PIN.
 */
@Composable
internal fun KidsProfileDialog(
    name: String,
    avatarUrl: String?,
    world: KidsWorld,
    onAppearance: () -> Unit,
    onSwitchProfile: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val glow = Color(world.glow)
        val sky = Color(world.sky)
        val shape = RoundedCornerShape(28.dp)

        Surface(
            shape = shape,
            color = sky,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, glow.copy(alpha = 0.6f)),
            modifier = Modifier
                .widthIn(min = 320.dp, max = 420.dp)
                .padding(16.dp)
                .testTag("kids-profile-dialog"),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Avatar + Name + World
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(3.dp, glow, CircleShape)
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
                            text = name.trim().firstOrNull()?.uppercase().orEmpty().ifBlank { "B" },
                            color = Primary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (name.isBlank()) "Barneprofil" else name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Verd: ${world.title}",
                        color = glow,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Val 1: Mi verd (utan PIN)
                    val appInteraction = remember { MutableInteractionSource() }
                    val appFocused by appInteraction.collectIsFocusedAsState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (appFocused) glow else SurfaceRaised)
                            .focusOutline(appInteraction, RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = appInteraction,
                                indication = null,
                                role = Role.Button,
                                onClick = {
                                    onDismiss()
                                    onAppearance()
                                }
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            app.reelstack.ui.components.SpoleIcons.Palette,
                            contentDescription = null,
                            tint = if (appFocused) Color(0xFF101211) else glow,
                            modifier = Modifier.size(26.dp),
                        )
                        Column {
                            Text(
                                "Mi verd",
                                color = if (appFocused) Color(0xFF101211) else Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Vel fargar, stjerner og landskap",
                                color = if (appFocused) Color(0xFF101211).copy(alpha = 0.85f) else Muted,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    // Val 2: Byt profil (krev PIN)
                    val switchInteraction = remember { MutableInteractionSource() }
                    val switchFocused by switchInteraction.collectIsFocusedAsState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (switchFocused) Primary else SurfaceRaised)
                            .focusOutline(switchInteraction, RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = switchInteraction,
                                indication = null,
                                role = Role.Button,
                                onClick = {
                                    onDismiss()
                                    onSwitchProfile()
                                }
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            app.reelstack.ui.components.SpoleIcons.Lock,
                            contentDescription = null,
                            tint = if (switchFocused) Color(0xFF101211) else Primary,
                            modifier = Modifier.size(26.dp),
                        )
                        Column {
                            Text(
                                "Byt profil",
                                color = if (switchFocused) Color(0xFF101211) else Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Gå ut av barnemodus (krev PIN)",
                                color = if (switchFocused) Color(0xFF101211).copy(alpha = 0.85f) else Muted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                // Lukk
                app.reelstack.ui.components.SpoleSecondaryButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text("Lukk")
                }
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

    androidx.compose.ui.window.Dialog(
        onDismissRequest = viewModel::closeSheet,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = viewModel::closeSheet,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
            ) {
                PinEntrySheet(
                    title = stringResource(R.string.profile_enter_pin_title),
                    subtitle = stringResource(R.string.profile_enter_pin_subtitle),
                    error = state.pinError,
                    lockoutSeconds = state.pinLockoutSeconds,
                    isSetup = sheet.isSetup,
                    onPinComplete = { pin -> viewModel.submitPin(pin, sheet.targetProfileId, sheet.isSetup) },
                    onForgotPin = { password -> viewModel.recoverPinWithPassword(password, sheet.targetProfileId) },
                    onCancel = viewModel::closeSheet,
                )
            }
        }
    }
}
