package app.reelstack.ui.components

import android.provider.Settings
import app.reelstack.data.repository.DeviceIdentity
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
import app.reelstack.data.network.jellyfinAuthorization

@Composable
fun MediaArtwork(
    url: String?,
    @DrawableRes fallbackRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    source: ServiceKind? = null,
    crossfadeDurationMillis: Int = 260,
    /**
     * Reports width / height once the image is decoded. A caller that has to pick between a wide
     * and a portrait frame cannot know which it has been given until then — services return a
     * poster whenever no still exists.
     */
    onAspectRatio: ((Float) -> Unit)? = null,
) {
    val context = LocalContext.current
    val fallback = painterResource(fallbackRes)
    val motionEnabled = Settings.Global.getFloat(context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    val fadeDuration = if (motionEnabled) crossfadeDurationMillis else 0
    val model = remember(url, fallbackRes, source, fadeDuration) {
        runCatching {
            val builder = ImageRequest.Builder(context)
                .data(url ?: fallbackRes)
                .crossfade(fadeDuration)
            if (url != null && (source == ServiceKind.JELLYFIN || source == ServiceKind.EMBY)) {
                val connection = (context.applicationContext as? ReelstackApplication)
                    ?.container
                    ?.connectionRepository
                    ?.get(source)
                if (connection != null && connection.token.isNotBlank() &&
                    url.startsWith("${connection.baseUrl.trimEnd('/')}/")
                ) {
                    val headers = NetworkHeaders.Builder()
                    if (source == ServiceKind.JELLYFIN) {
                        headers.set(
                            "Authorization",
                            jellyfinAuthorization(DeviceIdentity.get(context), connection.token),
                        )
                    } else {
                        headers.set("X-Emby-Token", connection.token)
                    }
                    builder.httpHeaders(headers.build())
                }
            }
            builder.build()
        }.getOrElse {
            ImageRequest.Builder(context)
                .data(fallbackRes)
                .build()
        }
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = contentScale,
        onSuccess = onAspectRatio?.let { report ->
            { state ->
                val image = state.result.image
                if (image.width > 0 && image.height > 0) {
                    report(image.width.toFloat() / image.height.toFloat())
                }
            }
        },
        modifier = modifier,
    )
}
