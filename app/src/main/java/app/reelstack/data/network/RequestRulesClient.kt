package app.reelstack.data.network

import app.reelstack.data.model.*
import kotlinx.serialization.json.*

class RequestRulesClient(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val profiles: AccountProfileClient = AccountProfileClient(transport = transport),
) {
    fun load(connection: ServiceConnection, expectedUserId: String, mediaType: String): RequestRules {
        require(connection.kind == ServiceKind.SEERR && connection.sessionCookie && mediaType in setOf("movie", "tv"))
        val actor = profiles.load(connection)
        check(actor.isPersonal && actor.id == expectedUserId && actor.id.toIntOrNull()?.let { it > 0 } == true)
        val quota = try {
            val response = transport.get(EndpointValidator.resolve(connection.baseUrl, "api/v1/user/${actor.id}/quota"),
                seerrCookieHeaders(connection.token) + ("Accept" to "application/json"))
            if (response.statusCode in 200..299) parseRequestQuota(response.body, mediaType) else null
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (_: Exception) { null }
        return RequestRules(actor.canRequestType(mediaType), !actor.automaticallyApproves(mediaType), quota)
    }
}

internal fun parseRequestQuota(payload: String, mediaType: String): RequestQuota? {
    val root = Json.parseToJsonElement(payload) as? JsonObject ?: return null
    val quota = root[mediaType] as? JsonObject ?: return null
    fun nonnegative(key: String) = quota[key]?.jsonPrimitive?.intOrNull?.takeIf { it >= 0 }
    val limit = nonnegative("limit")
    val remaining = nonnegative("remaining")
    val restricted = quota["restricted"]?.jsonPrimitive?.booleanOrNull
    if (limit == null && remaining == null && restricted == null) return null
    return RequestQuota(limit, remaining, nonnegative("days"), restricted == true)
}
