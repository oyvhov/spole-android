package app.reelstack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import app.reelstack.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.reelstack.data.network.PublicUser
import app.reelstack.ui.components.SpoleSecondaryButton
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.readableServerAddress

/**
 * First-run sign-in. A server picked from the network arrives with its address settled, so the
 * remote starts on «Godkjenn på mobilen» and nothing has to be typed. A typed address is checked
 * when the button is pressed: the button is never disabled, because a disabled button cannot take
 * focus and the remote would skip straight past the one thing it is looking for.
 */
@Composable
internal fun CombinedSetupSheet(draft: ConnectionDraft, onJellyfin: (String) -> Unit,
    onSeerr: (String) -> Unit, onStart: () -> Unit, onClose: () -> Unit,
    onAuthMode: (ConnectionAuthMode) -> Unit = {}, onUsername: (String) -> Unit = {},
    onPassword: (String) -> Unit = {}, onSeerrEnabled: (Boolean) -> Unit = {},
    onImport: (String) -> Unit = {}, onPickUser: (PublicUser) -> Unit = {}) {
    val busy = draft.saving || draft.quickConnectWaiting
    val codeStep = busy
    val action = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }
    val urlFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val startInteraction = remember { MutableInteractionSource() }
    val buttonShape = RoundedCornerShape(14.dp)
    var importing by remember { mutableStateOf(false) }
    var setupLink by remember { mutableStateOf("") }
    val known = draft.setupImported || draft.addressResolved
    var editingAddresses by remember(known) { mutableStateOf(!known) }
    val account = draft.authMode == ConnectionAuthMode.ACCOUNT
    val discovered = draft.addressResolved && !draft.setupImported
    val television = app.reelstack.ui.components.isTelevision()
    LaunchedEffect(codeStep, editingAddresses) {
        if (!television) return@LaunchedEffect
        withFrameNanos { }
        runCatching {
            when {
                codeStep -> action.requestFocus()
                !editingAddresses -> startFocus.requestFocus()
                else -> urlFocus.requestFocus()
            }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (editingAddresses || codeStep) Text(stringResource(if (codeStep) R.string.setup_step_sign_in else R.string.setup_step_services),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(when {
            codeStep -> stringResource(if (account) R.string.setup_heading_signing_in else R.string.setup_heading_approve)
            !editingAddresses && draft.serverName.isNotBlank() -> stringResource(R.string.setup_heading_sign_in_to, draft.serverName)
            !editingAddresses -> stringResource(R.string.setup_heading_pick_sign_in)
            else -> stringResource(R.string.setup_heading_connect)
        }, style = MaterialTheme.typography.headlineMedium)
        if (!codeStep) {
            if (!draft.setupImported && editingAddresses) SpoleSecondaryButton(onClick = { importing = !importing }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_have_link))
            }
            if (importing) {
                // Every field here is one the remote walks past; see RemoteTextField.
                app.reelstack.ui.components.RemoteTextField(setupLink, { setupLink = it }, television, ImeAction.Done,
                    Modifier.fillMaxWidth().testTag("setup-link"), label = stringResource(R.string.setup_paste_link),
                    keyboardType = KeyboardType.Uri, autoCorrect = false, shape = buttonShape)
                SpoleSecondaryButton(onClick = { onImport(setupLink); importing = false; setupLink = "" },
                    modifier = Modifier.fillMaxWidth().testTag("setup-import")) { Text(stringResource(R.string.setup_use_addresses)) }
            }
            if (!editingAddresses) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.setup_line_jellyfin, readableServerAddress(draft.url)), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("setup-jellyfin-url"))
                        if (draft.alsoConnect) Text(stringResource(R.string.setup_line_seerr, readableServerAddress(draft.companionUrl)), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("setup-seerr-url"))
                        if (draft.seerrFound && draft.alsoConnect) Text(stringResource(R.string.setup_seerr_found),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (discovered) SpoleSecondaryButton(onClick = onClose, modifier = Modifier.testTag("setup-change-server")) {
                        Text(stringResource(R.string.setup_change_server))
                    } else {
                        val editInteraction = remember { MutableInteractionSource() }
                        TextButton(onClick = { editingAddresses = true }, interactionSource = editInteraction,
                            modifier = Modifier.focusOutline(editInteraction, buttonShape).testTag("setup-edit-addresses")) {
                            Text(stringResource(R.string.setup_change))
                        }
                    }
                }
            }
            if (editingAddresses) app.reelstack.ui.components.RemoteTextField(draft.url, onJellyfin, television,
                if (draft.alsoConnect) ImeAction.Next else ImeAction.Go,
                Modifier.fillMaxWidth().focusRequester(urlFocus).testTag("setup-jellyfin-url"),
                label = stringResource(R.string.setup_jellyfin_address), placeholder = stringResource(R.string.setup_jellyfin_hint),
                supportingText = stringResource(R.string.setup_manual_note),
                keyboardType = KeyboardType.Uri, autoCorrect = false, shape = buttonShape,
                // Next keeps its own move to the Seerr field; Go starts.
                onImeAction = if (draft.alsoConnect) null else { { onStart() } })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.setup_also_seerr))
                    Text(stringResource(R.string.setup_also_seerr_why), style = MaterialTheme.typography.bodySmall)
                }
                val switchInteraction = remember { MutableInteractionSource() }
                Switch(draft.alsoConnect, {
                    if (it && draft.companionUrl.isBlank()) editingAddresses = true
                    onSeerrEnabled(it)
                }, Modifier.focusOutline(switchInteraction, RoundedCornerShape(50)).testTag("setup-seerr-enabled"),
                    interactionSource = switchInteraction)
            }
            if (draft.alsoConnect && editingAddresses) app.reelstack.ui.components.RemoteTextField(draft.companionUrl, onSeerr,
                television, ImeAction.Go, Modifier.fillMaxWidth().testTag("setup-seerr-url"),
                label = stringResource(R.string.setup_seerr_address), placeholder = stringResource(R.string.setup_seerr_hint),
                keyboardType = KeyboardType.Uri, autoCorrect = false, shape = buttonShape, onImeAction = { onStart() })
            if (editingAddresses || account) Text(stringResource(if (account) R.string.setup_account_note else
                R.string.setup_approve_note),
                style = MaterialTheme.typography.bodyMedium)
            if (account) {
                LoginUserChoices(draft, onPickUser = { user ->
                    onPickUser(user)
                    runCatching { if (user.hasPassword) passwordFocus.requestFocus() else startFocus.requestFocus() }
                })
                app.reelstack.ui.components.RemoteTextField(draft.username, onUsername, television, ImeAction.Next,
                    Modifier.fillMaxWidth().testTag("setup-username"), label = stringResource(R.string.setup_username),
                    autoCorrect = false, shape = buttonShape)
                OutlinedTextField(draft.password, onPassword, Modifier.fillMaxWidth().focusRequester(passwordFocus).testTag("setup-password"),
                    label = { Text(stringResource(R.string.setup_password)) }, shape = buttonShape, singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onStart() }))
            }
        } else if (draft.quickConnectWaiting) {
            QuickConnectPanel(draft)
            Text(if (draft.alsoConnect) stringResource(R.string.setup_one_approval_note)
                else stringResource(R.string.setup_page_continues))
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(when {
                draft.resolvingAddress -> R.string.setup_resolving
                account -> R.string.setup_confirming_account
                draft.quickConnectCode == null -> R.string.setup_making_code
                else -> R.string.setup_finishing_sign_in
            }), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("setup-progress"))
        }
        draft.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("setup-error"))
        }
        if (!busy) Button(onClick = onStart,
            interactionSource = startInteraction, shape = buttonShape,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).focusRequester(startFocus)
                .focusOutline(startInteraction, buttonShape).testTag("setup-start")) {
            Text(stringResource(if (draft.error != null) R.string.setup_try_again else if (account) R.string.setup_sign_in else R.string.setup_approve_on_phone))
        }
        if (!busy) SpoleSecondaryButton(onClick = {
            onAuthMode(if (account) ConnectionAuthMode.QUICK_CONNECT else ConnectionAuthMode.ACCOUNT)
        }, modifier = Modifier.fillMaxWidth().testTag("setup-method")) {
            Text(stringResource(if (account) R.string.setup_approve_on_phone_instead else R.string.setup_use_password))
        }
        SpoleSecondaryButton(onClick = onClose,
            modifier = Modifier.fillMaxWidth().focusRequester(action).testTag("setup-cancel")) {
            Text(stringResource(if (busy) R.string.setup_cancel else R.string.action_back))
        }
    }
}

/**
 * The names a server shows on its own sign-in screen, as remote-sized choices. Picking one fills
 * the user name and moves on to the password, which is the only thing left to type. Nothing is
 * drawn when the server hides its users, as Jellyfin does by default.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoginUserChoices(draft: ConnectionDraft, onPickUser: (PublicUser) -> Unit,
    firstFocus: FocusRequester? = null) {
    if (draft.loginUsers.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.login_pick_user), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            draft.loginUsers.forEachIndexed { index, user ->
                val interaction = remember(user.id) { MutableInteractionSource() }
                val shape = RoundedCornerShape(12.dp)
                FilterChip(selected = draft.username.equals(user.name, ignoreCase = true), onClick = { onPickUser(user) },
                    label = { Text(user.name) }, shape = shape, interactionSource = interaction, enabled = !draft.saving,
                    modifier = (if (index == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier).heightIn(min = 48.dp)
                        .focusOutline(interaction, shape).testTag("login-user-${user.id}"))
            }
        }
    }
}
