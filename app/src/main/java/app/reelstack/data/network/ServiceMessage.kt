package app.reelstack.data.network

import android.content.Context
import androidx.annotation.StringRes
import app.reelstack.localization.LocalizedText

/**
 * A failure that already knows what to say, in whatever language the reader chose.
 *
 * Everything in this package that ends in a user-facing sentence — an unreachable server, a
 * rejected API key, a redirect that needs a different address — throws one of these. Anything else
 * that escapes the network layer is a defect in Spole, not a situation the user can act on, and
 * the UI must not present its text as advice: `IndexOutOfBoundsException: Index 3 out of bounds`
 * is not an instruction, and showing it in the same field as "Sjekk tenaradressa og nettet" makes
 * the two indistinguishable.
 *
 * The sentence travels as a [LocalizedText], not a string: see there for why the data layer must
 * not pick the words.
 */
interface LocalizedFailure {
    val localized: LocalizedText
}

/** A service could not do what was asked, and the person using Spole can act on why. */
class ServiceMessage(override val localized: LocalizedText) :
    Exception("ServiceMessage(${localized.resId})"), LocalizedFailure

/** A server address the user typed cannot be used, and the reason names what to change. */
class InvalidEndpoint(override val localized: LocalizedText) :
    IllegalArgumentException("InvalidEndpoint(${localized.resId})"), LocalizedFailure

/** Fails with a sentence meant for the user. The network-layer counterpart to `error(…)`. */
internal fun serviceError(@StringRes resId: Int, vararg args: Any): Nothing =
    throw ServiceMessage(LocalizedText(resId, args.toList()))

/** The same, when the sentence was already chosen elsewhere. */
internal fun serviceError(text: LocalizedText): Nothing = throw ServiceMessage(text)

/** Rejects a typed address, naming what is wrong with it. */
internal fun invalidEndpoint(@StringRes resId: Int, vararg args: Any): Nothing =
    throw InvalidEndpoint(LocalizedText(resId, args.toList()))

/**
 * The message to show for [this], or null when the failure is a defect rather than something the
 * user can do anything about.
 */
fun Throwable.readableMessage(context: Context): String? =
    (this as? LocalizedFailure)?.localized?.text(context)

/** The decision behind [this], for tests and for callers that render it themselves. */
fun Throwable.localizedFailure(): LocalizedText? = (this as? LocalizedFailure)?.localized
