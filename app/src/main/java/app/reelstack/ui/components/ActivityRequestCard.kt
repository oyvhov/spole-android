package app.reelstack.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ActivityRequestGroup
import app.reelstack.data.model.RequestStage
import app.reelstack.data.model.TrackedRequest
import app.reelstack.localization.requestStageLabel
import app.reelstack.ui.TrackedRequestCard
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Success
import app.reelstack.ui.theme.Warning

@Composable
internal fun ActivityRequestCard(group: ActivityRequestGroup, onDetails: (String) -> Unit,
    onNotify: (String, Boolean) -> Unit, onCancel: (String) -> Unit, cancelling: Set<String>) {
    var chosenKey by rememberSaveable(group.key) { mutableStateOf(group.requests.first().key) }
    var choose by rememberSaveable(group.key) { mutableStateOf(false) }
    val selected = group.requests.firstOrNull { it.key == chosenKey } ?: group.requests.first()
    val artwork = group.requests.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl
    val choices: (@Composable () -> Unit)? = if (group.requests.size > 1) {
        {
            val interaction = remember { MutableInteractionSource() }
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 4.dp)
                .focusOutline(interaction, RoundedCornerShape(10.dp), glow = false)
                .clickable(interactionSource = interaction, indication = mediaCardIndication(),
                    onClickLabel = stringResource(R.string.activity_choose_request), onClick = { choose = true })
                .testTag("activity-request-selector-${group.requests.first().key}")
                .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(requestSeasonLabel(selected), color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text(requestStatusLabel(selected), style = MaterialTheme.typography.bodySmall,
                        color = when (selected.stage) {
                            RequestStage.FAILED, RequestStage.DECLINED -> Warning
                            RequestStage.AVAILABLE -> Success
                            else -> MaterialTheme.colorScheme.onSurface
                        })
                }
                Icon(SpoleIcons.ChevronDown, null, tint = Muted, modifier = Modifier.size(18.dp))
            }
        }
    } else null
    // One stable cover for the group; every callback still targets the selected original request.
    TrackedRequestCard(selected.copy(artworkUrl = artwork), { onDetails(selected.key) }, { onNotify(selected.key, it) },
        onCancel = { onCancel(selected.key) }, cancelling = selected.key in cancelling, requestChoices = choices)
    if (choose && group.requests.size > 1) SpoleChoiceDialog(group.requests.first().title, { choose = false }, SpoleIcons.ListLines) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            group.requests.forEach { request ->
                SpoleChoiceRow(requestSeasonLabel(request), request.key == selected.key,
                    modifier = Modifier.testTag("activity-request-choice-${request.key}"),
                    supportingText = requestStatusLabel(request), checkmark = true) {
                    chosenKey = request.key
                    choose = false
                }
            }
        }
    }
}

@Composable
private fun requestSeasonLabel(request: TrackedRequest): String =
    stringResource(R.string.flow_season_numbers, request.seasons.sorted().joinToString(", ")) +
        (if (request.is4k) " · 4K" else "")

@Composable
private fun requestStatusLabel(request: TrackedRequest): String =
    requestStageLabel(request.stage) + (request.percent?.let { " · $it %" } ?: "")
