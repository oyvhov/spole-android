package app.reelstack.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The account switch from the profile menu: the picked picture flies to the corner circle, the
 * switch itself happens early in the flight, and the home rows are held back until the picture
 * has landed. Pure timing, with a test clock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSwitchMotionTest {
    private val orb = Rect(100f, 400f, 156f, 456f)
    private val circle = Rect(1700f, 60f, 1740f, 100f)
    private val picture = ProfileSwitchMotion.Picture(avatarUrl = null, letter = "E")

    private fun TestScope.motion() = ProfileSwitchMotion(backgroundScope, clock = { testScheduler.currentTime })

    @Test fun theSwitchHappensEarlyInTheFlightAndTheRowsWaitForTheLanding() = runTest {
        val motion = motion()
        motion.reportTarget(circle, rootWidthPx = 1920f)
        motion.pickedFrom(orb, picture)
        var switchedAt = -1L
        motion.switch(motionWanted = true) { switchedAt = testScheduler.currentTime }

        assertNotNull(motion.flight)
        assertEquals(orb, motion.flight!!.from)
        assertEquals(circle, motion.flight!!.to)
        assertTrue(motion.holding)
        assertEquals(-1L, switchedAt)

        advanceTimeBy(ProfileSwitchMotion.SwitchAtMs + 1)
        assertEquals(ProfileSwitchMotion.SwitchAtMs, switchedAt)
        assertNotNull("the picture is still on its way", motion.flight)
        assertTrue(motion.holding)

        advanceTimeBy(ProfileSwitchMotion.LandAtMs - ProfileSwitchMotion.SwitchAtMs)
        assertNull("landed: the circle shows its own picture", motion.flight)
        assertEquals(ProfileSwitchMotion.LandAtMs, motion.landedAt)
        assertTrue("the rows wait a moment after the landing", motion.holding)

        advanceTimeBy(ProfileSwitchMotion.RowsAfterLandMs + 1)
        assertFalse(motion.holding)
    }

    @Test fun withoutAKnownCircleOrWithMotionReducedTheSwitchIsImmediateAndNothingFlies() = runTest {
        val noTarget = motion()
        noTarget.pickedFrom(orb, picture)
        var switched = false
        noTarget.switch(motionWanted = true) { switched = true }
        assertTrue(switched)
        assertNull(noTarget.flight)
        assertFalse(noTarget.holding)

        val reduced = motion()
        reduced.reportTarget(circle, rootWidthPx = 1920f)
        reduced.pickedFrom(orb, picture)
        switched = false
        reduced.switch(motionWanted = false) { switched = true }
        assertTrue(switched)
        assertNull(reduced.flight)
    }

    @Test fun aSecondPressDuringTheFlightIsIgnored() = runTest {
        val motion = motion()
        motion.reportTarget(circle, rootWidthPx = 1920f)
        motion.pickedFrom(orb, picture)
        var switches = 0
        motion.switch(motionWanted = true) { switches++ }
        motion.pickedFrom(orb, picture)
        motion.switch(motionWanted = true) { switches++ }
        advanceTimeBy(2_000)
        assertEquals(1, switches)
    }

    @Test fun theHoldNeverOutlivesItsLimitEvenIfTheSwitchFails() = runTest {
        // The failure still reaches the owner's scope, as a synchronous switch would; the page is cleaned up first.
        val quiet = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler) + CoroutineExceptionHandler { _, _ -> })
        val motion = ProfileSwitchMotion(quiet, clock = { testScheduler.currentTime })
        motion.reportTarget(circle, rootWidthPx = 1920f)
        motion.pickedFrom(orb, picture)
        motion.switch(motionWanted = true) { throw IllegalStateException("the switch failed") }
        advanceTimeBy(ProfileSwitchMotion.HoldLimitMs + 1)
        assertFalse(motion.holding)
        assertNull(motion.flight)
        assertNotNull(motion.landedAt)
    }

    @Test fun onlyACircleAtTheTopRightCountsAsTheTarget() = runTest {
        val motion = motion()
        motion.reportTarget(Rect(600f, 60f, 640f, 100f), rootWidthPx = 1920f)
        assertNull("a circle mid-screen is a page that is sliding, not the header", motion.target)
        motion.reportTarget(Rect(0f, 0f, 0f, 0f), rootWidthPx = 1920f)
        assertNull(motion.target)
        motion.reportTarget(circle, rootWidthPx = 1920f)
        assertEquals(circle, motion.target)
    }

    @Test fun thePickedOrbIsTranslatedFromScreenToRootCoordinates() = runTest {
        val motion = motion()
        motion.reportTarget(circle, rootWidthPx = 1920f)
        motion.rootOnScreen = Offset(0f, 48f)
        motion.pickedFrom(orb.translate(0f, 48f), picture, onScreen = true)
        motion.switch(motionWanted = true) {}
        assertEquals(orb, motion.flight!!.from)
    }
}
