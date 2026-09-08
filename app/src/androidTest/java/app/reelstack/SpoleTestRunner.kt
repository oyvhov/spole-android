package app.reelstack

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.runner.AndroidJUnitRunner
import java.util.Locale

/** Fixture language only. This runner is never used on the user's review installation. */
class SpoleTestRunner : AndroidJUnitRunner() {
    override fun onStart() {
        targetContext.getSharedPreferences("spole_language", Context.MODE_PRIVATE)
            .edit().putString("language", "nn").commit()
        // Standalone Compose hosts are framework test activities rather than LocalizedActivity.
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            targetContext.getSystemService(android.app.LocaleManager::class.java)
                .applicationLocales = LocaleList(Locale.forLanguageTag("nn"))
        }
        super.onStart()
    }
}
