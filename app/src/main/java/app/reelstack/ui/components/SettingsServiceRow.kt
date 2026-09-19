package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.theme.Caution
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.Success
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Warning
import coil3.compose.AsyncImage

@Composable
internal fun SettingsServiceRow(
    connection: ServiceConnection,
    account: String? = null,
    avatarUrl: String? = null,
    warning: String? = null,
    tag: String = "service-${connection.kind}",
    onClick: () -> Unit,
) {
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
    val shape = RoundedCornerShape(16.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .focusOutline(interaction, shape)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { stateDescription = status }
            .testTag(tag)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Left: Service Icon & Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(min = 160.dp),
        ) {
            ServiceSymbol(connection.kind, Modifier.size(32.dp).testTag("settings-service-icon-${connection.kind}"))
            Text(
                text = connection.kind.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Middle: User Profile with Avatar (if signed in)
        Box(modifier = Modifier.weight(1f)) {
            if (!account.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SurfaceRaised)
                            .border(1.5.dp, Primary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = account.trim().firstOrNull()?.uppercase().orEmpty(),
                                color = Primary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Column {
                        Text(
                            text = account,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (detail != null) {
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Right: Status badge & chevron
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    when (health) {
                        ServiceHealth.OK -> SpoleIcons.DoneCircle
                        ServiceHealth.WARNING, ServiceHealth.ERROR -> SpoleIcons.Alert
                        ServiceHealth.CHECKING -> SpoleIcons.Clock
                        else -> SpoleIcons.Info
                    },
                    null,
                    Modifier.size(16.dp).testTag("service-health-${connection.kind}-$health"),
                    tint = color,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = color,
                )
            }

            Icon(
                SpoleIcons.ChevronRight,
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
