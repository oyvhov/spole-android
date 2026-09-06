package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.ServiceLogo
import app.reelstack.ui.components.ServiceSymbol
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.ReelPage
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor

/** First-run setup keeps successful connections visible while the next service is added. */
@Composable
fun WelcomeScreen(
    state: ReelstackUiState,
    onConnect: (ServiceKind) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state.configuredCount > 0
    var advancedServices by rememberSaveable { mutableStateOf(false) }
    ReelPage {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            app.reelstack.ui.components.SpoleWelcomeArt(Modifier.padding(bottom = 24.dp))
            Text(if (ready) "KLART" else "KOM I GANG", color = Muted,
                fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Text(if (ready) "Din samling.\nDi oversikt." else "Alt du ser.\nÉin stad.",
                color = TextColor, fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-2).sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Text(if (ready) "Du er klar. Kople til fleire tenester no, eller finn dei i Innstillingar seinare."
                else "Start med Jellyfin eller Emby. Du treng berre tenaradressa og kontoen din. Seerr kan leggjast til i same steg som Jellyfin.",
                color = Muted, style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp))
        }
        // Jellyfin leads as a filled button: connecting a real server is the point of this screen,
        // and it used to be the lowest-contrast element on it.
        listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY, ServiceKind.SEERR).plus(
            if (advancedServices) listOf(ServiceKind.RADARR, ServiceKind.SONARR) else emptyList()
        ).forEach { kind ->
            item(key = kind.name) {
                val connected = state.connections.any { it.kind == kind && it.baseUrl.isNotBlank() }
                val primary = kind == ServiceKind.JELLYFIN && !connected && !ready
                Surface(
                    onClick = { onConnect(kind) },
                    color = if (primary) Primary else SurfaceRaised,
                    contentColor = if (primary) app.reelstack.ui.theme.Ink else TextColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = if (connected) "${kind.displayName} er tilkopla. Opne for å endre."
                        else "Kople til ${kind.displayName}"
                    },
                ) {
                    Row(Modifier.heightIn(min = 64.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        ServiceSymbol(kind, Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Text(kind.displayName, fontWeight = FontWeight.SemiBold)
                            Text(if (connected) "Tilkopla · klar til bruk" else when (kind) {
                                ServiceKind.JELLYFIN -> "Bibliotek · Quick Connect eller konto"
                                ServiceKind.EMBY -> "Logg inn med brukarnamn og passord"
                                ServiceKind.SEERR -> "Oppdag · logg inn med Jellyfin-konto"
                                ServiceKind.RADARR -> "Filmar, utgjevingar og nedlastingar"
                                ServiceKind.SONARR -> "Episodar, kalender og nedlastingar"
                            }, color = if (primary) app.reelstack.ui.theme.Ink.copy(alpha = 0.72f)
                            else if (connected) Primary else Muted,
                                fontSize = 12.sp, lineHeight = 16.sp)
                        }
                        // "Kople til" says what happens; a bare plus reads as "add another".
                        if (connected) {
                            Icon(Icons.Rounded.Check, null, tint = Primary, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Kople til", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = if (primary) app.reelstack.ui.theme.Ink else PrimarySoft)
                        }
                    }
                }
            }
        }
        item {
            // A disclosure for an optional path, so it stays quieter than connecting a server.
            TextButton(onClick = { advancedServices = !advancedServices },
                colors = ButtonDefaults.textButtonColors(contentColor = Muted),
                modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(if (advancedServices) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null, Modifier.size(18.dp))
                Text(if (advancedServices) "Skjul tenarverktøy" else "Tenarverktøy for administratorar",
                    modifier = Modifier.padding(start = 8.dp))
            }
            if (ready) {
                Button(onClick = onContinue, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp).heightIn(min = 56.dp)) {
                    Text("Opne oversikta mi", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                }
            } else {
                // Demo is a fallback, so it stays quieter than connecting a real server.
                TextButton(onClick = onContinue,
                    colors = ButtonDefaults.textButtonColors(contentColor = Muted),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 10.dp)) {
                    Text("Utforsk med demodata først", fontSize = 13.sp)
                }
            }
            Text("Tilkoplingane blir lagra på denne eininga. Du kan endre dei når som helst i Innstillingar.",
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 16.dp))
        }
    }
    }
}
