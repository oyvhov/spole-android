package app.reelstack.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.network.DiscoveredServer
import app.reelstack.ui.components.DiscoveredServerCard
import app.reelstack.ui.components.SpoleBrandMark
import app.reelstack.ui.components.SpoleSecondaryButton
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.isTelevision
import app.reelstack.ui.layout.WindowLayoutPolicy
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.ReelPage
import app.reelstack.ui.theme.SurfaceRaised

/**
 * First run: the servers already in the house come first, typing an address second.
 *
 * On a television the remote starts on «Skriv inn adressa». When a server answers before the
 * viewer has pressed anything, focus moves to it — a choice nobody has started making can still
 * be improved; one they are in the middle of is left alone.
 */
@Composable
internal fun SimpleWelcomeScreen(
    onCombined: () -> Unit,
    onOther: () -> Unit,
    modifier: Modifier = Modifier,
    discovered: List<DiscoveredServer> = emptyList(),
    discovering: Boolean = false,
    onPickServer: (DiscoveredServer) -> Unit = {},
    onRescan: () -> Unit = {},
) {
    val manualFocus = remember { FocusRequester() }
    val serverFocus = remember { FocusRequester() }
    var touched by remember { mutableStateOf(false) }
    val television = isTelevision()
    LaunchedEffect(Unit) { manualFocus.requestFocus() }
    LaunchedEffect(discovered.firstOrNull()?.id) {
        if (television && discovered.isNotEmpty() && !touched) {
            withFrameNanos { }
            runCatching { serverFocus.requestFocus() }
        }
    }
    ReelPage {
        BoxWithConstraints(modifier.fillMaxSize()
            .onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown) touched = true; false }
            .padding(horizontal = if (television) 40.dp else 24.dp, vertical = 24.dp)) {
            val wide = WindowLayoutPolicy(maxWidth.value, maxHeight.value).useInlineHeader
            val introduction: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SpoleBrandMark(Modifier.size(if (wide) 56.dp else 48.dp))
                    Text(stringResource(R.string.welcome_headline), style = MaterialTheme.typography.headlineLarge)
                    Text(stringResource(R.string.welcome_subtitle), style = MaterialTheme.typography.bodyLarge)
                    // The two-pane screen has room to show the whole journey before it starts.
                    if (wide) app.reelstack.ui.components.SetupJourney(Modifier.padding(top = 20.dp))
                    else if (television) Text(stringResource(R.string.setup_discovery_tv_hint), color = Muted,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
            val actions: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.setup_discovery_heading), style = MaterialTheme.typography.labelLarge,
                            color = Muted, modifier = Modifier.weight(1f, fill = false))
                        if (discovering) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Muted)
                    }
                    discovered.forEachIndexed { index, server ->
                        DiscoveredServerCard(server, { onPickServer(server) },
                            if (index == 0) Modifier.focusRequester(serverFocus) else Modifier)
                    }
                    if (discovered.isEmpty()) {
                        Surface(color = SurfaceRaised.copy(alpha = .55f), shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("setup-discovery-status")) {
                            Text(stringResource(if (discovering) R.string.setup_discovery_searching else R.string.setup_discovery_none),
                                color = Muted, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite })
                        }
                    }
                    ManualAddressButton(primary = discovered.isEmpty() && !discovering, onCombined,
                        Modifier.focusRequester(manualFocus))
                    if (!discovering) SpoleSecondaryButton(onClick = onRescan, modifier = Modifier.fillMaxWidth().testTag("setup-rescan")) {
                        Text(stringResource(R.string.setup_discovery_rescan))
                    }
                    Text(stringResource(R.string.welcome_setup_hint), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SpoleSecondaryButton(onClick = onOther, modifier = Modifier.fillMaxWidth().testTag("setup-other")) {
                        Text(stringResource(R.string.welcome_other_methods))
                    }
                }
            }
            if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                Box(Modifier.weight(.85f).fillMaxHeight().verticalScroll(rememberScrollState()), contentAlignment = Alignment.CenterStart) {
                    introduction()
                }
                Box(Modifier.weight(1.15f).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("setup-actions")) {
                    actions()
                }
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 560.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    introduction(); actions()
                }
            }
        }
    }
}

@Composable
private fun ManualAddressButton(primary: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    Button(onClick = onClick, interactionSource = interaction, shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Primary else SurfaceRaised,
            contentColor = if (primary) Ink else MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).focusOutline(interaction, shape).testTag("setup-combined")) {
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.setup_manual_title), fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.setup_manual_note), fontSize = 14.sp, lineHeight = 19.sp,
                color = if (primary) Ink.copy(alpha = .75f) else Muted)
        }
    }
}
