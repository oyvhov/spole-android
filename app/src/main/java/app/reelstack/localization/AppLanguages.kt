package app.reelstack.localization

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/**
 * App-owned locale selection works on every supported Android version without changing the
 * phone's locale or a server's metadata. Native system picker integration follows once the
 * entire English catalogue is ready; this first pass deliberately labels English as preview.
 */
object AppLanguages {
    private const val STORE = "spole_language"
    private const val KEY = "language"

    fun selected(context: Context): AppLanguage {
        val store = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val saved = store.getString(KEY, null)
        val legacyConnections = context.getSharedPreferences("reelstack_connections", Context.MODE_PRIVATE)
        val existing = context.getSharedPreferences("reelstack_preferences", Context.MODE_PRIVATE)
            .all.isNotEmpty() || app.reelstack.data.model.ServiceKind.entries.any {
                val prefix = it.name.lowercase(Locale.ROOT)
                legacyConnections.contains("$prefix.url") || legacyConnections.contains("$prefix.last_url")
            }
        val choice = AppLanguage.initial(saved, existing)
        if (saved == null) store.edit { putString(KEY, choice.tag) }
        return choice
    }

    fun wrap(context: Context, language: AppLanguage = selected(context)): Context {
        // Use the system configuration, not this Activity's already overridden locale.
        val locales = if (language == AppLanguage.SYSTEM) android.content.res.Resources.getSystem().configuration.locales
            else LocaleList(Locale.forLanguageTag(language.tag))
        val configuration = Configuration(context.resources.configuration).apply { setLocales(locales) }
        return context.createConfigurationContext(configuration)
    }

    fun select(context: Context, language: AppLanguage) {
        if (selected(context) == language) return
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit { putString(KEY, language.tag) }
        activity(context)?.recreate()
    }

    private fun activity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> activity(context.baseContext)
        else -> null
    }
}
