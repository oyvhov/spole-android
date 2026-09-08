package app.reelstack.diagnostics

import android.content.Context
import android.os.Build
import app.reelstack.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Records an unhandled crash to a file inside the app's own storage, and nowhere else.
 *
 * Spole has never logged anything, which is right for a client that holds someone's media
 * history — but it also meant a crash left no trace at all, and a release built with R8 produces
 * a stack trace nobody can read without the mapping file for that exact version. This keeps the
 * one piece of evidence that matters, on the device, until the user chooses to share it.
 *
 * Nothing is uploaded. There is no consent dialog because there is nothing to consent to: no
 * network call is made, and the file is readable only by this app.
 */
object CrashReporter {
    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(appContext, thread, throwable) }
            // Always hand the crash on. Swallowing it would leave the process alive in an unknown
            // state instead of letting Android tear it down and show its own dialog.
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** The recorded crashes, newest last, or null when nothing has been recorded. */
    fun report(context: Context): String? =
        logFile(context).takeIf { it.exists() && it.length() > 0 }?.readText()?.ifBlank { null }

    fun clear(context: Context) {
        runCatching { logFile(context).delete() }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val trace = StringWriter().also { PrintWriter(it).use(throwable::printStackTrace) }.toString()
        val entry = buildString {
            appendLine("--- ${Instant.now()}")
            appendLine("Spole ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Tråd: ${thread.name}")
            appendLine(scrub(trace))
        }

        val file = logFile(context)
        // Keep the file small and keep the newest crash. A log that grows without bound is a bug
        // of its own, and the crash a user is about to report is the last one, not the first.
        val existing = runCatching { file.readText() }.getOrDefault("")
        val combined = (existing + entry).let {
            if (it.length <= MAX_CHARS) it else it.takeLast(MAX_CHARS)
        }
        runCatching { file.writeText(combined) }
    }

    /**
     * Removes the two things a stack trace can carry that the user did not agree to share: the
     * address of their server, and anything that looks like a credential. Class and method names
     * are what makes the report useful and are not secrets.
     */
    internal fun scrub(text: String): String = text
        .replace(URL_PATTERN, "<adresse fjerna>")
        .replace(SECRET_PATTERN) { match -> "${match.groupValues[1]}=<fjerna>" }

    private fun logFile(context: Context) = File(context.applicationContext.filesDir, FILE_NAME)

    private const val FILE_NAME = "spole-crash.log"
    private const val MAX_CHARS = 64 * 1024
    private val URL_PATTERN = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
    private val SECRET_PATTERN =
        Regex("""\b(token|api[_-]?key|password|pw|authorization|cookie)\b\s*[=:]\s*\S+""", RegexOption.IGNORE_CASE)
}
