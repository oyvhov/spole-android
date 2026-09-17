package app.reelstack.localization

import android.content.Context
import androidx.annotation.StringRes

/**
 * A sentence the data layer decided on, in a form it can hand upwards without knowing the language.
 *
 * The network layer has to choose *which* sentence — only it knows that the server answered 429
 * with a `Retry-After` of thirty seconds — but it has no business choosing the words. It used to
 * choose both, in nynorsk, which meant an English installation met nynorsk the moment anything went
 * wrong: exactly when wording matters most.
 *
 * So the decision travels as a resource id and its arguments, and whoever holds a `Context` turns
 * it into words. That also makes the decision testable without a device: a unit test can assert
 * *which* message and *which* address, instead of matching a string that now exists in three
 * languages.
 */
data class LocalizedText(
    /**
     * A string resource, or a plurals resource when [quantity] is set.
     *
     * Kotlin has no union type for "one of these two resource kinds", so the field is `@AnyRes` and
     * the two constructors below carry the precise annotation for their own path — which is where a
     * mistake would actually be made.
     */
    @androidx.annotation.AnyRes val resId: Int,
    val args: List<Any> = emptyList(),
    /** When set, [resId] names a plurals resource and this is the quantity to choose a form by. */
    val quantity: Int? = null,
    /**
     * Text that came from a server and is not ours to translate.
     *
     * A media line is a mix: "Film" is a word Spole chose and owes the reader in their language,
     * while "12", "★ 8.3", "Netflix" and an episode title came from Jellyfin or TMDB and mean the
     * same in every language. Carrying both kinds in one type is what lets a list of facts travel
     * as decisions rather than as a pre-written sentence.
     */
    val literal: String? = null,
) {
    constructor(@StringRes resId: Int, vararg args: Any) : this(resId, args.toList())

    // `quantity` decides which of the two kinds `resId` is; lint cannot see that from here.
    @Suppress("ResourceType")
    fun text(context: Context): String {
        literal?.let { return it }
        val localized = AppLanguages.wrap(context)
        return if (quantity == null) localized.getString(resId, *args.toTypedArray())
        else localized.resources.getQuantityString(resId, quantity, *args.toTypedArray())
    }

    companion object {
        /** Server text, passed through untouched. */
        fun raw(value: String) = LocalizedText(resId = 0, literal = value)

        /** A count with a sentence whose wording depends on it. */
        @Suppress("ResourceType")
        fun plural(@androidx.annotation.PluralsRes resId: Int, quantity: Int) =
            LocalizedText(resId = resId, args = listOf<Any>(quantity), quantity = quantity)
    }
}
