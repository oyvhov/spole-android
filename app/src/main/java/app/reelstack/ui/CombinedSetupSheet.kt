package app.reelstack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    LaunchedEffect(codeStep, draft.setupImported, editingAddresses) {
        if (codeStep) action.requestFocus()
        else if (draft.setupImported && !editingAddresses) startFocus.requestFocus()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!draft.setupImported || codeStep) Text(if (codeStep) "2 av 2 · Logg inn" else "1 av 2 · Tenestene dine",
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (codeStep) { if (account) "Loggar inn" else "Godkjenn i Jellyfin" }
            else if (draft.setupImported && !editingAddresses) "Vel innlogging" else "Kople til tenestene dine",
            style = MaterialTheme.typography.headlineMedium)
        if (!codeStep) {
            if (!draft.setupImported) SpoleSecondaryButton(onClick = { importing = !importing }, modifier = Modifier.fillMaxWidth()) {
                Text("Eg har ei oppsettslenkje")
            }
            if (importing) {
                OutlinedTextField(setupLink, { setupLink = it }, Modifier.fillMaxWidth().testTag("setup-link"),
                    label = { Text("Lim inn oppsettslenkja") }, shape = buttonShape, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
                SpoleSecondaryButton(onClick = { onImport(setupLink); importing = false; setupLink = "" },
                    modifier = Modifier.fillMaxWidth().testTag("setup-import")) { Text("Bruk adressene") }
            }
            if (!editingAddresses) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Jellyfin · ${draft.url}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("setup-jellyfin-url"))
                        if (draft.alsoConnect) Text("Seerr · ${draft.companionUrl}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("setup-seerr-url"))
                    }
                    TextButton(onClick = { editingAddresses = true }, modifier = Modifier.testTag("setup-edit-addresses")) { Text("Endre") }
                }
            }
            if (editingAddresses) OutlinedTextField(draft.url, onJellyfin, Modifier.fillMaxWidth().testTag("setup-jellyfin-url"),
                shape = buttonShape,
                label = { Text("Jellyfin-adresse") }, placeholder = { Text("https://jellyfin.dittdomene.no") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Kople til Seerr òg")
                    Text("Valfritt · for å ønskje filmar og seriar", style = MaterialTheme.typography.bodySmall)
                }
                Switch(draft.alsoConnect, {
                    if (it && draft.companionUrl.isBlank()) editingAddresses = true
                    onSeerrEnabled(it)
                }, Modifier.testTag("setup-seerr-enabled"))
            }
            if (draft.alsoConnect && editingAddresses) OutlinedTextField(draft.companionUrl, onSeerr, Modifier.fillMaxWidth().testTag("setup-seerr-url"),
                shape = buttonShape,
                label = { Text("Seerr-adresse") }, placeholder = { Text("https://seerr.dittdomene.no") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
            if (!draft.setupImported || account) Text(if (account) "Bruk Jellyfin-kontoen din. Passordet blir ikkje lagra." else
                "Godkjenn på mobilen dersom du alt er innlogga i Jellyfin der.",
                style = MaterialTheme.typography.bodyMedium)
            if (account) {
                OutlinedTextField(draft.username, onUsername, Modifier.fillMaxWidth().testTag("setup-username"),
                    label = { Text("Brukarnamn") }, shape = buttonShape, singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false))
                OutlinedTextField(draft.password, onPassword, Modifier.fillMaxWidth().testTag("setup-password"),
                    label = { Text("Passord") }, shape = buttonShape, singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false))
            }
        } else if (draft.quickConnectWaiting) {
            Text("Opne Jellyfin på mobilen eller i nettlesaren. Gå til brukarikonet → Quick Connect og skriv inn koden.")
            Text(draft.quickConnectCode.orEmpty(), style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.testTag("setup-code"))
            Text(if (draft.alsoConnect) "Denne eine godkjenninga koplar til begge tenestene. Denne sida går vidare av seg sjølv."
                else "Denne sida går vidare av seg sjølv når du har godkjent.")
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(if (account) "Stadfestar kontoen din…" else if (draft.quickConnectCode == null) "Lagar kode…" else "Fullfører innlogginga…",
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
            Text(if (draft.error != null) "Prøv igjen" else if (account) "Logg inn" else "Godkjenn på mobilen")
        }
        if (!busy) SpoleSecondaryButton(onClick = {
            onAuthMode(if (account) ConnectionAuthMode.QUICK_CONNECT else ConnectionAuthMode.ACCOUNT)
        }, modifier = Modifier.fillMaxWidth().testTag("setup-method")) {
            Text(if (account) "Godkjenn på mobilen i staden" else "Brukarnamn og passord")
        }
        SpoleSecondaryButton(onClick = onClose,
            modifier = Modifier.fillMaxWidth().focusRequester(action).testTag("setup-cancel")) {
            Text(if (busy) "Avbryt" else "Tilbake")
        }
    }
}
