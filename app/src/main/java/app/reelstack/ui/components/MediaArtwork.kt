package app.reelstack.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import app.reelstack.ReelstackApplication
import app.reelstack.data.model.ServiceKind

@Composable
fun MediaArtwork(
    url: String?,
    @DrawableRes fallbackRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    source: ServiceKind? = null,
) {
    val context = LocalContext.current
    val fallback = painterResource(fallbackRes)
    val model = remember(url, fallbackRes, source) {
        val builder = ImageRequest.Builder(context)
            .data(url ?: fallbackRes)
            .crossfade(260)
        if (url != null && (source == ServiceKind.JELLYFIN || source == ServiceKind.EMBY)) {
            val connection = (context.applicationContext as? ReelstackApplication)
                ?.container
                ?.connectionRepository
                ?.get(source)
            if (connection != null && connection.token.isNotBlank() &&
                url.startsWith("${connection.baseUrl.trimEnd('/')}/")
            ) {
                builder.httpHeaders(
                    NetworkHeaders.Builder()
                        .set("X-Emby-Token", connection.token)
                        .build(),
                )
            }
        }
        builder.build()
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = contentScale,
        modifier = modifier,
    )
}
