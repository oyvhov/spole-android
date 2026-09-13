package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.theme.Caution
import app.reelstack.ui.theme.Success
import app.reelstack.ui.theme.Warning

@Composable
internal fun SettingsServiceRow(connection: ServiceConnection, account: String? = null, warning: String? = null,
    tag: String = "service-${connection.kind}", onClick: () -> Unit) {
    val health = connection.health(warning)
    val status = stringResource(when (health) {
        ServiceHealth.OK -> R.string.service_connected
        ServiceHealth.WARNING -> R.string.service_partial
        ServiceHealth.ERROR -> R.string.service_failed
        ServiceHealth.CHECKING -> R.string.service_checking
        ServiceHealth.UNCHECKED -> R.string.service_unchecked
        ServiceHealth.UNCONFIGURED -> R.string.settings_tv_connection_action
    })
    val color = when (health) {
        ServiceHealth.OK -> Success
        ServiceHealth.WARNING -> Caution
        ServiceHealth.ERROR -> Warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val detail = when (health) {
        ServiceHealth.WARNING -> warning ?: connection.detail?.removePrefix("Tilkopla · ")
        ServiceHealth.ERROR -> connection.detail
        else -> null
    }?.takeIf { it.isNotBlank() && it != status }
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape)
        .focusOutline(interaction, shape).clip(shape)
        .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
            role = Role.Button, onClick = onClick)
        .semantics { stateDescription = status }.testTag(tag).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ServiceSymbol(connection.kind, Modifier.size(28.dp).testTag("settings-service-icon-${connection.kind}"))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(connection.kind.displayName, style = MaterialTheme.typography.titleMedium)
            account?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(when (health) {
                    ServiceHealth.OK -> SpoleIcons.DoneCircle
                    ServiceHealth.WARNING, ServiceHealth.ERROR -> SpoleIcons.Alert
                    ServiceHealth.CHECKING -> SpoleIcons.Clock
                    else -> SpoleIcons.Info
                }, null, Modifier.size(17.dp).testTag("service-health-${connection.kind}-$health"), tint = color)
                Text(status, style = MaterialTheme.typography.bodySmall, color = color)
            }
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(SpoleIcons.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
