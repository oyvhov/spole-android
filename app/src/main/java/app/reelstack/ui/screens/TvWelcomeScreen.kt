package app.reelstack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.ServiceSymbol
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.*

/** Sofa-distance setup: branding never pushes the primary service choices below the fold. */
@Composable
internal fun TvWelcomeScreen(state: ReelstackUiState, onConnect: (ServiceKind) -> Unit,
    onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val ready = state.configuredCount > 0
    var advanced by rememberSaveable { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp)) {
        val wide = maxWidth >= 680.dp
        val introduction: @Composable () -> Unit = {
            Column(Modifier.padding(end = if (wide) 24.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(R.drawable.spole_mark), null, Modifier.size(40.dp))
                    Text("Spole", color = MaterialTheme.colorScheme.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(if (ready) R.string.welcome_title_ready else R.string.welcome_title),
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.tv_setup_intro), color = Muted, style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.tv_setup_remote), color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        val actions: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ready) TvSetupAction(stringResource(R.string.welcome_open), onContinue,
                    Modifier.focusRequester(first).testTag("tv-setup-continue"), primary = true)
                listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY, ServiceKind.SEERR).plus(
                    if (advanced) listOf(ServiceKind.RADARR, ServiceKind.SONARR) else emptyList()
                ).forEach { kind ->
                    val connected = state.connections.any { it.kind == kind && it.baseUrl.isNotBlank() }
                    val interaction = remember { MutableInteractionSource() }
                    val shape = RoundedCornerShape(16.dp)
                    Surface(onClick = { onConnect(kind) }, interactionSource = interaction,
                        color = SurfaceRaised, contentColor = MaterialTheme.colorScheme.onSurface, shape = shape,
                        modifier = Modifier.fillMaxWidth()
                            .then(if (!ready && kind == ServiceKind.JELLYFIN) Modifier.focusRequester(first) else Modifier)
                            .focusOutline(interaction, shape).testTag("tv-setup-${kind.name}")) {
                        Row(Modifier.heightIn(min = 76.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            ServiceSymbol(kind, Modifier.size(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text(kind.displayName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(if (connected) R.string.welcome_connected else when (kind) {
                                    ServiceKind.JELLYFIN -> R.string.tv_setup_jellyfin
                                    ServiceKind.EMBY -> R.string.welcome_emby
                                    ServiceKind.SEERR -> R.string.tv_setup_seerr
                                    ServiceKind.RADARR -> R.string.welcome_radarr
                                    ServiceKind.SONARR -> R.string.welcome_sonarr
                                }), color = Muted, fontSize = 14.sp, lineHeight = 19.sp)
                            }
                            if (connected) Icon(Icons.Rounded.CheckCircle, null, tint = Success)
                        }
                    }
                }
                TvSetupAction(stringResource(if (advanced) R.string.welcome_hide_admin else R.string.welcome_admin),
                    { advanced = !advanced }, Modifier.testTag("tv-setup-advanced"))
                if (!ready) TvSetupAction(stringResource(R.string.welcome_demo), onContinue, Modifier.testTag("tv-setup-demo"))
            }
        }
        if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(Modifier.weight(.9f).fillMaxHeight().verticalScroll(rememberScrollState())) { introduction() }
            Box(Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("tv-setup-actions")) { actions() }
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            introduction(); actions()
        }
    }
}

@Composable
private fun TvSetupAction(label: String, onClick: () -> Unit, modifier: Modifier, primary: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Button(onClick, interactionSource = interaction, shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) Primary else SurfaceRaised,
            contentColor = if (primary) Ink else MaterialTheme.colorScheme.onSurface),
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).focusOutline(interaction, shape)) {
        Text(label)
    }
}
