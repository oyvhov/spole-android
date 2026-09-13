package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.ServiceSymbol
import app.reelstack.ui.components.SpoleBrandMark
import app.reelstack.ui.components.SpoleSecondaryButton
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.ReelPage

@Composable
internal fun SimpleWelcomeScreen(onCombined: () -> Unit, onOther: () -> Unit, modifier: Modifier = Modifier) {
    val first = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    LaunchedEffect(Unit) { first.requestFocus() }
    ReelPage {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SpoleBrandMark(Modifier.size(48.dp))
                Text("Velkomen til Spole", style = MaterialTheme.typography.headlineLarge)
                Text("Filmane og seriane dine, samla på éin stad.", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onCombined, interactionSource = interaction, shape = shape,
                    modifier = Modifier.fillMaxWidth().focusRequester(first).focusOutline(interaction, shape)
                    .testTag("setup-combined"), contentPadding = PaddingValues(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServiceSymbol(ServiceKind.JELLYFIN, Modifier.size(24.dp))
                        ServiceSymbol(ServiceKind.SEERR, Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Kom i gang", style = MaterialTheme.typography.titleMedium)
                            Text("Jellyfin · Seerr valfritt", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Text("Bruk oppsettslenkja eller adressene du har fått frå den som deler biblioteket med deg.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SpoleSecondaryButton(onClick = onOther, modifier = Modifier.fillMaxWidth().testTag("setup-other")) {
                    Text("Andre innloggingsmåtar")
                }
            }
        }
    }
}
