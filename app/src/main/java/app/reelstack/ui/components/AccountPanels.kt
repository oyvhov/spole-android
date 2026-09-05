package app.reelstack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.Caution
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor

@Composable
fun SettingsAccounts(state: ReelstackUiState, onConnectionClick: (ServiceKind) -> Unit) {
    Surface(color = SurfaceRaised, shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp).testTag("settings-accounts"),
    ) {
      Column {
        SettingsAccountPanel(state, ServiceKind.SEERR, onClick = { onConnectionClick(ServiceKind.SEERR) })
        SettingsAccountPanel(state, ServiceKind.JELLYFIN, compact = true, onClick = { onConnectionClick(ServiceKind.JELLYFIN) })
        if (state.connections.any { it.kind == ServiceKind.EMBY && it.baseUrl.isNotBlank() }) {
            SettingsAccountPanel(state, ServiceKind.EMBY, compact = true, onClick = { onConnectionClick(ServiceKind.EMBY) })
        }
      }
    }
}

@Composable
private fun SettingsAccountPanel(state: ReelstackUiState, source: ServiceKind, compact: Boolean = false, onClick: () -> Unit) {
    val connection = state.connections.firstOrNull { it.kind == source }
    val loading = source in state.loadingAccounts
    val hasError = source in state.accountErrors
    val account = state.verifiedPanelAccount(source)
    val isSeerr = source == ServiceKind.SEERR
    val overviewOnly = isSeerr && (account?.isPersonal == false || connection.isSeerrApiKey())

    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().testTag("settings-account-${source.name.lowercase()}"),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (compact && account == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServiceSymbol(source, Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text(source.displayName, color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (loading) "Hentar kontoen…" else if (hasError) "Logg inn på nytt" else "Bruk kontoen din",
                            color = Muted, fontSize = 12.sp)
                    }
                    if (!loading) TextButton(onClick = onClick) { Text("Logg inn") }
                }
                return@Column
            }
            if (account != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (compact) ServiceSymbol(source, Modifier.size(24.dp))
                    else AccountAvatar(account, connection, Modifier.size(56.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (compact) source.displayName else account.displayName,
                            color = TextColor,
                            fontSize = if (isSeerr) 21.sp else 16.sp,
                            lineHeight = if (isSeerr) 26.sp else 21.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(if (compact) account.displayName else account.source.displayName, color = Muted, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 3.dp))
                        if (account.isAdmin && !compact) Text("Administrator", color = Primary, fontSize = 11.sp,
                            modifier = Modifier.padding(top = 5.dp))
                    }
                    if (!isSeerr || (account.isPersonal && !overviewOnly)) {
                        AccountEditButton(onClick, "Endre ${source.displayName}-konto")
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (loading) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    } else {
                        ServiceSymbol(source, Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(source.displayName, color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                loading -> "Hentar kontoen…"
                                hasError -> "Kunne ikkje stadfeste kontoen."
                                overviewOnly -> "Ingen personleg konto stadfesta."
                                else -> "Logg inn med kontoen din."
                            },
                            color = if (hasError && !loading) Caution else Muted,
                            fontSize = 13.sp, lineHeight = 19.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            if (overviewOnly) {
                Text("Administratornøkkel · berre oversikt", color = Muted, fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 12.dp))
            }

            if (!loading && (account == null || overviewOnly)) {
                if (hasError) {
                    Text("Opne innlogginga for å prøve igjen.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 8.dp))
                }
                TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        when {
                            isSeerr -> "Logg inn med Jellyfin"
                            hasError -> "Prøv igjen"
                            else -> "Logg inn på ${source.displayName}"
                        },
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun RequestIdentity(state: ReelstackUiState, onSignIn: () -> Unit) {
    val source = ServiceKind.SEERR
    val connection = state.connections.firstOrNull { it.kind == source && it.baseUrl.isNotBlank() } ?: return
    val loading = source in state.loadingAccounts
    val hasError = source in state.accountErrors
    val account = state.verifiedPanelAccount(source)
    val overviewOnly = account?.isPersonal == false || connection.isSeerrApiKey()
    val personalAccount = account?.takeIf { it.isPersonal && !overviewOnly }

    Surface(
        color = SurfaceRaised,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("request-identity"),
    ) {
        if (personalAccount != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            ) {
                AccountAvatar(personalAccount, connection, Modifier.size(28.dp))
                Text("Som ${personalAccount.displayName}", color = TextColor, fontSize = 13.sp, lineHeight = 19.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                AccountEditButton(onSignIn, "Endre konto for førespurnader")
            }
        } else {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (loading) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        when {
                            loading -> "Stadfestar kontoen for førespurnader…"
                            hasError -> "Kunne ikkje stadfeste Seerr-kontoen."
                            overviewOnly -> "Administratornøkkel · berre oversikt"
                            else -> "Logg inn for å sende førespurnader som deg."
                        },
                        color = if (hasError && !loading) Caution else Muted,
                        fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f),
                    )
                }
                if (!loading) {
                    if (hasError || overviewOnly) {
                        Text(
                            if (hasError) "Opne innlogginga for å prøve igjen."
                            else "Logg inn med Jellyfin for å bruke din eigen Seerr-konto.",
                            color = Muted, fontSize = 12.sp, lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    TextButton(onClick = onSignIn, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (hasError) "Prøv igjen" else "Logg inn med Jellyfin", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountEditButton(onClick: () -> Unit, description: String) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Rounded.Edit, contentDescription = description, tint = Primary, modifier = Modifier.size(18.dp))
    }
}

// Keep verified profiles stable during refresh; the ViewModel clears them when credentials change.
private fun ReelstackUiState.verifiedPanelAccount(source: ServiceKind): ServiceAccount? =
    accounts[source]?.takeIf {
        source !in accountErrors && it.source == source && it.displayName.isNotBlank()
    }

/** Prefer a verified personal identity, never the owner represented by a shared API key. */
fun ReelstackUiState.preferredHomeAccount(): ServiceAccount? =
    listOf(ServiceKind.SEERR, ServiceKind.JELLYFIN, ServiceKind.EMBY).firstNotNullOfOrNull { source ->
        val connection = connections.firstOrNull { it.kind == source && it.baseUrl.isNotBlank() && it.token.isNotBlank() }
        verifiedPanelAccount(source)?.takeIf { it.isPersonal && connection != null && !connection.isSeerrApiKey() }
    }

private fun ServiceConnection?.isSeerrApiKey(): Boolean =
    this != null && kind == ServiceKind.SEERR && baseUrl.isNotBlank() && token.isNotBlank() && !sessionCookie
