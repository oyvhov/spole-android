package app.reelstack.data.network

import kotlinx.serialization.json.*

data class RequestHistoryPage(
    val items: List<RemoteRequest>,
    val nextOffset: Int,
    val hasMore: Boolean,
    val total: Int?,
)

/** Advance by server records, including malformed/foreign rows, never by visible cards. */
internal fun parseRequestHistoryPage(payload: String, userId: String, offset: Int, limit: Int): RequestHistoryPage {
    val root = Json.parseToJsonElement(payload).jsonObject
    val rows = root["results"] as? JsonArray ?: serviceError("Fekk ikkje lese førespurnadshistorikken.")
    val total = (root["pageInfo"] as? JsonObject)?.get("results")?.jsonPrimitive?.intOrNull?.takeIf { it >= 0 }
    val next = Math.addExact(offset, rows.size)
    return RequestHistoryPage(
        ServicePayloadParser.requests(payload).filter { it.ownerId == userId }.distinctBy { it.id },
        next, rows.isNotEmpty() && (total?.let { next < it } ?: (rows.size >= limit)), total,
    )
}
