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
    contentDescription: String?,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackRes: Int = 0,
    contentScale: ContentScale = ContentScale.Crop,
    source: ServiceKind? = null,
    protectAspectRatio: Boolean = true,
    crossfadeDurationMillis: Int = 260,
    onError: (() -> Unit)? = null,
    /**
     * Reports width / height once the image is decoded. A caller that has to pick between a wide
     * and a portrait frame cannot know which it has been given until then — services return a
     * poster whenever no still exists.
     */
    onAspectRatio: ((Float) -> Unit)? = null,
) {
    val context = LocalContext.current
    val fallback = if (fallbackRes != 0) painterResource(fallbackRes) else null
    // One binder call per frame per poster is what this used to be. The theme reads it once.
    val fadeDuration = if (app.reelstack.ui.theme.LocalMotionEnabled.current) crossfadeDurationMillis else 0
    val model = remember(url, fallbackRes, source, fadeDuration) {
        runCatching {
            val builder = ImageRequest.Builder(context)
                .data(url ?: fallbackRes.takeIf { it != 0 })
                .crossfade(fadeDuration)
            if (url != null) MediaAuthHeaders.forUrl(context, source, url)?.let(builder::httpHeaders)
            builder.build()
        }.getOrElse {
            ImageRequest.Builder(context)
                .apply { if (fallbackRes != 0) data(fallbackRes) }
                .build()
        }
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = if (protectAspectRatio && contentScale == ContentScale.Crop) SafeArtworkCrop else contentScale,
        onError = onError?.let { callback -> { callback() } },
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

/** Chooses the scale before the first bitmap frame, including cached portrait fallbacks. */
internal val SafeArtworkCrop = object : ContentScale {
    override fun computeScaleFactor(srcSize: androidx.compose.ui.geometry.Size,
        dstSize: androidx.compose.ui.geometry.Size): androidx.compose.ui.layout.ScaleFactor {
        val landscapeFrame = dstSize.width > dstSize.height * 1.3f
        val portraitSource = srcSize.width < srcSize.height * 1.15f
        val portraitFrame = dstSize.height > dstSize.width * 1.3f
        val landscapeSource = srcSize.width > srcSize.height * 1.3f
        return (if ((landscapeFrame && portraitSource) || (portraitFrame && landscapeSource)) ContentScale.Fit else ContentScale.Crop)
            .computeScaleFactor(srcSize, dstSize)
    }
}
