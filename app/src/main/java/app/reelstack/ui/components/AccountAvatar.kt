package app.reelstack.ui.components

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.network.loadProfileImage
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.SurfaceRaised
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AccountAvatar(account: ServiceAccount?, connection: ServiceConnection?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val image by produceState<ByteArray?>(null, account, connection?.token, connection?.baseUrl) {
        value = null
        val url = account?.avatarUrl ?: return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "homereel-android"
                loadProfileImage(url, connection, deviceId)
            }.getOrNull()
        }
    }
    Box(modifier.clip(CircleShape).background(SurfaceRaised), contentAlignment = Alignment.Center) {
        val initial = account?.displayName?.trim()?.firstOrNull()?.uppercase()
        if (initial != null) Text(initial, color = Primary, fontSize = 20.sp)
        else Icon(Icons.Rounded.Person, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
        image?.let { data ->
            AsyncImage(model = data, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}
