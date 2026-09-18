package app.reelstack.ui.sheets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.PublicUser
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.*
import coil3.compose.AsyncImage

/**
 * Picking a kid account is the one screen a parent meets before handing the device over, so it
 * reads like a profile chooser rather than a form: faces first, one tap to sign in, and a password
 * step that keeps the chosen face on screen instead of dropping a bare dialog over the list.
 */
@Composable
fun AddProfileSheet(
    publicUsers: List<PublicUser>,
    loading: Boolean,
    error: String? = null,
    hasServer: Boolean = true,
    servers: List<KidServerOption> = emptyList(),
    onAddUser: (PublicUser, password: String) -> Unit,
    onAddManual: (username: String, password: String, serverUrl: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf<AddProfileStep>(AddProfileStep.Choose) }

    // A server that answers with nothing public leaves the username form as the only way in.
    LaunchedEffect(publicUsers, loading, hasServer) {
        if (!loading && hasServer && publicUsers.isEmpty() && step is AddProfileStep.Choose) {
            step = AddProfileStep.Manual
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("add-profile-sheet"),
    ) {
        SheetHeader(
            title = when (step) {
                AddProfileStep.Choose -> stringResource(R.string.profile_add_heading)
                else -> stringResource(R.string.profile_add_title)
            },
            onDismiss = onDismiss,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = when {
                !hasServer -> stringResource(R.string.profile_add_no_server)
                step is AddProfileStep.Manual && publicUsers.isEmpty() ->
                    stringResource(R.string.profile_add_no_users_hint)
                step is AddProfileStep.Manual -> stringResource(R.string.profile_add_manual_title)
                else -> stringResource(R.string.profile_add_subtitle)
            },
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )

        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = error,
                color = Warning,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                modifier = Modifier.testTag("add-profile-error"),
            )
        }

        Spacer(Modifier.height(20.dp))

        AnimatedContent(
            targetState = if (loading) AddProfileStep.Loading else step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "add-profile-step",
        ) { current ->
            when (current) {
                AddProfileStep.Loading -> LoadingPanel()

                AddProfileStep.Choose -> ChooseAccountPanel(
                    publicUsers = publicUsers,
                    onPick = { user ->
                        step = if (user.hasPassword) AddProfileStep.Password(user) else AddProfileStep.Choose
                        if (!user.hasPassword) onAddUser(user, "")
                    },
                    onManual = { step = AddProfileStep.Manual },
                )

                is AddProfileStep.Password -> PasswordPanel(
                    user = current.user,
                    onBack = { step = AddProfileStep.Choose },
                    onSubmit = { password -> onAddUser(current.user, password) },
                )

                AddProfileStep.Manual -> ManualPanel(
                    canGoBack = publicUsers.isNotEmpty(),
                    enabled = hasServer,
                    servers = servers,
                    onBack = { step = AddProfileStep.Choose },
                    onSubmit = onAddManual,
                )
            }
        }
    }
}

/** One media server the sheet can sign a kid account in against. */
data class KidServerOption(
    val kind: ServiceKind,
    val baseUrl: String,
    val label: String,
)

private sealed interface AddProfileStep {
    data object Loading : AddProfileStep
    data object Choose : AddProfileStep
    data object Manual : AddProfileStep
    data class Password(val user: PublicUser) : AddProfileStep
}

@Composable
private fun SheetHeader(title: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Primary,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(44.dp).testTag("add-profile-close-button"),
        ) {
            Icon(SpoleIcons.Close, contentDescription = stringResource(R.string.action_close), tint = Muted)
        }
    }
}

