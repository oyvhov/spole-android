package app.reelstack.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.DiscoveredServer
import app.reelstack.data.network.DiscoveredServerKind
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.SurfaceRaised

internal fun DiscoveredServerKind.serviceKind(): ServiceKind = when (this) {
    DiscoveredServerKind.JELLYFIN -> ServiceKind.JELLYFIN
    DiscoveredServerKind.EMBY -> ServiceKind.EMBY
}

/** "192.168.1.20:8097" — the scheme is noise on a living-room screen; HTTPS stays visible. */
internal fun readableServerAddress(address: String): String =
    if (address.startsWith("http://")) address.removePrefix("http://") else address

/**
 * One server that answered on the network, sized for a remote: the name is the largest thing on
 * it, the kind and address say which box in the house it is, and the whole card is one target.
 */
@Composable
internal fun DiscoveredServerCard(server: DiscoveredServer, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    val kind = server.kind.serviceKind()
    Surface(onClick = onClick, interactionSource = interaction, shape = shape, color = SurfaceRaised,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().focusOutline(interaction, shape).testTag("setup-server-${server.id}")) {
        Row(Modifier.heightIn(min = 76.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ServiceSymbol(kind, Modifier.size(30.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(server.name, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.setup_discovery_server_detail, kind.displayName, readableServerAddress(server.address)),
                    color = Muted, fontSize = 14.sp, lineHeight = 19.sp)
            }
            Text(stringResource(R.string.setup_discovery_sign_in), color = Primary, fontSize = 15.sp,
                lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
