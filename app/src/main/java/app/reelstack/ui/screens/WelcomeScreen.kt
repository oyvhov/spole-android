package app.reelstack.ui.screens

import androidx.compose.ui.res.stringResource
import app.reelstack.R

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
    val television = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    if (television) {
        TvWelcomeScreen(state, onConnect, onContinue, modifier)
        return
    }
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
            Text(if (ready) stringResource(R.string.welcome_ready) else stringResource(R.string.welcome_start), color = Muted,
                fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Text(if (ready) stringResource(R.string.welcome_title_ready) else stringResource(R.string.welcome_title),
                color = TextColor, fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-2).sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Text(if (ready) stringResource(R.string.welcome_ready_note)
                else stringResource(R.string.welcome_note),
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
                val actionLabel = if (connected) stringResource(R.string.welcome_connected_accessibility, kind.displayName)
                    else stringResource(R.string.service_connect, kind.displayName)
                Surface(
                    onClick = { onConnect(kind) },
                    color = if (primary) Primary else SurfaceRaised,
                    contentColor = if (primary) app.reelstack.ui.theme.Ink else TextColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = actionLabel
                    },
                ) {
                    Row(Modifier.heightIn(min = 64.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        ServiceSymbol(kind, Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Text(kind.displayName, fontWeight = FontWeight.SemiBold)
                            Text(if (connected) stringResource(R.string.welcome_connected) else when (kind) {
                                ServiceKind.JELLYFIN -> stringResource(R.string.welcome_jellyfin)
                                ServiceKind.EMBY -> stringResource(R.string.welcome_emby)
                                ServiceKind.SEERR -> stringResource(R.string.welcome_seerr)
                                ServiceKind.RADARR -> stringResource(R.string.welcome_radarr)
                                ServiceKind.SONARR -> stringResource(R.string.welcome_sonarr)
                            }, color = if (primary) app.reelstack.ui.theme.Ink.copy(alpha = 0.72f)
                            else if (connected) Primary else Muted,
                                fontSize = 12.sp, lineHeight = 16.sp)
                        }
                        // stringResource(R.string.action_connect) says what happens; a bare plus reads as "add another".
                        if (connected) {
                            Icon(Icons.Rounded.Check, null, tint = Primary, modifier = Modifier.size(20.dp))
                        } else {
                            Text(stringResource(R.string.action_connect), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
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
                Text(if (advancedServices) stringResource(R.string.welcome_hide_admin) else stringResource(R.string.welcome_admin),
                    modifier = Modifier.padding(start = 8.dp))
            }
            if (ready) {
                Button(onClick = onContinue, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp).heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.welcome_open), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                }
            } else {
                // Demo is a fallback, so it stays quieter than connecting a real server.
                TextButton(onClick = onContinue,
                    colors = ButtonDefaults.textButtonColors(contentColor = Muted),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 10.dp)) {
                    Text(stringResource(R.string.welcome_demo), fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
            Text(stringResource(R.string.welcome_storage),
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 16.dp))
        }
    }
    }
}
