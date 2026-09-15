package app.reelstack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.trailerLink

@Composable
internal fun TrailerPreview(url: String, title: String) {
    val safe = trailerLink(url) ?: return
    var open by remember(url) { mutableStateOf(false) }
    val context = LocalContext.current
    TextButton(onClick = { open = true }, modifier = Modifier.testTag("detail-trailer")) {
        Icon(SpoleIcons.PlaySimple, null, Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.phase_trailer))
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MediaArtwork("https://i.ytimg.com/vi/${safe.substringAfter("v=")}/hqdefault.jpg", null,
                Modifier.fillMaxWidth().aspectRatio(16f / 9f), fallbackRes = R.drawable.media_placeholder)
            Text(stringResource(R.string.phase_trailer_hint), style = MaterialTheme.typography.bodyMedium)
        } },
        confirmButton = { SpoleSecondaryButton(onClick = {
            runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(safe))) }
                .onSuccess { open = false }
                .onFailure { android.widget.Toast.makeText(context, R.string.phase_trailer_error, android.widget.Toast.LENGTH_LONG).show() }
        }, modifier = Modifier.testTag("trailer-play")) { Text(stringResource(R.string.phase_quick_play)) } },
        dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_close)) } })
}
