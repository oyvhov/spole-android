package app.reelstack.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import app.reelstack.ui.components.DetailTextSkeleton

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
    onConnectionAuthModeChange: (ConnectionAuthMode) -> Unit,
    onConnectionUsernameChange: (String) -> Unit,
    onConnectionPasswordChange: (String) -> Unit,
    onTestAndSaveConnection: () -> Unit,
    onRemoveConnection: (ServiceKind) -> Unit,
    onAddMedia: (String) -> Unit,
) {
    val sheet = state.activeSheet ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Color(0xFF15111F),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color(0xB8040308),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 5.dp).size(width = 36.dp, height = 4.dp)
                    .background(Color(0xFF81788D), CircleShape),
            )
        },
    ) {
        when (sheet) {
            is AppSheet.SessionDetails -> SessionSheet(state, sheet.sessionKey, onPlaybackToggle)
            is AppSheet.MediaDetails -> MediaDetailsSheet(state, sheet.mediaId)
            is AppSheet.LibraryDetails -> LibraryDetailsSheet(state, sheet.mediaId)
            is AppSheet.TitleDetails -> state.contentDetails?.let {
                RichTitleDetailsSheet(state = state, onAddMedia = onAddMedia)
            }
            is AppSheet.ConnectionEditor -> connectionDraft?.let {
                ConnectionEditorSheet(
                    draft = it,
                    configured = state.connections.firstOrNull { item -> item.kind == it.kind }?.baseUrl?.isNotBlank() == true,
                    onDismiss = onDismiss,
                    onNameChange = onConnectionNameChange,
                    onUrlChange = onConnectionUrlChange,
                    onTokenChange = onConnectionTokenChange,
                    onUserIdChange = onConnectionUserIdChange,
                    onAuthModeChange = onConnectionAuthModeChange,
                    onUsernameChange = onConnectionUsernameChange,
                    onPasswordChange = onConnectionPasswordChange,
                    onTestAndSave = onTestAndSaveConnection,
                    onRemove = { onRemoveConnection(it.kind) },
                )
            }
        }
    }
}

