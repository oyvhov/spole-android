package app.reelstack.cast

import android.content.Context
import app.reelstack.R
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

object CastConfiguration {
    fun receiverApplicationId(context: Context): String = context.getString(R.string.cast_receiver_application_id).trim()
    fun isEnabled(context: Context): Boolean = receiverApplicationId(context).isNotBlank()
}

/** Android must discover this from the manifest before it creates the shared Cast context. */
class SpoleCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions = CastOptions.Builder()
        // An empty team configuration must not make app startup crash. The route UI stays hidden
        // until a real Custom Receiver id is supplied, so the default receiver is never exposed.
        .setReceiverApplicationId(CastConfiguration.receiverApplicationId(context)
            .ifBlank { CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID })
        .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider> = emptyList()
}
