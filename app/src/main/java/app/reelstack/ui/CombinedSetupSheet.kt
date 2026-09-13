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
    onSeerr: (String) -> Unit, onStart: () -> Unit, onClose: () -> Unit) {
    val busy = draft.saving || draft.quickConnectWaiting
    val codeStep = busy
    val action = remember { FocusRequester() }
    val startInteraction = remember { MutableInteractionSource() }
    val buttonShape = RoundedCornerShape(14.dp)
    LaunchedEffect(codeStep) { if (codeStep) action.requestFocus() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (codeStep) "2 av 2 · Godkjenn" else "1 av 2 · Tenestene dine",
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (codeStep) "Godkjenn i Jellyfin" else "Kople til Jellyfin + Seerr",
            style = MaterialTheme.typography.headlineMedium)
        if (!codeStep) {
            Text("Skriv inn dei to adressene. Seerr må vere kopla til same Jellyfin-tenar.")
            OutlinedTextField(draft.url, onJellyfin, Modifier.fillMaxWidth().testTag("setup-jellyfin-url"),
                shape = buttonShape,
                label = { Text("Jellyfin-adresse") }, placeholder = { Text("https://jellyfin.dittdomene.no") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
            OutlinedTextField(draft.companionUrl, onSeerr, Modifier.fillMaxWidth().testTag("setup-seerr-url"),
                shape = buttonShape,
                label = { Text("Seerr-adresse") }, placeholder = { Text("https://seerr.dittdomene.no") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
            Text("Ingen passord. Du godkjenner innlogginga på ei eining der du alt brukar Jellyfin.",
                style = MaterialTheme.typography.bodyMedium)
        } else if (draft.quickConnectWaiting) {
            Text("Opne Jellyfin på mobilen eller i nettlesaren. Gå til brukarikonet → Quick Connect og skriv inn koden.")
            Text(draft.quickConnectCode.orEmpty(), style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.testTag("setup-code"))
            Text("Denne eine godkjenninga koplar til begge tenestene. Denne sida går vidare av seg sjølv.")
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(if (draft.quickConnectCode == null) "Lagar kode…" else "Koplar til Seerr…",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        draft.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("setup-error"))
        }
        if (!busy) Button(onClick = onStart,
            enabled = draft.url.isNotBlank() && draft.companionUrl.isNotBlank(),
            interactionSource = startInteraction, shape = buttonShape,
            modifier = Modifier.fillMaxWidth().focusOutline(startInteraction, buttonShape).testTag("setup-start")) {
            Text(if (draft.error == null) "Hald fram" else "Prøv igjen")
        }
        SpoleSecondaryButton(onClick = onClose,
            modifier = Modifier.fillMaxWidth().focusRequester(action).testTag("setup-cancel")) {
            Text(if (busy) "Avbryt" else "Tilbake")
        }
    }
}