@Composable
private fun RichTitleDetailsSheet(state: ReelstackUiState, onAddMedia: (String) -> Unit) {
    val details = state.contentDetails ?: return
    val discoverMedia = (state.discover + state.searchResults).firstOrNull { it.id == details.key }
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .animateContentSize(animationSpec = spring())
            .padding(start = 18.dp, end = 18.dp, bottom = 40.dp),
    ) {
        CinematicTitleHero(
            title = details.title,
            eyebrow = details.eyebrow,
            artworkUrl = details.artworkUrl,
            artworkRes = details.artworkRes,
            source = details.source,
        )
        if (details.facts.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 16.dp),
            ) {
                details.facts.take(5).forEach { fact -> DetailPill(fact) }
            }
        }
        if (details.genres.isNotEmpty()) {
            Text(
                details.genres.take(4).joinToString(" · "),
                color = PrimarySoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp, top = 15.dp, end = 6.dp),
            )
        }
        Text(
            details.overview ?: details.subtitle,
            color = Color(0xFFE8E0EF),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(start = 6.dp, top = 14.dp, end = 6.dp),
        )
        if (details.loading) {
            DetailTextSkeleton(Modifier.fillMaxWidth().padding(top = 14.dp))
        }
        details.error?.let {
            Text(it, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
        }
        if (discoverMedia != null && !discoverMedia.inLibrary) {
            val adding = discoverMedia.id in state.requestingMediaIds
            Button(
                onClick = { onAddMedia(discoverMedia.id) },
                enabled = !discoverMedia.requested && !adding,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Ink,
                    disabledContainerColor = Color(0xFF2A473B),
                    disabledContentColor = Color(0xFFC9F4DB),
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp).height(56.dp),
            ) {
                if (adding) {
                    CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(19.dp))
                } else {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(
                    when {
                        adding -> "Legg til…"
                        discoverMedia.requested -> "Lagd til"
                        else -> "Legg til i mediesamlinga"
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CinematicTitleHero(
    title: String,
    eyebrow: String,
    artworkUrl: String?,
    artworkRes: Int,
    source: ServiceKind?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(264.dp)
            .clip(RoundedCornerShape(28.dp)),
    ) {
        MediaArtwork(
            url = artworkUrl,
            fallbackRes = artworkRes,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            source = source,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x12090711),
                    0.44f to Color(0x22090711),
                    1f to Color(0xF20B0812),
                ),
            ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 19.dp),
        ) {
            Text(
                eyebrow.uppercase(),
                color = PrimarySoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun DetailPill(text: String) {
    Text(
        text = text,
        color = Color(0xFFE8E0EF),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF30283F))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun SheetHeader(title: String, description: String, onDismiss: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(description, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
        onDismiss?.let {
            IconButton(onClick = it) { Icon(Icons.Rounded.Close, contentDescription = "Lukk") }
        }
    }
}

@Composable
private fun SessionSheet(state: ReelstackUiState, sessionKey: String, onPlaybackToggle: (String) -> Unit) {
    val session = state.sessions.firstOrNull { it.key == sessionKey } ?: return
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 34.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(286.dp).clip(RoundedCornerShape(28.dp)),
        ) {
            MediaArtwork(
                url = session.artworkUrl,
                fallbackRes = if (session.sessionId?.startsWith("demo-") == true) {
                    R.drawable.session_still
                } else {
                    R.drawable.media_placeholder
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                source = session.source,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0x26090711),
                        0.48f to Color(0x18090711),
                        1f to Color(0xF20A0711),
                    ),
                ),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                    .clip(CircleShape).background(Color(0xB5120E1B))
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            ) {
                Box(Modifier.size(7.dp).background(Primary, CircleShape))
                Text(
                    if (session.paused) "På pause" else "Spelar no",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    "${session.source?.displayName ?: "Medietenar"} · ${session.userName} · ${session.deviceName}".uppercase(),
                    color = PrimarySoft,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    session.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(session.subtitle, color = Color(0xFFD1C8D8), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                LinearProgressIndicator(
                    progress = { session.progress.coerceIn(0f, 1f) },
                    color = Primary,
                    trackColor = Color(0x45FFFFFF),
                    modifier = Modifier.fillMaxWidth().padding(top = 13.dp).height(4.dp).clip(CircleShape),
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xA5221B30),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
            ) {
                SessionMetric("Straum", session.streamMethod, Modifier.weight(1f))
                SessionMetric("Kvalitet", session.quality, Modifier.weight(1f))
                SessionMetric("Att", session.timeLeft, Modifier.weight(1f))
            }
        }
        Button(
            onClick = { onPlaybackToggle(session.key) },
            enabled = state.pendingSessionKey == null,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color(0xFF160D20)),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(58.dp),
        ) {
            if (state.pendingSessionKey == session.key) {
                CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Text("Sender kommando…", modifier = Modifier.padding(start = 8.dp))
            } else {
                AnimatedContent(
                    targetState = session.paused,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "playback-action",
                ) { paused ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                        Text(if (paused) "Hald fram" else "Set på pause", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = Color(0xFF8E849B), fontSize = 9.sp)
        Text(
            value,
            color = Color(0xFFF1EBF8),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun MediaDetailsSheet(state: ReelstackUiState, mediaId: String) {
    val media = state.incoming.firstOrNull { it.id == mediaId } ?: return
    val downloading = media.state == IncomingState.DOWNLOADING
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 40.dp)) {
        SheetHeader(media.title, "${if (downloading) "Film" else "Serie"} · ${media.source.displayName}")
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
                    if (downloading) "Ferdig om om lag 24 minutt" else "Ventar på godkjenning",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    if (downloading) "Radarr fann ei 4K-utgjeving og sende henne til nedlastingsklienten." else "Seerr varslar deg når tittelen er godkjend.",
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
    val media = (state.recentMovies + state.recentSeries).firstOrNull { it.id == mediaId } ?: return
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 40.dp)) {
        SheetHeader(media.title, "Bibliotek i ${media.source.displayName}")
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
            Text("${(progress * 100).toInt()} % sett", color = PrimarySoft, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = Primary,
                trackColor = Color(0x2BCEBCEB),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(7.dp).clip(CircleShape),
            )
        }
        Text(
            "Opne ${media.source.displayName} for å spele av tittelen.",
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
    onAuthModeChange: (ConnectionAuthMode) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTestAndSave: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier.imePadding().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        SheetHeader("Kople til ${draft.kind.displayName}", draft.kind.role, onDismiss)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = draft.name,
            onValueChange = onNameChange,
            label = { Text("Namn på tilkoplinga") },
            singleLine = true,
            shape = RoundedCornerShape(17.dp),
            colors = connectionFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.url,
            onValueChange = onUrlChange,
            label = { Text("Tenaradresse") },
            placeholder = { Text("https://media.example.com") },
            singleLine = true,
            shape = RoundedCornerShape(17.dp),
            colors = connectionFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        if (draft.kind == ServiceKind.JELLYFIN) {
            Text(
                "Innlogging",
                color = PrimarySoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 15.dp, bottom = 7.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = draft.authMode == ConnectionAuthMode.QUICK_CONNECT,
                    onClick = { onAuthModeChange(ConnectionAuthMode.QUICK_CONNECT) },
                    label = { Text("Quick Connect") },
                    colors = connectionChipColors(),
                )
                FilterChip(
                    selected = draft.authMode == ConnectionAuthMode.ACCOUNT,
                    onClick = { onAuthModeChange(ConnectionAuthMode.ACCOUNT) },
                    label = { Text("Brukarnamn") },
                    colors = connectionChipColors(),
                )
                FilterChip(
                    selected = draft.authMode == ConnectionAuthMode.API_KEY,
                    onClick = { onAuthModeChange(ConnectionAuthMode.API_KEY) },
                    label = { Text("Tilgangsteikn") },
                    colors = connectionChipColors(),
                )
            }
        }
        val usesAccount = draft.kind == ServiceKind.JELLYFIN && draft.authMode == ConnectionAuthMode.ACCOUNT
        val usesQuickConnect = draft.kind == ServiceKind.JELLYFIN &&
            draft.authMode == ConnectionAuthMode.QUICK_CONNECT
        if (usesQuickConnect) {
            QuickConnectPanel(draft)
        }
        if (usesAccount) {
            OutlinedTextField(
                value = draft.username,
                onValueChange = onUsernameChange,
                label = { Text("Brukarnamn") },
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            OutlinedTextField(
                value = draft.password,
                onValueChange = onPasswordChange,
                label = { Text("Passord") },
                supportingText = { Text("Kan stå tomt for ein konto utan passord.") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(17.dp),
                colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            Text(
                "Passordet blir sendt direkte til Jellyfin for innlogging og blir aldri lagra i HomeReel.",
                color = Muted,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (!usesAccount && !usesQuickConnect &&
            (draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.EMBY)
        ) {
            OutlinedTextField(
                value = draft.userId,
                onValueChange = onUserIdChange,
                label = { Text("Profil-ID (valfri)") },
                placeholder = { Text("Bruk ein bestemt medieprofil") },
                supportingText = { Text("La feltet stå tomt for automatisk val av ein profil med alle bibliotek.") },
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
        if (!usesAccount && !usesQuickConnect) {
            OutlinedTextField(
                value = draft.token,
                onValueChange = onTokenChange,
                label = { Text("API-nøkkel eller tilgangsteikn") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(17.dp),
                colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }

        draft.warning?.let { MessageCard(it, warning = true) }
        draft.error?.let { MessageCard(it, warning = false) }

        Button(
            onClick = onTestAndSave,
            enabled = !draft.saving,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(56.dp),
        ) {
            if (draft.saving) {
                CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Text(
                    when {
                        usesQuickConnect && draft.quickConnectCode == null -> "Lagar kode…"
                        usesQuickConnect -> "Koplar til…"
                        usesAccount -> "Loggar inn…"
                        else -> "Testar tilkoplinga…"
                    },
                    modifier = Modifier.padding(start = 9.dp),
                )
            } else {
                Icon(
                    if (usesQuickConnect) Icons.Rounded.Devices else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                )
                Text(
                    when {
                        usesQuickConnect && draft.quickConnectCode != null -> "Lag ny kode"
                        usesQuickConnect -> "Start Quick Connect"
                        usesAccount -> "Logg inn og lagre"
                        else -> "Test og lagre"
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        if (configured) {
            TextButton(
                onClick = onRemove,
                colors = ButtonDefaults.textButtonColors(contentColor = Warning),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(19.dp))
                Text("Fjern tilkoplinga", modifier = Modifier.padding(start = 7.dp))
            }
        }
    }
}

@Composable
private fun QuickConnectPanel(draft: ConnectionDraft) {
    AnimatedContent(
        targetState = draft.quickConnectCode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "quick-connect-code",
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) { code ->
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF211A30),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (code == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Devices,
                            contentDescription = null,
                            tint = PrimarySoft,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(
                            "Logg inn utan passord",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "HomeReel lagar ein kort kode som du godkjenner i ein Jellyfin-app der du allereie er innlogga.",
                            color = Muted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(Primary, CircleShape))
                        Text(
                            if (draft.quickConnectWaiting) "Ventar på godkjenning" else "Fullfører innlogginga",
                            color = PrimarySoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 7.dp),
                        )
                    }
                    Text(
                        code.chunked(3).joinToString("  "),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        "Opne Jellyfin på ein annan eining, gå til Innstillingar → Quick Connect, og skriv inn koden.",
                        color = Muted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun connectionChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Primary,
    selectedLabelColor = Ink,
    containerColor = SurfaceRaised.copy(alpha = 0.78f),
    labelColor = Muted,
)

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