@Composable
private fun LoadingPanel() {
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Primary, modifier = Modifier.size(36.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChooseAccountPanel(
    publicUsers: List<PublicUser>,
    onPick: (PublicUser) -> Unit,
    onManual: () -> Unit,
) {
    // The server name only earns its place when accounts come from more than one server.
    val showServer = publicUsers.mapNotNull { it.serverKind }.distinct().size > 1

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            publicUsers.forEach { user ->
                AccountTile(
                    user = user,
                    showServer = showServer,
                    onClick = { onPick(user) },
                )
            }
            ManualTile(onClick = onManual)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AccountTile(
    user: PublicUser,
    showServer: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(shape)
            .focusOutline(interaction, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("public-user-row-${user.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(SurfaceRaised)
                    .border(1.dp, ControlOutline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!user.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    InitialGlyph(user.name, size = 34.sp)
                }
            }
            if (user.hasPassword) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Surface)
                        .border(1.dp, ControlOutline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SpoleIcons.Lock,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = user.name,
            color = Primary,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (showServer) {
            Text(
                text = stringResource(R.string.profile_add_server_label, user.serverKind.displayName()),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ManualTile(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(shape)
            .focusOutline(interaction, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("add-profile-manual-toggle"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Surface)
                .border(1.dp, ControlOutline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(SpoleIcons.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.profile_add_manual_tile),
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PasswordPanel(
    user: PublicUser,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var password by remember(user.id) { mutableStateOf("") }
    var visible by remember(user.id) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(user.id) { runCatching { focusRequester.requestFocus() } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SurfaceRaised)
                .border(1.dp, ControlOutline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (!user.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                InitialGlyph(user.name, size = 36.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = user.name,
            color = Primary,
            fontSize = 19.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )

        Text(
            text = stringResource(R.string.profile_add_needs_password),
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )

        Spacer(Modifier.height(20.dp))

        PasswordField(
            value = password,
            onValueChange = { password = it },
            visible = visible,
            onToggleVisible = { visible = !visible },
            onSubmit = { onSubmit(password) },
            modifier = Modifier.focusRequester(focusRequester).testTag("add-profile-password-input"),
        )

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { onSubmit(password) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .testTag("add-profile-confirm-password-button"),
        ) {
            Text(stringResource(R.string.login_sign_in), fontSize = 16.sp)
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("add-profile-password-back"),
        ) {
            Text(stringResource(R.string.action_back), color = Muted, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ManualPanel(
    canGoBack: Boolean,
    enabled: Boolean,
    servers: List<KidServerOption>,
    onBack: () -> Unit,
    onSubmit: (String, String, String?) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var selectedServer by remember(servers) { mutableStateOf(servers.firstOrNull()) }
    val submit = { if (username.isNotBlank()) onSubmit(username.trim(), password, selectedServer?.baseUrl) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // With two media servers configured, a name alone does not say which one owns the
        // account. Guessing sent kid names to the wrong server and failed as a bad password.
        if (servers.size > 1) {
            Text(
                text = stringResource(R.string.profile_add_server_choice),
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                servers.forEach { server ->
                    ServerChip(
                        label = server.label,
                        selected = server.baseUrl == selectedServer?.baseUrl,
                        onClick = { selectedServer = server },
                    )
                }
            }
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.profile_add_username_label)) },
            singleLine = true,
            enabled = enabled,
            colors = profileFieldColors(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("add-profile-manual-username"),
        )

        PasswordField(
            value = password,
            onValueChange = { password = it },
            visible = visible,
            onToggleVisible = { visible = !visible },
            onSubmit = submit,
            enabled = enabled,
            label = stringResource(R.string.profile_add_password_label),
            modifier = Modifier.testTag("add-profile-manual-password"),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canGoBack) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted),
                    border = BorderStroke(1.dp, ControlOutline),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                ) {
                    Text(stringResource(R.string.action_back), fontSize = 15.sp)
                }
            }

            Button(
                onClick = submit,
                enabled = enabled && username.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 56.dp)
                    .testTag("add-profile-manual-submit"),
            ) {
                Text(stringResource(R.string.profile_add_submit_button), fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ServerChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else SurfaceRaised)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Primary else ControlOutline, shape)
            .focusOutline(interaction, shape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("add-profile-server-$label"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (selected) Primary else Muted, fontSize = 15.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        label = label?.let { { Text(it) } },
        placeholder = if (label == null) {
            { Text(stringResource(R.string.profile_add_password_hint), color = Muted) }
        } else null,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        colors = profileFieldColors(),
        shape = RoundedCornerShape(14.dp),
        trailingIcon = {
            IconButton(
                onClick = onToggleVisible,
                modifier = Modifier.size(44.dp).testTag("add-profile-password-visibility"),
            ) {
                Icon(
                    imageVector = if (visible) SpoleIcons.EyeOff else SpoleIcons.Eye,
                    contentDescription = stringResource(
                        if (visible) R.string.profile_add_hide_password else R.string.profile_add_show_password,
                    ),
                    tint = Muted,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun InitialGlyph(name: String, size: androidx.compose.ui.unit.TextUnit) {
    val initial = name.trim().firstOrNull()?.uppercase()
    if (initial != null) {
        Text(
            text = initial,
            color = Primary,
            fontSize = size,
            lineHeight = size,
            modifier = Modifier.clearAndSetSemantics { },
        )
    } else {
        Icon(SpoleIcons.Person, contentDescription = null, tint = Muted, modifier = Modifier.size(28.dp))
    }
}

private fun ServiceKind?.displayName(): String = when (this) {
    ServiceKind.EMBY -> "Emby"
    ServiceKind.JELLYFIN -> "Jellyfin"
    else -> ""
}

@Composable
private fun profileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary.copy(alpha = 0.72f),
    unfocusedBorderColor = ControlOutline,
    disabledBorderColor = ControlOutline.copy(alpha = 0.6f),
    focusedContainerColor = SurfaceRaised,
    unfocusedContainerColor = SurfaceRaised,
    disabledContainerColor = SurfaceRaised,
    cursorColor = Primary,
    focusedTextColor = Primary,
    unfocusedTextColor = Primary,
    focusedLabelColor = Muted,
    unfocusedLabelColor = Muted,
)
