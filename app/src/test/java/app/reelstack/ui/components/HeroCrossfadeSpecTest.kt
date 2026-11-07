package app.reelstack.ui.components

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * When the hero turns to the next title, the outgoing picture stays fully on screen until the
 * incoming one has faded in over it. On the Shield it vanished in the first frame, and the hero
 * stood empty and dark for half a second (2026-11-07).
 */
class HeroCrossfadeSpecTest {
    private fun opacityAt(spec: androidx.compose.animation.core.AnimationSpec<Float>, from: Float, to: Float, ms: Long): Float {
        val vectorized = spec.vectorize(Float.VectorConverter)
        val value = vectorized.getValueFromNanos(ms * 1_000_000, AnimationVector1D(from), AnimationVector1D(to), AnimationVector1D(0f))
        return value.value
    }

    @Test fun theOutgoingPictureStaysWholeUntilTheIncomingHasFadedIn() {
        val spec = heroArtworkOpacitySpec(motion = true, incoming = false, durationMillis = 1100)
        assertEquals(1f, opacityAt(spec, 1f, 0f, 10), 0f)
        assertEquals(1f, opacityAt(spec, 1f, 0f, 550), 0f)
        assertEquals(1f, opacityAt(spec, 1f, 0f, 1090), 0f)
        assertEquals(0f, opacityAt(spec, 1f, 0f, 1110), 0f)
    }

    @Test fun theIncomingPictureFadesInOverTheWholeDuration() {
        val spec = heroArtworkOpacitySpec(motion = true, incoming = true, durationMillis = 1100)
        val half = opacityAt(spec, 0f, 1f, 550)
        assertEquals(true, half > .2f && half < .9f)
        assertEquals(1f, opacityAt(spec, 0f, 1f, 1100), 0f)
    }

    @Test fun withoutMotionThePictureSimplyChanges() {
        assertEquals(0f, opacityAt(heroArtworkOpacitySpec(motion = false, incoming = false, durationMillis = 1100), 1f, 0f, 0), 0f)
        assertEquals(1f, opacityAt(heroArtworkOpacitySpec(motion = false, incoming = true, durationMillis = 1100), 0f, 1f, 0), 0f)
    }
}
