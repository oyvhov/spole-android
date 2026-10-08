package app.reelstack.ui.components

import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KonamiCodeTest {
    private val code = listOf(KEYCODE_DPAD_UP, KEYCODE_DPAD_UP, KEYCODE_DPAD_DOWN, KEYCODE_DPAD_DOWN,
        KEYCODE_DPAD_LEFT, KEYCODE_DPAD_RIGHT, KEYCODE_DPAD_LEFT, KEYCODE_DPAD_RIGHT)

    private fun KonamiCode.press(keys: List<Int>) = keys.map(::feed)

    @Test fun theSequenceSpinsTheReelOnItsLastPress() {
        val presses = KonamiCode().press(code)
        assertEquals(listOf(false, false, false, false, false, false, false, true), presses)
    }

    /** Almost everyone presses ↑ one time too many at the start. */
    @Test fun anExtraUpBeforeItStillCounts() {
        assertEquals(1, KonamiCode().press(listOf(KEYCODE_DPAD_UP) + code).count { it })
    }

    @Test fun ordinaryMenuMovesAndABrokenSequenceDoNothing() {
        val konami = KonamiCode()
        assertFalse(konami.press(listOf(KEYCODE_DPAD_DOWN, KEYCODE_DPAD_DOWN, KEYCODE_DPAD_RIGHT, KEYCODE_DPAD_CENTER)).any { it })
        assertFalse(konami.press(code.take(6) + KEYCODE_DPAD_CENTER + code.drop(6)).any { it })
    }

    @Test fun itCanBeEnteredAgainRightAway() {
        assertEquals(2, KonamiCode().press(code + code).count { it })
    }
}
