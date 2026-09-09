package app.reelstack.ui.components

import app.reelstack.data.repository.DeviceIdentity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

private object ProfileImageMemory {
    private val images = object : LinkedHashMap<String, ByteArray>(12, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?) = size > 12
    }
    @Synchronized fun get(key: String): ByteArray? = images[key]
    @Synchronized fun put(key: String, image: ByteArray) { images[key] = image }
}

@Composable
fun AccountAvatar(account: ServiceAccount?, connection: ServiceConnection?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cacheKey = account?.let { "${it.source}:${it.id}:${it.avatarUrl}" }
    val image by produceState<ByteArray?>(cacheKey?.let(ProfileImageMemory::get), cacheKey, connection?.token, connection?.baseUrl) {
        value = cacheKey?.let(ProfileImageMemory::get)
        if (value != null) return@produceState
        val url = account?.avatarUrl ?: return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                loadProfileImage(url, connection, DeviceIdentity.get(context))
            }.getOrNull()?.also { data -> cacheKey?.let { ProfileImageMemory.put(it, data) } }
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

/** The same account affordance on Home, Discover and Activity. */
@Composable
fun AccountAvatarButton(
    account: ServiceAccount?,
    connection: ServiceConnection?,
    onClick: () -> Unit,
    description: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.size(48.dp).focusOutline(interaction, androidx.compose.foundation.shape.CircleShape)
            .testTag(testTag).semantics { contentDescription = description },
    ) {
        AccountAvatar(account, connection, Modifier.size(40.dp))
    }
}
