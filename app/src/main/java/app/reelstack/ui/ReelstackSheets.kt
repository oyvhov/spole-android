package app.reelstack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Warning
import app.reelstack.ui.components.MediaArtwork

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelstackSheets(
    state: ReelstackUiState,
    connectionDraft: ConnectionDraft?,
    onDismiss: () -> Unit,
    onPlaybackToggle: (String) -> Unit,
    onConnectionNameChange: (String) -> Unit,
    onConnectionUrlChange: (String) -> Unit,
    onConnectionTokenChange: (String) -> Unit,
    onConnectionUserIdChange: (String) -> Unit,
    onTestAndSaveConnection: () -> Unit,
    onRemoveConnection: (ServiceKind) -> Unit,
) {
    val sheet = state.activeSheet ?: return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B1726),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color(0xB8040308),
        dragHandle = {
            Box(
                Modifier.padding(top = 11.dp, bottom = 6.dp).size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFF696174), CircleShape),
            )
        },
    ) {
        when (sheet) {
            is AppSheet.SessionDetails -> SessionSheet(state, sheet.sessionKey, onPlaybackToggle)
            is AppSheet.MediaDetails -> MediaDetailsSheet(state, sheet.mediaId)
            is AppSheet.LibraryDetails -> LibraryDetailsSheet(state, sheet.mediaId)
            is AppSheet.ConnectionEditor -> connectionDraft?.let {
                ConnectionEditorSheet(
                    draft = it,
                    configured = state.connections.firstOrNull { item -> item.kind == it.kind }?.baseUrl?.isNotBlank() == true,
                    onDismiss = onDismiss,
                    onNameChange = onConnectionNameChange,
                    onUrlChange = onConnectionUrlChange,
                    onTokenChange = onConnectionTokenChange,
                    onUserIdChange = onConnectionUserIdChange,
                    onTestAndSave = onTestAndSaveConnection,
                    onRemove = { onRemoveConnection(it.kind) },
                )
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, description: String, onDismiss: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(description, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
        onDismiss?.let {
            IconButton(onClick = it) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
        }
    }
}

@Composable
private fun SessionSheet(state: ReelstackUiState, sessionKey: String, onPlaybackToggle: (String) -> Unit) {
    val session = state.sessions.firstOrNull { it.key == sessionKey } ?: return
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 34.dp)) {
        SheetHeader("Live session", "${session.source?.displayName ?: "Media server"} · ${session.deviceName}")
        MediaArtwork(
            url = session.artworkUrl,
            fallbackRes = if (session.sessionId?.startsWith("demo-") == true) R.drawable.session_still else R.drawable.media_placeholder,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            source = session.source,
            modifier = Modifier.fillMaxWidth().padding(top = 17.dp).height(132.dp).clip(RoundedCornerShape(22.dp)),
        )
        Text("${session.userName} · ${session.deviceName}".uppercase(), color = PrimarySoft, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text(session.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 3.dp))
        Text(session.subtitle, color = Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            DetailCell("Stream", session.streamMethod, Modifier.weight(1f))
            DetailCell("Quality", session.quality, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            DetailCell("Server", session.source?.displayName ?: "Media server", Modifier.weight(1f))
            DetailCell("Viewer", session.userName, Modifier.weight(1f))
        }
        Button(
            onClick = { onPlaybackToggle(session.key) },
            enabled = state.pendingSessionKey == null,
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color(0xFF160D20)),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(52.dp),
        ) {
            if (state.pendingSessionKey == session.key) {
                CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Text("Sending command…", modifier = Modifier.padding(start = 8.dp))
            } else {
                Icon(if (session.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                Text(if (session.paused) "Resume playback" else "Pause playback", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun DetailCell(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(Color(0xB82F283E)).padding(12.dp)) {
        Text(label.uppercase(), color = Color(0xFF8E849B), fontSize = 9.sp)
        Text(value, color = Color(0xFFF1EBF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun MediaDetailsSheet(state: ReelstackUiState, mediaId: String) {
    val media = state.incoming.firstOrNull { it.id == mediaId } ?: return
    val downloading = media.state == IncomingState.DOWNLOADING
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 40.dp)) {
        SheetHeader(media.title, "${if (downloading) "Movie" else "Series"} · ${media.source.displayName}")
        Row(modifier = Modifier.padding(top = 18.dp)) {
            MediaArtwork(
                url = media.artworkUrl,
                fallbackRes = media.artworkRes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 116.dp, height = 164.dp).clip(RoundedCornerShape(20.dp)),
            )
            Column(Modifier.weight(1f).padding(start = 18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(CircleShape)
                        .background((if (downloading) Primary else Warning).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Icon(
                        if (downloading) Icons.Rounded.Download else Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = if (downloading) PrimarySoft else Warning,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(media.status, color = if (downloading) PrimarySoft else Warning, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                }
                Text(
                    if (downloading) "Arriving in about 24 minutes" else "Waiting for approval",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    if (downloading) "Radarr found a 4K release and sent it to your download client." else "Seerr will notify you when the request is approved.",
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryDetailsSheet(state: ReelstackUiState, mediaId: String) {
    val media = (state.continueWatching + state.recentlyAdded).firstOrNull { it.id == mediaId } ?: return
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 40.dp)) {
        SheetHeader(media.title, "${media.source.displayName} library")
        MediaArtwork(
            url = media.artworkUrl,
            fallbackRes = media.artworkRes,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            source = media.source,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(190.dp).clip(RoundedCornerShape(24.dp)),
        )
        Text(media.subtitle, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 15.dp))
        media.progress?.let { progress ->
            Text("${(progress * 100).toInt()}% watched", color = PrimarySoft, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = Primary,
                trackColor = Color(0x2BCEBCEB),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(7.dp).clip(CircleShape),
            )
        }
        Text(
            "Open ${media.source.displayName} to play this title.",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

@Composable
private fun ConnectionEditorSheet(
    draft: ConnectionDraft,
    configured: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onTestAndSave: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.imePadding().padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
        SheetHeader("Connect ${draft.kind.displayName}", draft.kind.role, onDismiss)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = draft.name,
            onValueChange = onNameChange,
            label = { Text("Connection name") },
            singleLine = true,
            shape = RoundedCornerShape(17.dp),
            colors = connectionFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.url,
            onValueChange = onUrlChange,
            label = { Text("Server address") },
            placeholder = { Text("https://media.example.com") },
            singleLine = true,
            shape = RoundedCornerShape(17.dp),
            colors = connectionFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        if (draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.EMBY) {
            OutlinedTextField(
                value = draft.userId,
                onValueChange = onUserIdChange,
                label = { Text("Profile ID (optional)") },
                placeholder = { Text("Use a specific media profile") },
                supportingText = { Text("Leave blank to detect a profile automatically.") },
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
        OutlinedTextField(
            value = draft.token,
            onValueChange = onTokenChange,
            label = { Text("API key or access token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(17.dp),
            colors = connectionFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )

        draft.warning?.let { MessageCard(it, warning = true) }
        draft.error?.let { MessageCard(it, warning = false) }

        Button(
            onClick = onTestAndSave,
            enabled = !draft.saving,
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(52.dp),
        ) {
            if (draft.saving) {
                CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Text("Testing connection…", modifier = Modifier.padding(start = 9.dp))
            } else {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                Text("Test and save", modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (configured) {
            TextButton(
                onClick = onRemove,
                colors = ButtonDefaults.textButtonColors(contentColor = Warning),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(19.dp))
                Text("Remove connection", modifier = Modifier.padding(start = 7.dp))
            }
        }
    }
}

@Composable
private fun MessageCard(text: String, warning: Boolean) {
    val accent = if (warning) Color(0xFFFFBE69) else Warning
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth().padding(top = 11.dp),
    ) {
        Text(text, color = accent, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun connectionFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary.copy(alpha = 0.72f),
    unfocusedBorderColor = Color(0x35DCCDF9),
    focusedContainerColor = SurfaceRaised.copy(alpha = 0.9f),
    unfocusedContainerColor = SurfaceRaised.copy(alpha = 0.75f),
    cursorColor = Primary,
)
