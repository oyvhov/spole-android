package app.reelstack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import app.reelstack.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.reelstack.ui.components.SpoleSecondaryButton
import app.reelstack.ui.components.focusOutline

/** Two steps; successful setup goes straight home after both identities are verified. */
@Composable
internal fun CombinedSetupSheet(draft: ConnectionDraft, onJellyfin: (String) -> Unit,
    onSeerr: (String) -> Unit, onStart: () -> Unit, onClose: () -> Unit,
    onAuthMode: (ConnectionAuthMode) -> Unit = {}, onUsername: (String) -> Unit = {},
    onPassword: (String) -> Unit = {}, onSeerrEnabled: (Boolean) -> Unit = {},
    onImport: (String) -> Unit = {}) {
    val busy = draft.saving || draft.quickConnectWaiting
    val codeStep = busy
    val action = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }
    val startInteraction = remember { MutableInteractionSource() }
    val buttonShape = RoundedCornerShape(14.dp)
    var importing by remember { mutableStateOf(false) }
    var setupLink by remember { mutableStateOf("") }
    var editingAddresses by remember(draft.setupImported) { mutableStateOf(!draft.setupImported) }
    val account = draft.authMode == ConnectionAuthMode.ACCOUNT
    val television = app.reelstack.ui.components.isTelevision()
    LaunchedEffect(codeStep, draft.setupImported, editingAddresses) {
        if (television && codeStep) action.requestFocus()
        else if (television && draft.setupImported && !editingAddresses) startFocus.requestFocus()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!draft.setupImported || codeStep) Text(stringResource(if (codeStep) R.string.setup_step_sign_in else R.string.setup_step_services),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(if (codeStep) { if (account) R.string.setup_heading_signing_in else R.string.setup_heading_approve }
            else if (draft.setupImported && !editingAddresses) R.string.setup_heading_pick_sign_in else R.string.setup_heading_connect),
            style = MaterialTheme.typography.headlineMedium)
        if (!codeStep) {
            if (!draft.setupImported) SpoleSecondaryButton(onClick = { importing = !importing }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_have_link))
            }
            if (importing) {
                OutlinedTextField(setupLink, { setupLink = it }, Modifier.fillMaxWidth().testTag("setup-link"),
                    label = { Text(stringResource(R.string.setup_paste_link)) }, shape = buttonShape, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
                SpoleSecondaryButton(onClick = { onImport(setupLink); importing = false; setupLink = "" },
                    modifier = Modifier.fillMaxWidth().testTag("setup-import")) { Text(stringResource(R.string.setup_use_addresses)) }
            }
            if (!editingAddresses) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.setup_line_jellyfin, draft.url), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("setup-jellyfin-url"))
                        if (draft.alsoConnect) Text(stringResource(R.string.setup_line_seerr, draft.companionUrl), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("setup-seerr-url"))
                    }
                    TextButton(onClick = { editingAddresses = true }, modifier = Modifier.testTag("setup-edit-addresses")) { Text(stringResource(R.string.setup_change)) }
                }
            }
            if (editingAddresses) OutlinedTextField(draft.url, onJellyfin, Modifier.fillMaxWidth().testTag("setup-jellyfin-url"),
                shape = buttonShape,
                label = { Text(stringResource(R.string.setup_jellyfin_address)) }, placeholder = { Text(stringResource(R.string.setup_jellyfin_hint)) },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.setup_also_seerr))
                    Text(stringResource(R.string.setup_also_seerr_why), style = MaterialTheme.typography.bodySmall)
                }
                Switch(draft.alsoConnect, {
                    if (it && draft.companionUrl.isBlank()) editingAddresses = true
                    onSeerrEnabled(it)
                }, Modifier.testTag("setup-seerr-enabled"))
            }
            if (draft.alsoConnect && editingAddresses) OutlinedTextField(draft.companionUrl, onSeerr, Modifier.fillMaxWidth().testTag("setup-seerr-url"),
                shape = buttonShape,
                label = { Text(stringResource(R.string.setup_seerr_address)) }, placeholder = { Text(stringResource(R.string.setup_seerr_hint)) },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
            if (!draft.setupImported || account) Text(stringResource(if (account) R.string.setup_account_note else
                R.string.setup_approve_note),
                style = MaterialTheme.typography.bodyMedium)
            if (account) {
                OutlinedTextField(draft.username, onUsername, Modifier.fillMaxWidth().testTag("setup-username"),
                    label = { Text(stringResource(R.string.setup_username)) }, shape = buttonShape, singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false))
                OutlinedTextField(draft.password, onPassword, Modifier.fillMaxWidth().testTag("setup-password"),
                    label = { Text(stringResource(R.string.setup_password)) }, shape = buttonShape, singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false))
            }
        } else if (draft.quickConnectWaiting) {
            QuickConnectPanel(draft)
            Text(if (draft.alsoConnect) stringResource(R.string.setup_one_approval_note)
                else stringResource(R.string.setup_page_continues))
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(if (account) stringResource(R.string.setup_confirming_account) else stringResource(if (draft.quickConnectCode == null) R.string.setup_making_code else R.string.setup_finishing_sign_in),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        draft.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("setup-error"))
        }
        if (!busy) Button(onClick = onStart,
            enabled = draft.url.isNotBlank() && (!draft.alsoConnect || draft.companionUrl.isNotBlank()) && (!account || draft.username.isNotBlank()),
            interactionSource = startInteraction, shape = buttonShape,
            modifier = Modifier.fillMaxWidth().focusRequester(startFocus).focusOutline(startInteraction, buttonShape).testTag("setup-start")) {
            Text(stringResource(if (draft.error != null) R.string.setup_try_again else if (account) R.string.setup_sign_in else R.string.setup_approve_on_phone))
        }
        if (!busy) SpoleSecondaryButton(onClick = {
            onAuthMode(if (account) ConnectionAuthMode.QUICK_CONNECT else ConnectionAuthMode.ACCOUNT)
        }, modifier = Modifier.fillMaxWidth().testTag("setup-method")) {
            Text(if (account) stringResource(R.string.setup_approve_on_phone_instead) else "Brukarnamn og passord")
        }
        SpoleSecondaryButton(onClick = onClose,
            modifier = Modifier.fillMaxWidth().focusRequester(action).testTag("setup-cancel")) {
            Text(if (busy) "Avbryt" else "Tilbake")
        }
    }
}
