package app.reelstack.data.network

/**
 * A failure whose message was written to be read by the person using the app.
 *
 * Everything in this package that ends in a user-facing sentence — an unreachable server, a
 * rejected API key, a redirect that needs a different address — throws this type. Anything else
 * that escapes the network layer is a defect in Spole, not a situation the user can act on, and
 * the UI must not present its text as advice: `IndexOutOfBoundsException: Index 3 out of bounds`
 * is not an instruction, and showing it in the same field as "Sjekk tenaradressa og nettet" makes
 * the two indistinguishable.
 *
 * `EndpointValidator` is deliberately not part of this: it validates a string the user just typed
 * and throws `IllegalArgumentException` with its own readable text. That call is a single pure
 * function, so the caller there already knows every message it can produce.
 */
class ServiceMessage(message: String) : Exception(message)

/** Fails with a sentence meant for the user. The network-layer counterpart to `error(…)`. */
internal fun serviceError(message: String): Nothing = throw ServiceMessage(message)

/**
 * The message to show for [this], or null when the failure is a defect rather than something the
 * user can do anything about.
 */
fun Throwable.readableMessage(): String? = (this as? ServiceMessage)?.message
