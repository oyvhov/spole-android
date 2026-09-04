package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.ServiceLogo
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.Muted
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(if (ready) "02 / KLAR FOR DEG" else "01 / GJER DET TIL DITT", color = Primary,
                fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(if (ready) "Din samling.\nDi oversikt." else "Alt du ser.\nÉin stad.",
                color = TextColor, fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-2).sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
            Text(if (ready) "Du er klar. Kople til fleire tenester no, eller finn dei i Innstillingar seinare."
                else "Kople til ei teneste for å sjå biblioteket ditt, nye episodar og det som kjem snart.",
                color = Muted, style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp))
        }
        ServiceKind.entries.forEach { kind ->
            item(key = kind.name) {
                val connected = state.connections.any { it.kind == kind && it.baseUrl.isNotBlank() }
                Surface(onClick = { onConnect(kind) }, color = SurfaceRaised,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (kind == ServiceKind.JELLYFIN || kind == ServiceKind.EMBY) {
                            ServiceLogo(kind, null, Modifier.size(28.dp))
                        } else {
                            Icon(when (kind) {
                                ServiceKind.SEERR -> Icons.Rounded.Search
                                ServiceKind.RADARR -> Icons.Rounded.Movie
                                else -> Icons.Rounded.Tv
                            }, null, tint = PrimarySoft, modifier = Modifier.size(28.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Text(kind.displayName, fontWeight = FontWeight.SemiBold, color = TextColor)
                            Text(if (connected) "Tilkopla · klar til bruk" else when (kind) {
                                ServiceKind.JELLYFIN -> "Bibliotek · Quick Connect eller konto"
                                ServiceKind.EMBY -> "Bibliotek og aktive avspelingar"
                                ServiceKind.SEERR -> "Oppdag · logg inn med Jellyfin-konto"
                                ServiceKind.RADARR -> "Filmar, utgjevingar og nedlastingar"
                                ServiceKind.SONARR -> "Episodar, kalender og nedlastingar"
                            }, color = if (connected) Primary else Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                        Icon(if (connected) Icons.Rounded.Check else Icons.Rounded.Add,
                            null, tint = if (connected) Primary else Muted, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        item {
            if (ready) {
                Button(onClick = onContinue, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(56.dp)) {
                    Text("Opne oversikta mi", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                }
            } else {
                TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Text("Utforsk med demodata først")
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 10.dp).size(18.dp))
                }
            }
            Text("Tilkoplingane blir lagra på denne eininga. Du kan endre dei når som helst i Innstillingar.",
                color = Muted, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
