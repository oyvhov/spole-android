package app.reelstack.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.reelstack.AppContainer
import app.reelstack.MainActivity
import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.ViewerAccess
import app.reelstack.data.network.RemotePlayback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * "Spelar no" on the home screen.
 *
 * Active playback is deliberately never written to the dashboard cache, so this cannot read a
 * stored copy — it asks the media servers directly. That is also the honest behaviour for a widget
 * whose whole claim is what is playing *now*: a cached answer would be wrong the moment someone
 * stops watching.
 */
class NowPlayingWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val localized = app.reelstack.localization.AppLanguages.wrap(context)
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, loadingViews(localized)) }
        refresh(context, manager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            refresh(context, manager, manager.getAppWidgetIds(ComponentName(context, NowPlayingWidget::class.java)))
        }
    }

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        val app = app.reelstack.localization.AppLanguages.wrap(context.applicationContext)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val views = runCatching {
                // A widget update must not hang: an unreachable server has to degrade to a line
                // of text, not a spinner that never resolves.
                withTimeoutOrNull(SESSION_TIMEOUT_MS) { sessionViews(app) } ?: errorViews(app)
            }.getOrElse { errorViews(app) }
            runCatching { ids.forEach { manager.updateAppWidget(it, views) } }
            pending.finish()
        }
    }

    private suspend fun sessionViews(context: Context): RemoteViews {
        val container = AppContainer(context)
        val servers = container.connectionRepository.list().filter {
            (it.kind == ServiceKind.JELLYFIN || it.kind == ServiceKind.EMBY) &&
                it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        if (servers.isEmpty()) return views(context, context.getString(R.string.widget_no_services), context.getString(R.string.widget_connect))

        val sessions = kotlinx.coroutines.coroutineScope {
            servers.map { connection ->
                async {
                    runCatching {
                        container.widgetMediaServerClient.sessions(connection, viewerAccess(container, connection))
                    }.getOrNull().orEmpty()
                }
            }.awaitAll().flatten()
        }
        return playbackViews(context, sessions)
    }


    private fun viewerAccess(container: AppContainer, connection: ServiceConnection): ViewerAccess {
        val account = runCatching { container.widgetAccountProfileClient.load(connection) }.getOrNull()
        return ViewerAccess(false, account?.let { mapOf(connection.kind to it) }.orEmpty())
    }

    private fun playbackViews(context: Context, playing: List<RemotePlayback>): RemoteViews {
        if (playing.isEmpty()) return views(context, context.getString(R.string.widget_empty), context.getString(R.string.widget_quiet))
        val first = playing.first()
        val heading = if (playing.size > 1) context.resources.getQuantityString(R.plurals.home_playback_count, playing.size, playing.size) else null
        val detail = listOfNotNull(
            first.subtitle.takeIf(String::isNotBlank),
            first.userName.takeIf(String::isNotBlank),
            first.timeLeft.takeIf(String::isNotBlank),
        ).joinToString(" · ")
        return views(context, first.title, detail, heading)
    }

    private fun loadingViews(context: Context) =
        views(context, context.getString(R.string.widget_loading), "")

    private fun errorViews(context: Context) =
        views(context, context.getString(R.string.widget_error), context.getString(R.string.widget_check))

    private fun views(context: Context, primary: String, secondary: String, heading: String? = null): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_now_playing).apply {
            setTextViewText(R.id.widget_primary, primary)
            setTextViewText(R.id.widget_secondary, secondary)
            setTextViewText(R.id.widget_heading, heading ?: context.getString(R.string.widget_title))
            setContentDescription(R.id.widget_root, context.getString(R.string.widget_accessibility, primary, secondary))
            // Tapping opens the app; tapping again after it is open just refreshes the widget.
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_REFRESH = "app.reelstack.widget.REFRESH"

        /**
         * A receiver holding its result open with `goAsync()` has to be done well inside the
         * broadcast window — around ten seconds when the broadcast arrives at foreground
         * priority. The old 12 s budget was on the wrong side of that: the system could finish
         * the result for us, leaving the widget stuck on "Hentar…" instead of saying it could
         * not reach the server. Eight seconds leaves room to actually draw the answer.
         */
        private const val SESSION_TIMEOUT_MS = 8_000L

        /** Lets the background refresh nudge the widget without knowing how it renders. */
        fun requestUpdate(context: Context) {
            runCatching {
                context.sendBroadcast(
                    Intent(context, NowPlayingWidget::class.java).setAction(ACTION_REFRESH),
                )
            }
        }
    }
}
