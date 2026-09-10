package app.reelstack.data.model

data class RequestQuota(val limit: Int?, val remaining: Int?, val days: Int?, val restricted: Boolean = false) {
    val unlimited: Boolean get() = limit == 0
    fun exceededBy(units: Int): Boolean = units > 0 && !unlimited && (restricted || remaining?.let { units > it } == true)
}

data class RequestRules(val canRequest: Boolean, val approvalRequired: Boolean, val quota: RequestQuota?)

/** These are Seerr's standard (non-4K) request permissions, not a locally chosen role. */
fun ServiceAccount.automaticallyApproves(type: String): Boolean = isPersonal &&
    (isAdmin || permissions and (2L or 16L or 128L or (if (type == "tv") 512L else 256L)) != 0L)

val RequestDraft.quotaExceeded: Boolean
    get() = rules?.quota?.exceededBy(if (media.mediaType == "tv") selected.size else 1) == true
