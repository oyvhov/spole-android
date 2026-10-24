package app.reelstack.ui.kids

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.ui.components.DetailTextSkeleton

/** The launcher's explicit details action, contained entirely in the child's own shell. */
@Composable
internal fun KidsLauncherDetailsContent(details: ContentDetails, onPlay: () -> Unit,
    onEpisodes: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (details.title.isNotBlank()) Text(details.title, style = MaterialTheme.typography.headlineMedium)
        when {
            details.loading -> DetailTextSkeleton()
            details.error != null -> Text(details.error, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                details.overview?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                if (details.facts.isNotEmpty()) Text(details.facts.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!details.remoteId.isNullOrBlank()) {
                    val series = details.mediaType.equals("Series", true) || details.mediaType.equals("Season", true)
                    val interaction = remember { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    val shape = RoundedCornerShape(18.dp)
                    Button(onClick = if (series) onEpisodes else onPlay, interactionSource = interaction, shape = shape,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).then(
                            if (focused) Modifier.border(6.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier)) {
                        Text(stringResource(if (series) R.string.calendar_show_episodes else R.string.player_play))
                    }
                }
            }
        }
        val closeInteraction = remember { MutableInteractionSource() }
        val closeFocused by closeInteraction.collectIsFocusedAsState()
        val closeShape = RoundedCornerShape(18.dp)
        TextButton(onClick = onClose, interactionSource = closeInteraction, shape = closeShape,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).then(
                if (closeFocused) Modifier.border(6.dp, MaterialTheme.colorScheme.onSurface, closeShape) else Modifier)) {
            Text(stringResource(R.string.action_close))
        }
    }
}
