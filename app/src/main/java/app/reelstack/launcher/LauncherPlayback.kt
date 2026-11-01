package app.reelstack.launcher

import android.content.Context
import android.content.Intent

/**
 * Tells the TV's launcher (Stova) whether a title is playing, paused or stopped, so its cinema mode
 * can dim the lights while the film plays and put them back when it pauses or stops. Only the state
 * crosses; nothing about the title or the account.
 */
object LauncherPlayback {
    const val ACTION = "app.reelstack.action.PLAYBACK_STATE"

    enum class State(val word: String) { PLAYING("playing"), PAUSED("paused"), STOPPED("stopped") }

    @Volatile private var last: State? = null

    /** One explicit broadcast per launcher package, and only when the state changes. */
    fun send(context: Context, state: State) {
        if (!changed(state)) return
        for (launcher in LauncherApproval.LAUNCHERS) {
            runCatching { context.sendBroadcast(Intent(ACTION).setPackage(launcher).putExtra("state", state.word)) }
        }
    }

    @Synchronized internal fun changed(state: State): Boolean {
        if (state == last) return false
        last = state
        return true
    }

    /** Tests start from nothing sent. */
    @Synchronized internal fun reset() { last = null }
}
