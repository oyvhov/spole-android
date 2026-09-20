package app.reelstack.ui.components

import android.provider.Settings
import app.reelstack.data.repository.DeviceIdentity
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.allowHardware
import coil3.request.transformations
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
    alignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.Center,
    trimTransparent: Boolean = false,
) {
    val context = LocalContext.current
    val fallback = if (fallbackRes != 0) painterResource(fallbackRes) else null
    // One binder call per frame per poster is what this used to be. The theme reads it once.
    val fadeDuration = if (app.reelstack.ui.theme.LocalMotionEnabled.current) crossfadeDurationMillis else 0
    val model = remember(url, fallbackRes, source, fadeDuration, trimTransparent) {
        runCatching {
            val builder = ImageRequest.Builder(context)
                .data(url ?: fallbackRes.takeIf { it != 0 })
                .crossfade(fadeDuration)
            if (url != null) MediaAuthHeaders.forUrl(context, source, url)?.let(builder::httpHeaders)
            if (trimTransparent) builder.allowHardware(false).transformations(ClearLogoTransformation)
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
        alignment = alignment,
    )
}

/**
 * Whether a picture and the frame it was given face different ways.
 *
 * Kept as one small function because two places need exactly the same answer: [SafeArtworkCrop],
 * which refuses to crop across an orientation change, and [RailArtwork], which has to know when to
 * put something behind the picture to fill the rest of the frame. The thresholds are deliberately
 * not 1.0 — a 4:3 still and a 5:7 poster are both close enough to square that neither is worth
 * treating as a mismatch.
 */
internal fun orientationDiffers(sourceRatio: Float, frameRatio: Float): Boolean {
    if (sourceRatio <= 0f || frameRatio <= 0f) return false
    val landscapeFrame = frameRatio > 1.3f
    val portraitFrame = frameRatio < 1f / 1.3f
    val portraitSource = sourceRatio < 1.15f
    val landscapeSource = sourceRatio > 1.3f
    return (landscapeFrame && portraitSource) || (portraitFrame && landscapeSource)
}

/**
 * One frame, whatever artwork the server happens to answer with.
 *
 * A rail decides its shape once and every card in it gets that shape, so the row has one width and
 * one baseline. The picture is never cropped across an orientation change — a poster dropped into a
 * wide frame would lose the top and bottom of itself — so when the two disagree the picture is
 * fitted and a blurred copy of itself fills the rest of the frame. That is the same treatment the
 * detail page gives a picture that does not match its column.
 *
 * The blurred layer is only composed when the mismatch is real, which the image reports once it is
 * decoded. Below Android 12 `blur` does nothing and the leftover is a quiet letterbox — still one
 * width per row, which was the point.
 */
@Composable
fun RailArtwork(
    url: String?,
    contentDescription: String?,
    frameRatio: Float,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackRes: Int = 0,
    source: ServiceKind? = null,
) {
    val mismatched = remember(url, frameRatio) { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(modifier) {
        if (mismatched.value) MediaArtwork(
            url = url,
            contentDescription = null,
            modifier = Modifier.matchParentSize()
                .blur(24.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                .graphicsLayer { alpha = 0.45f },
            fallbackRes = fallbackRes,
            contentScale = ContentScale.Crop,
            source = source,
            protectAspectRatio = false,
            crossfadeDurationMillis = 0,
        )
        MediaArtwork(
            url = url,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            fallbackRes = fallbackRes,
            contentScale = if (mismatched.value) ContentScale.Fit else ContentScale.Crop,
            source = source,
            protectAspectRatio = true,
            onAspectRatio = { ratio -> mismatched.value = orientationDiffers(ratio, frameRatio) },
        )
    }
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
