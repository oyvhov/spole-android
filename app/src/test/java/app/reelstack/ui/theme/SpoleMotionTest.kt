package app.reelstack.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The few curves the app moves with, in one place. Everything that opens, changes or lands takes
 * its length from here; with motion reduced every curve is the short linear fade, never a cut.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class SpoleMotionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun theLanguageHasFiveLengths() {
        assertEquals(140, SpoleMotion.FocusMs)
        assertEquals(280, SpoleMotion.ChangeMs)
        assertEquals(320, SpoleMotion.SheetEnterMs)
        assertEquals(200, SpoleMotion.SheetExitMs)
        assertEquals(450, SpoleMotion.LandMs)
        assertEquals(100, SpoleMotion.ReducedMs)
    }

    @Test fun withMotionTheTweenKeepsItsLengthAndCurve() {
        var spec: FiniteAnimationSpec<Float>? = null
        rule.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides true) { spec = motionTween(SpoleMotion.ChangeMs, delayMs = 80) }
        }
        val tween = spec as TweenSpec<Float>
        assertEquals(280, tween.durationMillis)
        assertEquals(80, tween.delay)
        assertEquals(FastOutSlowInEasing, tween.easing)
    }

    @Test fun withMotionReducedEveryTweenIsTheShortLinearFade() {
        var spec: FiniteAnimationSpec<Float>? = null
        rule.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides false) { spec = motionTween(SpoleMotion.LandMs, delayMs = 80) }
        }
        val tween = spec as TweenSpec<Float>
        assertEquals(100, tween.durationMillis)
        assertEquals(0, tween.delay)
        assertEquals(LinearEasing, tween.easing)
    }

    @Test fun landingIsASpringWithoutOvershootOrTheFadeWhenReduced() {
        var spring: FiniteAnimationSpec<Float>? = null
        var reduced: FiniteAnimationSpec<Float>? = null
        rule.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides true) { spring = landSpring() }
            CompositionLocalProvider(LocalMotionEnabled provides false) { reduced = landSpring() }
        }
        val land = spring as SpringSpec<Float>
        assertEquals(0.86f, land.dampingRatio, 0.001f)
        assertEquals(SpoleMotion.LandStiffness, land.stiffness, 0.001f)
        assertTrue(reduced is TweenSpec<Float>)
        assertEquals(100, (reduced as TweenSpec<Float>).durationMillis)
    }

    @Test fun theSameSpecsExistOutsideComposition() {
        assertEquals(140, (motionSpec<Float>(motion = true, SpoleMotion.FocusMs) as TweenSpec<Float>).durationMillis)
        assertEquals(100, (motionSpec<Float>(motion = false, SpoleMotion.FocusMs) as TweenSpec<Float>).durationMillis)
    }
}
